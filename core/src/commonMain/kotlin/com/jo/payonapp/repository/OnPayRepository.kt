package com.jo.payonapp.repository

import com.jo.payonapp.dto.CreatedReceiptResponse
import com.jo.payonapp.dto.ReceiptResponse
import com.jo.payonapp.dto.WalletResponse

interface OnPayRepository {
    suspend fun getWalletData(): Result<WalletResponse>
    suspend fun refreshWalletData(): Result<WalletResponse>
    suspend fun getReceipt(transactionCode: Long): Result<ReceiptResponse>
    suspend fun getCreatedReceipt(transactionId: String): Result<CreatedReceiptResponse>
    suspend fun payReceipt(transactionCode: Long): Result<String>
}