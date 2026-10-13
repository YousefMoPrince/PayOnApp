package com.jo.payonapp.reposervice

import com.jo.payonapp.api.ReceiptManager
import com.jo.payonapp.api.WalletManager
import com.jo.payonapp.dto.CreatedReceiptResponse
import com.jo.payonapp.dto.ReceiptResponse
import com.jo.payonapp.dto.WalletResponse
import com.jo.payonapp.network.PayOnApiService
import com.jo.payonapp.repository.OnPayRepository

class OnPayRepositoryImpl(
    private val apiService: PayOnApiService,
    private val walletManager: WalletManager,
    private val receiptManager: ReceiptManager
) : OnPayRepository {

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

    override suspend fun getReceipt(transactionCode: Long): Result<ReceiptResponse> {
        return runCatching {
            val response = apiService.getReceipt(transactionCode)
            val receiptData = response.data ?: throw Exception(response.message)
            receiptManager.saveReceipt(receiptData)
            receiptData
        }.recoverCatching { localException ->
            receiptManager.getReceipt(transactionCode) ?: throw localException
        }
    }

    override suspend fun getCreatedReceipt(transactionId: String): Result<CreatedReceiptResponse> {
        return runCatching {
            val response = apiService.getCreatedReceipt(transactionId)
            receiptManager.saveCreatedReceipt(response)
            response
        }
    }

    override suspend fun payReceipt(transactionCode: Long): Result<String> {
        return runCatching {
            val response = apiService.payReceipt(transactionCode)
            response.data ?: throw Exception(response.message)
        }
    }
}