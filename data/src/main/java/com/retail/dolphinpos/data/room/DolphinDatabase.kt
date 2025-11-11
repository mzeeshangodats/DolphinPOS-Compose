package com.retail.dolphinpos.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.retail.dolphinpos.data.entities.transaction.PaymentMethodConverter
import com.retail.dolphinpos.data.dao.CustomerDao
import com.retail.dolphinpos.data.dao.HoldCartDao
import com.retail.dolphinpos.data.dao.OnlineOrderDao
import com.retail.dolphinpos.data.dao.OrderDao
import com.retail.dolphinpos.data.dao.PendingOrderDao
import com.retail.dolphinpos.data.dao.ProductsDao
import com.retail.dolphinpos.data.dao.TransactionDao
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.entities.category.CategoryEntity
import com.retail.dolphinpos.data.entities.customer.CustomerEntity
import com.retail.dolphinpos.data.entities.holdcart.HoldCartEntity
import com.retail.dolphinpos.data.entities.order.OnlineOrderEntity
import com.retail.dolphinpos.data.entities.order.OrderEntity
import com.retail.dolphinpos.data.entities.order.PendingOrderEntity
import com.retail.dolphinpos.data.entities.products.CachedImageEntity
import com.retail.dolphinpos.data.entities.transaction.TransactionEntity
import com.retail.dolphinpos.data.entities.products.ProductImagesEntity
import com.retail.dolphinpos.data.entities.products.ProductsEntity
import com.retail.dolphinpos.data.entities.products.VariantImagesEntity
import com.retail.dolphinpos.data.entities.products.VariantsEntity
import com.retail.dolphinpos.data.entities.products.VendorEntity
import com.retail.dolphinpos.data.entities.user.ActiveUserDetailsEntity
import com.retail.dolphinpos.data.entities.user.BatchEntity
import com.retail.dolphinpos.data.entities.user.LocationEntity
import com.retail.dolphinpos.data.entities.user.RegisterEntity
import com.retail.dolphinpos.data.entities.user.RegisterStatusEntity
import com.retail.dolphinpos.data.entities.user.StoreEntity
import com.retail.dolphinpos.data.entities.user.StoreLogoUrlEntity
import com.retail.dolphinpos.data.entities.user.UserEntity
import com.retail.dolphinpos.data.entities.user.TimeSlotEntity

