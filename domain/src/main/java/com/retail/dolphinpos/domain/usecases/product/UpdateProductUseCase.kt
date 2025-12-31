package com.retail.dolphinpos.domain.usecases.product

import com.retail.dolphinpos.domain.model.product.CreateProductRequest
import com.retail.dolphinpos.domain.repositories.product.ProductRepository
import javax.inject.Inject

class UpdateProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: Int, request: CreateProductRequest): Result<Int> {
        return productRepository.updateProduct(productId, request)
    }
}

