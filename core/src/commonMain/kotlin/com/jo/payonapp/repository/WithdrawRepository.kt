package com.jo.payonapp.repository

import com.jo.payonapp.dto.TransactionRequest
import com.jo.payonapp.dto.TransactionResponse
import com.jo.payonapp.dto.WalletResponse

interface WithdrawRepository {
    suspend fun getWalletData(): Result<WalletResponse>
    suspend fun refreshWalletData(): Result<WalletResponse>
    suspend fun withdrawTransaction(
        idempotencyKey: String,
        withdrawRequest: TransactionRequest
    ): Result<TransactionResponse>
}