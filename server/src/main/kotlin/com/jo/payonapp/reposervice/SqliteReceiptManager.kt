package com.jo.payonapp.reposervice

import com.jo.payonapp.api.ReceiptManager
import com.jo.payonapp.dto.CreateReceiptRequest
import com.jo.payonapp.dto.CreatedReceiptResponse
import com.jo.payonapp.dto.CreatedReceiptResponseData
import com.jo.payonapp.dto.Currency
import com.jo.payonapp.dto.MerchantType
import com.jo.payonapp.dto.ReceiptResponse
import com.jo.payonapp.dto.ReceiptStatus
import com.jo.payonapp.local.PayOnDatabase
import com.jo.payonapp.local.ReceiptEntity
import java.util.UUID

class SqliteReceiptManager(
    database: PayOnDatabase
) : ReceiptManager {
    private val queries = database.receiptEntityQueries

    override fun saveCreatedReceipt(receipt: CreatedReceiptResponse) {
        val data = receipt.data
        queries.insertOrUpdateReceipt(
            transactionCode = data.transactionCode,
            transactionId = data.transactionId,
            clientName = data.clientName,
            merchantUserName = data.merchantUserName,
            merchantType = data.merchantType.name,
            amount = data.amount,
            accountNumber = data.accountNumber,
            payerAccountNumber = data.payerAccountNumber,
            currency = data.currency.name,
            idempotencyKey = data.idempotencyKey,
            state = receipt.state.name,
            createdAt = data.createdAt,
            updatedAt = data.updatedAt
        )
    }

    override fun saveCreatedReceipts(receipts: List<CreatedReceiptResponse>) {
        queries.transaction {
            receipts.forEach { saveCreatedReceipt(it) }
        }
    }

    override fun saveReceipt(receipt: ReceiptResponse) {
        queries.insertOrUpdateReceipt(
            transactionCode = receipt.transactionCode,
            transactionId = receipt.transactionId,
            merchantUserName = receipt.merchantUserName,
            amount = receipt.amount,
            currency = receipt.currency,
            clientName = null,
            merchantType = null,
            accountNumber = null,
            payerAccountNumber = null,
            idempotencyKey = null,
            state = null,
            createdAt = null,
            updatedAt = null
        )
    }

    override fun saveReceipts(receipts: List<ReceiptResponse>) {
        queries.transaction {
            receipts.forEach { saveReceipt(it) }
        }
    }

    override fun getReceipt(transactionCode: Long): ReceiptResponse? {
        val entity = queries.getReceiptByCode(transactionCode).executeAsOneOrNull() ?: return null
        return mapToReceiptResponse(entity)
    }

    override fun getReceipts(): List<ReceiptResponse> {
        return queries.getAllReceipts().executeAsList().map { mapToReceiptResponse(it) }
    }

    override fun getCreatedReceipt(transactionCode: Long): CreatedReceiptResponse? {
        val entity = queries.getReceiptByCode(transactionCode).executeAsOneOrNull() ?: return null
        return mapToCreatedReceiptResponse(entity)
    }

    override fun getCreatedReceipts(): List<CreatedReceiptResponse> {
        return queries.getAllReceipts().executeAsList()
            .filter { it.clientName != null }
            .map { mapToCreatedReceiptResponse(it) }
    }

    override fun clearReceipts() {
        queries.clearReceipts()
    }

    private fun mapToReceiptResponse(entity: ReceiptEntity): ReceiptResponse {
        return ReceiptResponse(
            transactionCode = entity.transactionCode,
            transactionId = entity.transactionId,
            merchantUserName = entity.merchantUserName,
            amount = entity.amount,
            currency = entity.currency
        )
    }

    private fun mapToCreatedReceiptResponse(entity: ReceiptEntity): CreatedReceiptResponse {
        return CreatedReceiptResponse(
            state = entity.state?.let { ReceiptStatus.valueOf(it) } ?: ReceiptStatus.PENDING,
            data = CreatedReceiptResponseData(
                transactionId = entity.transactionId,
                clientName = entity.clientName ?: "",
                merchantUserName = entity.merchantUserName,
                merchantType = entity.merchantType?.let { MerchantType.valueOf(it) } ?: MerchantType.INDIVIDUAL,
                amount = entity.amount,
                accountNumber = entity.accountNumber ?: 0L,
                payerAccountNumber = entity.payerAccountNumber ?: 0L,
                currency = try { Currency.valueOf(entity.currency) } catch (e: Exception) { Currency.EGP },
                transactionCode = entity.transactionCode,
                idempotencyKey = entity.idempotencyKey?: UUID.randomUUID().toString(),
                createdAt = entity.createdAt ?: "",
                updatedAt = entity.updatedAt ?: ""
            )
        )
    }
}