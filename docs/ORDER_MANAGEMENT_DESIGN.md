# Order Management Design - Single Table Approach

## Overview
Unified order management using a single `orders` table to handle both API-fetched orders and locally created orders.

## Database Schema

### OrderEntity (Single Table)
```kotlin
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Order identification
    @ColumnInfo(name = "order_no") val orderNumber: String,
    @ColumnInfo(name = "invoice_no") val invoiceNo: String? = null,
    @ColumnInfo(name = "server_id") val serverId: Int? = null,  // API order ID
    
    // Customer & Store info
    @ColumnInfo(name = "customer_id") val customerId: Int? = null,
    @ColumnInfo(name = "store_id") val storeId: Int,
    @ColumnInfo(name = "location_id") val locationId: Int,
    @ColumnInfo(name = "store_register_id") val storeRegisterId: Int? = null,
    @ColumnInfo(name = "batch_no") val batchNo: String? = null,
    
    // Payment info
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    @ColumnInfo(name = "transaction_id") val transactionId: String? = null,
    
    // Order items (JSON)
    @ColumnInfo(name = "order_items") val items: String,
    
    // Pricing
    @ColumnInfo(name = "sub_total") val subTotal: Double,
    val total: Double,
    @ColumnInfo(name = "apply_tax") val applyTax: Boolean = true,
    @ColumnInfo(name = "tax_value") val taxValue: Double,
    @ColumnInfo(name = "discount_amount") val discountAmount: Double = 0.0,
    @ColumnInfo(name = "cash_discount_amount") val cashDiscountAmount: Double = 0.0,
    @ColumnInfo(name = "reward_discount") val rewardDiscount: Double = 0.0,
    
    // Status & Sync
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,  // 1 = completed, 0 = pending
    @ColumnInfo(name = "order_source") val orderSource: String,     // "api" or "local"
    @ColumnInfo(name = "status") val status: String? = null,        // "completed" or "pending"
    
    // Other fields
    @ColumnInfo(name = "is_redeemed") val isRedeemed: Boolean = false,
    val source: String = "point-of-sale",
    @ColumnInfo(name = "redeem_points") val redeemPoints: Int? = null,
    @ColumnInfo(name = "discount_ids") val discountIds: String? = null,
    @ColumnInfo(name = "split_transactions") val transactions: String? = null,
    @ColumnInfo(name = "card_details") val cardDetails: String? = null,
    @ColumnInfo(name = "user_id") val userId: Int = 0,
    @ColumnInfo(name = "void_reason") val voidReason: String? = null,
    @ColumnInfo(name = "is_void") val isVoid: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
```

## Implementation Logic

### 1. When Online (Internet Available)

#### Fetching Orders from API
```kotlin
// Fetch orders from API
val apiOrders = ordersRepository.getOrdersDetails(...)

// Save to local database
apiOrders.forEach { order ->
    val entity = OrderEntity(
        serverId = order.id,
        orderNumber = order.orderNumber,
        orderSource = "api",
        isSynced = order.isSynced,  // From API: 1 = completed, 0 = pending
        status = if (order.isSynced == 1) "completed" else "pending",
        // ... other fields
    )
    orderDao.insertOrUpdate(entity)  // Upsert based on serverId
}
```

#### Display Logic
```kotlin
// Query orders
val allOrders = orderDao.getAllOrders()

// Group by status
val completedOrders = allOrders.filter { it.isSynced == true }  // isSynced = 1
val pendingOrders = allOrders.filter { it.isSynced == false }   // isSynced = 0
```

### 2. When Offline (No Internet)

#### Display Logic
```kotlin
// Query all orders from local database
val allOrders = orderDao.getAllOrders()

// Group by status (from local data)
val completedOrders = allOrders.filter { it.status == "completed" || it.isSynced == true }
val pendingOrders = allOrders.filter { it.status == "pending" || it.isSynced == false }
```

### 3. Creating Local Orders (Offline)

```kotlin
// When creating order offline
val localOrder = OrderEntity(
    orderNumber = generateLocalOrderNumber(),
    orderSource = "local",
    isSynced = false,  // Not synced yet
    status = "pending",
    // ... other fields
)
orderDao.insertOrder(localOrder)
```

### 4. Syncing Local Orders (When Online)

