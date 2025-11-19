package com.retail.dolphinpos.domain.repositories.home

import com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.model.home.customer.Customer

interface HomeRepository {
    suspend fun getCategories(): List<CategoryData>
    suspend fun getAllProducts(): List<Products>
    suspend fun getProductsByCategoryID(categoryID: Int): List<Products>
    suspend fun searchProducts(query: String): List<Products>
    suspend fun searchProductByBarcode(barcode: String): Products?

    suspend fun insertCustomerDetailsIntoLocalDB(customer: Customer): Long
    
    // Update quantity methods
    suspend fun deductProductQuantity(productId: Int, quantityToDeduct: Int)
    suspend fun deductVariantQuantity(variantId: Int, quantityToDeduct: Int)

}