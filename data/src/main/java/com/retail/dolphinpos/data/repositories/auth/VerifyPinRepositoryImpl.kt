package com.retail.dolphinpos.data.repositories.auth

import android.util.Log
import com.google.gson.Gson
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.entities.user.TimeSlotEntity
import com.retail.dolphinpos.data.mapper.UserMapper
import com.retail.dolphinpos.domain.model.home.create_order.TaxDetail
import com.retail.dolphinpos.domain.model.auth.active_user.ActiveUserDetails
import com.retail.dolphinpos.domain.model.auth.clock_in_out.ClockInOutHistoryData
import com.retail.dolphinpos.domain.model.auth.clock_in_out.ClockInOutRequest
import com.retail.dolphinpos.domain.model.auth.clock_in_out.ClockInOutResponse
import com.retail.dolphinpos.domain.model.auth.login.response.AllStoreUsers
import com.retail.dolphinpos.domain.model.auth.login.response.Locations
import com.retail.dolphinpos.domain.model.auth.login.response.Registers
import com.retail.dolphinpos.domain.model.auth.login.response.Store
import com.retail.dolphinpos.domain.repositories.auth.VerifyPinRepository

/**
 * VerifyPinRepository implementation.
 * 
 * NOTE: All operations use local database only - no API calls.
 * PIN verification is done entirely from the users table in local DB.
 */
class VerifyPinRepositoryImpl(
    private val userDao: UserDao,
    private val gson: Gson
) : VerifyPinRepository {

    override suspend fun getUser(pin: String, locationId: Int): AllStoreUsers? {
        val userEntity = userDao.getUserByPin(pin, locationId)
        return if (userEntity != null) {
            UserMapper.toUsers(userEntity)
        } else {
            null
        }
    }

    override suspend fun getStore(): Store {
        val storeEntity = userDao.getStore()
        return UserMapper.toStoreAgainstStoreID(storeEntity)
    }

    override suspend fun getLocationByLocationID(locationID: Int): Locations {
        val locationEntities = userDao.getLocationByLocationId(locationID)
        return UserMapper.toLocationAgainstLocationID(locationEntities, gson)
    }

    override suspend fun getRegisterByRegisterID(locationID: Int): Registers {
        val registerEntities = userDao.getRegisterByRegisterId(locationID)
        return UserMapper.toRegisterAgainstRegisterID(registerEntities)
    }

    override suspend fun insertActiveUserDetailsIntoLocalDB(activeUserDetails: ActiveUserDetails) {
        try {
            userDao.insertActiveUserDetails(
                UserMapper.toActiveUserDetailsEntity(
                    activeUserDetails
                )
            )
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getActiveUserDetailsByPin(pin: String): ActiveUserDetails {
        val activeUserDetailEntities = userDao.getActiveUserDetailsByPin(pin)
        return UserMapper.toActiveUserDetailsAgainstPin(activeUserDetailEntities)
    }

    override suspend fun getActiveUserDetails(): ActiveUserDetails? {
        val activeUserDetailEntity = userDao.getActiveUserDetails()
        return activeUserDetailEntity?.let {
            UserMapper.toActiveUserDetailsAgainstPin(it)
        }
    }

    override suspend fun hasOpenBatch(
        userId: Int,
        storeId: Int,
        registerId: Int
    ): Boolean {
        return userDao.hasOpenBatch(userId, storeId, registerId)
    }

    /**
     * Clock In/Out - stores time slot locally only.
     * No API calls - all operations are local.
     */
    override suspend fun clockInOut(request: ClockInOutRequest): Result<ClockInOutResponse> {
        return try {
            // Store time slot locally
            userDao.insertTimeSlot(
                TimeSlotEntity(
                    slug = request.slug,
                    storeId = request.storeId,
                    time = request.time,
                    userId = request.userId,
                    isSynced = false
                )
            )
            
            // Return success response
            val action = if (request.slug == "check-in") "in" else "out"
            Result.success(
                ClockInOutResponse(
                    message = "Clocked $action successfully (stored locally)"
                )
            )
        } catch (e: Exception) {
            Log.e("VerifyPinRepository", "Failed to store time slot: ${e.message}", e)
            Result.failure(Throwable("Failed to clock ${request.slug}: ${e.message}"))
        }
    }

    /**
     * Gets the last time slot slug for a user from local database.
     */
    override suspend fun getLastTimeSlotSlug(userId: Int): String? {
        return userDao.getLastTimeSlot(userId)?.slug
    }

    /**
     * Get clock in/out history - returns empty list as this is now local-only.
     * History is stored in TimeSlotEntity but no domain model mapping exists.
     * If history is needed, it should be retrieved from TimeSlotEntity directly.
     */
    override suspend fun getClockInOutHistory(userId: Int): Result<List<ClockInOutHistoryData>> {
        // Return empty list - history is stored locally in TimeSlotEntity
        // but there's no mapping to ClockInOutHistoryData domain model
        // If needed, this should be implemented by querying TimeSlotEntity
        return Result.success(emptyList())
    }

    override suspend fun getTaxDetailsByLocationId(locationId: Int): List<TaxDetail> {
        val taxDetailEntities = userDao.getTaxDetailsByLocationId(locationId)
        return taxDetailEntities.map { entity ->
            UserMapper.toTaxDetail(entity)
        }
    }
}