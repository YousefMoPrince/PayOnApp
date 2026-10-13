package com.jo.payonapp.reposervice

import com.jo.payonapp.api.WalletManager
import com.jo.payonapp.dto.WalletResponse
import com.jo.payonapp.dto.WalletStatus
import com.jo.payonapp.local.PayOnDatabase

class SqliteWalletManager(
    database: PayOnDatabase
) : WalletManager {
    private val queries = database.walletEntityQueries

    override fun saveWallet(wallet: WalletResponse) {
        queries.insertOrUpdateWallet(
            walletId = wallet.walletId,
            userId = wallet.userId,
            balance = wallet.balance,
            accountNumber = wallet.accountNumber,
            walletStatus = wallet.walletStatus.name
        )
    }

    override fun getWallet(): WalletResponse? {
        val entity = queries.getWallet().executeAsOneOrNull() ?: return null
        return WalletResponse(
            walletId = entity.walletId,
            userId = entity.userId,
            balance = entity.balance,
            accountNumber = entity.accountNumber,
            walletStatus = WalletStatus.valueOf(entity.walletStatus)
        )
    }

    override fun clearWallet() {
        queries.clearWallet()
    }
}