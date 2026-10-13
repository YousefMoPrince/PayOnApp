package com.jo.payonapp.intent

import com.jo.payonapp.dto.CreateReceiptRequest

sealed class MerchantIntent {
    object FetchMerchantData : MerchantIntent()
    object RefreshMerchantData : MerchantIntent()
    data class MerchantUserNameChanged(val merchantUserName: String) : MerchantIntent()
    data class MerchantAccountNumberChanged(val merchantAccountNumber: Long) : MerchantIntent()
    object SubmitBecomeMerchant : MerchantIntent()
    data class CreateReceipt(val request: CreateReceiptRequest) : MerchantIntent()
    object FetchReceipts : MerchantIntent()
}