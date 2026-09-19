package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryMeta(
  val name: String,
  val defaultLimit: Double,
  val icon: ImageVector,
  val iconName: String,
  val color: Color
)

object DefaultCategories {
  val list = listOf(
    CategoryMeta("Food & Dining", 350.0, Icons.Default.Fastfood, "fastfood", Color(0xFFF97316)),
    CategoryMeta("Groceries", 400.0, Icons.Default.LocalGroceryStore, "grocery", Color(0xFF10B981)),
    CategoryMeta("Transportation", 180.0, Icons.Default.DirectionsBus, "transport", Color(0xFF3B82F6)),
    CategoryMeta("Shopping", 250.0, Icons.Default.ShoppingBag, "shopping", Color(0xFFEC4899)),
    CategoryMeta("Bills & Utilities", 300.0, Icons.Default.Receipt, "bills", Color(0xFF8B5CF6)),
    CategoryMeta("Entertainment", 150.0, Icons.Default.Movie, "entertainment", Color(0xFFEAB308)),
    CategoryMeta("Healthcare", 120.0, Icons.Default.LocalHospital, "health", Color(0xFFEF4444)),
    CategoryMeta("Education", 100.0, Icons.Default.School, "education", Color(0xFF06B6D4)),
    CategoryMeta("Savings & Invest", 500.0, Icons.Default.AccountBalance, "savings", Color(0xFF14B8A6)),
    CategoryMeta("Other", 100.0, Icons.Default.Category, "other", Color(0xFF64748B))
  )

  fun getMeta(name: String): CategoryMeta {
    return list.find { it.name.equals(name, ignoreCase = true) }
      ?: CategoryMeta(name, 100.0, Icons.Default.Category, "other", Color(0xFF64748B))
  }
}
