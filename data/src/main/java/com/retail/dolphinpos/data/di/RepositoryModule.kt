package com.retail.dolphinpos.data.di

import android.content.Context
import com.google.gson.Gson
import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.data.dao.BatchReportDao
import com.retail.dolphinpos.data.dao.CustomerDao
import com.retail.dolphinpos.data.dao.OrderDao
import com.retail.dolphinpos.data.dao.ProductsDao
import com.retail.dolphinpos.data.dao.RefundDao
import com.retail.dolphinpos.data.dao.SyncCommandDao
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.repositories.auth.CashDenominationRepositoryImpl
import com.retail.dolphinpos.data.repositories.auth.LoginRepositoryImpl
import com.retail.dolphinpos.data.repositories.auth.StoreRegisterRepositoryImpl
import com.retail.dolphinpos.data.repositories.auth.VerifyPinRepositoryImpl
import com.retail.dolphinpos.data.repositories.home.HomeRepositoryImpl
import com.retail.dolphinpos.data.repositories.orders_details.OrdersRepositoryImpl
import com.retail.dolphinpos.data.repositories.report.BatchReportRepositoryImpl
import com.retail.dolphinpos.data.repositories.setup.HardwareSetupRepositoryImpl
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.service.ImageDownloadService
import com.retail.dolphinpos.data.customer_display.CustomerDisplayManager
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.domain.repositories.auth.CashDenominationRepository
import com.retail.dolphinpos.domain.repositories.auth.LoginRepository
import com.retail.dolphinpos.domain.repositories.auth.StoreRegistersRepository
import com.retail.dolphinpos.domain.repositories.auth.VerifyPinRepository
import com.retail.dolphinpos.domain.repositories.home.HomeRepository
import com.retail.dolphinpos.domain.repositories.home.OrdersRepository
import com.retail.dolphinpos.domain.repositories.product.ProductRepository
import com.retail.dolphinpos.domain.repositories.report.BatchReportRepository
import com.retail.dolphinpos.domain.repositories.refund.RefundRepository
import com.retail.dolphinpos.domain.repositories.setup.HardwareSetupRepository
import com.retail.dolphinpos.data.repositories.product.ProductRepositoryImpl
import com.retail.dolphinpos.data.repositories.refund.RefundLocalDataSource
import com.retail.dolphinpos.data.repositories.refund.RefundRemoteDataSource
import com.retail.dolphinpos.data.repositories.refund.RefundRepositoryImpl
import com.retail.dolphinpos.domain.usecases.sync.ScheduleSyncUseCase
import com.retail.dolphinpos.data.usecases.sync.ScheduleSyncUseCaseImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideLoginRepository(api: ApiService, userDao: UserDao, gson: Gson): LoginRepository {
        return LoginRepositoryImpl(api, userDao, gson)
    }

    @Provides
    @Singleton
    fun provideImageDownloadService(
        @ApplicationContext context: Context
    ): ImageDownloadService {
        return ImageDownloadService(context)
    }

    @Provides
    @Singleton
    fun provideStoreRegisterRepository(
        api: ApiService, userDao: UserDao, productsDao: ProductsDao, imageDownloadService: ImageDownloadService, gson: Gson
    ): StoreRegistersRepository {
        return StoreRegisterRepositoryImpl(api, userDao, productsDao, imageDownloadService, gson)
    }

    @Provides
    @Singleton
    fun provideVerifyPinRepository(
        userDao: UserDao,
        api: ApiService,
        gson: Gson
    ): VerifyPinRepository {
        return VerifyPinRepositoryImpl(userDao, api, gson)
    }

    @Provides
    @Singleton
    fun provideCashDenominationRepository(
        userDao: UserDao,
        apiService: ApiService
    ): CashDenominationRepository {
        return CashDenominationRepositoryImpl(userDao, apiService)
    }

    @Provides
    @Singleton
    fun provideHomeRepository(
        productsDao: ProductsDao,
        customerDao: CustomerDao,
        userDao: UserDao,
        storeRegistersRepository: StoreRegistersRepository,
        apiService: ApiService,
        networkMonitor: NetworkMonitor
    ): HomeRepository {
        return HomeRepositoryImpl(productsDao, customerDao, userDao, storeRegistersRepository, apiService, networkMonitor)
    }

    @Provides
    @Singleton
    fun provideProductRepository(
        productsDao: ProductsDao,
        apiService: ApiService,
        gson: Gson
    ): ProductRepository {
        return ProductRepositoryImpl(productsDao, apiService, gson)
    }

    @Provides
    @Singleton
    fun provideOrdersRepository(
        apiService: ApiService
    ): OrdersRepository {
        return OrdersRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideHardwareSetupRepository(
        @ApplicationContext context: Context
    ): HardwareSetupRepository {
        return HardwareSetupRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideBatchReportRepository(
        apiService: ApiService,
        userDao: UserDao,
        orderDao: OrderDao,
        batchReportDao: BatchReportDao,
        networkMonitor: NetworkMonitor
    ): BatchReportRepository {
        return BatchReportRepositoryImpl(apiService, userDao, orderDao, batchReportDao, networkMonitor)
    }

    @Provides
    @Singleton
    fun provideCustomerDisplayManager(
        @ApplicationContext context: Context,
        gson: Gson,
        preferenceManager: PreferenceManager
    ): CustomerDisplayManager {
        return CustomerDisplayManager(context, gson, preferenceManager)
    }

    @Provides
    @Singleton
    fun provideScheduleSyncUseCase(): ScheduleSyncUseCase {
        return ScheduleSyncUseCaseImpl()
    }
    
    @Provides
    @Singleton
    fun provideRefundLocalDataSource(refundDao: RefundDao): RefundLocalDataSource {
        return RefundLocalDataSource(refundDao)
    }
    
    @Provides
    @Singleton
    fun provideRefundRemoteDataSource(apiService: ApiService): RefundRemoteDataSource {
        return RefundRemoteDataSource(apiService)
    }
    
    @Provides
    @Singleton
    fun provideRefundRepository(
        localDataSource: RefundLocalDataSource,
        remoteDataSource: RefundRemoteDataSource,
        orderDao: OrderDao,
        productsDao: ProductsDao,
        syncCommandDao: SyncCommandDao,
        networkMonitor: NetworkMonitor,
        scheduleSyncUseCase: ScheduleSyncUseCase,
        gson: Gson
    ): RefundRepository {
        return RefundRepositoryImpl(
            localDataSource,
            remoteDataSource,
            orderDao,
            productsDao,
            syncCommandDao,
            networkMonitor,
            scheduleSyncUseCase,
            gson
        )
    }

}