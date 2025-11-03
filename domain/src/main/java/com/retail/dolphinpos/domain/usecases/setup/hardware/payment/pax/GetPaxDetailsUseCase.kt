package com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax

import com.retail.dolphinpos.domain.model.setup.hardware.payment.pax.PaxDetail
import com.retail.dolphinpos.domain.repositories.setup.HardwareSetupRepository
import javax.inject.Inject

class GetPaxDetailsUseCase @Inject constructor(
    private val hardwareSetupRepository: HardwareSetupRepository
) {
    suspend operator fun invoke(): PaxDetail? = hardwareSetupRepository.getPaxDetails()
}
