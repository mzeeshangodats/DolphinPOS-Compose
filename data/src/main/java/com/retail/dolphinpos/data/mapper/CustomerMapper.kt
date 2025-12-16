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
import com.retail.dolphinpos.domain.model.home.customer.AddCustomerRequest
import com.retail.dolphinpos.domain.model.home.customer.Customer

object CustomerMapper {

    // -------------------------
    // Domain → Entity Mappers
    // -------------------------

    fun toCustomerEntity(customer: Customer): CustomerEntity {
        // Parse birthday to get month and year if needed
        val birthdayString = if (customer.birthMonth.isNotEmpty() && customer.birthYear.isNotEmpty()) {
            "01/${customer.birthMonth}/${customer.birthYear}" // Format: day/month/year
        } else {
            ""
        }
        
        return CustomerEntity(
            userId = customer.storeId, // Note: Customer domain model doesn't have userId, using storeId as fallback
            storeId = customer.storeId,
            locationId = customer.storeId, // Note: Customer domain model doesn't have locationId, using storeId as fallback
            firstName = customer.firstName,
            lastName = customer.lastName,
            email = customer.email,
            phoneNumber = customer.phoneNumber,
            birthday = birthdayString,
            createdAt = customer.createdAt.toLongOrNull() ?: System.currentTimeMillis(),
            updatedAt = customer.updatedAt,
            isSynced = false
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
                id = customer.serverId ?: customer.id,
                storeId = customer.storeId,
                firstName = customer.firstName,
                lastName = customer.lastName,
                email = customer.email,
                phoneNumber = customer.phoneNumber,
                birthMonth = parseBirthMonth(customer.birthday),
                birthYear = parseBirthYear(customer.birthday),
                createdAt = customer.createdAt.toString(),
                updatedAt = customer.updatedAt,
                agreedToMarketingEmails = false,
                agreedToMarketingSMS = false,
                deletedAt = "",
                pointsEarned = 0
            )
        }
    }

    /**
     * Convert CustomerEntity to AddCustomerRequest for API sync
     */
    fun toAddCustomerRequest(customerEntity: CustomerEntity): AddCustomerRequest {
        return AddCustomerRequest(
            firstName = customerEntity.firstName,
            lastName = customerEntity.lastName,
            email = customerEntity.email,
            phoneNumber = customerEntity.phoneNumber,
            birthMonth = parseBirthMonth(customerEntity.birthday),
            birthYear = parseBirthYear(customerEntity.birthday),
            storeId = customerEntity.storeId
        )
    }

    /**
     * Parse birthday string (format: "day/month/year") to extract month
     */
    private fun parseBirthMonth(birthday: String): String {
        if (birthday.isEmpty() || birthday == "Select Birthday") {
            return ""
        }
        return try {
            val parts = birthday.split("/")
            if (parts.size >= 2) {
                parts[1].trim()
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Parse birthday string (format: "day/month/year") to extract year
     */
    private fun parseBirthYear(birthday: String): String {
        if (birthday.isEmpty() || birthday == "Select Birthday") {
            return ""
        }
        return try {
            val parts = birthday.split("/")
            if (parts.size >= 3) {
                parts[2].trim()
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

}