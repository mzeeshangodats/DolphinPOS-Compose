package com.retail.dolphinpos.domain.usecases.product

import com.retail.dolphinpos.domain.model.product.VendorListResponse
import com.retail.dolphinpos.domain.repositories.product.ProductRepository
import javax.inject.Inject

class GetVendorsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(): Result<VendorListResponse> {
        return productRepository.getVendors()
    }
}

