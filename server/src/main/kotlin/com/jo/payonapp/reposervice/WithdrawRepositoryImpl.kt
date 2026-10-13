package com.jo.payonapp.reposervice

import com.jo.payonapp.api.TransactionManager
import com.jo.payonapp.api.WalletManager
import com.jo.payonapp.dto.TransactionRequest
import com.jo.payonapp.dto.TransactionResponse
import com.jo.payonapp.dto.WalletResponse
import com.jo.payonapp.network.PayOnApiService
import com.jo.payonapp.repository.WithdrawRepository

class WithdrawRepositoryImpl(
    private val apiService: PayOnApiService,
    private val transactionManager: TransactionManager,
    private val walletManager: WalletManager
) : WithdrawRepository {

    override suspend fun getWalletData(): Result<WalletResponse> {
        val cached = walletManager.getWallet()
        if (cached != null) {
            return Result.success(cached)
        }
        return refreshWalletData()
    }

    override suspend fun refreshWalletData(): Result<WalletResponse> {
        return runCatching {
            val response = apiService.getWalletInfo()
            val walletData = response.data ?: throw Exception(response.message)
            walletManager.saveWallet(walletData)
            walletData
        }.recoverCatching { networkException ->
            walletManager.getWallet() ?: throw networkException
        }
    }

    override suspend fun withdrawTransaction(
        idempotencyKey: String,
        withdrawRequest: TransactionRequest
    ): Result<TransactionResponse> {
        return runCatching {
            val response = apiService.withdraw(idempotencyKey, withdrawRequest)
            val txData = response.data ?: throw Exception(response.message)
            transactionManager.saveTransaction(txData)
            txData
        }
    }
}