package com.retail.dolphinpos.domain.repositories.product

import com.retail.dolphinpos.domain.model.product.CreateProductRequest
import com.retail.dolphinpos.domain.model.product.FileUploadResponse
import com.retail.dolphinpos.domain.model.product.VendorListResponse
import com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData
import java.io.File

interface ProductRepository {
    suspend fun createProduct(request: CreateProductRequest): Result<Long> // Returns local product ID
    suspend fun syncProductToServer(productId: Long): Result<Int> // Returns server product ID
    suspend fun uploadFiles(files: List<File>, type: String = "product"): Result<FileUploadResponse>
    suspend fun getVendors(): Result<VendorListResponse>
    suspend fun getCategories(): List<CategoryData>
//    suspend fun getUnsyncedProducts(): List<Long>
}

