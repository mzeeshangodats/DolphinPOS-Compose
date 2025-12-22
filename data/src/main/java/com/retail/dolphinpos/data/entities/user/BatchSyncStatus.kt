package com.retail.dolphinpos.data.entities.user

/**
 * Enum for batch sync status
 */
enum class BatchSyncStatus {
    ACTIVE_LOCAL,      // Batch opened locally, not synced yet
    CLOSED_LOCAL,      // Batch closed locally, not synced yet
    SYNCED_OPEN,       // Batch open synced to server
    SYNCED_CLOSED      // Batch close synced to server
}

