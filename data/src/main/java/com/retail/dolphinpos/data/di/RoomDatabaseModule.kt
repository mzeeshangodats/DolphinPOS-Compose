package com.retail.dolphinpos.data.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.retail.dolphinpos.data.dao.CustomerDao
import com.retail.dolphinpos.data.dao.HoldCartDao
import com.retail.dolphinpos.data.dao.OnlineOrderDao
import com.retail.dolphinpos.data.dao.OrderDao
import com.retail.dolphinpos.data.dao.PendingOrderDao
import com.retail.dolphinpos.data.dao.ProductsDao
import com.retail.dolphinpos.data.dao.BatchReportDao
import com.retail.dolphinpos.data.dao.CreateOrderTransactionDao
import com.retail.dolphinpos.data.dao.TransactionDao
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.dao.SyncCommandDao
import com.retail.dolphinpos.data.dao.SyncLockDao
import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.data.repositories.hold_cart.HoldCartRepository
import com.retail.dolphinpos.data.repositories.online_order.OnlineOrderRepository
import com.retail.dolphinpos.data.repositories.order.OrderRepositoryImpl
import com.retail.dolphinpos.data.repositories.pending_order.PendingOrderRepositoryImpl
import com.retail.dolphinpos.data.repositories.sync.PosSyncRepository
import com.retail.dolphinpos.data.repositories.transaction.TransactionRepositoryImpl
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.domain.repositories.transaction.TransactionRepository
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
        return DolphinDatabase.getDatabase(context)
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
        productsDao: ProductsDao,
        gson: Gson
    ): PendingOrderRepositoryImpl {
        return PendingOrderRepositoryImpl(pendingOrderDao, apiService, productsDao, gson)
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
    fun provideCreateOrderTransactionDao(database: DolphinDatabase): CreateOrderTransactionDao {
        return database.createOrderTransactionDao()
    }

    @Provides
    fun provideTransactionDao(database: DolphinDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideOrderDao(database: DolphinDatabase): OrderDao {
        return database.orderDao()
    }

    @Provides
    fun provideBatchReportDao(database: DolphinDatabase): BatchReportDao {
        return database.batchReportDao()
    }

    @Provides
    fun provideSyncCommandDao(database: DolphinDatabase): SyncCommandDao {
        return database.syncCommandDao()
    }

    @Provides
    fun provideSyncLockDao(database: DolphinDatabase): SyncLockDao {
        return database.syncLockDao()
    }

    @Provides
    @Singleton
    fun provideOrderRepository(
        orderDao: OrderDao,
        apiService: ApiService,
        productsDao: ProductsDao,
        gson: Gson
    ): OrderRepositoryImpl {
        return OrderRepositoryImpl(orderDao, apiService, productsDao, gson)
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(
        apiService: ApiService,
        transactionDao: TransactionDao,
        networkMonitor: NetworkMonitor,
        gson: Gson
    ): TransactionRepository {
        return TransactionRepositoryImpl(apiService, transactionDao, networkMonitor, gson)
    }

    @Provides
    @Singleton
    fun providePosSyncRepository(
        database: DolphinDatabase
    ): PosSyncRepository {
        return PosSyncRepository(database)
    }
}