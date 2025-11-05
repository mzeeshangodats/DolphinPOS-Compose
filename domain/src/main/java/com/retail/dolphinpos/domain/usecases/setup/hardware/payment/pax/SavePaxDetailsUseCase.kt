package com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax

import com.retail.dolphinpos.domain.model.setup.hardware.payment.pax.PaxDetail
import com.retail.dolphinpos.domain.repositories.setup.HardwareSetupRepository
import javax.inject.Inject

class SavePaxDetailsUseCase @Inject constructor(
    private val hardwareSetupRepository: HardwareSetupRepository
) {

    suspend operator fun invoke(ipAddress: String, portNumber: String, communicationType: String) {
        hardwareSetupRepository.savePaxDetails(
            PaxDetail(
                ipAddress = ipAddress,
                portNumber = portNumber,
                timeOut = PaxDetail.PAX_DEFAULT_CONNECTION_TIME_OUT,
                communicationType = communicationType
            )
        )
    }

}