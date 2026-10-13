package com.jo.payonapp.di

import android.content.Context
import com.jo.payonapp.lifecycle.AppLifecycleObserver
import com.jo.payonapp.local.DatabaseDriverFactory
import com.jo.payonapp.local.PayOnDatabase
import com.jo.payonapp.notification.PayOnNotificationHelper
import com.jo.payonapp.storage.LocalImageStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidDatabaseModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single<PayOnDatabase> { PayOnDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single { PayOnNotificationHelper(androidContext()) }
    single { LocalImageStorage(baseDirectoryPath = androidContext().filesDir.absolutePath) }
    single { AppLifecycleObserver(authRepository = get(), externalScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)) }
}