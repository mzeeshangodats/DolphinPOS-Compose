package com.retail.dolphinpos.domain.usecases.product

import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.repositories.product.ProductRepository
import javax.inject.Inject

class GetProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: Int): Result<Products?> {
        return productRepository.getProductById(productId)
    }
}

