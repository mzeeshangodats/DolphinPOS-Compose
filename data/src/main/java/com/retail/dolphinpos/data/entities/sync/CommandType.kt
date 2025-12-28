package com.retail.dolphinpos.data.entities.sync

/**
 * Enum for command types in the sync queue
 */
enum class CommandType {
    OPEN_BATCH,
    CREATE_ORDER,
    CLOSE_BATCH,
    CREATE_REFUND
}

