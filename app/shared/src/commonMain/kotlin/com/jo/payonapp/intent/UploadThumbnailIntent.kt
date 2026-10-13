package com.jo.payonapp.intent


sealed class UploadThumbnailIntent {
    data class ImageSelected(val uriString: String) : UploadThumbnailIntent()
    data class UploadClicked(val fileBytes: ByteArray, val fileName: String) : UploadThumbnailIntent()
    object SkipClicked : UploadThumbnailIntent()
}