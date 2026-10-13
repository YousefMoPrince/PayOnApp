package com.jo.payonapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jo.payonapp.dto.NotificationResponse
import com.jo.payonapp.dto.TransactionResponse
import com.jo.payonapp.intent.MainIntent
import com.jo.payonapp.sideeffect.MainSideEffect
import com.jo.payonapp.state.MainState
import com.jo.payonapp.ui.theme.PayOnAppTheme
import com.jo.payonapp.viewmodel.MainViewModel
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PayOnAppTheme {
                val state by mainViewModel.state.collectAsStateWithLifecycle()
                val context = LocalContext.current
                var showNotifications by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    mainViewModel.sideEffect.collect { effect ->
                        when (effect) {
                            is MainSideEffect.ShowToast -> {
                                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                            }
                            is MainSideEffect.ShowNotificationBanner -> {
                                Toast.makeText(
                                    context,
                                    "${effect.title}: ${effect.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        state = state,
                        onIntent = { intent -> mainViewModel.processIntent(intent) },
                        onNavigateToTransfer = {
                            startActivity(Intent(this@MainActivity, TransferActivity::class.java))
                        },
                        onNavigateToWithdraw = {
                            startActivity(Intent(this@MainActivity, WithdrawActivity::class.java))
                        },
                        onNavigateToDeposit = {
                            startActivity(Intent(this@MainActivity, DepositActivity::class.java))
                        },
                        onNavigateToOnPay = {
                            startActivity(Intent(this@MainActivity, OnPayActivity::class.java))
                        },
                        onNavigateToNotifications = {
                            showNotifications = true
                            mainViewModel.processIntent(MainIntent.MarkNotificationsAsRead)
                        }
                    )

                    AnimatedVisibility(
                        visible = showNotifications,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showNotifications = false
                                }
                        )
                    }

                    AnimatedVisibility(
                        visible = showNotifications,
                        enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }),
                        exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.85f)
                                .background(Color.White)
                        ) {
                            NotificationsScreen(
                                state = state,
                                onBackClick = { showNotifications = false }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = state.isAllTransactionsVisible,
                        enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }),
                        exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
                    ) {
                        AllTransactionsScreen(
                            transactions = state.transactions,
                            onBackClick = { mainViewModel.processIntent(MainIntent.SetAllTransactionsVisible(false)) },
                            onTransactionClick = { transaction ->
                                mainViewModel.processIntent(MainIntent.SelectTransaction(transaction))
                            }
                        )
                    }

                    if (state.selectedTransaction != null) {
                        TransactionDetailsSheet(
                            transaction = state.selectedTransaction!!,
                            onDismiss = { mainViewModel.processIntent(MainIntent.SelectTransaction(null)) }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume executed - Refreshing Data")
        mainViewModel.processIntent(MainIntent.RefreshData)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainState,
    onIntent: (MainIntent) -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToWithdraw: () -> Unit,
    onNavigateToDeposit: () -> Unit,
    onNavigateToOnPay: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    var selectedItem = 1
    val outfitFontFamily = FontFamily(Font(R.font.outfit_semibold, FontWeight.SemiBold))
    val appColor = Color(0xFF447AE3)

    Scaffold(
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
                                selectedItem = index
                                when (index) {
                                    0 -> {
                                        val intent = Intent(context, ContactsActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        }
                                        context.startActivity(intent)
                                    }
                                    1 -> {}
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
                .background(Color(0xFFF4F7FC))
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = appColor)
                }
            } else {
                // Header Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Welcome back 👋",
                            fontFamily = outfitFontFamily,
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            text = state.username.ifBlank { "Valued User" },
                            fontFamily = outfitFontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White)
                            .size(44.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (state.unreadNotificationsCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFDC2626),
                                        contentColor = Color.White
                                    ) {
                                        val badgeText = if (state.unreadNotificationsCount > 9) "9+" else state.unreadNotificationsCount.toString()
                                        Text(text = badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.notificationsicon),
                                contentDescription = "Notifications",
                                tint = Color(0xFF1F2937),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Current Balance",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "E£${state.balance}",
                                    color = Color(0xFF111827),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Column {
                                Text(
                                    text = "Account Number",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (state.isAccountVisible) state.accountNumber else "•••• •••• ••••",
                                        color = Color(0xFF4B5563),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(
                                        painter = painterResource(id = R.drawable.visibility),
                                        contentDescription = "Toggle visibility",
                                        tint = appColor,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .clickable { onIntent(MainIntent.ToggleAccountVisibility) }
                                    )
                                }
                            }
                        }

                        Image(
                            painter = painterResource(id = R.drawable.card_logo),
                            contentDescription = "Card Logo",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .width(90.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionItem(iconRes = R.drawable.transfer_money, title = "Transfer", onClick = onNavigateToTransfer)
                    ActionItem(iconRes = R.drawable.withdraw_ic, title = "Withdraw", onClick = onNavigateToWithdraw)
                    ActionItem(iconRes = R.drawable.deposit_ic, title = "Deposit", onClick = onNavigateToDeposit)
                    ActionItem(iconRes = R.drawable.onpayicon, title = "onPay", onClick = onNavigateToOnPay)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activities",
                        fontFamily = outfitFontFamily,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )

                    if (state.transactions.isNotEmpty()) {
                        TextButton(
                            onClick = { onIntent(MainIntent.SetAllTransactionsVisible(true)) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "See All",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Recent Transactions List
                if (state.recentTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent activities found.",
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    state.recentTransactions.forEach { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onClick = { onIntent(MainIntent.SelectTransaction(transaction)) },
                            state = MainState()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(
    transactions: List<TransactionResponse>,
    onBackClick: () -> Unit,
    onTransactionClick: (TransactionResponse) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Transactions", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F7FC))
                .padding(paddingValues)
        ) {
            if (transactions.isEmpty()) {
                Text(
                    text = "No transactions found.",
                    color = Color(0xFF9CA3AF),
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = transactions,
                        key = { it.transactionId }
                    ) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction) },
                            state = MainState()
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsSheet(
    transaction: TransactionResponse,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val transactionColor = getTransactionColor(transaction.type)
    val isWithdrawal = transaction.type.equals("withdrawal", ignoreCase = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E7EB))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Transaction Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${if (isWithdrawal) "-" else "+"}E£${transaction.amount}",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = transactionColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when (transaction.status.uppercase()) {
                    "SUCCESS", "COMPLETED" -> Color(0xFFDCFCE7)
                    "PENDING" -> Color(0xFFFEF3C7)
                    else -> Color(0xFFFEE2E2)
                }
            ) {
                Text(
                    text = transaction.status.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (transaction.status.uppercase()) {
                        "SUCCESS", "COMPLETED" -> Color(0xFF166534)
                        "PENDING" -> Color(0xFF92400E)
                        else -> Color(0xFF991B1B)
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(label = "Type", value = transaction.type)
            DetailRow(label = "Receiver / Source", value = transaction.receiverUsername ?: "N/A")
            DetailRow(label = "Date & Time", value = transaction.timestamp)

            DetailRowWithCopy(
                label = "Transaction ID",
                value = transaction.transactionId,
                onCopy = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Transaction ID", transaction.transactionId))
                    Toast.makeText(context, "Transaction ID copied!", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF447AE3))
            ) {
                Text("Close", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFF6B7280), fontSize = 14.sp)
        Text(text = value, color = Color(0xFF111827), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DetailRowWithCopy(label: String, value: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFF6B7280), fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (value.length > 12) "${value.take(12)}..." else value,
                color = Color(0xFF111827),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_copy),
                contentDescription = "Copy",
                tint = Color(0xFF447AE3),
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable { onCopy() }
            )
        }
    }
}


@Composable
fun TransactionItem(
    transaction: TransactionResponse,
    state: MainState,
    onClick: () -> Unit = {}
) {
    val transactionColor = getTransactionColor(transaction.type)
    val isWithdrawal = transaction.type.equals("withdrawal", ignoreCase = true)
    val isTransfer = transaction.type.equals("transfer", ignoreCase = true)
    val transferToMe = transaction.receiverUsername?.equals(state.username)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val amountText = when {
                isTransfer -> "E£${transaction.amount}"
                isWithdrawal -> "-E£${transaction.amount}"
                else -> "+E£${transaction.amount}"
            }
            val transactionSymbol = when {
                isTransfer -> "→"
                isWithdrawal -> "↑"
                else -> "↓"
            }
            val receiverUsername = when{
                transferToMe == true -> "Me"
                else -> transaction.receiverUsername ?: "N/A"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(transactionColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = transactionSymbol,
                        color = transactionColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.type.replaceFirstChar { it.uppercase() },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    if (!transaction.receiverUsername.isNullOrEmpty()) {
                        Text(
                            text = "To: ${receiverUsername}",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = transactionColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = transaction.status,
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


@Composable
fun ActionItem(iconRes: Int, title: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(28.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF374151)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    state: MainState,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F7FC))
                .padding(paddingValues)
        ) {
            if (state.isLoadingNotifications) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF447AE3)
                )
            } else if (state.notificationsList.isEmpty()) {
                Text(
                    text = "No notifications yet.",
                    color = Color(0xFF9CA3AF),
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = state.notificationsList,
                        key = { it.id ?: it.title }
                    ) { notification ->
                        NotificationItem(notification = notification)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: NotificationResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead) Color(0xFFEFF6FF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF2563EB), shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    fontSize = 13.sp,
                    color = Color(0xFF4B5563)
                )
            }
        }
    }
}

