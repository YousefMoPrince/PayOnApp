package com.jo.payonapp.repository

import com.jo.payonapp.dto.WalletResponse

interface WalletRepository {
    suspend fun createWallet(): Result<WalletResponse>
    suspend fun getWalletInfo(): Result<WalletResponse>
    suspend fun refreshWalletInfo(): Result<WalletResponse>
}