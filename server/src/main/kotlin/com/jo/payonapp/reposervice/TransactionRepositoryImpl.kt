package com.jo.payonapp.reposervice

import com.jo.payonapp.api.TransactionManager
import com.jo.payonapp.api.WalletManager
import com.jo.payonapp.dto.TransactionResponse
import com.jo.payonapp.network.PayOnApiService
import com.jo.payonapp.repository.TransactionRepository

class TransactionRepositoryImpl(
    private val apiService: PayOnApiService,
    private val walletManager: WalletManager,
    private val transactionManager: TransactionManager
    ): TransactionRepository {

    override suspend fun getTransaction(id: String): Result<TransactionResponse> {
        return runCatching {
            val response = apiService.getTransaction(id)
            val response1 = apiService.getWalletInfo()
            val walletData = response1.data ?: throw Exception(response1.message) as Throwable
            walletManager.saveWallet(walletData)
            transactionManager.saveTransaction(response)
            response
        }
    }

}