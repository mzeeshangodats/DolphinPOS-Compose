package com.retail.dolphinpos.data.entities.user

/**
 * Batch sync status (server sync state).
 * Tracks the synchronization state of batch operations with the backend.
 * 
 * States progression:
 * - START_PENDING → START_SYNCED → CLOSE_PENDING → CLOSE_SYNCED
 * - Any state can transition to FAILED on error
 */
enum class BatchSyncStatus {
    /**
     * Batch was created locally but batch start has not been synced to server yet.
     * Orders CANNOT be synced in this state.
     */
    START_PENDING,
    
    /**
     * Batch start has been successfully synced to server.
     * Orders CAN be synced in this state.
     * This is the minimum state required for order sync.
     */
    START_SYNCED,
    
    /**
     * Batch was closed locally but batch close has not been synced to server yet.
     * Orders CAN still be synced in this state (they belong to an already-synced batch).
     */
    CLOSE_PENDING,
    
    /**
     * Batch close has been successfully synced to server.
     * Batch lifecycle is complete and fully synced.
     * Orders CAN still be synced in this state.
     */
    CLOSE_SYNCED,
    
    /**
     * Batch operation failed to sync (either start or close).
     * Orders CANNOT be synced if batch is in FAILED state (even if previously synced).
     */
    FAILED
}

