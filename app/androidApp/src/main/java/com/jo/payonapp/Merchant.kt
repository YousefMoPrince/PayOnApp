package com.jo.payonapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jo.payonapp.dto.CreateReceiptRequest
import com.jo.payonapp.dto.CreateReceiptRequestData
import com.jo.payonapp.dto.CreatedReceiptResponse
import com.jo.payonapp.dto.CreatedReceiptResponseData
import com.jo.payonapp.dto.Currency
import com.jo.payonapp.dto.MerchantType
import com.jo.payonapp.dto.ReceiptStatus
import com.jo.payonapp.intent.MerchantIntent
import com.jo.payonapp.sideeffect.MerchantSideEffect
import com.jo.payonapp.state.MerchantState
import com.jo.payonapp.ui.theme.PayOnAppTheme
import com.jo.payonapp.viewmodel.MerchantViewModel
import org.koin.android.ext.android.inject
import java.util.UUID

class Merchant : ComponentActivity() {

    private val viewModel: MerchantViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PayOnAppTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    viewModel.processIntent(MerchantIntent.FetchMerchantData)

                    viewModel.sideEffect.collect { effect ->
                        when (effect) {
                            is MerchantSideEffect.ShowToast -> {
                                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                            }
                            is MerchantSideEffect.ReceiptCreatedSuccessfully -> {
                                // Handled via state update and toast
                            }
                        }
                    }
                }

                MerchantScreen(
                    state = state,
                    onIntent = viewModel::processIntent,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantScreen(
    state: MerchantState,
    onIntent: (MerchantIntent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appColor = Color(0xFF447AE3)
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Merchant Dashboard", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            if (state.isMerchant) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = appColor,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Receipt")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEBF4FF))
                .padding(innerPadding)
        ) {
            if (state.isMerchant) {
                MerchantDetailsContent(
                    state = state,
                    onRefreshReceipts = { onIntent(MerchantIntent.FetchReceipts) }
                )
            } else {
                BecomeMerchantForm(state = state, onIntent = onIntent)
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

            if (showCreateDialog) {
                CreateReceiptDialog(
                    onDismiss = { showCreateDialog = false },
                    onCreate = { request ->
                        onIntent(MerchantIntent.CreateReceipt(request))
                        showCreateDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun MerchantDetailsContent(
    state: MerchantState,
    onRefreshReceipts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Merchant Account Active",
                    color = Color(0xFF2E7D32),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                MerchantInfoRow(
                    label = "Merchant Name",
                    value = state.merchantUserName,
                    onCopy = { copyToClipboard(context, state.merchantUserName, "Merchant Name") }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.5f))

                MerchantInfoRow(
                    label = "Account Number",
                    value = state.merchantAccountNumber.toString(),
                    onCopy = { copyToClipboard(context, state.merchantAccountNumber.toString(), "Account Number") }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Receipts List",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            TextButton(onClick = onRefreshReceipts) {
                Text("Refresh", color = Color(0xFF447AE3))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.receipts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No receipts found.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(state.receipts) { receipt ->
                    ReceiptItemCard(receipt = receipt)
                }
            }
        }
    }
}

@Composable
fun ReceiptItemCard(
    receipt: CreatedReceiptResponse,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = receipt.data.clientName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Text(
                    text = "${receipt.data.amount} ${receipt.data.currency}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF447AE3)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "code: ${receipt.data.transactionCode}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (receipt.state) {
                        ReceiptStatus.PENDING -> Color(0xFFFFF3E0)
                        ReceiptStatus.SUCCESS -> Color(0xE8D4FAD6)
                        ReceiptStatus.FAILED -> Color(0xFFFFEBEE)
                    }
                ) {
                    Text(
                        text = receipt.state.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when (receipt.state) {
                            ReceiptStatus.PENDING -> Color(0xFFE65100)
                            ReceiptStatus.SUCCESS -> Color(0xFF2E7D32)
                            ReceiptStatus.FAILED -> Color(0xFFC62828)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CreateReceiptDialog(
    onDismiss: () -> Unit,
    onCreate: (CreateReceiptRequest) -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(Currency.EGP) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Receipt", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Client Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Currency:", fontSize = 14.sp, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Currency.entries.forEach { curr ->
                        FilterChip(
                            selected = selectedCurrency == curr,
                            onClick = { selectedCurrency = curr },
                            label = { Text(curr.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull()

                    if (clientName.isNotBlank() && parsedAmount != null) {
                        val request = CreateReceiptRequest(
                            state = ReceiptStatus.PENDING, // الباك إند بيحدد الحالة تلقائياً
                            data = CreateReceiptRequestData(
                                amount = parsedAmount,
                                clientName = clientName.trim(),
                                currency = selectedCurrency,
                                idempotencyKey = UUID.randomUUID().toString()
                            )
                        )
                        onCreate(request)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF447AE3))
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun BecomeMerchantForm(
    state: MerchantState,
    onIntent: (MerchantIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val appColor = Color(0xFF447AE3)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Grow Your Business",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            text = "Fill in the details below to create your corporate merchant profile.",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier
                .padding(top = 4.dp, bottom = 30.dp)
                .align(Alignment.Start)
        )

        OutlinedTextField(
            value = state.merchantUserName,
            onValueChange = { onIntent(MerchantIntent.MerchantUserNameChanged(it)) },
            label = { Text("Merchant Name", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = appColor,
                unfocusedBorderColor = Color.LightGray,
                cursorColor = appColor,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onIntent(MerchantIntent.SubmitBecomeMerchant) },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = appColor)
        ) {
            Text("Submit Application", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
fun MerchantInfoRow(
    label: String,
    value: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.Gray, fontSize = 13.sp)
            Text(value, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
        }
        IconButton(onClick = onCopy) {
            Icon(
                painter = painterResource(id = R.drawable.ic_copy),
                contentDescription = "Copy $label",
                tint = Color(0xFF6B6B6B),
                modifier = Modifier.size(20.dp)
            )
        }
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
fun MerchantScreenFormPreview() {
    PayOnAppTheme {
        MerchantScreen(
            state = MerchantState(isMerchant = false),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, device = Devices.PIXEL_6)
@Composable
fun MerchantScreenDetailsPreview() {
    PayOnAppTheme {
        MerchantScreen(
            state = MerchantState(
                isMerchant = true,
                merchantUserName = "Nova Store",
                merchantAccountNumber = 8492048591L,
                receipts = listOf(
                    CreatedReceiptResponse(
                        state = ReceiptStatus.SUCCESS,
                        data = CreatedReceiptResponseData(
                            transactionId = UUID.randomUUID().toString(),
                            clientName = "Yousef Ibrahim",
                            merchantUserName = "Nova Store",
                            merchantType = MerchantType.ENTERPRISE,
                            amount = 150.00,
                            accountNumber = 123456789L,
                            payerAccountNumber = 987654321L,
                            currency = Currency.EGP,
                            transactionCode = 100201L,
                            idempotencyKey = UUID.randomUUID().toString(),
                            createdAt = "2026-03-01T12:00:00Z",
                            updatedAt = "2026-03-01T12:00:00Z"
                        )
                    ),
                    CreatedReceiptResponse(
                        state = ReceiptStatus.PENDING,
                        data = CreatedReceiptResponseData(
                            transactionId = UUID.randomUUID().toString(),
                            clientName = "Omar Ahmed",
                            merchantUserName = "Nova Store",
                            merchantType = MerchantType.ENTERPRISE,
                            amount = 300.50,
                            accountNumber = 987654321L,
                            payerAccountNumber = 123456789L,
                            currency = Currency.USD,
                            transactionCode = 100202L,
                            idempotencyKey = UUID.randomUUID().toString(),
                            createdAt = "2026-03-01T12:30:00Z",
                            updatedAt = "2026-03-01T12:30:00Z"
                        )
                    )
                )
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}


