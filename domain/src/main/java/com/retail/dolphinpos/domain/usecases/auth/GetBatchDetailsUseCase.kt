package com.retail.dolphinpos.domain.usecases.auth

import com.retail.dolphinpos.domain.model.auth.batch.Batch
import com.retail.dolphinpos.domain.model.auth.batch.BatchDetails
import com.retail.dolphinpos.domain.repositories.auth.CashDenominationRepository
import javax.inject.Inject

class GetBatchDetailsUseCase @Inject constructor(
    private val cashDenominationRepository: CashDenominationRepository
) {
    suspend operator fun invoke(): Batch = cashDenominationRepository.getBatchDetails()
}

