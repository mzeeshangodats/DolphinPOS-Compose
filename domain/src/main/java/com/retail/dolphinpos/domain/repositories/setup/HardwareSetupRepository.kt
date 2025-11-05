package com.retail.dolphinpos.domain.repositories.setup

import com.retail.dolphinpos.domain.model.setup.hardware.payment.pax.PaxDetail

interface HardwareSetupRepository {
    suspend fun getPaxDetails(): PaxDetail?
    suspend fun savePaxDetails(paxDetail: PaxDetail)
}

