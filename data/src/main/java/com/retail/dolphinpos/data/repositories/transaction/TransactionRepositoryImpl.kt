package com.retail.dolphinpos.data.repositories.transaction

import com.google.gson.Gson
import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.data.dao.TransactionDao
import com.retail.dolphinpos.data.entities.transaction.PaymentMethod
import com.retail.dolphinpos.data.entities.transaction.TransactionEntity
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.util.safeApiCall
import com.retail.dolphinpos.domain.model.TaxDetail
import com.retail.dolphinpos.domain.model.transaction.Transaction
import com.retail.dolphinpos.domain.model.transaction.TransactionItem
import com.retail.dolphinpos.domain.model.transaction.TransactionResponse
import com.retail.dolphinpos.domain.repositories.transaction.TransactionRepository
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class TransactionRepositoryImpl(
    private val apiService: ApiService,
    private val transactionDao: TransactionDao,
    private val networkMonitor: NetworkMonitor,
    private val gson: Gson
) : TransactionRepository {

    override suspend fun getTransactionsFromApi(
        storeId: Int,
        locationId: Int?,
        page: Int,
        limit: Int
    ): TransactionResponse {
        return safeApiCall(
            apiCall = {
                apiService.getTransactions(
                    storeId = storeId,
                    locationId = locationId,
                    page = page,
                    limit = limit
                )
            },
            defaultResponse = {
                TransactionResponse(
                    data = null
                )
            }
        )
    }

    override suspend fun getTransactionsFromLocal(storeId: Int): List<Transaction> {
        val entities = transactionDao.getTransactionsByStoreId(storeId)
        return entities.map { convertEntityToDomain(it) }
    }

    override suspend fun fetchAndSaveTransactionsFromApi(
        storeId: Int,
        locationId: Int?,
        page: Int,
        limit: Int
    ): List<Transaction> {
        val response = getTransactionsFromApi(storeId, locationId, page, limit)
        val transactionData = response.data
        return if (transactionData != null && transactionData.list.isNotEmpty()) {
            val entities = transactionData.list.map { apiTransaction ->
                convertApiTransactionToEntity(apiTransaction)
            }
            transactionDao.insertTransactions(entities)
            entities.map { convertEntityToDomain(it) }
        } else {
            emptyList()
        }
    }

    override suspend fun saveTransactionsToLocal(transactions: List<Transaction>) {
        val entities = transactions.map { convertDomainToEntity(it) }
        transactionDao.insertTransactions(entities)
    }

    override suspend fun saveTransactionToLocal(transaction: Transaction): Long {
        val entity = convertDomainToEntity(transaction)
        return transactionDao.insertTransaction(entity)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        val entity = convertDomainToEntity(transaction)
        transactionDao.updateTransaction(entity)
    }

    override suspend fun getUnsyncedTransactions(): List<Transaction> {
        val entities = transactionDao.getTransactionsByStatus("pending")
        return entities.map { convertEntityToDomain(it) }
    }

    override suspend fun syncTransactionToServer(transaction: Transaction): Result<Unit> {
        return if (networkMonitor.isNetworkAvailable()) {
            try {
                // Transactions are synced as part of order sync
                // This can be extended if there's a separate transaction sync endpoint
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("No network connection"))
        }
    }

    /**
     * Convert API TransactionItem to TransactionEntity
     */
    private fun convertApiTransactionToEntity(apiTransaction: TransactionItem): TransactionEntity {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        
        val createdAt = try {
            dateFormat.parse(apiTransaction.createdAt)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
        
        val updatedAt = apiTransaction.updatedAt?.let {
            try {
                dateFormat.parse(it)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        } ?: System.currentTimeMillis()

        // Get order number from nested order object
        val orderNo = apiTransaction.order?.orderNumber
        
        // Get batch number from nested batch object
        val batchNo = apiTransaction.batch?.batchNo

        // Convert amount from string to double
        val amount = apiTransaction.amount.toDoubleOrNull() ?: 0.0

        // Convert cardDetails to JSON string if it's an object
        val cardDetailsJson: String? = when {
            apiTransaction.cardDetails == null -> null
            apiTransaction.cardDetails is String -> apiTransaction.cardDetails as String
            else -> {
                // If it's an object, convert it to JSON string
                try {
                    gson.toJson(apiTransaction.cardDetails)
                } catch (e: Exception) {
                    null
                }
            }
        }

        // Extract taxDetails from order object
        // Aggregate store-level taxes (order.taxDetails) and product-level taxes (order.orderItems[].appliedTaxes)
        val aggregatedTaxDetails = mutableListOf<TaxDetail>()
        
        // Add store-level taxes from order.taxDetails
        apiTransaction.order?.taxDetails?.let { storeTaxDetails ->
            aggregatedTaxDetails.addAll(storeTaxDetails)
        }
        
        // Aggregate product-level taxes from orderItems
        apiTransaction.order?.orderItems?.forEach { orderItem ->
            orderItem.appliedTaxes?.forEach { productTax ->
                // Check if this tax already exists in aggregated list (by title and value)
                val existingTax = aggregatedTaxDetails.find { 
                    it.title == productTax.title && it.value == productTax.value 
                }
                if (existingTax != null) {
                    // Sum the amounts if tax already exists
                    val index = aggregatedTaxDetails.indexOf(existingTax)
                    aggregatedTaxDetails[index] = existingTax.copy(
                        amount = (existingTax.amount ?: 0.0) + (productTax.amount ?: 0.0)
                    )
                } else {
                    // Add new tax
                    aggregatedTaxDetails.add(productTax)
                }
            }
        }
        
        // Convert aggregated taxDetails to JSON string
        val taxDetailsJson: String? = if (aggregatedTaxDetails.isNotEmpty()) {
            try {
                gson.toJson(aggregatedTaxDetails)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
        
        return TransactionEntity(
            id = 0, // Will be auto-generated
            orderNo = orderNo,
            orderId = apiTransaction.orderId,
            storeId = apiTransaction.storeId,
            locationId = null, // Not provided in API response
            paymentMethod = PaymentMethod.fromString(apiTransaction.paymentMethod),
            status = apiTransaction.status,
            amount = amount,
            invoiceNo = apiTransaction.invoiceNo,
            batchId = apiTransaction.batchId,
            batchNo = batchNo,
            userId = apiTransaction.userId,
            orderSource = apiTransaction.orderSource,
            tax = apiTransaction.tax,
            tip = apiTransaction.tip,
            cardDetails = cardDetailsJson,
            createdAt = createdAt,
            updatedAt = updatedAt,
            taxDetails = taxDetailsJson
        )
    }

    /**
     * Convert TransactionEntity (data layer) to Transaction (domain layer)
     */
    private fun convertEntityToDomain(entity: TransactionEntity): Transaction {
        // Parse taxDetails from JSON string
        val taxDetails: List<TaxDetail>? = entity.taxDetails?.let {
            try {
                val type = object : TypeToken<List<TaxDetail>>() {}.type
                gson.fromJson<List<TaxDetail>>(it, type)
            } catch (e: Exception) {
                null
            }
        }
        
        return Transaction(
            id = entity.id,
            orderNo = entity.orderNo,
            orderId = entity.orderId,
            storeId = entity.storeId,
            locationId = entity.locationId,
            paymentMethod = entity.paymentMethod.value,
            status = entity.status,
            amount = entity.amount,
            invoiceNo = entity.invoiceNo,
            batchId = entity.batchId,
            batchNo = entity.batchNo,
            userId = entity.userId,
            orderSource = entity.orderSource,
            tax = entity.tax,
            tip = entity.tip,
            cardDetails = entity.cardDetails,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            taxDetails = taxDetails
        )
    }

    /**
     * Convert Transaction (domain layer) to TransactionEntity (data layer)
     */
    private fun convertDomainToEntity(transaction: Transaction): TransactionEntity {
        // Convert taxDetails to JSON string
        val taxDetailsJson: String? = transaction.taxDetails?.let {
            try {
                gson.toJson(it)
            } catch (e: Exception) {
                null
            }
        }
        
        return TransactionEntity(
            id = transaction.id,
            orderNo = transaction.orderNo,
            orderId = transaction.orderId,
            storeId = transaction.storeId,
            locationId = transaction.locationId,
            paymentMethod = PaymentMethod.fromString(transaction.paymentMethod),
            status = transaction.status,
            amount = transaction.amount,
            invoiceNo = transaction.invoiceNo,
            batchId = transaction.batchId,
            batchNo = transaction.batchNo,
            userId = transaction.userId,
            orderSource = transaction.orderSource,
            tax = transaction.tax,
            tip = transaction.tip,
            cardDetails = transaction.cardDetails,
            createdAt = transaction.createdAt,
            updatedAt = transaction.updatedAt,
            taxDetails = taxDetailsJson
        )
    }
}

