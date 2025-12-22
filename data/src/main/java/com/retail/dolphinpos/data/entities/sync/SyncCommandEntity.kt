package com.retail.dolphinpos.data.entities.sync

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

/**
 * Entity representing a sync command in the queue
 * Commands are processed in strict sequence order
 */
@Entity(tableName = "sync_command")
@TypeConverters(SyncTypeConverters::class)
data class SyncCommandEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Monotonic increasing sequence number
     * Defines strict processing order (FIFO)
     */
    val sequence: Long,
    
    /**
     * Type of command
     */
    val type: CommandType,
    
    /**
     * Batch ID (for OPEN_BATCH and CLOSE_BATCH commands)
     */
    val batchId: String?,
    
    /**
     * Order ID (for CREATE_ORDER commands)
     */
    val orderId: String?,
    
    /**
     * Current status of the command
     */
    val status: CommandStatus = CommandStatus.PENDING,
    
    /**
     * Number of attempts made
     */
    val attempts: Int = 0,
    
    /**
     * Last error message if failed
     */
    val lastError: String? = null,
    
    /**
     * Timestamp when command was created
     */
    val createdAt: Long = System.currentTimeMillis(),
    
    /**
     * Idempotency key: batchId for OPEN/CLOSE, orderId for CREATE_ORDER
     */
    val idempotencyKey: String
)

