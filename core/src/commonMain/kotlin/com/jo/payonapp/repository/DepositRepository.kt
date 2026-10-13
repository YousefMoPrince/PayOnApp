package com.jo.payonapp.repository

import com.jo.payonapp.dto.TransactionRequest
import com.jo.payonapp.dto.TransactionResponse
import com.jo.payonapp.dto.WalletResponse

interface DepositRepository {
    suspend fun depositTransaction(
        idempotencyKey: String,
        depositRequest: TransactionRequest
    ): Result<TransactionResponse>
}