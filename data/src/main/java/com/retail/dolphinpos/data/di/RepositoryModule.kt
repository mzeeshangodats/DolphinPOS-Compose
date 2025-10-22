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
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.service.ImageDownloadService
import com.retail.dolphinpos.domain.repositories.auth.CashDenominationRepository
import com.retail.dolphinpos.domain.repositories.auth.LoginRepository
import com.retail.dolphinpos.domain.repositories.auth.StoreRegistersRepository
import com.retail.dolphinpos.domain.repositories.auth.VerifyPinRepository
import com.retail.dolphinpos.domain.repositories.home.HomeRepository
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
        userDao: UserDao
    ): VerifyPinRepository {
        return VerifyPinRepositoryImpl(userDao)
    }

    @Provides
    @Singleton
    fun provideCashDenominationRepository(
        userDao: UserDao
    ): CashDenominationRepository {
        return CashDenominationRepositoryImpl(userDao)
    }

    @Provides
    @Singleton
    fun provideHomeRepository(
        productsDao: ProductsDao,
        customerDao: CustomerDao,
        storeRegistersRepository: StoreRegistersRepository
    ): HomeRepository {
        return HomeRepositoryImpl(productsDao, customerDao, storeRegistersRepository)
    }

}