package com.jo.payonapp.intent

sealed class AccountIntent {
    object FetchAccountData : AccountIntent()
    object RefreshData : AccountIntent()
    object LogOutClicked : AccountIntent()
}