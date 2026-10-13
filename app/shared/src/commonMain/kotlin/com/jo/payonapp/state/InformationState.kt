package com.jo.payonapp.state

data class InformationState(
    val isLoading: Boolean = false,
    val error: String? = null,

    val originalUsername: String = "",
    val originalFullName: String = "",
    val originalEmail: String = "",
    val originalPhoneNumber: String = "",
    val originalThumbnailPath: String? = null,
    val profileImageBytes: ByteArray? = null,

    val draftUsername: String = "",
    val draftFullName: String = "",
    val draftEmail: String = "",
    val draftPhoneNumber: String = "",
    val draftPassword: String = "",
    val selectedImageUriString: String? = null,

    val isUpdateSuccess: Boolean = false
) {
    val isSaveButtonEnabled: Boolean get() =
        draftUsername != originalUsername ||
                draftFullName != originalFullName ||
                draftEmail != originalEmail ||
                draftPhoneNumber != originalPhoneNumber ||
                selectedImageUriString != null
}