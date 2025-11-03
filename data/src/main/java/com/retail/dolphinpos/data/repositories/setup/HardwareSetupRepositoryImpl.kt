package com.retail.dolphinpos.data.repositories.setup

import android.content.Context
import com.retail.dolphinpos.common.utils.Constants.PAX_DETAIL
import com.retail.dolphinpos.common.utils.preferences.getObjectFromSharedPreference
import com.retail.dolphinpos.common.utils.preferences.saveObjectToSharedPreference
import com.retail.dolphinpos.domain.model.setup.hardware.payment.pax.PaxDetail
import com.retail.dolphinpos.domain.repositories.setup.HardwareSetupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class HardwareSetupRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : HardwareSetupRepository {

    override suspend fun getPaxDetails(): PaxDetail? {
        return context.getObjectFromSharedPreference<PaxDetail>(PAX_DETAIL)
    }

    override suspend fun savePaxDetails(paxDetail: PaxDetail) {
        context.saveObjectToSharedPreference(PAX_DETAIL, paxDetail)
    }
}

