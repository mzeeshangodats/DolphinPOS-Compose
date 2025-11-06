package com.retail.dolphinpos.data.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.retail.dolphinpos.data.dao.CustomerDao
import com.retail.dolphinpos.data.dao.HoldCartDao
import com.retail.dolphinpos.data.dao.OnlineOrderDao
import com.retail.dolphinpos.data.dao.PendingOrderDao
import com.retail.dolphinpos.data.dao.ProductsDao
import com.retail.dolphinpos.data.dao.TransactionDao
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.repositories.hold_cart.HoldCartRepository
import com.retail.dolphinpos.data.repositories.online_order.OnlineOrderRepository
import com.retail.dolphinpos.data.repositories.pending_order.PendingOrderRepositoryImpl
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
        )
//            .addMigrations(
//                DolphinDatabase.MIGRATION_1_2,
//                DolphinDatabase.MIGRATION_2_3,
//                DolphinDatabase.MIGRATION_3_4,
//                DolphinDatabase.MIGRATION_4_5
//            )
            .build()
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

    @Provides
    fun providePendingOrderDao(database: DolphinDatabase): PendingOrderDao {
        return database.pendingOrderDao()
    }

    @Provides
    @Singleton
    fun providePendingOrderRepository(
        pendingOrderDao: PendingOrderDao,
        apiService: com.retail.dolphinpos.data.service.ApiService,
        gson: Gson
    ): PendingOrderRepositoryImpl {
        return PendingOrderRepositoryImpl(pendingOrderDao, apiService, gson)
    }

    @Provides
    fun provideOnlineOrderDao(database: DolphinDatabase): OnlineOrderDao {
        return database.onlineOrderDao()
    }

    @Provides
    @Singleton
    fun provideOnlineOrderRepository(
        onlineOrderDao: OnlineOrderDao,
        gson: Gson
    ): OnlineOrderRepository {
        return OnlineOrderRepository(onlineOrderDao, gson)
    }

    @Provides
    fun provideTransactionDao(database: DolphinDatabase): TransactionDao {
        return database.transactionDao()
    }
}