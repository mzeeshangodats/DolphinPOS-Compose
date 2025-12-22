package com.retail.dolphinpos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.retail.dolphinpos.data.entities.sync.CommandStatus
import com.retail.dolphinpos.data.entities.sync.SyncCommandEntity
import com.retail.dolphinpos.data.entities.sync.SyncSequenceEntity

@Dao
interface SyncCommandDao {
    
    /**
     * Get the next pending command in sequence order
     */
    @Query("SELECT * FROM sync_command WHERE status = 'PENDING' ORDER BY sequence ASC LIMIT 1")
    suspend fun getNextPendingCommand(): SyncCommandEntity?
    
    /**
     * Get all pending commands (for debugging)
     */
    @Query("SELECT * FROM sync_command WHERE status = 'PENDING' ORDER BY sequence ASC")
    suspend fun getAllPendingCommands(): List<SyncCommandEntity>
    
    /**
     * Get command by ID
     */
    @Query("SELECT * FROM sync_command WHERE id = :id")
    suspend fun getCommandById(id: Long): SyncCommandEntity?
    
    /**
     * Mark command as running
     */
    @Update
    suspend fun updateCommand(command: SyncCommandEntity)
    
    /**
     * Insert a new command
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: SyncCommandEntity): Long
    
    /**
     * Reset all RUNNING commands to PENDING (recovery after crash)
     */
    @Query("UPDATE sync_command SET status = 'PENDING', attempts = attempts + 1 WHERE status = 'RUNNING'")
    suspend fun resetRunningToPending()
    
    /**
     * Get the current sequence value (for generating next sequence)
     */
    @Query("SELECT currentSequence FROM sync_sequence WHERE id = 1")
    suspend fun getCurrentSequence(): Long?
    
    /**
     * Initialize sequence table if it doesn't exist
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun initSequence(sequence: SyncSequenceEntity)
    
    /**
     * Atomically increment and get next sequence number
     * Uses SQLite's atomic increment within a transaction to ensure thread safety
     */
    @Transaction
    suspend fun getNextSequence(): Long {
        // Ensure sequence table exists
        initSequence(SyncSequenceEntity(id = 1, currentSequence = 0))
        
        // Atomically increment sequence (SQLite UPDATE is atomic)
        incrementSequence()
        
        // Get the new sequence value (within same transaction)
        val current = getCurrentSequence() ?: 0L
        return if (current > 0) current else 1L
    }
    
    /**
     * Increment sequence (called within transaction)
     */
    @Query("UPDATE sync_sequence SET currentSequence = currentSequence + 1 WHERE id = 1")
    suspend fun incrementSequence()
    
    /**
     * Get commands by batch ID
     */
    @Query("SELECT * FROM sync_command WHERE batchId = :batchId ORDER BY sequence ASC")
    suspend fun getCommandsByBatchId(batchId: String): List<SyncCommandEntity>
    
    /**
     * Get commands by order ID
     */
    @Query("SELECT * FROM sync_command WHERE orderId = :orderId")
    suspend fun getCommandsByOrderId(orderId: String): List<SyncCommandEntity>
    
    /**
     * Delete completed commands older than specified timestamp (cleanup)
     */
    @Query("DELETE FROM sync_command WHERE status = 'DONE' AND createdAt < :beforeTimestamp")
    suspend fun deleteOldCompletedCommands(beforeTimestamp: Long)
}

