package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryMeta(
  val name: String,
  val defaultLimit: Double,
  val icon: ImageVector,
  val iconName: String,
  val color: Color
)

data class PresetBudgetPhoto(
  val label: String,
  val url: String
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

  val selectableIcons: List<Pair<String, ImageVector>> = listOf(
    "grocery" to Icons.Default.LocalGroceryStore,
    "fastfood" to Icons.Default.Fastfood,
    "shopping" to Icons.Default.ShoppingBag,
    "transport" to Icons.Default.DirectionsBus,
    "entertainment" to Icons.Default.Movie,
    "bills" to Icons.Default.Receipt,
    "health" to Icons.Default.LocalHospital,
    "education" to Icons.Default.School,
    "savings" to Icons.Default.AccountBalance,
    "fitness" to Icons.Default.FitnessCenter,
    "pets" to Icons.Default.Pets,
    "travel" to Icons.Default.Flight,
    "gift" to Icons.Default.CardGiftcard,
    "coffee" to Icons.Default.LocalCafe,
    "home" to Icons.Default.Home,
    "tech" to Icons.Default.Computer,
    "gaming" to Icons.Default.SportsEsports,
    "other" to Icons.Default.Category
  )

  val selectableColors: List<Color> = listOf(
    Color(0xFF10B981), // Emerald Green
    Color(0xFF3B82F6), // Ocean Blue
    Color(0xFFF97316), // Orange
    Color(0xFFEC4899), // Pink
    Color(0xFF8B5CF6), // Purple
    Color(0xFFEAB308), // Amber / Gold
    Color(0xFFEF4444), // Crimson Red
    Color(0xFF06B6D4), // Cyan
    Color(0xFF14B8A6), // Teal
    Color(0xFF6366F1), // Indigo
    Color(0xFF64748B)  // Slate
  )

  // Curated, fast-loading Unsplash CDN presets for quick budget photos
  val presetPhotos: List<PresetBudgetPhoto> = listOf(
    PresetBudgetPhoto("Groceries", "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=600&q=80"),
    PresetBudgetPhoto("Dining", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=600&q=80"),
    PresetBudgetPhoto("Coffee", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=600&q=80"),
    PresetBudgetPhoto("Shopping", "https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=600&q=80"),
    PresetBudgetPhoto("Travel", "https://images.unsplash.com/photo-1488646953014-85cb44e25828?auto=format&fit=crop&w=600&q=80"),
    PresetBudgetPhoto("Tech", "https://images.unsplash.com/photo-1519389950473-47ba0277781c?auto=format&fit=crop&w=600&q=80"),
    PresetBudgetPhoto("Fitness", "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=600&q=80"),
    PresetBudgetPhoto("Home", "https://images.unsplash.com/photo-1484154218962-a197022b5858?auto=format&fit=crop&w=600&q=80")
  )

  fun getIcon(iconName: String): ImageVector {
    return selectableIcons.find { it.first == iconName }?.second
      ?: list.find { it.iconName == iconName }?.icon
      ?: Icons.Default.Category
  }

  fun getMeta(name: String, iconName: String? = null, colorHex: Long? = null): CategoryMeta {
    val defaultMatch = list.find { it.name.equals(name, ignoreCase = true) }
    if (defaultMatch != null && iconName == null && colorHex == null) {
      return defaultMatch
    }
    val resolvedIconName = iconName ?: defaultMatch?.iconName ?: "other"
    val resolvedIcon = getIcon(resolvedIconName)
    val resolvedColor = if (colorHex != null) Color(colorHex) else defaultMatch?.color ?: Color(0xFF10B981)
    val defaultLimit = defaultMatch?.defaultLimit ?: 150.0

    return CategoryMeta(
      name = name,
      defaultLimit = defaultLimit,
      icon = resolvedIcon,
      iconName = resolvedIconName,
      color = resolvedColor
    )
  }
}
