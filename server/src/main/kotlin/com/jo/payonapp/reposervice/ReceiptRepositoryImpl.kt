package com.jo.payonapp.reposervice

import com.jo.payonapp.api.ReceiptManager
import com.jo.payonapp.dto.CreateReceiptRequest
import com.jo.payonapp.dto.CreatedReceiptResponse
import com.jo.payonapp.dto.ReceiptResponse
import com.jo.payonapp.network.PayOnApiService
import com.jo.payonapp.repository.ReceiptRepository

class ReceiptRepositoryImpl(
    private val apiService: PayOnApiService,
    private val receiptManager: ReceiptManager
) : ReceiptRepository {

    override suspend fun createReceipt(request: CreateReceiptRequest): Result<CreatedReceiptResponse> {
        return runCatching {
            val response = apiService.createReceipt(request)
            val receiptData = response.data ?: throw Exception(response.message)
            receiptManager.saveCreatedReceipt(receiptData)
            receiptData
        }
    }

    override suspend fun getReceipts(): Result<List<ReceiptResponse>> {
        val cached = receiptManager.getReceipts()
        if (cached.isNotEmpty()) {
            return Result.success(cached)
        }
        return refreshReceipts()
    }

    override suspend fun refreshReceipts(): Result<List<ReceiptResponse>> {
        return runCatching {
            val list = apiService.getReceipts()
            receiptManager.saveReceipts(list)
            list
        }.recoverCatching { networkException ->
            val cached = receiptManager.getReceipts()
            if (cached.isNotEmpty()) cached else throw networkException
        }
    }
}