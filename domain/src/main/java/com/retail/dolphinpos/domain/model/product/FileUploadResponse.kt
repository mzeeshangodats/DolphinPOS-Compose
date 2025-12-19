package com.retail.dolphinpos.domain.model.product

import com.google.gson.annotations.JsonAdapter
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

data class FileUploadResponse(
    val data: FileUploadData
)

data class FileUploadData(
    @JsonAdapter(FileUploadImageAdapter::class)
    val images: FileUploadImageList
)

// Wrapper to handle both single image and array of images
data class FileUploadImageList(
    val images: List<FileUploadImage>
) {
    companion object {
        fun fromSingle(image: FileUploadImage): FileUploadImageList {
            return FileUploadImageList(listOf(image))
        }
    }
}

data class FileUploadImage(
    val url: String,
    val mimetype: String? = null,
    val originalname: String? = null
)

// Custom adapter to handle both single object and array
class FileUploadImageAdapter : JsonDeserializer<FileUploadImageList> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): FileUploadImageList {
        return when {
            json == null -> FileUploadImageList(emptyList())
            json.isJsonArray -> {
                val images = json.asJsonArray.map { element ->
                    context?.deserialize<FileUploadImage>(element, FileUploadImage::class.java)
                }.filterNotNull()
                FileUploadImageList(images)
            }
            json.isJsonObject -> {
                val image = context?.deserialize<FileUploadImage>(json, FileUploadImage::class.java)
                FileUploadImageList(listOfNotNull(image))
            }
            else -> throw JsonParseException("Expected array or object for images")
        }
    }
}

