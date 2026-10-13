package com.jo.payonapp.api

import com.jo.payonapp.network.NetworkConstants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

class HttpClientFactory(
    private val engine: HttpClientEngine
) {
    fun createAuthHttpClient(tokenManager: TokenManager): HttpClient {
        return HttpClient(engine) {
            install(WebSockets) {
                pingInterval = 15.seconds
            }

            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = null
            }

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }

            defaultRequest {
                url(NetworkConstants.Base_Url)
            }
            install(DefaultRequest) {
                header("ngrok-skip-browser-warning", "true")
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        val accessToken = tokenManager.getAccessToken()
                        val refreshToken = tokenManager.getRefreshToken()

                        if (!accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
                            BearerTokens(accessToken = accessToken, refreshToken = refreshToken)
                        } else null
                    }

                    refreshTokens {
                        val currentRefreshToken = oldTokens?.refreshToken ?: tokenManager.getRefreshToken()

                        if (currentRefreshToken.isNullOrEmpty()) {
                            tokenManager.clearSession()
                            return@refreshTokens null
                        }

                        try {
                            val response: Map<String, String> = client.post(NetworkConstants.Endpoints.Refresh_Token) {
                                contentType(ContentType.Application.Json)
                                setBody(mapOf("refreshToken" to currentRefreshToken))
                            }.body()

                            val newAccessToken = response["accessToken"] ?: response["token"] ?: return@refreshTokens null
                            val newRefreshToken = response["refreshToken"] ?: currentRefreshToken

                            tokenManager.updateTokens(newAccessToken, newRefreshToken)

                            BearerTokens(accessToken = newAccessToken, refreshToken = newRefreshToken)
                        } catch (e: Exception) {
                            tokenManager.clearSession()
                            null
                        }
                    }

                    sendWithoutRequest { request ->
                        val path = request.url.encodedPath
                        !path.contains(NetworkConstants.Endpoints.LOGIN, ignoreCase = true) ||
                                path.contains(NetworkConstants.Endpoints.REGISTER, ignoreCase = true) ||
                                path.contains(NetworkConstants.Endpoints.Refresh_Token, ignoreCase = true)
                    }
                }
            }
        }
    }
}