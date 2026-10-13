package com.jo.payonapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.jo.payonapp.intent.InformationIntent
import com.jo.payonapp.sideeffect.InformationSideEffect
import com.jo.payonapp.state.InformationState
import com.jo.payonapp.ui.theme.PayOnAppTheme
import com.jo.payonapp.viewmodel.InformationViewModel
import org.koin.android.ext.android.inject

class PersonalInformation : ComponentActivity() {

    private val viewModel: InformationViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PayOnAppTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val context = LocalContext.current
                var showUploadDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.processIntent(InformationIntent.FetchAccountData)

                    viewModel.sideEffect.collect { effect ->
                        when (effect) {
                            is InformationSideEffect.ShowToast -> {
                                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                            }
                            is InformationSideEffect.NavigateToSignInActivity -> {
                                val intent = Intent(context, SignInActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                context.startActivity(intent)
                                finish()
                            }
                            is InformationSideEffect.NavigateToUploadThumbnailSmallScreen -> {
                                showUploadDialog = true
                            }
                        }
                    }
                }

                InformationScreen(
                    state = state,
                    onIntent = viewModel::processIntent,
                    onBackClick = { finish() }
                )

                if (showUploadDialog && state.selectedImageUriString != null) {
                    val uri = Uri.parse(state.selectedImageUriString)
                    UploadImageDialog(
                        uri = uri,
                        onDismiss = { showUploadDialog = false },
                        onConfirmUpload = {
                            val bytes = uriToBytes(context, uri)
                            if (bytes != null) {
                                val fileName = "thumbnail_${System.currentTimeMillis()}.jpg"
                                viewModel.processIntent(InformationIntent.UploadClicked(bytes, fileName))
                            } else {
                                Toast.makeText(context, "Failed to process image file", Toast.LENGTH_SHORT).show()
                            }
                            showUploadDialog = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformationScreen(
    state: InformationState,
    onIntent: (InformationIntent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appColor = Color(0xFF447AE3)
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onIntent(InformationIntent.ImageSelected(it.toString())) }
    }

    val imageBitmap = remember(state.profileImageBytes) {
        state.profileImageBytes?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Personal Information", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEBF4FF))
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(30.dp))

                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Profile Picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.logobluee),
                                contentDescription = "Default Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                tint = Color.Unspecified
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(appColor)
                            .clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Change Photo", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                EditableInfoField(
                    label = "Username",
                    value = state.draftUsername,
                    onValueChange = { onIntent(InformationIntent.UsernameChanged(it)) },
                    onCopy = { copyToClipboard(context, state.originalUsername, "Username") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                EditableInfoField(
                    label = "Full Name",
                    value = state.draftFullName,
                    onValueChange = { onIntent(InformationIntent.FullNameChanged(it)) },
                    onCopy = { copyToClipboard(context, state.originalFullName, "Full Name") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                EditableInfoField(
                    label = "Email Address",
                    value = state.draftEmail,
                    onValueChange = { onIntent(InformationIntent.EmailChanged(it)) },
                    onCopy = { copyToClipboard(context, state.originalEmail, "Email") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                EditableInfoField(
                    label = "Phone Number",
                    value = state.draftPhoneNumber,
                    onValueChange = { onIntent(InformationIntent.PhoneNumberChanged(it)) },
                    onCopy = { copyToClipboard(context, state.originalPhoneNumber, "Phone Number") }
                )

                Spacer(modifier = Modifier.height(50.dp))

                Button(
                    onClick = { onIntent(InformationIntent.UpdateData) },
                    enabled = state.isSaveButtonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = appColor,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Save Changes", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(30.dp))
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = appColor)
                }
            }
        }
    }
}

@Composable
fun EditableInfoField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray) },
        trailingIcon = {
            IconButton(onClick = onCopy) {
                Icon(painter = painterResource(id = R.drawable.ic_copy), contentDescription = "Copy Field", tint = Color(0xFF6B6B6B))
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF447AE3),
            unfocusedBorderColor = Color.Gray,
            cursorColor = Color(0xFF447AE3),
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        )
    )
}

@Composable
fun UploadImageDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onConfirmUpload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Profile Picture") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Do you want to upload the selected photo?")
                Spacer(modifier = Modifier.height(16.dp))
                AsyncImage(
                    model = uri,
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmUpload) {
                Text("Upload", color = Color(0xFF447AE3), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

fun uriToBytes(context: Context, uri: Uri): ByteArray? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

@Preview(showBackground = true, showSystemUi = true, device = Devices.PIXEL_6)
@Composable
fun InformationScreenPreview() {
    PayOnAppTheme {
        InformationScreen(
            state = InformationState(
                originalUsername = "YousefMoPrince",
                draftUsername = "YousefMoPrince",
                originalFullName = "Yousef Mohammed",
                draftFullName = "Yousef Mohammed"
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}