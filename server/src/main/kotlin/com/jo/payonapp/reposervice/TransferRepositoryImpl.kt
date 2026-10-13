package com.jo.payonapp.reposervice

import com.jo.payonapp.api.TransactionManager
import com.jo.payonapp.api.WalletManager
import com.jo.payonapp.dto.Status
import com.jo.payonapp.dto.TransactionRequest
import com.jo.payonapp.dto.TransactionResponse
import com.jo.payonapp.dto.WalletResponse
import com.jo.payonapp.network.PayOnApiService
import com.jo.payonapp.repository.TransferRepository

class TransferRepositoryImpl(
    private val apiService: PayOnApiService,
    private val transactionManager: TransactionManager,
    private val walletManager: WalletManager
) : TransferRepository {

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

    override suspend fun transferTransaction(
        idempotencyKey: String,
        transferRequest: TransactionRequest
    ): Result<TransactionResponse> {
        return runCatching {
            val response = apiService.transfer(idempotencyKey, transferRequest)
            val txData = response.data ?: throw Exception(response.message)
            transactionManager.saveTransaction(txData)
            txData
        }
    }

    override suspend fun transferAcknowledgement(
        idempotencyKey: String,
        status: Status
    ): Result<TransactionResponse> {
        return runCatching {
            val response = apiService.transferAck(idempotencyKey, status)
            val txData = response.data ?: throw Exception(response.message)
            transactionManager.saveTransaction(txData)
            txData
        }
    }
}