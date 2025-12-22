package com.retail.dolphinpos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.retail.dolphinpos.data.entities.sync.SyncLockEntity

@Dao
interface SyncLockDao {
    
    /**
     * Initialize lock table with unlocked state
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun initLock(lock: SyncLockEntity)
    
    /**
     * Try to acquire lock atomically
     * Returns true if lock was acquired, false if already locked
     */
    @Transaction
    suspend fun tryAcquireLock(workerId: String, timeoutMs: Long = 300000): Boolean {
        // Ensure lock table exists
        initLock(SyncLockEntity(id = 1, lockedBy = null, lockedAt = null))
        
        val currentLock = getLock()
        
        // If locked, check if lock has expired (timeout)
        if (currentLock?.lockedBy != null) {
            val lockedAt = currentLock.lockedAt ?: return false
            val now = System.currentTimeMillis()
            
            // If lock expired, release it
            if (now - lockedAt > timeoutMs) {
                releaseLock()
            } else {
                // Lock is still valid
                return false
            }
        }
        
        // Acquire lock
        updateLock(workerId, System.currentTimeMillis())
        return true
    }
    
    /**
     * Release lock
     */
    @Query("UPDATE sync_lock SET lockedBy = NULL, lockedAt = NULL WHERE id = 1")
    suspend fun releaseLock()
    
    /**
     * Get current lock state
     */
    @Query("SELECT * FROM sync_lock WHERE id = 1")
    suspend fun getLock(): SyncLockEntity?
    
    /**
     * Update lock (called within transaction)
     */
    @Query("UPDATE sync_lock SET lockedBy = :workerId, lockedAt = :lockedAt WHERE id = 1")
    suspend fun updateLock(workerId: String, lockedAt: Long)
}