```kotlin
// Get unsynced local orders
val unsyncedOrders = orderDao.getUnsyncedLocalOrders()

// Sync to server
unsyncedOrders.forEach { order ->
    val result = syncOrderToServer(order)
    if (result.isSuccess) {
        // Update order
        orderDao.updateOrder(
            order.copy(
                isSynced = true,
                status = "completed",
                serverId = result.data.id
            )
        )
    }
}
```

## Database Queries

### OrderDao
```kotlin
@Dao
interface OrderDao {
    // Get all orders
    @Query("SELECT * FROM orders ORDER BY created_at DESC")
    suspend fun getAllOrders(): List<OrderEntity>
    
    // Get orders by status
    @Query("SELECT * FROM orders WHERE is_synced = :isSynced ORDER BY created_at DESC")
    suspend fun getOrdersBySyncStatus(isSynced: Boolean): List<OrderEntity>
    
    // Get orders by source
    @Query("SELECT * FROM orders WHERE order_source = :source ORDER BY created_at DESC")
    suspend fun getOrdersBySource(source: String): List<OrderEntity>
    
    // Get unsynced local orders
    @Query("SELECT * FROM orders WHERE order_source = 'local' AND is_synced = 0")
    suspend fun getUnsyncedLocalOrders(): List<OrderEntity>
    
    // Get order by server ID (for API orders)
    @Query("SELECT * FROM orders WHERE server_id = :serverId")
    suspend fun getOrderByServerId(serverId: Int): OrderEntity?
    
    // Upsert order
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateOrder(order: OrderEntity): Long
    
    // Update order
    @Update
    suspend fun updateOrder(order: OrderEntity)
    
    // Delete order
    @Delete
    suspend fun deleteOrder(order: OrderEntity)
}
```

## Migration Strategy

### Step 1: Create New Unified Table
1. Create `OrderEntity` with new schema
2. Add migration to copy data from both tables

### Step 2: Migrate Data
```kotlin
// Migration script
val migration = object : Migration(OLD_VERSION, NEW_VERSION) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create new orders table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_no TEXT NOT NULL,
                server_id INTEGER,
                order_source TEXT NOT NULL,
                is_synced INTEGER NOT NULL DEFAULT 0,
                status TEXT,
                -- ... other fields
            )
        """)
        
        // Migrate online_orders
        database.execSQL("""
            INSERT INTO orders (order_no, order_source, is_synced, status, ...)
            SELECT order_no, 'api', 1, 'completed', ...
            FROM online_orders
        """)
        
        // Migrate pending_orders
        database.execSQL("""
            INSERT INTO orders (order_no, order_source, is_synced, status, ...)
            SELECT order_no, 'local', is_synced, 
                   CASE WHEN is_synced = 1 THEN 'completed' ELSE 'pending' END, ...
            FROM pending_orders
        """)
    }
}
```

### Step 3: Update Repositories
1. Update `OrdersRepository` to use new `OrderDao`
2. Update `OrdersViewModel` to query single table
3. Remove old `OnlineOrderDao` and `PendingOrderDao` (after migration)

## Benefits

1. **Single Source of Truth**: All orders in one place
2. **Simpler Queries**: No need to union two tables
3. **Consistent Status**: Same status logic for all orders
4. **Easier Maintenance**: One table to manage
5. **Better Performance**: Single table queries are faster

## Display Logic in UI

```kotlin
// In OrdersScreen/ViewModel
fun loadOrders() {
    if (networkMonitor.isNetworkAvailable()) {
        // Fetch from API and save to local
        fetchOrdersFromApi()
    }
    
    // Always load from local database
    val allOrders = orderDao.getAllOrders()
    
    // Group by status
    val completedOrders = allOrders.filter { it.isSynced == true }
    val pendingOrders = allOrders.filter { it.isSynced == false }
    
    _orders.value = allOrders
    _completedOrders.value = completedOrders
    _pendingOrders.value = pendingOrders
}
```

## Offline Order Flow with WorkManager

### 1. Order Creation (Offline)

When an order is placed offline:
```kotlin
// In HomeViewModel.createOrder()
val orderEntity = OrderEntity(
    orderNumber = generateOrderNumber(),
    orderSource = "local",
    isSynced = false,  // ✅ isSynced = 0 (false)
    status = "pending", // ✅ Status = "pending"
    // ... other fields
)
orderDao.insertOrder(orderEntity)
```

