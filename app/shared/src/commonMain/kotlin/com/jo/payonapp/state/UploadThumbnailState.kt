package com.jo.payonapp.state


data class UploadThumbnailState(
    val selectedImageUri: String? = null,
    val isLoading: Boolean = false,
    val currentStatusMessage: String? = null,
    val errorMessage: String? = null
)