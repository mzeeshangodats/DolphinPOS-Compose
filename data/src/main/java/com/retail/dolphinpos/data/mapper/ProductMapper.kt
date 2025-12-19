package com.retail.dolphinpos.data.mapper

import com.retail.dolphinpos.data.entities.category.CategoryEntity
import com.retail.dolphinpos.data.entities.products.ProductImagesEntity
import com.retail.dolphinpos.data.entities.products.ProductsEntity
import com.retail.dolphinpos.data.entities.products.VariantImagesEntity
import com.retail.dolphinpos.data.entities.products.VariantsEntity
import com.retail.dolphinpos.data.entities.products.VendorEntity
import com.google.gson.Gson
import com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData
import com.retail.dolphinpos.domain.model.home.catrgories_products.ProductImage
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.model.home.catrgories_products.Variant
import com.retail.dolphinpos.domain.model.home.catrgories_products.VariantImage
import com.retail.dolphinpos.domain.model.home.catrgories_products.Vendor
import com.retail.dolphinpos.domain.model.product.CreateProductRequest
import com.retail.dolphinpos.domain.model.product.ProductVariantRequest

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

    fun toProductEntity(products: Products, categoryId: Int, storeId: Int, locationId: Int): ProductsEntity {
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
            plu = products.plu,
            storeId = storeId,
            locationId = locationId,
            chargeTaxOnThisProduct = products.chargeTaxOnThisProduct,
            cardTax = products.cardTax,
            cashTax = products.cashTax
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
            cardPrice = variants.cardPrice,
            cashPrice = variants.cashPrice,
            quantity = variants.quantity,
            title = variants.title,
            sku = variants.sku,
            plu = variants.plu
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
                plu = product.plu,
                locationId = product.locationId,
                chargeTaxOnThisProduct = product.chargeTaxOnThisProduct,
                vendor = null,
                variants = emptyList(),
                images = emptyList(),
                secondaryBarcodes = null,
                cardTax = product.cardTax,
                cashTax = product.cashTax
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
            price = null,
            quantity = variantsEntity.quantity,
            sku = variantsEntity.sku,
            plu = variantsEntity.plu,
            cardPrice = variantsEntity.cardPrice,
            cashPrice = variantsEntity.cashPrice,
            barCode = null,
            attributes = null,
            images = emptyList()
        )
    }

    fun toVariant(variantsEntity: VariantsEntity, variantImages: List<VariantImage>): Variant {
        return Variant(
            id = variantsEntity.id,
            title = variantsEntity.title,
            price = null,
            quantity = variantsEntity.quantity,
            sku = variantsEntity.sku,
            plu = variantsEntity.plu,
            cardPrice = variantsEntity.cardPrice,
            cashPrice = variantsEntity.cashPrice,
            barCode = null,
            attributes = null,
            images = variantImages
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

    // -------------------------
    // CreateProductRequest → Entity Mappers
    // -------------------------

    fun toProductEntityFromRequest(request: CreateProductRequest): ProductsEntity {
        val gson = Gson()
        return ProductsEntity(
            id = 0, // Will be auto-generated
            serverId = null,
            categoryId = request.categoryId,
            storeId = request.storeId,
            name = request.name,
            description = request.description,
            quantity = request.quantity,
            status = request.status,
            cashPrice = request.cashPrice ?: request.price ?: "0.00",
            cardPrice = request.cardPrice ?: request.price ?: "0.00",
            price = request.price,
            compareAtPrice = null, // Can be added if needed
            costPrice = request.costPrice,
            barCode = request.barCode,
            secondaryBarCodes = if (request.secondaryBarCodes.isEmpty()) null else gson.toJson(request.secondaryBarCodes),
            chargeTaxOnThisProduct = true, // Default
            locationId = request.locationId,
            cardTax = 0.0,
            cashTax = 0.0,
            trackQuantity = request.trackQuantity,
            continueSellingWhenOutOfStock = request.continueSellingWhenOutOfStock,
            productVendorId = request.productVendorId,
            currentVendorId = request.currentVendorId,
            salesChannel = if (request.salesChannel.isEmpty()) null else gson.toJson(request.salesChannel),
            shippingWeight = request.shippingWeight,
            shippingWeightUnit = request.shippingWeightUnit,
            isPhysicalProduct = request.isPhysicalProduct,
            customsInformation = request.customsInformation,
            isEBTEligible = false, // Can be added to request if needed
            isIDRequired = false, // Can be added to request if needed
            isSynced = false, // Not synced yet
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun toVariantEntityFromRequest(
        variant: ProductVariantRequest,
        productId: Int,
        gson: Gson
    ): VariantsEntity {
        return VariantsEntity(
            id = 0, // Will be auto-generated
            serverId = null,
            productId = productId,
            cardPrice = variant.cardPrice ?: variant.price,
            cashPrice = variant.cashPrice ?: variant.price,
            price = variant.price,
            costPrice = variant.costPrice,
            quantity = variant.quantity,
            sku = variant.sku,
            title = variant.title,
            barCode = variant.barCode,
            locationId = variant.locationId,
            isSynced = false, // Not synced yet
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    // -------------------------
    // Entity → CreateProductRequest Mappers
    // -------------------------

    fun toCreateProductRequest(
        product: ProductsEntity,
        productImages: List<ProductImagesEntity>,
        variants: List<VariantsEntity>,
        variantImagesMap: Map<Int, List<VariantImagesEntity>> = emptyMap(),
        gson: Gson
    ): CreateProductRequest {
        return CreateProductRequest(
            name = product.name ?: "",
            description = product.description ?: "",
            images = productImages.map { image ->
                com.retail.dolphinpos.domain.model.product.ProductImageRequest(
                    fileURL = image.fileURL ?: "",
                    originalName = image.originalName ?: ""
                )
            },
            status = product.status ?: "active",
            price = product.price,
            costPrice = product.costPrice,
            trackQuantity = product.trackQuantity,
            quantity = product.quantity,
            continueSellingWhenOutOfStock = product.continueSellingWhenOutOfStock,
            salesChannel = product.salesChannel?.let { 
                val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                gson.fromJson<List<String>>(it, type)
            } ?: emptyList(),
            productVendorId = product.productVendorId,
            currentVendorId = product.currentVendorId,
            categoryId = product.categoryId,
            storeId = product.storeId,
            locationId = product.locationId,
            shippingWeight = product.shippingWeight,
            shippingWeightUnit = product.shippingWeightUnit,
            isPhysicalProduct = product.isPhysicalProduct,
            customsInformation = product.customsInformation,
            barCode = product.barCode,
            secondaryBarCodes = product.secondaryBarCodes?.let { 
                val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                gson.fromJson<List<String>>(it, type)
            } ?: emptyList(),
            //varients = emptyMap(), // Can be derived from variants if needed
            cashPrice = product.cashPrice,
            cardPrice = product.cardPrice,
            variants = variants.map { variant ->
                val variantImages = variantImagesMap[variant.id]?.map { imageEntity ->
                    com.retail.dolphinpos.domain.model.product.ProductImageRequest(
                        fileURL = imageEntity.fileURL ?: "",
                        originalName = imageEntity.originalName ?: ""
                    )
                } ?: emptyList()
                
                ProductVariantRequest(
                    title = variant.title ?: "",
                    price = variant.price,
                    costPrice = variant.costPrice,
                    quantity = variant.quantity,
                    barCode = variant.barCode,
                    sku = variant.sku,
                    cashPrice = variant.cashPrice,
                    cardPrice = variant.cardPrice,
                    locationId = variant.locationId ?: product.locationId,
                    images = variantImages.ifEmpty { null }
                )
            }
        )
    }

}