package com.jo.payonapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import com.jo.payonapp.MainActivity
import com.jo.payonapp.R
import com.jo.payonapp.intent.UploadThumbnailIntent
import com.jo.payonapp.notification.PayOnNotificationHelper

import com.jo.payonapp.repository.NotificationRepository
import com.jo.payonapp.sideeffect.UploadThumbnailSideEffect
import com.jo.payonapp.state.UploadThumbnailState
import com.jo.payonapp.ui.theme.PayOnAppTheme
import com.jo.payonapp.viewmodel.UploadThumbnailViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class UploadThumbnailActivity : ComponentActivity() {

    private val uploadThumbnailViewModel: UploadThumbnailViewModel by inject()
    private val notificationRepository: NotificationRepository by inject()
    private val notificationHelper: PayOnNotificationHelper by inject()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission denied. You won't see welcome alerts.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkAndRequestNotificationPermission()
        observeSystemNotifications()

        setContent {
            PayOnAppTheme {
                val state by uploadThumbnailViewModel.state.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    uploadThumbnailViewModel.sideEffect.collect { effect ->
                        when (effect) {
                            is UploadThumbnailSideEffect.NavigateToMainActivity -> {
                                val intent = Intent(this@UploadThumbnailActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            is UploadThumbnailSideEffect.ShowToast -> {
                                Toast.makeText(this@UploadThumbnailActivity, effect.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                UploadThumbnailScreen(
                    state = state,
                    onIntent = { intent -> uploadThumbnailViewModel.processIntent(intent) }
                )
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun observeSystemNotifications() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                notificationRepository.observeRealTimeNotifications().collect { notificationResponse ->
                    notificationHelper.makeNotification(
                        title = notificationResponse.title ?: "PayOn Welcome",
                        message = notificationResponse.message ?: "Welcome to PayOn family!",
                        eventType = notificationResponse.eventType ?: "welcome",
                        notificationId = notificationResponse.id ?: ""
                    )
                    notificationRepository.getNotification(notificationResponse.id?: "")
                }
            }
        }
    }
}

@Composable
fun UploadThumbnailScreen(
    state: UploadThumbnailState,
    onIntent: (UploadThumbnailIntent) -> Unit
) {
    val context = LocalContext.current
    val outfitFontFamily = FontFamily(Font(R.font.outfit_semibold, FontWeight.SemiBold))

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            onIntent(UploadThumbnailIntent.ImageSelected(it.toString()))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF447AE3))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Profile Picture",
            fontFamily = outfitFontFamily,
            fontSize = 26.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add a photo so your friends can recognize you in transactions",
            fontFamily = outfitFontFamily,
            fontSize = 14.sp,
            color = Color(0xFFE0E0E0),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
                .border(2.dp, Color.White, CircleShape)
                .clickable(enabled = !state.isLoading) {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (state.selectedImageUri != null) {
                AsyncImage(
                    model = state.selectedImageUri,
                    contentDescription = "Selected Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Add photo",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        if (state.isLoading) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.currentStatusMessage ?: "Processing...",
                fontFamily = outfitFontFamily,
                fontSize = 15.sp,
                color = Color.White
            )
        }

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage!!,
                fontFamily = outfitFontFamily,
                fontSize = 14.sp,
                color = Color.Red,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        if (!state.isLoading) {
            Button(
                onClick = {
                    state.selectedImageUri?.let { uriString ->
                        val uri = Uri.parse(uriString)
                        val bytes = uriToByteArray(context, uri)
                        if (bytes != null) {
                            val fileName = "thumbnail_${System.currentTimeMillis()}.jpg"
                            onIntent(UploadThumbnailIntent.UploadClicked(bytes, fileName))
                        } else {
                            Toast.makeText(context, "Failed to process image file", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = state.selectedImageUri != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF447AE3),
                    disabledContainerColor = Color.White.copy(alpha = 0.4f),
                    disabledContentColor = Color.White
                )
            ) {
                Text(text = "Upload & Continue", fontFamily = outfitFontFamily, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Skip for now",
                fontFamily = outfitFontFamily,
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier
                    .clickable { onIntent(UploadThumbnailIntent.SkipClicked) }
                    .padding(8.dp)
            )
        }
    }
}

fun uriToByteArray(context: Context, uri: Uri): ByteArray? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        }
    } catch (e: Exception) {
        null
    }
}

@Preview(name = "Initial State", showBackground = true, showSystemUi = true, device = Devices.PIXEL_6A)
@Composable
fun UploadThumbnailScreenPreview() {
    PayOnAppTheme {
        UploadThumbnailScreen(
            state = UploadThumbnailState(
                selectedImageUri = null,
                isLoading = false,
                errorMessage = null
            ),
            onIntent = {}
        )
    }
}