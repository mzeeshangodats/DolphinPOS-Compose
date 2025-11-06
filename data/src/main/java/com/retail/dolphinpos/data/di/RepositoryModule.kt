package com.retail.dolphinpos.data.di

import android.content.Context
import com.retail.dolphinpos.data.dao.CustomerDao
import com.retail.dolphinpos.data.dao.ProductsDao
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
import com.retail.dolphinpos.domain.repositories.auth.CashDenominationRepository
import com.retail.dolphinpos.domain.repositories.auth.LoginRepository
import com.retail.dolphinpos.domain.repositories.auth.StoreRegistersRepository
import com.retail.dolphinpos.domain.repositories.auth.VerifyPinRepository
import com.retail.dolphinpos.domain.repositories.home.HomeRepository
import com.retail.dolphinpos.domain.repositories.home.OrdersRepository
import com.retail.dolphinpos.domain.repositories.report.BatchReportRepository
import com.retail.dolphinpos.domain.repositories.setup.HardwareSetupRepository
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
    fun provideLoginRepository(api: ApiService, userDao: UserDao): LoginRepository {
        return LoginRepositoryImpl(api, userDao)
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
        api: ApiService, userDao: UserDao, productsDao: ProductsDao, imageDownloadService: ImageDownloadService
    ): StoreRegistersRepository {
        return StoreRegisterRepositoryImpl(api, userDao, productsDao, imageDownloadService)
    }

    @Provides
    @Singleton
    fun provideVerifyPinRepository(
        userDao: UserDao,
        api: ApiService
    ): VerifyPinRepository {
        return VerifyPinRepositoryImpl(userDao, api)
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
        storeRegistersRepository: StoreRegistersRepository
    ): HomeRepository {
        return HomeRepositoryImpl(productsDao, customerDao, userDao, storeRegistersRepository)
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
        userDao: UserDao
    ): BatchReportRepository {
        return BatchReportRepositoryImpl(apiService, userDao)
    }

}