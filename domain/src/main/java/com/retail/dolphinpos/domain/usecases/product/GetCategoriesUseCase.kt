package com.retail.dolphinpos.domain.usecases.product

import com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData
import com.retail.dolphinpos.domain.repositories.product.ProductRepository
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(): List<CategoryData> {
        return productRepository.getCategories()
    }
}

