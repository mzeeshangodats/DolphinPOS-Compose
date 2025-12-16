package com.retail.dolphinpos.domain.model.home.customer

import com.google.gson.annotations.SerializedName

/**
 * Error response structure for customer API errors
 * Example:
 * {
 *   "message": "Please fix the following errors.",
 *   "errors": {
 *     "email": "Email already exists",
 *     "phoneNumber": "Phone Number field is required."
 *   }
 * }
 */
data class CustomerErrorResponse(
    val message: String?,
    val errors: CustomerErrors?
)

/**
 * Nested errors object containing field-specific error messages
 */
data class CustomerErrors(
    @SerializedName("email")
    val email: String? = null,
    
    @SerializedName("phoneNumber")
    val phoneNumber: String? = null,
    
    @SerializedName("firstName")
    val firstName: String? = null,
    
    @SerializedName("lastName")
    val lastName: String? = null
)

