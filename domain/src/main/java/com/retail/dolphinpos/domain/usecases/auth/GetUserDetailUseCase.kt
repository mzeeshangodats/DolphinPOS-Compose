package com.retail.dolphinpos.domain.usecases.auth

import com.retail.dolphinpos.domain.model.auth.active_user.ActiveUserDetails
import com.retail.dolphinpos.domain.repositories.auth.VerifyPinRepository
import javax.inject.Inject

class GetUserDetailUseCase @Inject constructor(
    private val verifyPinRepository: VerifyPinRepository
) {
    suspend operator fun invoke(): ActiveUserDetails? = verifyPinRepository.getActiveUserDetails()
    
    suspend fun invoke(pin: String): ActiveUserDetails = verifyPinRepository.getActiveUserDetailsByPin(pin)
}

