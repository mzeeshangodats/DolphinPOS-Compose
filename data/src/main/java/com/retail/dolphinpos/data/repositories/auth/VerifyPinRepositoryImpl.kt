package com.retail.dolphinpos.data.repositories.auth

import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.entities.user.TimeSlotEntity
import com.retail.dolphinpos.data.mapper.UserMapper
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.util.safeApiCallResult
import com.retail.dolphinpos.domain.model.auth.active_user.ActiveUserDetails
import com.retail.dolphinpos.domain.model.auth.clock_in_out.ClockInOutHistoryData
import com.retail.dolphinpos.domain.model.auth.clock_in_out.ClockInOutRequest
import com.retail.dolphinpos.domain.model.auth.clock_in_out.ClockInOutResponse
import com.retail.dolphinpos.domain.model.auth.login.response.AllStoreUsers
import com.retail.dolphinpos.domain.model.auth.login.response.Locations
import com.retail.dolphinpos.domain.model.auth.login.response.Registers
import com.retail.dolphinpos.domain.model.auth.login.response.Store
import com.retail.dolphinpos.domain.repositories.auth.VerifyPinRepository
import retrofit2.HttpException
import java.io.IOException

class VerifyPinRepositoryImpl(
    private val userDao: UserDao,
    private val apiService: ApiService
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
        return UserMapper.toLocationAgainstLocationID(locationEntities)
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

    override suspend fun hasOpenBatch(
        userId: Int,
        storeId: Int,
        registerId: Int
    ): Boolean {
        return userDao.hasOpenBatch(userId, storeId, registerId)
    }

    override suspend fun clockInOut(request: ClockInOutRequest): Result<ClockInOutResponse> {
        return try {
            safeApiCallResult(
                apiCall = { apiService.clockInOut(request) },
                defaultMessage = "Clock In/Out failed",
                messageExtractor = { errorResponse -> errorResponse.message }
            )
        } catch (e: IOException) {
            // Offline queue
            try {
                userDao.insertTimeSlot(
                    TimeSlotEntity(
                        slug = request.slug,
                        storeId = request.storeId,
                        time = request.time,
                        userId = request.userId,
                        isSynced = false
                    )
                )
            } catch (_: Exception) {
            }
            Result.failure(Throwable("OFFLINE_QUEUED"))
        } catch (e: Exception) {
            Result.failure(Throwable(e.message ?: "Clock In/Out failed"))
        }
    }

    override suspend fun getLastTimeSlotSlug(userId: Int): String? {
        return userDao.getLastTimeSlot(userId)?.slug
    }

    override suspend fun getClockInOutHistory(userId: Int): Result<List<ClockInOutHistoryData>> {
        return try {
            val result = safeApiCallResult(
                apiCall = { apiService.getClockInOutHistory(userId) },
                defaultMessage = "Failed to load history"
            )
            result.map { response -> response.data }
        } catch (e: Exception) {
            Result.failure(Throwable(e.message ?: "Failed to load history"))
        }
    }
}