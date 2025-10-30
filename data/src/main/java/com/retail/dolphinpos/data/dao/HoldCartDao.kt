package com.retail.dolphinpos.data.dao

import androidx.room.*
import com.retail.dolphinpos.data.entities.holdcart.HoldCartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HoldCartDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoldCart(holdCart: HoldCartEntity): Long

    @Update
    suspend fun updateHoldCart(holdCart: HoldCartEntity)

    @Delete
    suspend fun deleteHoldCart(holdCart: HoldCartEntity)

    @Query("DELETE FROM hold_cart WHERE id = :holdCartId")
    suspend fun deleteHoldCartById(holdCartId: Long)

    @Query("SELECT * FROM hold_cart WHERE userId = :userId AND storeId = :storeId AND registerId = :registerId ORDER BY createdAt DESC")
    suspend fun getHoldCartsByUser(userId: Int, storeId: Int, registerId: Int): List<HoldCartEntity>

    @Query("SELECT * FROM hold_cart WHERE userId = :userId AND storeId = :storeId AND registerId = :registerId ORDER BY createdAt DESC")
    fun getHoldCartsByUserFlow(userId: Int, storeId: Int, registerId: Int): Flow<List<HoldCartEntity>>

    @Query("SELECT * FROM hold_cart WHERE id = :holdCartId")
    suspend fun getHoldCartById(holdCartId: Long): HoldCartEntity?

    @Query("SELECT COUNT(*) FROM hold_cart WHERE userId = :userId AND storeId = :storeId AND registerId = :registerId")
    suspend fun getHoldCartCount(userId: Int, storeId: Int, registerId: Int): Int

    @Query("SELECT COUNT(*) FROM hold_cart WHERE userId = :userId AND storeId = :storeId AND registerId = :registerId")
    fun getHoldCartCountFlow(userId: Int, storeId: Int, registerId: Int): Flow<Int>

    @Query("DELETE FROM hold_cart WHERE userId = :userId AND storeId = :storeId AND registerId = :registerId")
    suspend fun deleteAllHoldCartsByUser(userId: Int, storeId: Int, registerId: Int)
}
