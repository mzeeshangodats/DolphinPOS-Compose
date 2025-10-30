package com.retail.dolphinpos.data.util

import com.google.gson.Gson
import retrofit2.HttpException

/**
 * Data class to represent error responses from the server
 */
data class ErrorResponse(
    val message: String?
)

/**
 * Extension function to parse error message from HttpException
 * @return The error message from the response body, or a default message if parsing fails
 */
fun HttpException.getErrorMessage(): String {
    return try {
        val errorBody = this.response()?.errorBody()?.string()
        if (errorBody != null) {
            try {
                val gson = Gson()
                val errorResponse: ErrorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                errorResponse.message ?: "Unknown error occurred"
            } catch (parseException: Exception) {
                "Failed to parse error response"
            }
        } else {
            "Unknown error occurred"
        }
    } catch (e: Exception) {
        "An error occurred: ${e.message}"
    }
}

/**
 * Parse a specific response type from an HttpException error body
 * @return The parsed response object, or null if parsing fails
 */
inline fun <reified T> HttpException.parseErrorResponse(): T? {
    return try {
        val errorBody = this.response()?.errorBody()?.string()
        if (errorBody != null) {
            try {
                val gson = Gson()
                gson.fromJson(errorBody, T::class.java)
            } catch (parseException: Exception) {
                null
            }
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

