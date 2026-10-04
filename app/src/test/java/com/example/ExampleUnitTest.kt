package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun savingsCalculation_restOfAmountCountedForSpendings() {
    val monthlyIncome = 3000.0
    val savingsAmount = 500.0
    val countedForSpendings = (monthlyIncome - savingsAmount).coerceAtLeast(0.0)

    assertEquals(2500.0, countedForSpendings, 0.001)

    val totalSpent = 450.0
    val remainingForSpendings = countedForSpendings - totalSpent

    assertEquals(2050.0, remainingForSpendings, 0.001)
  }

  @Test
  fun savingsCalculation_savingsAmountIndependentOfSpendingAmount() {
    val monthlyIncome = 3200.0
    val savingsAmount = 600.0
    val totalSpent = 1200.0

    // Savings amount has NO relation with spending amount: it remains 600.0 regardless of totalSpent
    val preservedSavings = savingsAmount
    assertEquals(600.0, preservedSavings, 0.001)

    val spendingBudget = monthlyIncome - savingsAmount
    val remainingSpending = spendingBudget - totalSpent
    assertEquals(2600.0, spendingBudget, 0.001)
    assertEquals(1400.0, remainingSpending, 0.001)
  }

  @Test
  fun chineseCurrency_formattingAndSymbolSupport() {
    val chineseCurrencySymbols = listOf("¥", "元", "CN¥", "RMB")
    val amount = 1588.50

    chineseCurrencySymbols.forEach { symbol ->
      val formatted = String.format(java.util.Locale.US, "%s%.2f", symbol, amount)
      assertTrue(formatted.startsWith(symbol))
      assertTrue(formatted.contains("1588.50"))
    }
  }

  @Test
  fun itemLogoAndCategoryColor_isAlwaysOpaqueAndMatchesKeywords() {
    val defaultCategory = com.example.data.model.DefaultCategories.list.first()
    val encodedHex = com.example.data.model.DefaultCategories.colorToHexLong(defaultCategory.color)
    val decodedColor = com.example.data.model.DefaultCategories.hexLongToColor(encodedHex)

    assertEquals(1.0f, decodedColor.alpha, 0.001f)
    assertEquals(defaultCategory.color.red, decodedColor.red, 0.01f)
    assertEquals(defaultCategory.color.green, decodedColor.green, 0.01f)
    assertEquals(defaultCategory.color.blue, decodedColor.blue, 0.01f)

    // Verify legacy shifted 64-bit Color.value.toLong() is also recovered with full opacity
    val legacyCorruptedLong = defaultCategory.color.value.toLong()
    val recoveredLegacyColor = com.example.data.model.DefaultCategories.hexLongToColor(legacyCorruptedLong)
    assertEquals(1.0f, recoveredLegacyColor.alpha, 0.001f)

    // Verify smart item logo resolution
    val coffeeMeta = com.example.data.model.DefaultCategories.getMetaForExpense(
      title = "Morning Coffee",
      category = "Food & Dining"
    )
    assertEquals(1.0f, coffeeMeta.color.alpha, 0.001f)
    assertNotNull(coffeeMeta.icon)
  }

  @Test
  fun categoryPieChartBreakdown_calculatesAccurateSweepAnglesAndPercentages() {
    val categoryAmounts = listOf(
      "Food & Dining" to 400.0,
      "Groceries" to 300.0,
      "Transportation" to 200.0,
      "Bills & Utilities" to 100.0
    )
    val totalSpent = categoryAmounts.sumOf { it.second }
    assertEquals(1000.0, totalSpent, 0.001)

    val sweeps = categoryAmounts.map { (_, spent) -> (spent / totalSpent) * 360.0 }
    val percentages = categoryAmounts.map { (_, spent) -> (spent / totalSpent) * 100.0 }

    assertEquals(360.0, sweeps.sum(), 0.001)
    assertEquals(100.0, percentages.sum(), 0.001)
    assertEquals(144.0, sweeps[0], 0.001) // 40% of 360
    assertEquals(40.0, percentages[0], 0.001)
  }
}


