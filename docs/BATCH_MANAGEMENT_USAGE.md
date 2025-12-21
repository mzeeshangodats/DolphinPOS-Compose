# Batch Management System - Usage Guide

This document explains how to use the batch management system in the POS app.

## Overview

The batch management system ensures:
- Batches work offline immediately
- Orders always belong to exactly one batch
- Batches are synced to backend before their orders
- Failed batch syncs prevent order syncs

## Key Components

### 1. BatchRepository

The main interface for batch operations:

```kotlin
@Inject
lateinit var batchRepository: BatchRepository
```

### 2. Starting a Batch

When a user starts a new batch (e.g., at the start of day):

```kotlin
suspend fun startNewBatch() {
    val userId = preferenceManager.getUserID()
    val storeId = preferenceManager.getStoreID()
    val registerId = preferenceManager.getOccupiedRegisterID()
    val locationId = preferenceManager.getLocationID()
    val startingCashAmount = 100.0 // Cash in register
    
    // Start batch locally (works offline)
    val batch = batchRepository.startBatch(
        userId = userId,
        storeId = storeId,
        registerId = registerId,
        locationId = locationId,
        startingCashAmount = startingCashAmount
    )
    
    // Batch is now active and ready to receive orders
    // BatchStartWorker is automatically scheduled to sync to backend
}
```

### 3. Getting Active Batch

To get the currently active batch (for attaching orders):

```kotlin
suspend fun getActiveBatchForOrders(): BatchEntity? {
    val registerId = preferenceManager.getOccupiedRegisterID()
    return batchRepository.getActiveBatch(registerId)
}
```

### 4. Creating Orders with Batch ID

When creating an order, always attach it to the active batch:

```kotlin
suspend fun createOrder(orderItems: List<OrderItem>): OrderEntity {
    // Get active batch
    val registerId = preferenceManager.getOccupiedRegisterID()
    val activeBatch = batchRepository.getActiveBatch(registerId)
        ?: throw IllegalStateException("No active batch found")
    
    // Create order with batchId (UUID)
    val order = OrderEntity(
        orderNumber = generateOrderNumber(),
        batchId = activeBatch.batchId, // CRITICAL: Use UUID batchId
        batchNo = activeBatch.batchNo, // Optional: human-readable reference
        storeId = activeBatch.storeId!!,
        locationId = activeBatch.locationId!!,
        // ... other order fields
    )
    
    // Save order locally
    orderDao.insertOrder(order)
    
    // OrderSyncWorker will sync this order AFTER batch is synced
    return order
}
```

### 5. Closing a Batch

When closing a batch (end of day):

```kotlin
suspend fun closeCurrentBatch(closingCashAmount: Double): Boolean {
    val registerId = preferenceManager.getOccupiedRegisterID()
    val activeBatch = batchRepository.getActiveBatch(registerId)
        ?: return false // No active batch
    
    // Close the batch
    val success = batchRepository.closeBatch(
        batchId = activeBatch.batchId,
        closingCashAmount = closingCashAmount
    )
    
    return success
}
```

## WorkManager Chain

The system uses WorkManager to ensure proper sync order:

```
BatchStartWorker → OrderSyncWorker
```

1. **BatchStartWorker**:
   - Syncs batch to backend
   - Marks batch as `SYNCED` on success
   - Marks batch as `SYNC_FAILED` on failure
   - Outputs `batchId` for chaining

2. **OrderSyncWorker**:
   - Only runs if BatchStartWorker succeeds
   - Only syncs orders for batches with `SYNCED` status
   - Skips orders for `SYNC_PENDING` or `SYNC_FAILED` batches

## Error Handling

### Batch Sync Failed

If batch sync fails:
- Batch status is set to `SYNC_FAILED`
- Orders for that batch will NOT be synced
- UI should show error state to user
- User may need to retry batch sync manually

Example error handling:

```kotlin
suspend fun checkBatchSyncStatus(batchId: String): BatchSyncStatus? {
    val batch = batchRepository.getBatchById(batchId)
    return batch?.syncStatus
}

// In UI
val batchStatus = checkBatchSyncStatus(batchId)
when (batchStatus) {
    BatchSyncStatus.SYNC_FAILED -> {
        // Show error to user
        // Offer retry option
    }
    BatchSyncStatus.SYNC_PENDING -> {
        // Show "Syncing..." state
    }
    BatchSyncStatus.SYNCED -> {
        // All good
    }
    null -> {
        // Batch not found
    }
}
```

## Important Notes

1. **Always use batchId (UUID)**, not batchNo, when referencing batches
2. **Orders must always have a batchId** - never create orders without a batch
3. **Check batch sync status** before showing sync status in UI
4. **Batch operations work offline** - no network required for batch start/close
5. **WorkManager handles sync** - you don't need to manually trigger sync

## Database Schema

### BatchEntity
- `batchId`: UUID (Primary Key) - Generated locally
- `batchNo`: String - Human-readable identifier
- `syncStatus`: BatchSyncStatus (SYNC_PENDING, SYNCED, SYNC_FAILED)
- `closedAt`: Long? - null = active batch

### OrderEntity
- `batchId`: String (UUID) - Foreign key to BatchEntity
- `batchNo`: String? - Deprecated, kept for backward compatibility
- `isSynced`: Boolean - Order sync status

## Migration Notes

The database migration (v13 → v14) handles:
- Converting batch primary key from Int to UUID (String)
- Adding `batch_id` column to orders table
- Migrating existing `batch_no` references to `batch_id`
- Adding `syncStatus` column to batch table

Existing data is preserved during migration.

