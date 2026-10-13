package com.jo.payonapp

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import coil3.ImageLoader
import com.jo.payonapp.di.androidDatabaseModule
import com.jo.payonapp.di.initKoin
import com.jo.payonapp.lifecycle.AppLifecycleObserver
import com.jo.payonapp.notification.PayOnNotificationHelper
import com.jo.payonapp.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext

class PayOnApplication : Application() {

    lateinit var imageLoader: ImageLoader
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val notificationRepository: NotificationRepository by inject()
    private val notificationHelper: PayOnNotificationHelper by inject()
    private val appLifecycleObserver: AppLifecycleObserver by inject()
    override fun onCreate() {
        super.onCreate()

        imageLoader = ImageLoader.Builder(this).build()

        initKoin(platformModules = listOf(androidDatabaseModule)) {
            androidContext(this@PayOnApplication)
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        observeGlobalNotifications()
    }
        fun observeGlobalNotifications() {
             applicationScope.launch {
                 notificationRepository.observeRealTimeNotifications().collect { notification ->
                     notificationHelper.makeNotification(
                         notificationId = notification.id ?: System.currentTimeMillis().toString(),
                         title = notification.title.ifBlank { "PayOn Alert" },
                         eventType = notification.eventType,
                         message = notification.message
                     )

                     notification.id?.takeIf { it.isNotBlank() }?.let { id ->
                         runCatching { notificationRepository.getNotification(id) }
                     }
                 }
             }
         }
    }


