package com.jo.payonapp.reposervice

import com.jo.payonapp.api.TransactionManager
import com.jo.payonapp.api.WalletManager
import com.jo.payonapp.dto.TransactionRequest
import com.jo.payonapp.dto.TransactionResponse
import com.jo.payonapp.dto.WalletResponse
import com.jo.payonapp.network.PayOnApiService
import com.jo.payonapp.repository.DepositRepository

class DepositRepositoryImpl(
    private val apiService: PayOnApiService,
    private val transactionManager: TransactionManager,
    private val walletManager: WalletManager
) : DepositRepository {
    override suspend fun depositTransaction(
        idempotencyKey: String,
        depositRequest: TransactionRequest
    ): Result<TransactionResponse> {
        return runCatching {
            val response = apiService.deposit(idempotencyKey, depositRequest)
            val txData = response.data ?: throw Exception(response.message)
            transactionManager.saveTransaction(txData)
            txData
        }
    }
}