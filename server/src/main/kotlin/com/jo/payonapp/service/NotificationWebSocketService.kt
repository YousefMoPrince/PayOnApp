package com.jo.payonapp.service

import com.jo.payonapp.api.TokenManager
import com.jo.payonapp.dto.NotificationResponse
import com.jo.payonapp.network.NetworkConstants
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout

class NotificationWebSocketService(
    private val client: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val tokenManager: TokenManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectionJob: Job? = null

    private val _notificationsFlow = MutableSharedFlow<NotificationResponse>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val notificationsFlow: SharedFlow<NotificationResponse> = _notificationsFlow.asSharedFlow()

    private val _connectionStateFlow = MutableSharedFlow<Boolean>(replay = 1)
    val connectionStateFlow: SharedFlow<Boolean> = _connectionStateFlow.asSharedFlow()

    fun connect() {
        if (connectionJob?.isActive == true) return

        connectionJob = scope.launch {
            var initialDelay = 2000L
            val maxDelay = 30000L

            while (isActive) {
                var pingJob: Job? = null
                try {
                    val currentToken = tokenManager.getAccessToken() ?: ""
                    if (currentToken.isBlank()) {
                        _connectionStateFlow.emit(false)
                        delay(5000)
                        continue
                    }

                    client.webSocket(urlString = NetworkConstants.unSafe_WS_Url, request = {
                        timeout {
                            requestTimeoutMillis = null
                            socketTimeoutMillis = null
                        }
                    }) {
                        val connectFrame = "CONNECT\n" +
                                "accept-version:1.1,1.2\n" +
                                "heart-beat:10000,10000\n" +
                                "Authorization:Bearer $currentToken\n\n" +
                                "\u0000"

                        send(Frame.Text(connectFrame))

                        pingJob = launch {
                            while (isActive) {
                                delay(10000)
                                runCatching { send(Frame.Text("\n")) }
                            }
                        }

                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val rawText = frame.readText()

                                val stompFrames = rawText.split("\u0000")

                                for (stompFrame in stompFrames) {
                                    val trimmedFrame = stompFrame.trim()
                                    if (trimmedFrame.isBlank()) continue

                                    when {
                                        trimmedFrame.startsWith("CONNECTED") -> {
                                            _connectionStateFlow.emit(true)
                                            initialDelay = 2000L

                                            val subscribeFrame = "SUBSCRIBE\n" +
                                                    "id:sub-0\n" +
                                                    "destination:/user/queue/notifications\n\n" +
                                                    "\u0000"
                                            send(Frame.Text(subscribeFrame))
                                        }

                                        trimmedFrame.startsWith("MESSAGE") -> {
                                            val jsonPayload = extractStompBody(trimmedFrame)
                                            if (jsonPayload.isNotBlank()) {
                                                runCatching {
                                                    val notification = json.decodeFromString<NotificationResponse>(jsonPayload)
                                                    _notificationsFlow.emit(notification)
                                                }
                                            }
                                        }

                                        trimmedFrame.startsWith("ERROR") -> {
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        throw e
                    }
                } finally {
                    pingJob?.cancel()
                    _connectionStateFlow.emit(false)
                }

                if (isActive) {
                    delay(initialDelay)
                    initialDelay = (initialDelay * 2).coerceAtMost(maxDelay)
                }
            }
        }
    }

    private fun extractStompBody(rawFrame: String): String {
        val parts = rawFrame.split("\n\n", limit = 2)
        if (parts.size < 2) return ""
        return parts[1].replace("\u0000", "").trim()
    }


    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        _connectionStateFlow.tryEmit(false)
    }
}