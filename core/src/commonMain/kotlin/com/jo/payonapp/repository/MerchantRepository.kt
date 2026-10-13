package com.jo.payonapp.repository

import com.jo.payonapp.dto.BecomeMerchantRequest
import com.jo.payonapp.dto.CreateReceiptRequest
import com.jo.payonapp.dto.CreatedReceiptResponse
import com.jo.payonapp.dto.MerchantResponse
import com.jo.payonapp.dto.ReceiptResponse

interface MerchantRepository {
    suspend fun getMerchantData(): Result<MerchantResponse>
    suspend fun refreshMerchantData(): Result<MerchantResponse>
    suspend fun becomeMerchant(request: BecomeMerchantRequest): Result<MerchantResponse>
    suspend fun createReceipt(request: CreateReceiptRequest): Result<CreatedReceiptResponse>
    suspend fun getMerchantReceipts(): Result<List<CreatedReceiptResponse>>
}