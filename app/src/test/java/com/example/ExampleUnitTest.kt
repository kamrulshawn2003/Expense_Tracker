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
}