### 2. WorkManager Sync (When Internet Restores)

The `OrderSyncWorker` runs periodically (every 15 minutes) or when network is connected:

```kotlin
@HiltWorker
class OrderSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val orderRepository: OrderRepository  // Updated to use unified table
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Get all unsynced local orders
            val unsyncedOrders = orderRepository.getUnsyncedLocalOrders()
            
            for (order in unsyncedOrders) {
                try {
                    // Sync to server
                    orderRepository.syncOrderToServer(order).onSuccess { response ->
                        // ✅ Update isSynced = 1 (true) and status = "completed"
                        orderRepository.updateOrder(
                            order.copy(
                                isSynced = true,  // ✅ Updated to 1
                                status = "completed", // ✅ Updated status
                                serverId = response.data.id
                            )
                        )
                        Log.d("OrderSyncWorker", "Successfully synced order ${order.orderNumber}")
                    }.onFailure { e ->
                        Log.e("OrderSyncWorker", "Failed to sync order ${order.orderNumber}: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.e("OrderSyncWorker", "Error syncing order: ${e.message}")
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

### 3. WorkManager Configuration

WorkManager is configured to run when network is available:
```kotlin
// In WorkManagerConfiguration.kt
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)  // ✅ Only runs when internet is available
    .build()

val syncRequest = PeriodicWorkRequestBuilder<OrderSyncWorker>(
    15, TimeUnit.MINUTES,  // Runs every 15 minutes
    5, TimeUnit.MINUTES    // Flexible window of 5 minutes
)
    .setConstraints(constraints)
    .addTag("ORDER_SYNC")
    .build()

WorkManager.getInstance(context).enqueue(syncRequest)
```

## Updated Repository Methods

### OrderRepository
```kotlin
interface OrderRepository {
    // Save order offline
    suspend fun saveOrderToLocal(orderRequest: CreateOrderRequest): Long {
        val orderEntity = OrderEntity(
            // ... order fields
            orderSource = "local",
            isSynced = false,  // ✅ isSynced = 0
            status = "pending"
        )
        return orderDao.insertOrder(orderEntity)
    }
    
    // Sync order to server
    suspend fun syncOrderToServer(order: OrderEntity): Result<CreateOrderResponse> {
        val result = apiService.createOrder(convertToRequest(order))
        
        result.onSuccess { response ->
            // ✅ Update isSynced = 1 and status = "completed"
            orderDao.updateOrder(
                order.copy(
                    isSynced = true,  // ✅ Updated to 1
                    status = "completed",
                    serverId = response.data.id
                )
            )
        }
        
        return result
    }
    
    // Get unsynced local orders
    suspend fun getUnsyncedLocalOrders(): List<OrderEntity> {
        return orderDao.getUnsyncedLocalOrders() // WHERE order_source = 'local' AND is_synced = 0
    }
}
```

## Display Logic

### OrdersScreen - Show Orders by Status

```kotlin
fun loadOrders() {
    if (networkMonitor.isNetworkAvailable()) {
        // Fetch from API and save to local
        fetchOrdersFromApi()
    }
    
    // Always load from local database
    val allOrders = orderDao.getAllOrders()
    
    // Group by isSynced status
    val completedOrders = allOrders.filter { it.isSynced == true }  // isSynced = 1
    val pendingOrders = allOrders.filter { it.isSynced == false }   // isSynced = 0
    
    _orders.value = allOrders
    _completedOrders.value = completedOrders
    _pendingOrders.value = pendingOrders
}
```

## Summary

**Yes, the flow is correct:**

1. ✅ **Offline Order**: Saved with `isSynced = 0` (false) and `status = "pending"`
2. ✅ **WorkManager**: Automatically runs when internet is available
3. ✅ **Sync Success**: Updates `isSynced = 1` (true) and `status = "completed"` in the orders table
4. ✅ **UI Display**: Shows orders grouped by `isSynced` status (1 = completed, 0 = pending)

## Conclusion

**Recommendation: Use Single Table Approach**

This approach simplifies the codebase, improves maintainability, and provides a consistent way to handle orders from both API and local sources. The WorkManager automatically handles syncing when internet is available, updating the `isSynced` field from 0 to 1 upon successful sync.

