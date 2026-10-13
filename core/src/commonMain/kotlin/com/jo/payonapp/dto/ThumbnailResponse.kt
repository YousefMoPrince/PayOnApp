package com.jo.payonapp.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ThumbnailResponse(
    @SerialName("thumbnailName")
    val thumbnailName: String,
    @SerialName("thumbnailPath")
    val thumbnailPath: String
)
