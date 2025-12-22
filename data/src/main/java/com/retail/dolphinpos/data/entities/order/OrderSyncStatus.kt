package com.retail.dolphinpos.data.entities.order

/**
 * Enum for order sync status
 */
enum class OrderSyncStatus {
    LOCAL_ONLY,    // Order created locally, not synced yet
    SYNCED         // Order synced to server
}

