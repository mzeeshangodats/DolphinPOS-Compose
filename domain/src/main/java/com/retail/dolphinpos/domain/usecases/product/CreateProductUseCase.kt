package com.retail.dolphinpos.domain.usecases.product

import com.retail.dolphinpos.domain.model.product.CreateProductRequest
import com.retail.dolphinpos.domain.repositories.product.ProductRepository
import javax.inject.Inject

class CreateProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(request: CreateProductRequest): Result<Long> {
        return productRepository.createProduct(request)
    }
}

