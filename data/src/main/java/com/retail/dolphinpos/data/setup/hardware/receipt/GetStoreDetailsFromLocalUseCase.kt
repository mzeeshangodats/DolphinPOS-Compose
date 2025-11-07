package com.retail.dolphinpos.data.setup.hardware.receipt

import com.retail.dolphinpos.domain.usecases.auth.GetStoreDetailsUseCase
import javax.inject.Inject

class GetStoreDetailsFromLocalUseCase @Inject constructor(
    private val getStoreDetailsUseCase: GetStoreDetailsUseCase
) {

    suspend operator fun invoke() = getStoreDetailsUseCase()

}