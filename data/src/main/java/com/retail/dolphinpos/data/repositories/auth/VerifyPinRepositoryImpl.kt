package com.retail.dolphinpos.data.repositories.auth

import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.mapper.UserMapper
import com.retail.dolphinpos.domain.model.auth.active_user.ActiveUserDetails
import com.retail.dolphinpos.domain.model.auth.login.response.AllStoreUsers
import com.retail.dolphinpos.domain.model.auth.login.response.Locations
import com.retail.dolphinpos.domain.model.auth.login.response.Registers
import com.retail.dolphinpos.domain.model.auth.login.response.Store
import com.retail.dolphinpos.domain.repositories.auth.VerifyPinRepository

class VerifyPinRepositoryImpl(
    private val userDao: UserDao
) : VerifyPinRepository {

    override suspend fun getUser(pin: String): AllStoreUsers? {
        val userEntity = userDao.getUserByPin(pin)
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

}