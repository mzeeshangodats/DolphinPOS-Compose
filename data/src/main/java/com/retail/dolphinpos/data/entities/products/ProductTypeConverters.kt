package com.retail.dolphinpos.data.entities.products

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ProductTypeConverters {
    private val gson = Gson()

    // Map<String, String> converters for attributes
    @TypeConverter
    fun fromAttributesMap(value: Map<String, String>?): String? {
        return if (value == null) null else gson.toJson(value)
    }

    @TypeConverter
    fun toAttributesMap(value: String?): Map<String, String>? {
        return if (value == null) null else {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(value, type)
        }
    }

    // List<String> converters for salesChannel and secondaryBarCodes
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return if (value == null) null else gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return if (value == null) null else {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, type)
        }
    }
}

