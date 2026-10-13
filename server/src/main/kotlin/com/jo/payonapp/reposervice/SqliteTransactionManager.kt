package com.jo.payonapp.reposervice

import com.jo.payonapp.api.TransactionManager
import com.jo.payonapp.dto.TransactionResponse
import com.jo.payonapp.local.PayOnDatabase

class SqliteTransactionManager(
    database: PayOnDatabase
) : TransactionManager {
    private val queries = database.transactionEntityQueries

    override fun saveTransaction(tx: TransactionResponse) {
        queries.insertOrUpdateTransaction(
            transactionId = tx.transactionId,
            amount = tx.amount,
            type = tx.type,
            receiverUsername = tx.receiverUsername,
            status = tx.status,
            idempotencyKey = tx.idempotencyKey?:"",
            timestamp = tx.timestamp
        )
    }

    override fun saveTransactions(transactions: List<TransactionResponse>) {
        queries.transaction {
            transactions.forEach { saveTransaction(it) }
        }
    }

    override fun getTransactions(): List<TransactionResponse> {
        return queries.getTransactions().executeAsList().map { entity ->
            TransactionResponse(
                transactionId = entity.transactionId,
                amount = entity.amount,
                type = entity.type,
                receiverUsername = entity.receiverUsername,
                status = entity.status,
                idempotencyKey = entity.idempotencyKey,
                timestamp = entity.timestamp
            )
        }
    }

    override fun clearTransactions() {
        queries.clearTransactions()
    }
}