package com.jo.payonapp.repository

import com.jo.payonapp.dto.CreateReceiptRequest
import com.jo.payonapp.dto.CreatedReceiptResponse
import com.jo.payonapp.dto.ReceiptResponse

interface ReceiptRepository {
    suspend fun createReceipt(request: CreateReceiptRequest): Result<CreatedReceiptResponse>
    suspend fun getReceipts(): Result<List<ReceiptResponse>>
    suspend fun refreshReceipts(): Result<List<ReceiptResponse>>
}