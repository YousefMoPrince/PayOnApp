package com.jo.payonapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jo.payonapp.dto.Status
import com.jo.payonapp.intent.TransferIntent
import com.jo.payonapp.sideeffect.TransferSideEffect
import com.jo.payonapp.state.TransferState
import com.jo.payonapp.ui.theme.PayOnAppTheme
import com.jo.payonapp.viewmodel.TransferViewModel
import org.koin.android.ext.android.inject
import java.math.BigDecimal



class TransferActivity : ComponentActivity() {

    private val viewModel: TransferViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incomingUsername = intent.getStringExtra("username")
        if (!incomingUsername.isNullOrEmpty()) {
            viewModel.processIntent(TransferIntent.UpdateReceiver(incomingUsername))
        }

        enableEdgeToEdge()

        setContent {
            PayOnAppTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val context = LocalContext.current
                var currentScreen by remember { mutableStateOf(TransferScreen.INPUT) }
                var lastAmount by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    viewModel.sideEffect.collect { effect ->
                        when (effect) {
                            is TransferSideEffect.NavigateToSuccessFullTransferScreen -> {
                                currentScreen = TransferScreen.SUCCESS
                            }
                            is TransferSideEffect.NavigateToFailedScreen -> {
                                currentScreen = TransferScreen.FAILED
                            }
                            is TransferSideEffect.NavigateToMainActivity -> {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                context.startActivity(intent)
                                finish()
                            }
                            is TransferSideEffect.ShowToast -> {
                                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                TransferMainContent(
                    state = state,
                    currentScreen = currentScreen,
                    lastAmount = lastAmount,
                    onIntent = viewModel::processIntent,
                    onConfirm = { receiver, amount ->
                        lastAmount = amount
                        val parsedAmount = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        viewModel.processIntent(TransferIntent.UpdateReceiver(receiver))
                        viewModel.processIntent(TransferIntent.UpdateAmount(parsedAmount.toDouble()))
                        viewModel.processIntent(TransferIntent.TransferTransaction)
                    },
                    onAcknowledge = { status ->
                        viewModel.processIntent(TransferIntent.TransferAcknowledgement(status))
                    },
                    onContactsClick = {
                        val intent = Intent(context, ContactsActivity::class.java)
                        context.startActivity(intent)
                    },
                    onBackClick = { finish() },
                    onAnotherPayment = {
                        currentScreen = TransferScreen.INPUT
                        viewModel.processIntent(TransferIntent.RefreshWalletData)
                    },
                    onRetry = {
                        currentScreen = TransferScreen.INPUT
                    }
                )
            }
        }
    }
}

@Composable
fun TransferMainContent(
    state: TransferState,
    currentScreen: TransferScreen,
    lastAmount: String,
    onIntent: (TransferIntent) -> Unit,
    onConfirm: (String, String) -> Unit,
    onAcknowledge: (Status) -> Unit,
    onContactsClick: () -> Unit,
    onBackClick: () -> Unit,
    onAnotherPayment: () -> Unit,
    onRetry: () -> Unit
) {
    val appColor = Color(0xFF447AE3)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF8FAFF)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                TransferScreen.INPUT -> {
                    TransferInputScreen(
                        state = state,
                        onIntent = onIntent,
                        onConfirm = onConfirm,
                        onAcknowledge = onAcknowledge,
                        onContactsClick = onContactsClick,
                        onBackClick = onBackClick
                    )
                }
                TransferScreen.SUCCESS -> {
                    SuccessfulTransferScreen(
                        amount = lastAmount,
                        receiver = state.receiverUsername,
                        transactionId = state.transactionId,
                        onBackHome = onBackClick,
                        onAnotherPayment = onAnotherPayment
                    )
                }
                TransferScreen.FAILED -> {
                    OperationFailedScreen(
                        errorMessage = state.error ?: "Unknown Error",
                        onBackHome = onBackClick,
                        onRetry = onRetry
                    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferInputScreen(
    state: TransferState,
    onIntent: (TransferIntent) -> Unit,
    onConfirm: (String, String) -> Unit,
    onAcknowledge: (Status) -> Unit,
    onContactsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var receiver by remember { mutableStateOf(state.receiverUsername) }
    var amount by remember { mutableStateOf("") }
    val outfitFont = FontFamily(Font(R.font.outfit_semibold))

    LaunchedEffect(state.receiverUsername) {
        if (state.receiverUsername.isNotEmpty()) {
            receiver = state.receiverUsername
        }
    }

    if (state.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(text = "Confirm Transfer", fontFamily = outfitFont, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to transfer E£$amount to @$receiver?",
                    fontFamily = outfitFont,
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { onAcknowledge(Status.SUCCESS) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E76E5))
                ) {
                    Text("Confirm", color = Color.White, fontFamily = outfitFont)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { onAcknowledge(Status.CANCELLED) }
                ) {
                    Text("Cancel", color = Color.Red, fontFamily = outfitFont)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEBF4FF))
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1A1C1E)
                )
            }

            Text(
                text = "Transfer",
                fontFamily = outfitFont,
                fontSize = 28.sp,
                color = Color(0xFF1A1C1E),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Receiver Username",
            fontFamily = outfitFont,
            fontSize = 14.sp,
            color = Color(0xFF1A1C1E)
        )

        OutlinedTextField(
            value = receiver,
            onValueChange = { receiver = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
            textStyle = LocalTextStyle.current.copy(fontFamily = outfitFont, fontSize = 16.sp, color = Color.Black),
            placeholder = { Text("Enter username", color = Color.Gray) },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = onContactsClick) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Open Contacts",
                        tint = Color(0xFF4E76E5)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = Color(0xFF4E76E5)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Text(
            text = "Enter transfer amount",
            fontFamily = outfitFont,
            fontSize = 14.sp,
            color = Color(0xFF1A1C1E)
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(85.dp),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 36.sp,
                fontFamily = outfitFont,
                textAlign = TextAlign.Center,
                color = Color.Black
            ),
            placeholder = {
                Text(
                    "E£0.00",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 36.sp,
                    color = Color.Gray
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = Color(0xFF4E76E5),
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(25.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.isAccountVisible) state.accountNumber.toString() else "XXXXXXXXXXXXXXX",
                            fontFamily = outfitFont,
                            color = Color(0xFF8E9199),
                            fontSize = 16.sp
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.visibility),
                            contentDescription = "Toggle visibility",
                            tint = Color(0xFFC0C0C0),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onIntent(TransferIntent.ToggleAccountVisibility) }
                                .padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = "Available: E£${state.balance}",
                        fontFamily = outfitFont,
                        color = Color(0xFFC4C6CF),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Image(
                    painter = painterResource(id = R.drawable.card_transaction),
                    contentDescription = "Card Logo",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .width(100.dp)
                        .fillMaxHeight()
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onConfirm(receiver, amount) },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(bottom = 10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E76E5)),
            shape = RoundedCornerShape(10.dp),
            enabled = amount.isNotEmpty() && receiver.isNotEmpty() && !state.isLoading
        ) {
            Text(
                text = "Transfer",
                fontFamily = outfitFont,
                fontSize = 18.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun SuccessfulTransferScreen(
    amount: String,
    receiver: String,
    transactionId: String,
    onBackHome: () -> Unit,
    onAnotherPayment: () -> Unit
) {
    val outfitFont = FontFamily(Font(R.font.outfit_semibold))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Box(
            modifier = Modifier
                .size(90.dp)
                .background(Color(0xFFE8F5E9), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.yesincircle),
                contentDescription = "Success",
                tint = Color(0xFF3AC100),
                modifier = Modifier.size(45.dp)
            )
        }

        Text(
            text = "Transfer Successful! 🎉",
            fontFamily = outfitFont,
            fontSize = 24.sp,
            color = Color(0xFF3AC100),
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(modifier = Modifier.height(30.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp)
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(15.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                ReceiptRow(label = "Amount Transferred", value = "E£ $amount", font = outfitFont)
                Spacer(modifier = Modifier.height(20.dp))
                ReceiptRow(label = "To Receiver", value = "@$receiver", font = outfitFont)
                Spacer(modifier = Modifier.height(20.dp))
                ReceiptRow(label = "Transaction ID", value = transactionId, font = outfitFont)

                HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = Color(0xFFE0E0E0))

                Text(
                    text = "Status: Completed",
                    fontFamily = outfitFont,
                    fontSize = 12.sp,
                    color = Color(0xFF3AC100),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp, vertical = 30.dp)
        ) {
            Button(
                onClick = onBackHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF447AE3)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Back to Home", fontFamily = outfitFont, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onAnotherPayment,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                border = BorderStroke(1.5.dp, Color(0xFF447AE3)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Make another transfer", fontFamily = outfitFont, fontSize = 16.sp, color = Color(0xFF447AE3))
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String, font: FontFamily) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontFamily = font, fontSize = 14.sp, color = Color(0xFFADADAD))
        Text(text = value, fontFamily = font, fontSize = 14.sp, color = Color.Black)
    }
}

