package com.retail.dolphinpos.domain.repositories.auth

import com.retail.dolphinpos.domain.model.auth.active_user.ActiveUserDetails
import com.retail.dolphinpos.domain.model.auth.login.response.AllStoreUsers
import com.retail.dolphinpos.domain.model.auth.login.response.Locations
import com.retail.dolphinpos.domain.model.auth.login.response.Registers
import com.retail.dolphinpos.domain.model.auth.login.response.Store

interface VerifyPinRepository {

    suspend fun getUser(pin: String): AllStoreUsers?

    suspend fun getStore(): Store

    suspend fun getLocationByLocationID(locationID: Int): Locations

    suspend fun getRegisterByRegisterID(locationID: Int): Registers

    suspend fun insertActiveUserDetailsIntoLocalDB(activeUserDetails: ActiveUserDetails)

    suspend fun getActiveUserDetailsByPin(pin: String): ActiveUserDetails

    suspend fun hasOpenBatch(userId: Int, storeId: Int, registerId: Int): Boolean

}