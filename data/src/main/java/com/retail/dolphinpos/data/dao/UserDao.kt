package com.retail.dolphinpos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.retail.dolphinpos.data.entities.user.ActiveUserDetailsEntity
import com.retail.dolphinpos.data.entities.user.BatchEntity
import com.retail.dolphinpos.data.entities.user.TimeSlotEntity
import com.retail.dolphinpos.data.entities.user.LocationEntity
import com.retail.dolphinpos.data.entities.user.RegisterEntity
import com.retail.dolphinpos.data.entities.user.RegisterStatusEntity
import com.retail.dolphinpos.data.entities.user.StoreEntity
import com.retail.dolphinpos.data.entities.user.StoreLogoUrlEntity
import com.retail.dolphinpos.data.entities.user.TaxDetailEntity
import com.retail.dolphinpos.data.entities.user.UserEntity

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoreDetails(storeEntity: StoreEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoreLogoUrlDetails(storeLogoUrlEntity: StoreLogoUrlEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locationEntity: LocationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegisters(registerEntity: RegisterEntity): Long

    @Query("SELECT * FROM user WHERE pin = :pin AND locationId = :locationId")
    suspend fun getUserByPin(pin: String, locationId: Int): UserEntity?

    @Query("SELECT * FROM user WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): UserEntity?

    @Query("SELECT * FROM store")
    suspend fun getStore(): StoreEntity

    @Query("SELECT * FROM store_logo_url WHERE storeID = :storeId")
    suspend fun getStoreLogoUrl(storeId: Int?): StoreLogoUrlEntity?

    @Query("SELECT * FROM store_locations WHERE storeID = :storeId")
    suspend fun getLocationsByStoreId(storeId: Int?): List<LocationEntity>

    @Query("SELECT * FROM location_registers WHERE locationID = :locationID")
    suspend fun getRegistersByLocationId(locationID: Int?): List<RegisterEntity>

    @Query("SELECT * FROM store_locations WHERE id = :locationID")
    suspend fun getLocationByLocationId(locationID: Int?): LocationEntity

    @Query("SELECT * FROM location_registers WHERE id = :registerID")
    suspend fun getRegisterByRegisterId(registerID: Int?): RegisterEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveUserDetails(activeUserDetailsEntity: ActiveUserDetailsEntity)

    @Query("SELECT * FROM active_user_details WHERE pin = :pin")
    suspend fun getActiveUserDetailsByPin(pin: String): ActiveUserDetailsEntity

    @Query("SELECT * FROM active_user_details LIMIT 1")
    suspend fun getActiveUserDetails(): ActiveUserDetailsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchDetails(batchEntity: BatchEntity)

    @Query("DELETE FROM batch")
    suspend fun deleteAllBatches()

    @Query("SELECT * FROM batch")
    suspend fun getBatchDetails(): BatchEntity

    @Query("SELECT * FROM batch ORDER BY startedAt DESC")
    suspend fun getAllBatches(): List<BatchEntity>

    @Query("SELECT * FROM batch WHERE batchNo = :batchNo LIMIT 1")
    suspend fun getBatchByBatchNo(batchNo: String): BatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegisterStatusDetails(registerStatusEntity: RegisterStatusEntity)

    @Query("SELECT * FROM register_status_details")
    suspend fun getRegisterStatusDetail(): RegisterStatusEntity

    @Query("SELECT CASE WHEN closedAt IS NULL THEN 0 ELSE 1 END FROM batch WHERE userId = :userId AND storeId = :storeId AND registerId = :registerId")
    suspend fun isLatestBatchClosed(userId: Int, storeId: Int, registerId: Int): Boolean

    @Query(" SELECT EXISTS(SELECT 1 FROM batch WHERE userId = :userId AND storeId = :storeId AND registerId = :registerId AND closedAt IS NULL)")
    suspend fun hasOpenBatch(userId: Int, storeId: Int, registerId: Int): Boolean

    @Query("SELECT * FROM batch WHERE isSynced = 0")
    suspend fun getUnsyncedBatches(): List<BatchEntity>

    @Update
    suspend fun updateBatch(batchEntity: BatchEntity)

    // -----------------------------
    // Time Slot (Clock In/Out) DAO
    // -----------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeSlot(timeSlotEntity: TimeSlotEntity)

    @Query("SELECT * FROM time_slots WHERE isSynced = 0")
    suspend fun getPendingTimeSlots(): List<TimeSlotEntity>

    @Query("UPDATE time_slots SET isSynced = 1 WHERE id = :id")
    suspend fun markTimeSlotSynced(id: Int)

    @Query("SELECT * FROM time_slots WHERE userId = :userId ORDER BY id DESC LIMIT 1")
    suspend fun getLastTimeSlot(userId: Int): com.retail.dolphinpos.data.entities.user.TimeSlotEntity?

    // -----------------------------
    // Tax Detail DAO
    // -----------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaxDetails(taxDetails: List<TaxDetailEntity>)

    @Query("SELECT * FROM tax_details WHERE locationId = :locationId")
    suspend fun getTaxDetailsByLocationId(locationId: Int): List<TaxDetailEntity>

    @Query("DELETE FROM tax_details WHERE locationId = :locationId")
    suspend fun deleteTaxDetailsByLocationId(locationId: Int)

    @Query("DELETE FROM tax_details")
    suspend fun deleteAllTaxDetails()

    // -----------------------------
    // Batch History DAO
    // -----------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchHistory(batchHistoryEntity: com.retail.dolphinpos.data.entities.user.BatchHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchHistoryList(batchHistoryList: List<com.retail.dolphinpos.data.entities.user.BatchHistoryEntity>)

    @Query("SELECT * FROM batch_history WHERE storeId = :storeId ORDER BY createdAt DESC")
    suspend fun getBatchHistoryByStoreId(storeId: Int): List<com.retail.dolphinpos.data.entities.user.BatchHistoryEntity>

    @Query("SELECT * FROM batch_history WHERE storeId = :storeId AND substr(createdAt, 1, 10) >= :startDate AND substr(createdAt, 1, 10) <= :endDate ORDER BY createdAt DESC")
    suspend fun getBatchHistoryByStoreIdAndDateRange(
        storeId: Int,
        startDate: String,
        endDate: String
    ): List<com.retail.dolphinpos.data.entities.user.BatchHistoryEntity>

    @Query("DELETE FROM batch_history WHERE storeId = :storeId")
    suspend fun deleteBatchHistoryByStoreId(storeId: Int)
}