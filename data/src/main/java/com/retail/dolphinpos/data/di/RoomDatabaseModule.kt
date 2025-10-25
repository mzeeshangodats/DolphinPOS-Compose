package com.retail.dolphinpos.data.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.retail.dolphinpos.data.dao.CustomerDao
import com.retail.dolphinpos.data.dao.HoldCartDao
import com.retail.dolphinpos.data.dao.ProductsDao
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.repository.HoldCartRepository
import com.retail.dolphinpos.data.room.DolphinDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): DolphinDatabase {
        return Room.databaseBuilder(
            context, DolphinDatabase::class.java, "dolphin_retail_pos"
        ).build()
    }

    @Provides
    fun provideUserDao(database: DolphinDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideProductsDao(database: DolphinDatabase): ProductsDao {
        return database.productsDao()
    }

    @Provides
    fun provideCustomersDao(database: DolphinDatabase): CustomerDao {
        return database.customerDao()
    }

    @Provides
    fun provideHoldCartDao(database: DolphinDatabase): HoldCartDao {
        return database.holdCartDao()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideHoldCartRepository(
        holdCartDao: HoldCartDao,
        gson: Gson
    ): HoldCartRepository {
        return HoldCartRepository(holdCartDao, gson)
    }
}