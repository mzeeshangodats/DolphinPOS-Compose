package com.retail.dolphinpos.data.mapper

import com.retail.dolphinpos.data.entities.category.CategoryEntity
import com.retail.dolphinpos.data.entities.customer.CustomerEntity
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
import com.retail.dolphinpos.domain.model.home.customer.Customer

object CustomerMapper {

    // -------------------------
    // Domain → Entity Mappers
    // -------------------------

    fun toCustomerEntity(customer: Customer): CustomerEntity {
        return CustomerEntity(
            userId = customer.userId,
            storeId = customer.storeId,
            locationId = customer.locationId,
            firstName = customer.firstName,
            lastName = customer.lastName,
            email = customer.email,
            birthday = customer.birthday,
            createdAt = customer.createdAt,
            updatedAt = customer.updatedAt
        )
    }

    // -------------------------
    // Entity → Domain Mappers
    // -------------------------

    fun toCustomer(
        customerEntity: List<CustomerEntity>,
    ): List<Customer> {
        return customerEntity.map { customer ->
            Customer(
                id = customer.id,
                userId = customer.userId,
                storeId = customer.storeId,
                locationId = customer.locationId,
                firstName = customer.firstName,
                lastName = customer.lastName,
                email = customer.email,
                birthday = customer.birthday,
                createdAt = customer.createdAt,
                updatedAt = customer.updatedAt
            )
        }
    }

}