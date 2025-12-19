package com.retail.dolphinpos.domain.usecases.product

import com.retail.dolphinpos.domain.repositories.product.ProductRepository
import javax.inject.Inject

class SyncProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: Long): Result<Int> {
        return productRepository.syncProductToServer(productId)
    }
}

