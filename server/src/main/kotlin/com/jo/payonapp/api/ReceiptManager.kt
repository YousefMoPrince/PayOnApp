package com.jo.payonapp.api

import com.jo.payonapp.dto.CreatedReceiptResponse
import com.jo.payonapp.dto.ReceiptResponse

interface ReceiptManager {
    fun saveCreatedReceipt(receipt: CreatedReceiptResponse)
    fun saveCreatedReceipts(receipts: List<CreatedReceiptResponse>)
    fun saveReceipt(receipt: ReceiptResponse)
    fun saveReceipts(receipts: List<ReceiptResponse>)
    fun getReceipt(transactionCode: Long): ReceiptResponse?
    fun getCreatedReceipt(transactionCode: Long): CreatedReceiptResponse?
    fun getCreatedReceipts(): List<CreatedReceiptResponse>
    fun getReceipts(): List<ReceiptResponse>
    fun clearReceipts()
}