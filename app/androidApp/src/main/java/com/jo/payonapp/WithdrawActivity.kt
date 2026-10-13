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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jo.payonapp.intent.WithdrawIntent
import com.jo.payonapp.sideeffect.WithdrawSideEffect
import com.jo.payonapp.state.WithdrawState
import com.jo.payonapp.ui.theme.PayOnAppTheme
import com.jo.payonapp.viewmodel.WithdrawViewModel
import org.koin.android.ext.android.inject

class WithdrawActivity : ComponentActivity() {

    private val viewModel: WithdrawViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PayOnAppTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val context = LocalContext.current
                var currentScreen by remember { mutableStateOf(WithdrawScreen.INPUT) }
                var lastAmount by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    viewModel.sideEffect.collect { effect ->
                        when (effect) {
                            is WithdrawSideEffect.NavigateToSuccessFullWithdrawScreen -> {
                                currentScreen = WithdrawScreen.SUCCESS
                            }
                            is WithdrawSideEffect.NavigateToFailedScreen -> {
                                currentScreen = WithdrawScreen.FAILED
                            }
                            is WithdrawSideEffect.NavigateToMainActivity -> {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                context.startActivity(intent)
                                finish()
                            }
                            is WithdrawSideEffect.ShowToast -> {
                                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                WithdrawMainContent(
                    state = state,
                    currentScreen = currentScreen,
                    lastAmount = lastAmount,
                    onIntent = viewModel::processIntent,
                    onConfirm = { amount ->
                        lastAmount = amount
                        viewModel.processIntent(WithdrawIntent.UpdateAmount(amount.toDouble()))
                        viewModel.processIntent(WithdrawIntent.WithdrawTransaction)
                    },
                    onBackClick = { finish() },
                    onAnotherPayment = {
                        currentScreen = WithdrawScreen.INPUT
                        viewModel.processIntent(WithdrawIntent.RefreshWalletData)
                    },
                    onRetry = {
                        currentScreen = WithdrawScreen.INPUT
                        viewModel.processIntent(WithdrawIntent.WithdrawTransaction)
                    }
                )
            }
        }
    }
}

@Composable
fun WithdrawMainContent(
    state: WithdrawState,
    currentScreen: WithdrawScreen,
    lastAmount: String,
    onIntent: (WithdrawIntent) -> Unit,
    onConfirm: (String) -> Unit,
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
                WithdrawScreen.INPUT -> {
                    WithdrawInputScreen(
                        state = state,
                        onIntent = onIntent,
                        onConfirm = onConfirm,
                        onBackClick = onBackClick
                    )
                }
                WithdrawScreen.SUCCESS -> {
                    SuccessfulWithdrawScreen(
                        amount = lastAmount,
                        accountNumber = state.accountNumber.toString(),
                        transactionId = state.transactionId,
                        onBackHome = onBackClick,
                        onAnotherPayment = onAnotherPayment
                    )
                }
                WithdrawScreen.FAILED -> {
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
fun WithdrawInputScreen(
    state: WithdrawState,
    onIntent: (WithdrawIntent) -> Unit,
    onConfirm: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    val outfitFont = FontFamily(Font(R.font.outfit_semibold))

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
                text = "Withdraw",
                fontFamily = outfitFont,
                fontSize = 28.sp,
                color = Color(0xFF1A1C1E),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Enter withdrawal amount",
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
                                .clickable { onIntent(WithdrawIntent.ToggleAccountVisibility) }
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
            onClick = { onConfirm(amount) },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(bottom = 10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E76E5)),
            shape = RoundedCornerShape(10.dp),
            enabled = amount.isNotEmpty() && !state.isLoading
        ) {
            Text(
                text = "Confirm",
                fontFamily = outfitFont,
                fontSize = 18.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun SuccessfulWithdrawScreen(
    amount: String,
    accountNumber: String,
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
            text = "Withdrawal Successful! 🎉",
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
                ReceiptRow(label = "Amount Withdrawn", value = "E£ $amount", font = outfitFont)
                Spacer(modifier = Modifier.height(20.dp))
                ReceiptRow(label = "From Account", value = accountNumber, font = outfitFont)
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
                Text(text = "Make another withdrawal", fontFamily = outfitFont, fontSize = 16.sp, color = Color(0xFF447AE3))
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
            text = "Operation Failed! 🚫",
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

        Text(
            text = "Please check your balance or try again later.",
            fontFamily = outfitFont,
            fontSize = 14.sp,
            color = Color(0xFF808080),
            modifier = Modifier.padding(top = 12.dp),
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
fun WithdrawScreenPreview() {
    PayOnAppTheme {
        WithdrawMainContent(
            state = WithdrawState(
                isLoading = false,
                accountNumber = 123456789,
                isAccountVisible = true,
                balance = 5000.00
            ),
            currentScreen = WithdrawScreen.INPUT,
            lastAmount = "",
            onIntent = {},
            onConfirm = {},
            onBackClick = {},
            onAnotherPayment = {},
            onRetry = {}
        )
    }
}