package com.retail.dolphinpos.domain.repositories.auth

import com.retail.dolphinpos.domain.model.home.create_order.TaxDetail
import com.retail.dolphinpos.domain.model.auth.active_user.ActiveUserDetails
import com.retail.dolphinpos.domain.model.auth.login.response.AllStoreUsers
import com.retail.dolphinpos.domain.model.auth.login.response.Locations
import com.retail.dolphinpos.domain.model.auth.login.response.Registers
import com.retail.dolphinpos.domain.model.auth.login.response.Store
import com.retail.dolphinpos.domain.model.auth.clock_in_out.ClockInOutRequest
import com.retail.dolphinpos.domain.model.auth.clock_in_out.ClockInOutResponse
import com.retail.dolphinpos.domain.model.auth.clock_in_out.ClockInOutHistoryData

interface VerifyPinRepository {

    suspend fun getUser(pin: String, locationId: Int): AllStoreUsers?

    suspend fun getStore(): Store

    suspend fun getLocationByLocationID(locationID: Int): Locations

    suspend fun getRegisterByRegisterID(locationID: Int): Registers

    suspend fun insertActiveUserDetailsIntoLocalDB(activeUserDetails: ActiveUserDetails)

    suspend fun getActiveUserDetailsByPin(pin: String): ActiveUserDetails

    suspend fun getActiveUserDetails(): ActiveUserDetails?

    suspend fun hasOpenBatch(userId: Int, storeId: Int, registerId: Int): Boolean

    suspend fun clockInOut(request: ClockInOutRequest): Result<ClockInOutResponse>

    suspend fun getLastTimeSlotSlug(userId: Int): String?

    suspend fun getClockInOutHistory(userId: Int): Result<List<ClockInOutHistoryData>>

    suspend fun getTaxDetailsByLocationId(locationId: Int): List<TaxDetail>
}