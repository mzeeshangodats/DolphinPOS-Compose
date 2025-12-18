package com.retail.dolphinpos.data.repositories.home

import com.google.gson.Gson
import com.retail.dolphinpos.data.dao.CustomerDao
import com.retail.dolphinpos.data.dao.ProductsDao
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.mapper.CustomerMapper
import com.retail.dolphinpos.data.mapper.ProductMapper
import com.retail.dolphinpos.data.mapper.UserMapper
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.util.safeApiCallResult
import com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.model.home.catrgories_products.PLUProductResponse
import com.retail.dolphinpos.domain.model.home.catrgories_products.PLUProduct
import com.retail.dolphinpos.domain.model.home.catrgories_products.ProductImage
import com.retail.dolphinpos.domain.model.home.customer.CustomerErrorResponse
import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.domain.repositories.auth.StoreRegistersRepository
import com.retail.dolphinpos.domain.repositories.home.HomeRepository
import retrofit2.HttpException

class HomeRepositoryImpl(
    private val productsDao: ProductsDao,
    private val customerDao: CustomerDao,
    private val userDao: UserDao,
    private val storeRegistersRepository: StoreRegistersRepository,
    private val apiService: ApiService,
    private val networkMonitor: NetworkMonitor
) : HomeRepository {

    override suspend fun getCategories(): List<CategoryData> {
        val categoryEntities = productsDao.getCategories()
        return ProductMapper.toCategory(categoryEntities)
    }

    override suspend fun getAllProducts(): List<Products> {
        val productEntities = productsDao.getAllProducts()
        return productEntities.map { productEntity ->
            // Get product images with local paths
            val productImages = storeRegistersRepository.getProductImagesWithLocalPaths(productEntity.id)
            
            // Get variants for this product
            val variantEntities = productsDao.getVariantsByProductId(productEntity.id)
            val variants = variantEntities.map { variantEntity ->
                // Get variant images with local cached paths
                val variantImages = storeRegistersRepository.getVariantImagesWithLocalPaths(variantEntity.id)
                ProductMapper.toVariant(variantEntity, variantImages)
            }
            
            // Create Products object with cached images and variants
            Products(
                id = productEntity.id,
                categoryId = productEntity.categoryId,
                storeId = productEntity.storeId,
                name = productEntity.name,
                description = productEntity.description,
                quantity = productEntity.quantity,
                status = productEntity.status,
                cashPrice = productEntity.cashPrice,
                cardPrice = productEntity.cardPrice,
                barCode = productEntity.barCode,
                plu = productEntity.plu,
                locationId = productEntity.locationId,
                chargeTaxOnThisProduct = productEntity.chargeTaxOnThisProduct,
                vendor = null,
                variants = variants,
                images = productImages,
                secondaryBarcodes = null,
                cardTax = productEntity.cardTax,
                cashTax = productEntity.cashTax
            )
        }
    }

    override suspend fun getProductsByCategoryID(categoryID: Int): List<Products> {
        val productEntities = productsDao.getProductsByCategoryID(categoryID)
        return productEntities.map { productEntity ->
            // Get product images with local paths
            val productImages = storeRegistersRepository.getProductImagesWithLocalPaths(productEntity.id)
            
            // Get variants for this product
            val variantEntities = productsDao.getVariantsByProductId(productEntity.id)
            val variants = variantEntities.map { variantEntity ->
                // Get variant images with local cached paths
                val variantImages = storeRegistersRepository.getVariantImagesWithLocalPaths(variantEntity.id)
                ProductMapper.toVariant(variantEntity, variantImages)
            }
            
            // Create Products object with cached images and variants
            Products(
                id = productEntity.id,
                categoryId = productEntity.categoryId,
                storeId = productEntity.storeId,
                name = productEntity.name,
                description = productEntity.description,
                quantity = productEntity.quantity,
                status = productEntity.status,
                cashPrice = productEntity.cashPrice,
                cardPrice = productEntity.cardPrice,
                barCode = productEntity.barCode,
                locationId = productEntity.locationId,
                chargeTaxOnThisProduct = productEntity.chargeTaxOnThisProduct,
                vendor = null,
                variants = variants,
                images = productImages,
                secondaryBarcodes = null,
                cardTax = productEntity.cardTax,
                cashTax = productEntity.cashTax
            )
        }
    }

    override suspend fun searchProducts(query: String): List<Products> {
        // Search products by name and barcode
        val productEntities = productsDao.searchProducts(query)
        
        // Also search variants by SKU and get their parent products
        val variantEntities = productsDao.searchVariantsBySku(query)
        val variantProductIds = variantEntities.map { it.productId }.distinct()
        val variantParentProducts = variantProductIds.mapNotNull { productId ->
            productsDao.getProductById(productId)
        }
        
        // Combine and deduplicate product entities
        val allProductEntities = (productEntities + variantParentProducts).distinctBy { it.id }
        
        return allProductEntities.map { productEntity ->
            // Get product images with local paths
            val productImages = storeRegistersRepository.getProductImagesWithLocalPaths(productEntity.id)
            
            // Get variants for this product
            val variantEntities = productsDao.getVariantsByProductId(productEntity.id)
            val variants = variantEntities.map { variantEntity ->
                // Get variant images with local cached paths
                val variantImages = storeRegistersRepository.getVariantImagesWithLocalPaths(variantEntity.id)
                ProductMapper.toVariant(variantEntity, variantImages)
            }
            
            // Create Products object with cached images and variants
            Products(
                id = productEntity.id,
                categoryId = productEntity.categoryId,
                storeId = productEntity.storeId,
                name = productEntity.name,
                description = productEntity.description,
                quantity = productEntity.quantity,
                status = productEntity.status,
                cashPrice = productEntity.cashPrice,
                cardPrice = productEntity.cardPrice,
                barCode = productEntity.barCode,
                plu = productEntity.plu,
                locationId = productEntity.locationId,
                chargeTaxOnThisProduct = productEntity.chargeTaxOnThisProduct,
                vendor = null,
                variants = variants,
                images = productImages,
                secondaryBarcodes = null,
                cardTax = productEntity.cardTax,
                cashTax = productEntity.cashTax
            )
        }
    }

    override suspend fun searchProductByBarcode(barcode: String): Products? {
        // First, try to find a product by barcode
        val productEntity = productsDao.searchProductByBarcode(barcode)
        if (productEntity != null) {
            // Get product images with local paths
            val productImages = storeRegistersRepository.getProductImagesWithLocalPaths(productEntity.id)
            
            // Get variants for this product
            val variantEntities = productsDao.getVariantsByProductId(productEntity.id)
            val variants = variantEntities.map { variantEntity ->
                // Get variant images with local cached paths
                val variantImages = storeRegistersRepository.getVariantImagesWithLocalPaths(variantEntity.id)
                ProductMapper.toVariant(variantEntity, variantImages)
            }
            
            return Products(
                id = productEntity.id,
                categoryId = productEntity.categoryId,
                storeId = productEntity.storeId,
                name = productEntity.name,
                description = productEntity.description,
                quantity = productEntity.quantity,
                status = productEntity.status,
                cashPrice = productEntity.cashPrice,
                cardPrice = productEntity.cardPrice,
                barCode = productEntity.barCode,
                locationId = productEntity.locationId,
                chargeTaxOnThisProduct = productEntity.chargeTaxOnThisProduct,
                vendor = null,
                variants = variants,
                images = productImages,
                secondaryBarcodes = null,
                cardTax = productEntity.cardTax,
                cashTax = productEntity.cashTax
            )
        }
        
        // If product not found, try to find a variant by SKU
        val variantEntity = productsDao.searchVariantBySku(barcode)
        if (variantEntity != null) {
            // Get the parent product
            val parentProductEntity = productsDao.getProductById(variantEntity.productId)
                ?: return null
            
            // Get product images with local paths
            val productImages = storeRegistersRepository.getProductImagesWithLocalPaths(parentProductEntity.id)
            
            // Get all variants for this product
            val variantEntities = productsDao.getVariantsByProductId(parentProductEntity.id)
            val variants = variantEntities.map { vEntity ->
                // Get variant images with local cached paths
                val variantImages = storeRegistersRepository.getVariantImagesWithLocalPaths(vEntity.id)
                ProductMapper.toVariant(vEntity, variantImages)
            }
            
            return Products(
                id = parentProductEntity.id,
                categoryId = parentProductEntity.categoryId,
                storeId = parentProductEntity.storeId,
                name = parentProductEntity.name,
                description = parentProductEntity.description,
                quantity = parentProductEntity.quantity,
                status = parentProductEntity.status,
                cashPrice = parentProductEntity.cashPrice,
                cardPrice = parentProductEntity.cardPrice,
                barCode = parentProductEntity.barCode,
                locationId = parentProductEntity.locationId,
                chargeTaxOnThisProduct = parentProductEntity.chargeTaxOnThisProduct,
                vendor = null,
                variants = variants,
                images = productImages,
                secondaryBarcodes = null,
                cardTax = parentProductEntity.cardTax,
                cashTax = parentProductEntity.cashTax
            )
        }
        
        return null
    }

    override suspend fun searchProductByPLU(plu: String, storeId: Int, locationId: Int): Result<Products?> {
        // If offline, search from local database
        if (!networkMonitor.isNetworkAvailable()) {
            return try {
                val productEntity = productsDao.searchProductByPLU(plu, storeId, locationId)
                if (productEntity != null) {
                    // Get product images with local paths
                    val productImages = storeRegistersRepository.getProductImagesWithLocalPaths(productEntity.id)
                    
                    // Get variants for this product
                    val variantEntities = productsDao.getVariantsByProductId(productEntity.id)
                    val variants = variantEntities.map { variantEntity ->
                        // Get variant images with local cached paths
                        val variantImages = storeRegistersRepository.getVariantImagesWithLocalPaths(variantEntity.id)
                        ProductMapper.toVariant(variantEntity, variantImages)
                    }
                    
                    // Convert to domain model
                    val product = Products(
                        id = productEntity.id,
                        categoryId = productEntity.categoryId,
                        storeId = productEntity.storeId,
                        name = productEntity.name,
                        description = productEntity.description,
                        quantity = productEntity.quantity,
                        status = productEntity.status,
                        cashPrice = productEntity.cashPrice,
                        cardPrice = productEntity.cardPrice,
                        barCode = productEntity.barCode,
                        plu = productEntity.plu,
                        locationId = productEntity.locationId,
                        chargeTaxOnThisProduct = productEntity.chargeTaxOnThisProduct,
                        vendor = null,
                        variants = variants,
                        images = productImages,
                        secondaryBarcodes = null,
                        cardTax = productEntity.cardTax,
                        cashTax = productEntity.cashTax
                    )
                    Result.success(product)
                } else {
                    Result.success(null)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
        // If online, search from API
        return safeApiCallResult(
            apiCall = {
                val response = apiService.searchProductByPLU(plu, storeId, locationId)
                if (response.success) {
                    response.data?.products?.let { productsList ->
                        if (productsList.isNotEmpty()) {
                            val pluProduct = productsList[0]
                            // Convert PLUProduct to Products domain model
                            convertPLUProductToProducts(pluProduct)
                        } else {
                            null
                        }
                    } ?: null
                } else {
                    null
                }
            },
            defaultMessage = "Failed to search product by PLU"
        )
    }

    private fun convertPLUProductToProducts(pluProduct: PLUProduct): Products {
        // Convert images
        val productImages = pluProduct.images?.map { image ->
            ProductImage(
                fileURL = image.fileURL ?: "",
                originalName = image.originalName ?: ""
            )
        } ?: emptyList()

        // Convert vendor
        val vendor = pluProduct.vendor?.let {
            com.retail.dolphinpos.domain.model.home.catrgories_products.Vendor(
                id = it.id ?: 0,
                title = it.title ?: ""
            )
        }

        return Products(
            id = pluProduct.id,
            name = pluProduct.name,
            description = pluProduct.description,
            status = pluProduct.status,
            price = pluProduct.price ?: "",
            compareAtPrice = pluProduct.compareAtPrice,
            costPrice = pluProduct.costPrice,
            continueSellingWhenOutOfStock = pluProduct.continueSellingWhenOutOfStock ?: false,
            createdAt = "",
            currentVendorId = pluProduct.currentVendorId ?: 0,
            categoryId = pluProduct.categoryId ?: 0,
            storeId = pluProduct.storeId ?: 0,
            locationId = pluProduct.locationId ?: 0,
            quantity = pluProduct.quantity ?: 0,
            cashPrice = pluProduct.cashPrice ?: pluProduct.price ?: "0.00",
            cardPrice = pluProduct.cardPrice ?: pluProduct.price ?: "0.00",
            barCode = pluProduct.barCode,
            plu = pluProduct.plu,
            chargeTaxOnThisProduct = pluProduct.chargeTaxOnThisProduct ?: false,
            isEBTEligible = pluProduct.isEBTEligible ?: false,
            isHSTEligible = pluProduct.isHSTEligible ?: false,
            isIDRequired = pluProduct.isIDRequired ?: false,
            isProductBarCode = pluProduct.isProductBarCode ?: false,
            trackQuantity = pluProduct.trackQuantity ?: false,
            images = productImages,
            variants = emptyList(), // Variants would need to be converted if available
            vendor = vendor,
            secondaryBarcodes = null,
            cardTax = 0.0, // Would need to calculate from taxDetails if available
            cashTax = 0.0,
            taxAmount = pluProduct.taxAmount,
            taxDetails = pluProduct.taxDetails
        )
    }

    override suspend fun insertCustomerDetailsIntoLocalDB(
        userId: Int,
        storeId: Int,
        locationId: Int,
        firstName: String,
        lastName: String,
        email: String,
        phoneNumber: String,
        birthday: String
    ): Long {
        try {
            val customerEntity = com.retail.dolphinpos.data.entities.customer.CustomerEntity(
                userId = userId,
                storeId = storeId,
                locationId = locationId,
                firstName = firstName,
                lastName = lastName,
                email = email,
                phoneNumber = phoneNumber,
                birthday = birthday,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis().toString(),
                isSynced = false
            )
            return customerDao.insertCustomer(customerEntity)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun syncCustomerToServer(customerId: Int): Result<com.retail.dolphinpos.domain.model.home.customer.AddCustomerResponse> {
        return try {
            val customerEntity = customerDao.getCustomerById(customerId)
                ?: return Result.failure(Exception("Customer not found"))
            
            val addCustomerRequest = CustomerMapper.toAddCustomerRequest(customerEntity)
            
            // Try to sync with server and handle structured errors
            val result = try {
                val response = apiService.addCustomer(addCustomerRequest)
                Result.success(response)
            } catch (e: HttpException) {
                // Parse structured error response
                val errorMessage = try {
                    val errorBody = e.response()?.errorBody()?.string()
                    if (errorBody != null) {
                        val gson = Gson()
                        val errorResponse: CustomerErrorResponse? = 
                            gson.fromJson(errorBody, CustomerErrorResponse::class.java)
                        
                        errorResponse?.let { error ->
                            buildString {
                                // Add main message if available
                                error.message?.let { 
                                    append(it)
                                    if (error.errors != null) append("\n")
                                }
                                
                                // Format field-specific errors
                                error.errors?.let { errors ->
                                    if (errors.email != null) append("Email: ${errors.email}\n")
                                    if (errors.phoneNumber != null) append("Phone Number: ${errors.phoneNumber}\n")
                                    if (errors.firstName != null) append("First Name: ${errors.firstName}\n")
                                    if (errors.lastName != null) append("Last Name: ${errors.lastName}\n")
                                }
                            }.trim().takeIf { it.isNotEmpty() } ?: "Customer sync failed"
                        } ?: "Customer sync failed"
                    } else {
                        "Customer sync failed"
                    }
                } catch (parseException: Exception) {
                    "Customer sync failed"
                }
                Result.failure(Exception(errorMessage))
            } catch (e: Exception) {
                Result.failure(e)
            }
            
            // Update customer as synced on success
            result.onSuccess { response ->
                val updatedEntity = customerEntity.copy(
                    isSynced = true,
                    serverId = response.customer.id,
                    updatedAt = System.currentTimeMillis().toString()
                )
                customerDao.updateCustomer(updatedEntity)
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUnsyncedCustomers(): List<Int> {
        return customerDao.getUnsyncedCustomers().map { it.id }
    }

    override suspend fun deductProductQuantity(productId: Int, quantityToDeduct: Int) {
        try {
            productsDao.deductProductQuantity(productId, quantityToDeduct)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun deductVariantQuantity(variantId: Int, quantityToDeduct: Int) {
        try {
            productsDao.deductVariantQuantity(variantId, quantityToDeduct)
        } catch (e: Exception) {
            throw e
        }
    }

}