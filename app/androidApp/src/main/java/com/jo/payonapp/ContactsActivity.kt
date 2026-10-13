package com.jo.payonapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.jo.payonapp.dto.AuthResponse
import com.jo.payonapp.intent.ContactsIntent
import com.jo.payonapp.network.NetworkConstants
import com.jo.payonapp.sideeffect.ContactsSideEffect
import com.jo.payonapp.state.ContactsState
import com.jo.payonapp.ui.theme.PayOnAppTheme
import com.jo.payonapp.viewmodel.ContactsViewModel
import org.koin.android.ext.android.inject

class ContactsActivity : ComponentActivity() {

    private val viewModel: ContactsViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PayOnAppTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val context = LocalContext.current

                val triggerSync: () -> Unit = {
                    val numbersList = fetchContacts(context)
                    viewModel.syncContacts(numbersList)
                }

                ContactsPermissionHandler(
                    onContactsLoaded = { numbersList ->
                        viewModel.syncContacts(numbersList)
                    }
                )

                LaunchedEffect(Unit) {
                    viewModel.sideEffect.collect { effect ->
                        when (effect) {
                            is ContactsSideEffect.ShowToast -> {
                                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                ContactsScreenLayout(
                    state = state,
                    onIntent = { intent ->
                        if (intent is ContactsIntent.RefreshContacts) {
                            triggerSync()
                        } else {
                            viewModel.processIntent(intent)
                        }
                    },
                    onContentClick = { contact ->
                        val intent = Intent(context, TransferActivity::class.java).apply {
                            putExtra("username", contact.userName)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}


fun formatImageUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    val baseUrl = NetworkConstants.Base_Url.trimEnd('/')
    val cleanPath = if (path.startsWith("/")) path else "/$path"
    return "$baseUrl$cleanPath"
}

@Composable
fun ContactsScreenLayout(
    state: ContactsState,
    onIntent: (ContactsIntent) -> Unit,
    onContentClick: (AuthResponse) -> Unit
) {
    val selectedItem = 0
    val outfitFontFamily = FontFamily(Font(R.font.outfit_semibold, FontWeight.SemiBold))
    val appColor = Color(0xFF447AE3)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val items = listOf("Contacts", "Home", "Account")
                val icons = listOf(Icons.Filled.Person, Icons.Filled.Home, Icons.Filled.AccountCircle)
                val context = LocalContext.current

                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item, fontWeight = if (selectedItem == index) FontWeight.Bold else FontWeight.Normal) },
                        selected = selectedItem == index,
                        onClick = {
                            if (selectedItem != index) {
                                when (index) {
                                    0 -> {}
                                    1 -> {
                                        val intent = Intent(context, MainActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        }
                                        context.startActivity(intent)
                                    }
                                    2 -> {
                                        val intent = Intent(context, AccountActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = appColor,
                            selectedTextColor = appColor,
                            indicatorColor = appColor.copy(alpha = 0.12f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(innerPadding)
        ) {
            ContactsHeader(
                isRefreshing = state.isRefreshing,
                isOffline = state.isOfflineMode,
                onRefreshClick = { onIntent(ContactsIntent.RefreshContacts) }
            )

            SearchBarComponent(
                query = state.searchQuery,
                onQueryChange = { onIntent(ContactsIntent.UpdateSearchQuery(it)) },
                outfitFontFamily = outfitFontFamily
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = appColor)
                }
            } else if (state.filteredContacts.isEmpty()) {
                EmptyContactsState(
                    searchQuery = state.searchQuery,
                    error = state.error,
                    onRetry = { onIntent(ContactsIntent.RefreshContacts) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = state.filteredContacts,
                        key = { index, contact -> contact.userId ?: contact.userName ?: "$index" }
                    ) { _, contact ->
                        ContactItemRow(
                            contact = contact,
                            outfitFontFamily = outfitFontFamily,
                            onClick = { onContentClick(contact) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsHeader(
    isRefreshing: Boolean,
    isOffline: Boolean,
    onRefreshClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "PayOn Contacts",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            if (isOffline) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Offline - Cached contacts",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFDC2626),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        IconButton(
            onClick = onRefreshClick,
            enabled = !isRefreshing,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), CircleShape)
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF447AE3),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Contacts",
                    tint = Color(0xFF334155)
                )
            }
        }
    }
}

@Composable
fun SearchBarComponent(
    query: String,
    onQueryChange: (String) -> Unit,
    outfitFontFamily: FontFamily
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    fontFamily = outfitFontFamily,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                ),
                singleLine = true,
                cursorBrush = SolidColor(Color(0xFF447AE3)),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Search by username or phone...",
                            color = Color(0xFF94A3B8),
                            fontFamily = outfitFontFamily,
                            fontSize = 15.sp
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.weight(1f)
            )

            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = Color(0xFF64748B),
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") }
                )
            }
        }
    }
}

@Composable
fun ContactItemRow(
    contact: AuthResponse,
    outfitFontFamily: FontFamily,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val customImageLoader = if (isPreview) {
        ImageLoader(context)
    } else {
        (context.applicationContext as PayOnApplication).imageLoader
    }

    val displayUsername = contact.safeUsername
    val displayPhone = contact.safePhone

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(
                rawPath = contact.thumbnailPath,
                userName = contact.userName ?: "user",
                imageLoader = customImageLoader
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayUsername,
                    fontFamily = outfitFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = displayPhone,
                    fontFamily = outfitFontFamily,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Send",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ContactAvatar(
    rawPath: String?,
    userName: String,
    imageLoader: ImageLoader
) {
    val formattedUrl = remember(rawPath) { formatImageUrl(rawPath) }
    var isError by remember(formattedUrl) { mutableStateOf(false) }

    if (!formattedUrl.isNullOrBlank() && !isError) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(formattedUrl)
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
            contentDescription = userName,
            contentScale = ContentScale.Crop,
            onState = { state ->
                if (state is AsyncImagePainter.State.Error) {
                    isError = true
                }
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0))
        )
    } else {
        val initials = userName.replace("@", "").take(2).uppercase()
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFDBEAFE)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.ifBlank { "U" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D4ED8)
            )
        }
    }
}

