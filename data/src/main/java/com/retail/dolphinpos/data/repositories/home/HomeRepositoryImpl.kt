package com.retail.dolphinpos.data.repositories.home

import com.retail.dolphinpos.data.dao.CustomerDao
import com.retail.dolphinpos.data.dao.ProductsDao
import com.retail.dolphinpos.data.mapper.CustomerMapper
import com.retail.dolphinpos.data.mapper.ProductMapper
import com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.model.home.customer.Customer
import com.retail.dolphinpos.domain.repositories.auth.StoreRegistersRepository
import com.retail.dolphinpos.domain.repositories.home.HomeRepository

class HomeRepositoryImpl(
    private val productsDao: ProductsDao,
    private val customerDao: CustomerDao,
    private val storeRegistersRepository: StoreRegistersRepository
) : HomeRepository {

    override suspend fun getCategories(): List<CategoryData> {
        val categoryEntities = productsDao.getCategories()
        return ProductMapper.toCategory(categoryEntities)
    }

    override suspend fun getProductsByCategoryID(categoryID: Int): List<Products> {
        val productEntities = productsDao.getProductsByCategoryID(categoryID)
        return productEntities.map { productEntity ->
            // Get product images with local paths
            val productImages = storeRegistersRepository.getProductImagesWithLocalPaths(productEntity.id)
            
            // Create Products object with cached images
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
                variants = emptyList(),
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
            
            // Create Products object with cached images
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
                variants = emptyList(),
                images = productImages,
                secondaryBarcodes = null
            )
        }
    }

    override suspend fun insertCustomerDetailsIntoLocalDB(customer: Customer) {
            try {
                customerDao.insertCustomer(
                    CustomerMapper.toCustomerEntity(
                        customer
                    )
                )
            } catch (e: Exception) {
                throw e
            }
    }

}