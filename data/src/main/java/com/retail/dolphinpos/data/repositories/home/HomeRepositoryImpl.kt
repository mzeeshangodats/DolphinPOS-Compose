package com.retail.dolphinpos.data.repositories.home

import com.retail.dolphinpos.data.dao.CustomerDao
import com.retail.dolphinpos.data.dao.ProductsDao
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.mapper.CustomerMapper
import com.retail.dolphinpos.data.mapper.ProductMapper
import com.retail.dolphinpos.data.mapper.UserMapper
import com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.model.home.customer.Customer
import com.retail.dolphinpos.domain.repositories.auth.StoreRegistersRepository
import com.retail.dolphinpos.domain.repositories.home.HomeRepository

class HomeRepositoryImpl(
    private val productsDao: ProductsDao,
    private val customerDao: CustomerDao,
    private val userDao: UserDao,
    private val storeRegistersRepository: StoreRegistersRepository
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
                locationId = productEntity.locationId,
                chargeTaxOnThisProduct = productEntity.chargeTaxOnThisProduct,
                vendor = null,
                variants = variants,
                images = productImages,
                secondaryBarcodes = null
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
                secondaryBarcodes = null
            )
        }
    }

    override suspend fun searchProducts(query: String): List<Products> {
        val productEntities = productsDao.searchProducts(query)
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
                secondaryBarcodes = null
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
                secondaryBarcodes = null
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
                secondaryBarcodes = null
            )
        }
        
        return null
    }

    override suspend fun insertCustomerDetailsIntoLocalDB(customer: Customer): Long {
            try {
                return customerDao.insertCustomer(
                    CustomerMapper.toCustomerEntity(
                        customer
                    )
                )
            } catch (e: Exception) {
                throw e
            }
    }

}