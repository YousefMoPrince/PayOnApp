package com.jo.payonapp.api

import com.jo.payonapp.dto.TransactionResponse

interface TransactionManager {
    fun saveTransaction(tx: TransactionResponse)
    fun saveTransactions(transactions: List<TransactionResponse>)
    fun getTransactions(): List<TransactionResponse>
    fun clearTransactions()
}