@Composable
fun getTransactionColor(type: String): Color {
    return when (type.lowercase().trim()) {
        "deposit" -> Color(0xFF16A34A)
        "withdrawal" -> Color(0xFFDC2626)
        "transfer" -> Color(0xFF2563EB)
        else -> Color.Gray
    }
}

// =============================================================
// 📸 PREVIEWS FOR EVERY COMPOSABLE
// =============================================================

@Preview(showBackground = true, device = Devices.PIXEL_6)
@Composable
fun MainScreenPreview() {
    PayOnAppTheme {
        MainScreen(
            state = MainState(
                isLoading = false,
                username = "Yousef Mohammed",
                balance = "12,450.00",
                isAccountVisible = true,
                accountNumber = "8492048591",
                transactions = listOf(
                    TransactionResponse("1", 500.0, "Deposit", null, "SUCCESS", "key1", "10:30 AM"),
                    TransactionResponse("2", 150.0, "Withdrawal", "ATM", "COMPLETED", "key2", "11:15 AM")
                ),
                unreadNotificationsCount = 3
            ),
            onIntent = {},
            onNavigateToTransfer = {},
            onNavigateToWithdraw = {},
            onNavigateToDeposit = {},
            onNavigateToOnPay = {},
            onNavigateToNotifications = {}
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_6)
@Composable
fun AllTransactionsScreenPreview() {
    PayOnAppTheme {
        AllTransactionsScreen(
            transactions = listOf(
                TransactionResponse("1", 500.0, "Deposit", null, "SUCCESS", "key1", "10:30 AM"),
                TransactionResponse("2", 150.0, "Withdrawal", "Omar", "COMPLETED", "key2", "11:15 AM"),
                TransactionResponse("3", 1200.0, "Transfer", "Ali", "SUCCESS", "key3", "02:00 PM")
            ),
            onBackClick = {},
            onTransactionClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionDetailsSheetPreview() {
    PayOnAppTheme {
        TransactionDetailsSheet(
            transaction = TransactionResponse(
                transactionId = "TXN-984201840219",
                amount = 1250.0,
                type = "Transfer",
                receiverUsername = "Omar Bakry",
                status = "SUCCESS",
                idempotencyKey = "IDEM-849201",
                timestamp = "2026-10-13 14:32:00"
            ),
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DetailRowPreview() {
    PayOnAppTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailRow(label = "Status", value = "Completed")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetailRowWithCopyPreview() {
    PayOnAppTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailRowWithCopy(label = "Transaction ID", value = "TXN-1234567890", onCopy = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionItemPreview() {
    PayOnAppTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TransactionItem(
                transaction = TransactionResponse(
                    transactionId = "1",
                    amount = 350.0,
                    type = "Transfer",
                    receiverUsername = "Ashraf",
                    status = "SUCCESS",
                    idempotencyKey = "key1",
                    timestamp = "Just now"
                ),
                state = MainState(),
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActionItemPreview() {
    PayOnAppTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            ActionItem(
                iconRes = R.drawable.transfer_money,
                title = "Transfer",
                onClick = {}
            )
        }
    }
}