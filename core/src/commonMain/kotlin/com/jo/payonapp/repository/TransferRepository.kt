package com.jo.payonapp.repository

import com.jo.payonapp.dto.Status
import com.jo.payonapp.dto.TransactionRequest
import com.jo.payonapp.dto.TransactionResponse
import com.jo.payonapp.dto.WalletResponse

interface TransferRepository {
    suspend fun getWalletData(): Result<WalletResponse>
    suspend fun refreshWalletData(): Result<WalletResponse>
    suspend fun transferTransaction(
        idempotencyKey: String,
        transferRequest: TransactionRequest
    ): Result<TransactionResponse>
    suspend fun transferAcknowledgement(
        idempotencyKey: String,
        status: Status
    ): Result<TransactionResponse>
}