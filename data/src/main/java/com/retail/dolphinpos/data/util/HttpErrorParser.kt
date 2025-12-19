package com.retail.dolphinpos.data.util

import com.google.gson.Gson
import retrofit2.HttpException

/**
 * Data class to represent error responses from the server
 */
data class ErrorResponse(
    val message: String?,
    val errors: Map<String, Any>? = null
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
                
                // Build error message from both message and errors fields
                val errorMessages = mutableListOf<String>()
                
                // Add main message if available
                errorResponse.message?.let { errorMessages.add(it) }
                
                // Add field-specific errors
                errorResponse.errors?.forEach { (field, value) ->
                    val errorValue = when (value) {
                        is List<*> -> value.joinToString(", ")
                        is String -> value
                        else -> value.toString()
                    }
                    errorMessages.add("$field: $errorValue")
                }
                
                errorMessages.joinToString("\n").ifEmpty { "Unknown error occurred" }
            } catch (parseException: Exception) {
                // If parsing as ErrorResponse fails, try to parse as generic map
                try {
                    val gson = Gson()
                    val errorMap = gson.fromJson(errorBody, Map::class.java) as? Map<*, *>
                    val errorMessages = mutableListOf<String>()
                    
                    // Add main message if available
                    (errorMap?.get("message") as? String)?.let { errorMessages.add(it) }
                    
                    // Parse errors field
                    val errorsField = errorMap?.get("errors")
                    when (errorsField) {
                        is Map<*, *> -> {
                            errorsField.forEach { (field, value) ->
                                val errorValue = when (value) {
                                    is List<*> -> value.joinToString(", ")
                                    is String -> value
                                    else -> value.toString()
                                }
                                errorMessages.add("$field: $errorValue")
                            }
                        }
                        is String -> {
                            errorMessages.add(errorsField)
                        }
                    }
                    
                    errorMessages.joinToString("\n").ifEmpty { "Unknown error occurred" }
                } catch (e: Exception) {
                    "Failed to parse error response"
                }
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

/**
 * Generalized error handler for API calls that return response types directly
 * @param httpException The HttpException to handle
 * @param defaultResponse A lambda that creates a default error response when parsing fails
 * @return The parsed error response or the default response
 */
inline fun <reified T> handleApiError(
    httpException: HttpException,
    crossinline defaultResponse: () -> T
): T {
    val errorResponse: T? = httpException.parseErrorResponse<T>()
    return errorResponse ?: defaultResponse()
}

/**
 * Generalized error handler for API calls that return Result<T>
 * @param httpException The HttpException to handle
 * @param defaultMessage Default error message when parsing fails
 * @param messageExtractor Optional lambda to extract message from error response
 * @return Result.failure with the error message
 */
inline fun <reified T> handleApiErrorResult(
    httpException: HttpException,
    defaultMessage: String = "Request failed",
    crossinline messageExtractor: (T) -> String? = { null }
): Result<T> {
    val errorResponse: T? = httpException.parseErrorResponse<T>()
    val errorMessage = if (errorResponse != null) {
        messageExtractor(errorResponse) ?: httpException.getErrorMessage().takeIf { it.isNotBlank() } ?: defaultMessage
    } else {
        httpException.getErrorMessage().takeIf { it.isNotBlank() } ?: defaultMessage
    }
    return Result.failure(Exception(errorMessage))
}

/**
 * Extension function to safely execute API calls with error handling
 * @param apiCall The suspend function to execute
 * @param defaultResponse A lambda that creates a default error response when parsing fails
 * @return The API response or the default error response
 */
suspend inline fun <reified T> safeApiCall(
    crossinline apiCall: suspend () -> T,
    crossinline defaultResponse: () -> T
): T {
    return try {
        apiCall()
    } catch (e: HttpException) {
        handleApiError(e, defaultResponse)
    } catch (e: Exception) {
        throw e
    }
}

/**
 * Extension function to safely execute API calls with Result<T> return type
 * @param apiCall The suspend function to execute
 * @param defaultMessage Default error message when parsing fails
 * @param messageExtractor Optional lambda to extract message from error response
 * @return Result.success or Result.failure
 */
suspend inline fun <reified T> safeApiCallResult(
    crossinline apiCall: suspend () -> T,
    defaultMessage: String = "Request failed",
    crossinline messageExtractor: (T) -> String? = { null }
): Result<T> {
    return try {
        Result.success(apiCall())
    } catch (e: HttpException) {
        handleApiErrorResult<T>(e, defaultMessage, messageExtractor)
    } catch (e: java.io.IOException) {
        // Re-throw IOException so it can be handled by the caller (e.g., for offline queueing)
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

