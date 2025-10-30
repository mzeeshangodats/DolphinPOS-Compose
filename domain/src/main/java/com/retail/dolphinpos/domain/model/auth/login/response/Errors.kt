package com.retail.dolphinpos.domain.model.auth.login.response

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

data class Errors(
    @JsonAdapter(StringOrListAdapter::class)
    val username: List<String>?,
    @JsonAdapter(StringOrListAdapter::class)
    val password: List<String>?
) {
    fun getUsernameError(): String? = username?.firstOrNull()
    
    fun getPasswordError(): String? = password?.firstOrNull()
    
    fun hasErrors(): Boolean = !username.isNullOrEmpty() || !password.isNullOrEmpty()
}

/**
 * Custom deserializer for ErrorStringOrList
 * Handles both cases: single string or array of strings
 */
class StringOrListAdapter : JsonDeserializer<List<String>?> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): List<String>? {
        if (json == null || json.isJsonNull) {
            return null
        }
        
        return try {
            when {
                json.isJsonArray -> {
                    // Handle array case
                    json.asJsonArray.mapNotNull { it.asString }
                }
                json.isJsonPrimitive -> {
                    // Handle single string case
                    listOf(json.asString)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
