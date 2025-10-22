package com.retail.dolphinpos.data.entities.category

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey
    val id: Int,
    val title: String,
    val description: String,
    val productCount: Int
)
