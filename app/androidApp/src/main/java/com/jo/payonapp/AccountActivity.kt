package com.jo.payonapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.jo.payonapp.intent.AccountIntent
import com.jo.payonapp.network.NetworkConstants
import com.jo.payonapp.sideeffect.AccountSideEffect
import com.jo.payonapp.state.AccountState
import com.jo.payonapp.ui.theme.PayOnAppTheme
import com.jo.payonapp.viewmodel.AccountViewModel
import org.koin.android.ext.android.inject

class AccountActivity : ComponentActivity() {

    private val viewModel: AccountViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PayOnAppTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    viewModel.processIntent(AccountIntent.FetchAccountData)

                    viewModel.sideEffect.collect { effect ->
                        when (effect) {
                            is AccountSideEffect.ShowToast -> {
                                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                            }
                            is AccountSideEffect.NavigateToSignInActivity -> {
                                val intent = Intent(context, SignInActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                context.startActivity(intent)
                                finish()
                            }
                        }
                    }
                }

                AccountScreen(
                    state = state,
                    onIntent = viewModel::processIntent,
                    onNavigateToPersonalInformation = {
                        context.startActivity(Intent(context, PersonalInformation::class.java))
                    },
                    onNavigateToMerchant = {
                        context.startActivity(Intent(context, Merchant::class.java))
                    },
                    onNavigateToSupport = {
                        context.startActivity(Intent(context, Support::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun AccountScreen(
    state: AccountState,
    onIntent: (AccountIntent) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToPersonalInformation: () -> Unit = {},
    onNavigateToMerchant: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {}
) {
    val selectedItem = 2
    val appColor = Color(0xFF447AE3)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val items = listOf("Contacts", "Home", "Account")
                val icons = listOf(Icons.Filled.Person, Icons.Filled.Home, Icons.Filled.AccountCircle)
                val context = LocalContext.current

                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = {
                            if (selectedItem != index) {
                                when (index) {
                                    0 -> {
                                        val intent = Intent(context, ContactsActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        }
                                        context.startActivity(intent)
                                    }
                                    1 -> {
                                        val intent = Intent(context, MainActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        }
                                        context.startActivity(intent)
                                    }
                                    2 -> {}
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = appColor,
                            selectedTextColor = appColor,
                            indicatorColor = appColor.copy(alpha = 0.1f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEBF4FF))
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))


                val imageBitmap = remember(state.profileImageBytes) {
                    state.profileImageBytes?.let { bytes ->
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }
                }

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap as ImageBitmap,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.logobluee),
                            contentDescription = "Placeholder Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.Unspecified
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = state.username,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Text(
                    text = state.phoneNumber,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B6B6B)
                )

                Spacer(modifier = Modifier.height(50.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 30.dp)
                ) {
                    AccountMenuItem(
                        iconRes = R.drawable.infofill,
                        text = "Personal Information",
                        onClick = onNavigateToPersonalInformation
                    )
                    AccountMenuItem(
                        iconRes = R.drawable.card,
                        text = "Merchant",
                        onClick = onNavigateToMerchant
                    )
                    AccountMenuItem(
                        iconRes = R.drawable.support,
                        text = "Support",
                        onClick = onNavigateToSupport
                    )

                    Spacer(modifier = Modifier.height(60.dp))

                    OutlinedButton(
                        onClick = { onIntent(AccountIntent.LogOutClicked) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF447AE3)
                        ),
                        border = BorderStroke(2.dp, Color(0xFF447AE3))
                    ) {
                        Text(
                            text = "Logout",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
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
fun AccountMenuItem(
    iconRes: Int,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = text,
            modifier = Modifier.size(25.dp),
            tint = Color.Unspecified
        )

        Spacer(modifier = Modifier.width(20.dp))

        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF6B6B6B)
        )
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true, device = Devices.PIXEL_6)
fun AccountScreenPreview() {
    PayOnAppTheme {
        AccountScreen(
            state = AccountState(
                isLoading = false,
                username = "username",
                phoneNumber = "01xxxxxxxx",
                thumbnailPath = null,
                error = null
            ),
            onIntent = {},
            modifier = Modifier.fillMaxSize(),
            onNavigateToPersonalInformation = {},
            onNavigateToMerchant = {},
            onNavigateToSupport = {}
        )
    }
}