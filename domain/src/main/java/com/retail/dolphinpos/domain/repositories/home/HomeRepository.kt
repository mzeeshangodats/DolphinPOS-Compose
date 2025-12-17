package com.retail.dolphinpos.domain.repositories.home

import com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.model.home.customer.AddCustomerResponse

interface HomeRepository {
    suspend fun getCategories(): List<CategoryData>
    suspend fun getAllProducts(): List<Products>
    suspend fun getProductsByCategoryID(categoryID: Int): List<Products>
    suspend fun searchProducts(query: String): List<Products>
    suspend fun searchProductByBarcode(barcode: String): Products?
    suspend fun searchProductByPLU(plu: String, storeId: Int, locationId: Int): Result<Products?>

    suspend fun insertCustomerDetailsIntoLocalDB(
        userId: Int,
        storeId: Int,
        locationId: Int,
        firstName: String,
        lastName: String,
        email: String,
        phoneNumber: String,
        birthday: String
    ): Long
    
    suspend fun syncCustomerToServer(customerId: Int): Result<AddCustomerResponse>
    
    suspend fun getUnsyncedCustomers(): List<Int>
    
    // Update quantity methods
    suspend fun deductProductQuantity(productId: Int, quantityToDeduct: Int)
    suspend fun deductVariantQuantity(variantId: Int, quantityToDeduct: Int)

}