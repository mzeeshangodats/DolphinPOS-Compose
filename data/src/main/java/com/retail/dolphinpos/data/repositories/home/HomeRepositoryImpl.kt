package com.retail.dolphinpos.data.repositories.home

import com.retail.dolphinpos.data.dao.CustomerDao
import com.retail.dolphinpos.data.dao.ProductsDao
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.entities.products.ProductsEntity
import com.retail.dolphinpos.data.mapper.CustomerMapper
import com.retail.dolphinpos.data.mapper.ProductMapper
import com.retail.dolphinpos.data.mapper.UserMapper
import com.retail.dolphinpos.domain.model.home.barcode.ProductVariantMatch
import com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.model.home.catrgories_products.Variant
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
        return productEntities.map { productEntity -> buildProduct(productEntity) }
    }

    override suspend fun getProductsByCategoryID(categoryID: Int): List<Products> {
        val productEntities = productsDao.getProductsByCategoryID(categoryID)
        return productEntities.map { productEntity -> buildProduct(productEntity) }
    }

    override suspend fun searchProducts(query: String): List<Products> {
        val productEntities = productsDao.searchProducts(query)
        return productEntities.map { productEntity -> buildProduct(productEntity) }
    }

    override suspend fun getProductByBarcode(barcode: String): Products? {
        val productEntity = productsDao.getProductByBarcode(barcode) ?: return null
        return buildProduct(productEntity)
    }

    override suspend fun getVariantByBarcode(barcode: String): ProductVariantMatch? {
        val variantEntity = productsDao.getVariantByBarcode(barcode) ?: return null
        val productEntity = productsDao.getProductById(variantEntity.productId) ?: return null
        val product = buildProduct(productEntity)
        val variant = product.variants?.firstOrNull { it.id == variantEntity.id } ?: return null
        return ProductVariantMatch(product, variant)
    }

    private suspend fun buildProduct(productEntity: ProductsEntity): Products {
        val productImages = storeRegistersRepository.getProductImagesWithLocalPaths(productEntity.id)

        val variantEntities = productsDao.getVariantsByProductId(productEntity.id)
        val variants = variantEntities.map { variantEntity ->
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