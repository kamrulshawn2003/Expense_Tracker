package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
    CategoryMeta("Food & Dining", 350.0, Icons.Default.Restaurant, "fastfood", Color(0xFFF97316)),
    CategoryMeta("Groceries", 400.0, Icons.Default.LocalGroceryStore, "grocery", Color(0xFF10B981)),
    CategoryMeta("Transportation", 180.0, Icons.Default.DirectionsBus, "transport", Color(0xFF3B82F6)),
    CategoryMeta("Shopping", 250.0, Icons.Default.ShoppingBag, "shopping", Color(0xFFEC4899)),
    CategoryMeta("Bills & Utilities", 300.0, Icons.Default.Receipt, "bills", Color(0xFF8B5CF6)),
    CategoryMeta("Housing & Rent", 800.0, Icons.Default.Home, "home", Color(0xFF14B8A6)),
    CategoryMeta("Healthcare", 120.0, Icons.Default.LocalHospital, "health", Color(0xFFEF4444)),
    CategoryMeta("Education", 150.0, Icons.Default.School, "education", Color(0xFF06B6D4)),
    CategoryMeta("Entertainment", 150.0, Icons.Default.Movie, "entertainment", Color(0xFFEAB308)),
    CategoryMeta("Other", 100.0, Icons.Default.Category, "other", Color(0xFF64748B))
  )

  val selectableIcons: List<Pair<String, ImageVector>> = listOf(
    "fastfood" to Icons.Default.Restaurant,
    "grocery" to Icons.Default.LocalGroceryStore,
    "transport" to Icons.Default.DirectionsBus,
    "car" to Icons.Default.DirectionsCar,
    "shopping" to Icons.Default.ShoppingBag,
    "clothes" to Icons.Default.Checkroom,
    "bills" to Icons.Default.Receipt,
    "home" to Icons.Default.Home,
    "health" to Icons.Default.LocalHospital,
    "education" to Icons.Default.School,
    "entertainment" to Icons.Default.Movie,
    "coffee" to Icons.Default.LocalCafe,
    "fitness" to Icons.Default.FitnessCenter,
    "pets" to Icons.Default.Pets,
    "travel" to Icons.Default.Flight,
    "gift" to Icons.Default.CardGiftcard,
    "tech" to Icons.Default.Computer,
    "gaming" to Icons.Default.SportsEsports,
    "savings" to Icons.Default.AccountBalance,
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

  /**
   * Converts a Compose [Color] into a 32-bit unsigned ARGB hex [Long] (e.g. 0xFF10B981L)
   * suitable for storing in Room and passing to `Color(Long)`.
   */
  fun colorToHexLong(color: Color): Long {
    return color.toArgb().toLong() and 0xFFFFFFFFL
  }

  /**
   * Safely converts a stored [Long] back into an opaque Compose [Color].
   * Automatically repairs values previously saved via `color.value.toLong()` (where ARGB is in the upper 32 bits).
   */
  fun hexLongToColor(colorHex: Long?, fallback: Color = Color(0xFF10B981)): Color {
    if (colorHex == null || colorHex == 0L) return fallback
    val lower32 = colorHex and 0xFFFFFFFFL
    val upper32 = (colorHex ushr 32) and 0xFFFFFFFFL
    val argb32 = if (lower32 == 0L && upper32 != 0L) {
      upper32
    } else {
      lower32
    }
    if ((argb32 and 0x00FFFFFFL) == 0L && fallback != Color.Black) {
      return fallback
    }
    val opaqueArgb = argb32 or 0xFF000000L
    return Color(opaqueArgb)
  }

  fun getIcon(iconName: String?): ImageVector {
    if (iconName.isNullOrBlank()) return Icons.Default.Category
    val key = iconName.trim().lowercase()
    return selectableIcons.find { it.first.equals(key, ignoreCase = true) }?.second
      ?: list.find { it.iconName.equals(key, ignoreCase = true) }?.icon
      ?: when (key) {
        "restaurant", "food", "dining" -> Icons.Default.Restaurant
        "groceries", "market", "supermarket" -> Icons.Default.LocalGroceryStore
        "bus", "transit", "transportation" -> Icons.Default.DirectionsBus
        "shop", "mall" -> Icons.Default.ShoppingBag
        "utility", "utilities", "receipt" -> Icons.Default.Receipt
        "medical", "hospital", "pharmacy" -> Icons.Default.LocalHospital
        "book", "study", "school" -> Icons.Default.School
        "movie", "cinema" -> Icons.Default.Movie
        else -> Icons.Default.Category
      }
  }

  /**
   * Resolves the best icon for a specific expense item title, falling back to its category icon.
   */
  fun getExpenseItemIcon(title: String, categoryIcon: ImageVector): ImageVector {
    val t = title.trim().lowercase()
    if (t.isBlank()) return categoryIcon
    return when {
      t.contains("coffee") || t.contains("tea") || t.contains("cafe") || t.contains("latte") || t.contains("espresso") || t.contains("starbucks") -> Icons.Default.LocalCafe
      t.contains("breakfast") || t.contains("lunch") || t.contains("dinner") || t.contains("restaurant") || t.contains("meal") || t.contains("pizza") || t.contains("burger") || t.contains("noodle") || t.contains("dumpling") || t.contains("rice") || t.contains("snack") || t.contains("food") -> Icons.Default.Fastfood
      t.contains("grocery") || t.contains("groceries") || t.contains("market") || t.contains("fruit") || t.contains("vegetable") || t.contains("milk") || t.contains("bread") || t.contains("meat") || t.contains("egg") -> Icons.Default.LocalGroceryStore
      t.contains("gas") || t.contains("fuel") || t.contains("petrol") -> Icons.Default.LocalGasStation
      t.contains("car") || t.contains("taxi") || t.contains("uber") || t.contains("did") || t.contains("cab") || t.contains("parking") || t.contains("toll") -> Icons.Default.DirectionsCar
      t.contains("bus") || t.contains("train") || t.contains("subway") || t.contains("metro") || t.contains("transit") || t.contains("ticket") -> Icons.Default.DirectionsBus
      t.contains("flight") || t.contains("plane") || t.contains("air") || t.contains("hotel") || t.contains("trip") || t.contains("travel") -> Icons.Default.Flight
      t.contains("rent") || t.contains("house") || t.contains("apartment") || t.contains("mortgage") || t.contains("furniture") || t.contains("repair") -> Icons.Default.Home
      t.contains("electric") || t.contains("power") -> Icons.Default.ElectricBolt
      t.contains("water") -> Icons.Default.WaterDrop
      t.contains("wifi") || t.contains("internet") || t.contains("phone") || t.contains("mobile") || t.contains("broadband") -> Icons.Default.Wifi
      t.contains("bill") || t.contains("tax") || t.contains("insurance") || t.contains("fee") -> Icons.Default.Receipt
      t.contains("doctor") || t.contains("hospital") || t.contains("clinic") || t.contains("pharmacy") || t.contains("medicine") || t.contains("vitamin") || t.contains("dental") || t.contains("health") -> Icons.Default.LocalHospital
      t.contains("book") || t.contains("course") || t.contains("class") || t.contains("tuition") || t.contains("school") || t.contains("college") || t.contains("pen") || t.contains("study") -> Icons.Default.MenuBook
      t.contains("gym") || t.contains("fitness") || t.contains("workout") || t.contains("yoga") || t.contains("sport") -> Icons.Default.FitnessCenter
      t.contains("shirt") || t.contains("shoe") || t.contains("clothes") || t.contains("dress") || t.contains("jacket") || t.contains("pant") || t.contains("apparel") -> Icons.Default.Checkroom
      t.contains("laptop") || t.contains("computer") || t.contains("mouse") || t.contains("keyboard") || t.contains("headphone") || t.contains("charger") || t.contains("tech") -> Icons.Default.Computer
      t.contains("movie") || t.contains("cinema") || t.contains("netflix") || t.contains("music") || t.contains("concert") || t.contains("show") -> Icons.Default.Movie
      t.contains("game") || t.contains("gaming") || t.contains("steam") -> Icons.Default.SportsEsports
      t.contains("gift") || t.contains("present") || t.contains("birthday") -> Icons.Default.CardGiftcard
      t.contains("pet") || t.contains("dog") || t.contains("cat") || t.contains("vet") -> Icons.Default.Pets
      else -> categoryIcon
    }
  }

  fun getMeta(name: String, iconName: String? = null, colorHex: Long? = null): CategoryMeta {
    val defaultMatch = list.find { it.name.equals(name.trim(), ignoreCase = true) }
    val fallbackColor = defaultMatch?.color ?: Color(0xFF10B981)
    val resolvedIconName = iconName?.takeIf { it.isNotBlank() && it != "category" }
      ?: defaultMatch?.iconName
      ?: "other"
    val resolvedIcon = if (resolvedIconName == "other" && defaultMatch != null) {
      defaultMatch.icon
    } else {
      getIcon(resolvedIconName)
    }
    val resolvedColor = hexLongToColor(colorHex, fallbackColor)
    val defaultLimit = defaultMatch?.defaultLimit ?: 150.0

    return CategoryMeta(
      name = name,
      defaultLimit = defaultLimit,
      icon = resolvedIcon,
      iconName = resolvedIconName,
      color = resolvedColor
    )
  }

  fun getMetaForExpense(
    title: String,
    category: String,
    iconName: String? = null,
    colorHex: Long? = null
  ): CategoryMeta {
    val baseMeta = getMeta(category, iconName, colorHex)
    val itemIcon = getExpenseItemIcon(title, baseMeta.icon)
    return baseMeta.copy(icon = itemIcon)
  }
}

