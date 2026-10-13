package com.jo.payonapp

import androidx.compose.ui.graphics.Color

data class Transaction(
    val type: String,
    val amount: String,
    val status: String,
    val statusColor: Color
)