package com.retail.dolphinpos.domain.model.auth.cash_denomination

import com.google.gson.annotations.SerializedName

/**
 * Error response structure for batch API errors
 * Example:
 * {
 *   "message": "Please fix the following errors.",
 *   "errors": {
 *     "startingCashAmount": "Please enter a valid Starting Cash Amount."
 *   }
 * }
 */
data class BatchErrorResponse(
    val message: String?,
    val errors: BatchErrors?
)

/**
 * Nested errors object containing field-specific error messages
 */
data class BatchErrors(
    @SerializedName("startingCashAmount")
    val startingCashAmount: String? = null,
    
    @SerializedName("batchNo")
    val batchNo: String? = null,
    
    @SerializedName("storeId")
    val storeId: String? = null,
    
    @SerializedName("userId")
    val userId: String? = null,
    
    @SerializedName("locationId")
    val locationId: String? = null,
    
    @SerializedName("storeRegisterId")
    val storeRegisterId: String? = null
)

