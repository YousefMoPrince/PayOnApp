package com.jo.payonapp.repository

import com.jo.payonapp.dto.TransactionResponse

interface TransactionRepository {
    suspend fun getTransaction(id: String): Result<TransactionResponse>
}