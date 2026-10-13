package com.jo.payonapp.reposervice

import com.jo.payonapp.api.WalletManager
import com.jo.payonapp.dto.WalletResponse
import com.jo.payonapp.network.PayOnApiService
import com.jo.payonapp.repository.WalletRepository

class WalletRepositoryImpl(
    private val apiService: PayOnApiService,
    private val walletManager: WalletManager
) : WalletRepository {

    override suspend fun createWallet(): Result<WalletResponse> {
        return runCatching {
            val response = apiService.createWallet()
            val walletData = response.data ?: throw Exception(response.message)
            walletManager.saveWallet(walletData)
            walletData
        }
    }

    override suspend fun getWalletInfo(): Result<WalletResponse> {
        return runCatching {
            val walletData = walletManager.getWallet()?: throw Exception("Wallet not found")
            walletData
        }.recoverCatching {
            refreshWalletInfo().getOrNull() ?: throw it
        }
    }

    override suspend fun refreshWalletInfo(): Result<WalletResponse> {
        return runCatching {
            val response = apiService.getWalletInfo()
            val walletData = response.data ?: throw Exception(response.message)
            walletManager.saveWallet(walletData)
            walletData
        }.recoverCatching {
            walletManager.getWallet() ?: throw it
        }
    }
}