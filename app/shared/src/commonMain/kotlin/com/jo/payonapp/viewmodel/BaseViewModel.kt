package com.jo.payonapp.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

abstract class BaseViewModel {
    protected val viewModelScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    open fun onCleared() {
        viewModelScope.cancel()
    }
}