package com.jo.payonapp.api

import com.jo.payonapp.dto.MerchantResponse

interface MerchantManager {
    fun saveMerchant(merchant: MerchantResponse)
    fun getMerchant(): MerchantResponse?
    fun clearMerchant()
}