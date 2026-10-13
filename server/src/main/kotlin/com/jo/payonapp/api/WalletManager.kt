package com.jo.payonapp.api

import com.jo.payonapp.dto.WalletResponse

interface WalletManager {
    fun saveWallet(wallet: WalletResponse)
    fun getWallet(): WalletResponse?
    fun clearWallet()
}