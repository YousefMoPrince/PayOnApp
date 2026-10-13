package com.jo.payonapp.reposervice

import com.jo.payonapp.api.MerchantManager
import com.jo.payonapp.api.ReceiptManager
import com.jo.payonapp.dto.BecomeMerchantRequest
import com.jo.payonapp.dto.CreateReceiptRequest
import com.jo.payonapp.dto.CreatedReceiptResponse
import com.jo.payonapp.dto.MerchantResponse
import com.jo.payonapp.dto.ReceiptResponse
import com.jo.payonapp.network.PayOnApiService
import com.jo.payonapp.repository.MerchantRepository

class MerchantRepositoryImpl(
    private val apiService: PayOnApiService,
    private val merchantManager: MerchantManager,
    private val receiptManager: ReceiptManager
) : MerchantRepository {

    override suspend fun getMerchantData(): Result<MerchantResponse> {
        val cached = merchantManager.getMerchant()
        if (cached != null) {
            return Result.success(cached)
        }
        return refreshMerchantData()
    }

    override suspend fun refreshMerchantData(): Result<MerchantResponse> {
        return runCatching {
            val response = apiService.getMerchantInfo()
            val merchantData = response.data ?: throw Exception(response.message)
            merchantManager.saveMerchant(merchantData)
            merchantData
        }.recoverCatching { networkException ->
            merchantManager.getMerchant() ?: throw networkException
        }
    }

    override suspend fun becomeMerchant(request: BecomeMerchantRequest): Result<MerchantResponse> {
        return runCatching {
            val response = apiService.becomeMerchant(request)
            val merchantData = response.data ?: throw Exception(response.message)
            merchantManager.saveMerchant(merchantData)
            merchantData
        }
    }

    override suspend fun createReceipt(request: CreateReceiptRequest): Result<CreatedReceiptResponse> {
        return runCatching {
            val response = apiService.createReceipt(request)
            val receiptData = response.data ?: throw Exception(response.message)
            receiptManager.saveCreatedReceipt(receiptData)
            receiptData
        }
    }

    override suspend fun getMerchantReceipts(): Result<List<CreatedReceiptResponse>> {
        return runCatching {
            val response = apiService.getMerchantReceipts()
            val list = response.data ?: emptyList()
            receiptManager.saveCreatedReceipts(list)
            list
        }.recoverCatching { networkException ->
            val cached = receiptManager.getCreatedReceipts()
            cached.ifEmpty { throw networkException }
        }
    }
}
