package com.jo.payonapp.repository

import com.jo.payonapp.dto.AuthResponse
import com.jo.payonapp.dto.TransactionResponse
import com.jo.payonapp.dto.WalletResponse

interface HomeRepository {
    suspend fun getUserProfile(): Result<AuthResponse>
    suspend fun refreshUserProfile(): Result<AuthResponse>

    suspend fun getTransactions(): Result<List<TransactionResponse>>
    suspend fun refreshTransactionsInfo(): Result<List<TransactionResponse>>
}