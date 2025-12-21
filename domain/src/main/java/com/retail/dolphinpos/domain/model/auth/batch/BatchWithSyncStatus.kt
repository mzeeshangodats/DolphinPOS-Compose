package com.retail.dolphinpos.domain.model.auth.batch

/**
 * Batch domain model with sync status tracking.
 * 
 * This represents a POS batch in the domain layer.
 * Used by BatchRepository interface (domain layer should not depend on data layer entities).
 * 
 * Note: This is separate from the existing Batch model which is used for API responses.
 * This model includes sync status and UUID batchId for offline-first operations.
 */
data class BatchWithSyncStatus(
    /**
     * UUID generated locally on device
     */
    val batchId: String,
    
    /**
     * Human-readable batch number
     */
    val batchNo: String,
    
    val userId: Int?,
    val storeId: Int?,
    val registerId: Int?,
    val locationId: Int?,
    
    /**
     * Starting cash amount when batch was opened
     */
    val startingCashAmount: Double,
    
    /**
     * Timestamp when batch was started (locally)
     */
    val startedAt: Long,
    
    /**
     * Timestamp when batch was closed (null = active/open batch)
     */
    val closedAt: Long?,
    
    /**
     * Closing cash amount when batch was closed
     */
    val closingCashAmount: Double?,
    
    /**
     * Sync status of the batch
     */
    val syncStatus: BatchSyncStatus
)


