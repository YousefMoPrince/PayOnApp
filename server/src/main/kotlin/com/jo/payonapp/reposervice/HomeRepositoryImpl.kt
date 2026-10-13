package com.jo.payonapp.reposervice

import com.jo.payonapp.api.TokenManager
import com.jo.payonapp.api.TransactionManager
import com.jo.payonapp.api.WalletManager
import com.jo.payonapp.dto.AuthResponse
import com.jo.payonapp.dto.TransactionResponse
import com.jo.payonapp.dto.WalletResponse
import com.jo.payonapp.network.PayOnApiService
import com.jo.payonapp.repository.HomeRepository
import com.jo.payonapp.repository.WalletRepository

class HomeRepositoryImpl(
    private val apiService: PayOnApiService,
    private val tokenManager: TokenManager,
    private val transactionManager: TransactionManager
) : HomeRepository {

    override suspend fun getUserProfile(): Result<AuthResponse> {
        val cached = tokenManager.getSession()
        if (cached != null) {
            return Result.success(cached)
        }
        return refreshUserProfile()
    }

    override suspend fun refreshUserProfile(): Result<AuthResponse> {
        return runCatching {
            val response = apiService.getUserProfile()
            val userData = response.data ?: throw Exception(response.message)
            tokenManager.updateProfile(userData)
            userData
        }.recoverCatching { networkException ->
            tokenManager.getSession() ?: throw networkException
        }
    }

    override suspend fun getTransactions(): Result<List<TransactionResponse>> {
        val cached = transactionManager.getTransactions()
        if (cached.isNotEmpty()) {
            return Result.success(cached)
        }
        return refreshTransactionsInfo()
    }

    override suspend fun refreshTransactionsInfo(): Result<List<TransactionResponse>> {
        return runCatching {
            val response = apiService.getTransactions()
            val list = response.data ?: emptyList()
            transactionManager.saveTransactions(list)
            list
        }.recoverCatching { networkException ->
            val cached = transactionManager.getTransactions()
            cached.ifEmpty { throw networkException }
        }
    }
}