@Composable
fun EmptyContactsState(
    searchQuery: String,
    error: String?,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (searchQuery.isNotEmpty()) "🔍" else "📇",
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    searchQuery.isNotEmpty() -> "No contacts match '$searchQuery'"
                    error != null -> error
                    else -> "No registered PayOn contacts found."
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center
            )

            if (searchQuery.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF447AE3))
                ) {
                    Text("Sync Contacts", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ContactsPermissionHandler(onContactsLoaded: (List<String>) -> Unit) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val numbers = fetchContacts(context)
                onContactsLoaded(numbers)
            }
        }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }
}

fun fetchContacts(context: Context): List<String> {
    val phoneNumbers = mutableSetOf<String>()
    val contentResolver = context.contentResolver

    val cursor = contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
        null, null, null
    )

    cursor?.use {
        val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (it.moveToNext()) {
            val rawNumber = it.getString(numberIndex) ?: continue

            var cleanNumber = rawNumber.replace(Regex("[^\\d+]"), "")
            if (cleanNumber.startsWith("+20")) {
                cleanNumber = "0" + cleanNumber.substring(3)
            } else if (cleanNumber.startsWith("20") && cleanNumber.length > 10) {
                cleanNumber = "0" + cleanNumber.substring(2)
            }

            if (cleanNumber.isNotEmpty() && cleanNumber.length >= 11) {
                phoneNumbers.add(cleanNumber)
            }
        }
    }

    return phoneNumbers.toList()
}

// =============================================================
// 📸 PREVIEWS
// =============================================================

@Preview(showBackground = true, device = Devices.PIXEL_6)
@Composable
fun ContactsScreenLayoutPreview() {
    PayOnAppTheme {
        ContactsScreenLayout(
            state = ContactsState(
                isLoading = false,
                isOfflineMode = true,
                searchQuery = "",
                filteredContacts = listOf(
                    AuthResponse(userId = "1", userName = "omar_bakry", phoneNumber = "01012345678"),
                    AuthResponse(userId = "2", userName = "ali_net", phoneNumber = "01187654321")
                )
            ),
            onIntent = {},
            onContentClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ContactAvatarPreview() {
    PayOnAppTheme {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ContactAvatar(
                rawPath = null,
                userName = "yousef_m",
                imageLoader = ImageLoader(LocalContext.current)
            )
        }
    }
}