@Database(
    entities = [UserEntity::class, StoreEntity::class, StoreLogoUrlEntity::class, LocationEntity::class, RegisterEntity::class,
        ActiveUserDetailsEntity::class, BatchEntity::class, RegisterStatusEntity::class, CategoryEntity::class, ProductsEntity::class,
        ProductImagesEntity::class, VariantsEntity::class, VariantImagesEntity::class, VendorEntity::class, CustomerEntity::class,
        CachedImageEntity::class, HoldCartEntity::class, PendingOrderEntity::class, OnlineOrderEntity::class, OrderEntity::class, TransactionEntity::class, TimeSlotEntity::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(PaymentMethodConverter::class)
abstract class DolphinDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun productsDao(): ProductsDao
    abstract fun customerDao(): CustomerDao
    abstract fun holdCartDao(): HoldCartDao
    abstract fun pendingOrderDao(): PendingOrderDao
    abstract fun onlineOrderDao(): OnlineOrderDao
    abstract fun orderDao(): OrderDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: DolphinDatabase? = null

        fun getDatabase(context: Context): DolphinDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, DolphinDatabase::class.java, "dolphin_retail_pos"
                ).addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA foreign_keys = ON;")
                    }
                })
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
//                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }


        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_orders ADD COLUMN transaction_id TEXT")
                db.execSQL("ALTER TABLE pending_orders ADD COLUMN split_transactions TEXT")
                db.execSQL("ALTER TABLE pending_orders ADD COLUMN card_details TEXT")
                db.execSQL("ALTER TABLE online_orders ADD COLUMN transaction_id TEXT")
                db.execSQL("ALTER TABLE online_orders ADD COLUMN split_transactions TEXT")
                db.execSQL("ALTER TABLE online_orders ADD COLUMN card_details TEXT")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create unified orders table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS orders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        order_no TEXT NOT NULL,
                        invoice_no TEXT,
                        server_id INTEGER,
                        customer_id INTEGER,
                        store_id INTEGER NOT NULL,
                        location_id INTEGER NOT NULL,
                        store_register_id INTEGER,
                        batch_no TEXT,
                        payment_method TEXT NOT NULL,
                        transaction_id TEXT,
                        order_items TEXT NOT NULL,
                        sub_total REAL NOT NULL,
                        total REAL NOT NULL,
                        apply_tax INTEGER NOT NULL DEFAULT 1,
                        tax_value REAL NOT NULL,
                        discount_amount REAL NOT NULL DEFAULT 0.0,
                        cash_discount_amount REAL NOT NULL DEFAULT 0.0,
                        reward_discount REAL NOT NULL DEFAULT 0.0,
                        is_synced INTEGER NOT NULL DEFAULT 0,
                        order_source TEXT NOT NULL,
                        status TEXT,
                        is_redeemed INTEGER NOT NULL DEFAULT 0,
                        source TEXT NOT NULL DEFAULT 'point-of-sale',
                        redeem_points INTEGER,
                        discount_ids TEXT,
                        split_transactions TEXT,
                        card_details TEXT,
                        user_id INTEGER NOT NULL DEFAULT 0,
                        void_reason TEXT,
                        is_void INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                """.trimIndent())

                // Migrate data from online_orders table
                db.execSQL("""
                    INSERT INTO orders (
                        order_no, invoice_no, customer_id, store_id, location_id, 
                        store_register_id, batch_no, payment_method, transaction_id,
                        order_items, sub_total, total, apply_tax, tax_value,
                        discount_amount, cash_discount_amount, reward_discount,
                        is_synced, order_source, status, is_redeemed, source,
                        redeem_points, discount_ids, split_transactions, card_details,
                        user_id, void_reason, is_void, created_at, updated_at
                    )
                    SELECT 
                        o.order_no, o.invoice_no, o.customer_id, o.store_id, o.location_id,
                        o.store_register_id, o.batch_no, o.payment_method, o.transaction_id,
                        o.order_items, o.sub_total, o.total, o.apply_tax, o.tax_value,
                        o.discount_amount, o.cash_discount_amount, o.reward_discount,
                        1, 'api', 'completed', o.is_redeemed, o.source,
                        o.redeem_points, o.discount_ids, o.split_transactions, o.card_details,
                        o.user_id, o.void_reason, o.is_void, o.created_at, o.created_at
                    FROM online_orders o
                    WHERE NOT EXISTS (
                        SELECT 1 FROM orders ord WHERE ord.order_no = o.order_no
                    )
                """.trimIndent())

                // Migrate data from pending_orders table
                // Only insert orders that don't already exist (check by order_no)
                db.execSQL("""
                    INSERT INTO orders (
                        order_no, invoice_no, customer_id, store_id, location_id,
                        store_register_id, batch_no, payment_method, transaction_id,
                        order_items, sub_total, total, apply_tax, tax_value,
                        discount_amount, cash_discount_amount, reward_discount,
                        is_synced, order_source, status, is_redeemed, source,
                        redeem_points, discount_ids, split_transactions, card_details,
                        user_id, void_reason, is_void, created_at, updated_at
                    )
                    SELECT 
                        p.order_no, p.invoice_no, p.customer_id, p.store_id, p.location_id,
                        p.store_register_id, p.batch_no, p.payment_method, p.transaction_id,
                        p.order_items, p.sub_total, p.total, p.apply_tax, p.tax_value,
                        p.discount_amount, p.cash_discount_amount, p.reward_discount,
                        p.is_synced, 'local', 
                        CASE WHEN p.is_synced = 1 THEN 'completed' ELSE 'pending' END,
                        p.is_redeemed, p.source,
                        p.redeem_points, p.discount_ids, p.split_transactions, p.card_details,
                        p.user_id, p.void_reason, p.is_void, p.created_at, p.created_at
                    FROM pending_orders p
                    WHERE NOT EXISTS (
                        SELECT 1 FROM orders o WHERE o.order_no = p.order_no
                    )
                """.trimIndent())
                
                // Remove duplicate orders by order_no
                // Keep the order with the lowest id (first inserted) for each order_no
                db.execSQL("""
                    DELETE FROM orders 
                    WHERE id IN (
                        SELECT o2.id 
                        FROM orders o1
                        INNER JOIN orders o2 ON o1.order_no = o2.order_no
                        WHERE o1.id < o2.id
                    )
                """.trimIndent())

                // Create index on server_id for faster lookups
                db.execSQL("CREATE INDEX IF NOT EXISTS index_orders_server_id ON orders(server_id)")
                // Create index on order_no for faster lookups and duplicate prevention
                db.execSQL("CREATE INDEX IF NOT EXISTS index_orders_order_no ON orders(order_no)")
                // Create index on order_source and is_synced for faster queries
                db.execSQL("CREATE INDEX IF NOT EXISTS index_orders_source_synced ON orders(order_source, is_synced)")
                // Create index on store_id for faster queries
                db.execSQL("CREATE INDEX IF NOT EXISTS index_orders_store_id ON orders(store_id)")
            }
        }

    }
}