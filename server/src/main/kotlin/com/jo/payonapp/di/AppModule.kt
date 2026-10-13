package com.jo.payonapp.di

import com.jo.payonapp.api.*
import com.jo.payonapp.local.DatabaseDriverFactory
import com.jo.payonapp.local.PayOnDatabase
import com.jo.payonapp.network.PayOnApiService
import com.jo.payonapp.reposervice.*
import com.jo.payonapp.repository.*
import com.jo.payonapp.service.NotificationWebSocketService
import com.jo.payonapp.viewmodel.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val managerModule = module {
    single<TokenManager> { SqliteTokenManager(get()) }
    single<ContactManager> { SqliteContactManager(get()) }
    single<MerchantManager> { SqliteMerchantManager(get()) }
    single<NotificationManager> { SqliteNotificationManager(get()) }
    single<ReceiptManager> { SqliteReceiptManager(get()) }
    single<TransactionManager> { SqliteTransactionManager(get()) }
    single<WalletManager> { SqliteWalletManager(get()) }
}

val networkModule = module {
    single<HttpClientEngine> { OkHttp.create() }
    single { HttpClientFactory(get()) }
    single<HttpClient> { get<HttpClientFactory>().createAuthHttpClient(get()) }
    single<PayOnApiService> { PayOnServices(get()) }
    single {
        NotificationWebSocketService(
            client = get(),
            tokenManager = get()
        )
    }
}

val repositoryModule = module {
    single<AccountRepository> {
        AccountRepositoryImpl(
            apiService = get(),
            tokenManager = get(),
            localImageStorage = get()
        )
    }
    single<AuthRepository> {
        AuthRepositoryImpl(
            get(), get(), get(), get(),
            client = get()
        )
    }
    single<ContactsRepository> { ContactsRepositoryImpl(get(), get()) }
    single<DepositRepository> { DepositRepositoryImpl(get(), get(), walletManager = get()) }
    single<HomeRepository> {
        HomeRepositoryImpl(
            get(),
            get(),
            transactionManager = get()
        )
    }
    single<InformationRepository> {
        InformationRepositoryImpl(
            get(), get(),
            authRepository = get(),
            localImageStorage = get()
        )
    }
    single<MerchantRepository> {
        MerchantRepositoryImpl(
            get(), get(),
            receiptManager = get()
        )
    }
    single<NotificationRepository> {
        NotificationRepositoryImpl(
            get(), get(),
            notificationManager = get()
        )
    }
    single<OnPayRepository> { OnPayRepositoryImpl(get(), get(), receiptManager = get()) }
    single<ReceiptRepository> { ReceiptRepositoryImpl(get(), get()) }
    single<TransferRepository> { TransferRepositoryImpl(get(), get(), walletManager = get()) }
    single<WalletRepository> { WalletRepositoryImpl(get(), get()) }
    single<WithdrawRepository> { WithdrawRepositoryImpl(get(), get(), walletManager = get()) }
    single<TransactionRepository> { TransactionRepositoryImpl(get(), get(), transactionManager = get()) }
}

val jvmDatabaseModule = module {
    single { DatabaseDriverFactory() }
    single<PayOnDatabase> { PayOnDatabase(get<DatabaseDriverFactory>().createDriver()) }
}

val viewModelModule = module {
    factory { SignInViewModel(get(), get()) }
    factory {
        SignUpViewModel(
            get(),
            walletRepository = get()
        )
    }
    factory {
        MainViewModel(
            get(),
            notificationRepository = get(),
            walletRepository = get()
        )
    }
    factory { AccountViewModel(get()) }
    factory { ContactsViewModel(get()) }
    factory { DepositViewModel(get(), get()) }
    factory { InformationViewModel(
        get(),
        authRepository = get()
    ) }
    factory { MerchantViewModel(get()) }
    factory { OnPayViewModel(get()) }
    factory { TransferViewModel(
        get(),
        walletRepository = get()
    ) }
    factory { UploadThumbnailViewModel(get()) }
    factory { WithdrawViewModel(
        get(),
        walletRepository = get()
    ) }
}

val serverModules = listOf(
    managerModule,
    networkModule,
    repositoryModule,
    viewModelModule,
    jvmDatabaseModule
)

fun initKoin(
    platformModules: List<Module> = emptyList(),
    appDeclaration: KoinAppDeclaration = {}
) = startKoin {
    appDeclaration()
    modules(serverModules + platformModules)
}