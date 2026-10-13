package com.jo.payonapp.intent

sealed class InformationIntent {
    object FetchAccountData : InformationIntent()
    object RefreshData : InformationIntent()
    data class UsernameChanged(val username: String) : InformationIntent()
    data class FullNameChanged(val fullName: String) : InformationIntent()
    data class EmailChanged(val email: String) : InformationIntent()
    data class PhoneNumberChanged(val phone: String) : InformationIntent()
    data class ImageSelected(val imageUriString: String) : InformationIntent()
    data class UploadClicked(val fileBytes: ByteArray, val fileName: String) : InformationIntent()
    object UpdateData : InformationIntent()
    object LogOut : InformationIntent()
}