@Composable
private fun OperationFailedScreen(
    errorMessage: String,
    onBackHome: () -> Unit,
    onRetry: () -> Unit
) {
    val outfitFont = FontFamily(Font(R.font.outfit_semibold))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(100.dp))

        Box(
            modifier = Modifier
                .size(90.dp)
                .background(Color(0xFFFFEBEE), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.xincircle),
                contentDescription = "Failed",
                tint = Color(0xFFE24F4F),
                modifier = Modifier.size(45.dp)
            )
        }

        Text(
            text = "Transfer Failed! 🚫",
            fontFamily = outfitFont,
            fontSize = 24.sp,
            color = Color(0xFFE24F4F),
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "Error Details:",
            fontFamily = outfitFont,
            fontSize = 14.sp,
            color = Color(0xFFADADAD)
        )

        Text(
            text = errorMessage,
            fontFamily = outfitFont,
            fontSize = 20.sp,
            color = Color(0xFF1A1C1E),
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp, vertical = 30.dp)
        ) {
            Button(
                onClick = onBackHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF447AE3)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Back to Home", fontFamily = outfitFont, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                border = BorderStroke(1.5.dp, Color(0xFF447AE3)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Try Again", fontFamily = outfitFont, fontSize = 16.sp, color = Color(0xFF447AE3))
            }
        }
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true, device = Devices.PIXEL_6)
fun TransferScreenPreview() {
    PayOnAppTheme {
        TransferMainContent(
            state = TransferState(
                isLoading = false,
                accountNumber = 1234567890,
                balance = 2500.00,
                receiverUsername = "Ali_Zaki",
                isAccountVisible = true,
                showConfirmDialog = false
            ),
            currentScreen = TransferScreen.INPUT,
            lastAmount = "",
            onIntent = {},
            onConfirm = { _, _ -> },
            onAcknowledge = {},
            onContactsClick = {},
            onBackClick = {},
            onAnotherPayment = {},
            onRetry = {}
        )
    }
}