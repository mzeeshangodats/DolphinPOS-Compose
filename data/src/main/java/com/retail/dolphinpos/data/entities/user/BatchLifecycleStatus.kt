package com.retail.dolphinpos.data.entities.user

/**
 * Batch lifecycle status (business state).
 * Tracks whether a batch is open or closed from a business perspective.
 */
enum class BatchLifecycleStatus {
    OPEN,      // Batch is currently active/open
    CLOSED     // Batch has been closed
}

