package com.retail.dolphinpos.domain.usecases.auth

import com.retail.dolphinpos.domain.model.auth.login.response.Store
import com.retail.dolphinpos.domain.repositories.auth.VerifyPinRepository
import javax.inject.Inject

class GetStoreDetailsUseCase @Inject constructor(
    private val verifyPinRepository: VerifyPinRepository
) {
    suspend operator fun invoke(): Store? {
        return try {
            verifyPinRepository.getStore()
        } catch (e: Exception) {
            null
        }
    }
}

