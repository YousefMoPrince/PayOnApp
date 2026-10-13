package com.jo.payonapp.reposervice

import com.jo.payonapp.api.MerchantManager
import com.jo.payonapp.dto.MerchantResponse
import com.jo.payonapp.dto.MerchantType
import com.jo.payonapp.local.PayOnDatabase

class SqliteMerchantManager(
    database: PayOnDatabase
) : MerchantManager {
    private val queries = database.merchantEntityQueries

    override fun saveMerchant(merchant: MerchantResponse) {
        queries.insertOrUpdateMerchant(
            merchantId = merchant.merchantId,
            merchantUserName = merchant.merchantUserName,
            merchantUrl = merchant.merchantUrl,
            accountNumber = merchant.accountNumber,
            merchantType = merchant.merchantType.name
        )
    }

    override fun getMerchant(): MerchantResponse? {
        val entity = queries.getMerchant().executeAsOneOrNull() ?: return null
        return MerchantResponse(
            merchantId = entity.merchantId,
            merchantUserName = entity.merchantUserName,
            merchantUrl = entity.merchantUrl,
            accountNumber = entity.accountNumber,
            merchantType = MerchantType.valueOf(entity.merchantType)
        )
    }

    override fun clearMerchant() {
        queries.clearMerchant()
    }
}