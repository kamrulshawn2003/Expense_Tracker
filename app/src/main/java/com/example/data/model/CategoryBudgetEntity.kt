package com.example.data.model

import androidx.room.Entity

@Entity(tableName = "category_budgets", primaryKeys = ["userEmail", "categoryName"])
data class CategoryBudgetEntity(
  val categoryName: String,
  val monthlyLimit: Double,
  val iconName: String = "category",
  val colorHex: Long = 0xFF10B981,
  val pictureUri: String? = null,
  val userEmail: String = "guest"
)
