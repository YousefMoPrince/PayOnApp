package com.jo.payonapp.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.jo.payonapp.dto.UserStatus
import com.jo.payonapp.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AppLifecycleObserver(
    private val authRepository: AuthRepository,
    private val externalScope: CoroutineScope
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        externalScope.launch {
            authRepository.updateUserStatus(UserStatus.ONLINE)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        externalScope.launch {
            authRepository.updateUserStatus(UserStatus.OFFLINE)
        }
    }
}