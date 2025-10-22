package com.retail.dolphinpos.data.mapper

import com.retail.dolphinpos.data.entities.category.CategoryEntity
import com.retail.dolphinpos.data.entities.products.ProductImagesEntity
import com.retail.dolphinpos.data.entities.products.ProductsEntity
import com.retail.dolphinpos.data.entities.products.VariantImagesEntity
import com.retail.dolphinpos.data.entities.products.VariantsEntity
import com.retail.dolphinpos.data.entities.products.VendorEntity
import com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData
import com.retail.dolphinpos.domain.model.home.catrgories_products.ProductImage
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.model.home.catrgories_products.Variant
import com.retail.dolphinpos.domain.model.home.catrgories_products.VariantImage
import com.retail.dolphinpos.domain.model.home.catrgories_products.Vendor

object ProductMapper {

    // -------------------------
    // Domain → Entity Mappers
    // -------------------------

    fun toCategoryEntity(category: CategoryData): CategoryEntity {
        return CategoryEntity(
            id = category.id,
            title = category.title,
            description = category.description,
            productCount = category.productCount
        )
    }

    fun toProductEntity(products: Products, categoryId: Int): ProductsEntity {
        return ProductsEntity(
            id = products.id,
            categoryId = categoryId,
            name = products.name,
            description = products.description,
            quantity = products.quantity,
            status = products.status,
            cashPrice = products.cashPrice,
            cardPrice = products.cardPrice,
            barCode = products.barCode,
            storeId = products.storeId,
            locationId = products.locationId,
            chargeTaxOnThisProduct = products.chargeTaxOnThisProduct
        )
    }

    fun toProductImagesEntity(productImages: ProductImage, productId: Int): ProductImagesEntity {
        return ProductImagesEntity(
            productId = productId,
            fileURL = productImages.fileURL,
            originalName = productImages.originalName
        )
    }

    fun toProductVariantsEntity(variants: Variant, productId: Int): VariantsEntity {
        return VariantsEntity(
            id = variants.id,
            productId = productId,
            price = variants.price,
            quantity = variants.quantity,
            title = variants.title,
            sku = variants.sku
        )
    }

    fun toVariantImagesEntity(variantImages: VariantImage, variantId: Int): VariantImagesEntity {
        return VariantImagesEntity(
            variantId = variantId,
            fileURL = variantImages.fileURL,
            originalName = variantImages.originalName
        )
    }

    fun toProductVendorEntity(vendor: Vendor, productId: Int): VendorEntity {
        return VendorEntity(
            id = vendor.id,
            productId = productId,
            title = vendor.title
        )
    }

    // -------------------------
    // Entity → Domain Mappers
    // -------------------------

    fun toCategory(
        categoryEntity: List<CategoryEntity>,
    ): List<CategoryData> {
        return categoryEntity.map { category ->
            CategoryData(
                id = category.id,
                title = category.title,
                description = category.description,
                productCount = category.productCount,
                products = emptyList()
            )
        }
    }

    fun toProducts(
        productsEntity: List<ProductsEntity>,
    ): List<Products> {
        return productsEntity.map { product ->
            Products(
                id = product.id,
                categoryId = product.categoryId,
                storeId = product.storeId,
                name = product.name,
                description = product.description,
                quantity = product.quantity,
                status = product.status,
                cashPrice = product.cashPrice,
                cardPrice = product.cardPrice,
                barCode = product.barCode,
                locationId = product.locationId,
                chargeTaxOnThisProduct = product.chargeTaxOnThisProduct,
                vendor = null,
                variants = emptyList(),
                images = emptyList(),
                secondaryBarcodes = null
            )
        }
    }

    fun toProductImage(productImagesEntity: ProductImagesEntity): ProductImage {
        return ProductImage(
            fileURL = productImagesEntity.fileURL,
            originalName = productImagesEntity.originalName
        )
    }


    fun toVariant(variantsEntity: VariantsEntity): Variant {
        return Variant(
            id = variantsEntity.id,
            title = variantsEntity.title,
            price = variantsEntity.price,
            quantity = variantsEntity.quantity,
            sku = variantsEntity.sku,
            attributes = null,
            images = emptyList()
        )
    }

    fun toVariantImage(variantImagesEntity: VariantImagesEntity): VariantImage {
        return VariantImage(
            fileURL = variantImagesEntity.fileURL,
            originalName = variantImagesEntity.originalName
        )
    }

    fun toVendor(vendorEntity: VendorEntity): Vendor {
        return Vendor(
            id = vendorEntity.id,
            title = vendorEntity.title
        )
    }

}