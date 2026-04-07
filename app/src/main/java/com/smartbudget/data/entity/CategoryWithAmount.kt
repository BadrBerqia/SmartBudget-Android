package com.smartbudget.data.entity

import androidx.room.ColumnInfo

data class CategoryWithAmount(
    @ColumnInfo(name = "categoryId") val categoryId: Long,
    @ColumnInfo(name = "categoryName") val categoryName: String,
    @ColumnInfo(name = "totalAmount") val totalAmount: Double
)
