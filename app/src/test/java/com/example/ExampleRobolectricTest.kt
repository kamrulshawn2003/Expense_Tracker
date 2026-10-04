package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthlyGoalEntity
import com.example.util.AccountBackupPayload
import com.example.util.AccountSyncManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Expense Tracker", appName)
  }

  @Test
  fun `account backup serialization and restoration preserves previous expenses`() {
    val email = "alex.chen@example.com"
    val hash = AccountSyncManager.hashPassword("secret123")
    val payload = AccountBackupPayload(
      email = email,
      displayName = "Alex Chen",
      passwordHash = hash,
      createdAt = 1700000000000L,
      lastSyncedAt = 1700000050000L,
      expenses = listOf(
        ExpenseEntity(
          id = 1L,
          title = "Dumpling Dinner",
          amount = 68.50,
          category = "Food & Dining",
          dateMillis = 1700000010000L,
          note = "Shanghai soup dumplings",
          paymentMethod = "Digital Wallet",
          userEmail = email
        )
      ),
      budgets = listOf(
        CategoryBudgetEntity(
          categoryName = "Food & Dining",
          monthlyLimit = 1500.0,
          iconName = "restaurant",
          colorHex = 0xFF10B981,
          userEmail = email
        )
      ),
      goals = listOf(
        MonthlyGoalEntity(
          yearMonth = "2026-10",
          savingsGoal = 2000.0,
          monthlyIncome = 10000.0,
          currencySymbol = "¥",
          userEmail = email
        )
      )
    )

    val json = AccountSyncManager.serializeBackupJson(payload)
    val restored = AccountSyncManager.parseBackupJson(json)

    assertNotNull(restored)
    assertEquals(email, restored!!.email)
    assertEquals("Alex Chen", restored.displayName)
    assertEquals(hash, restored.passwordHash)
    assertEquals(1, restored.expenses.size)
    assertEquals("Dumpling Dinner", restored.expenses[0].title)
    assertEquals(68.50, restored.expenses[0].amount, 0.001)
    assertEquals(1, restored.goals.size)
    assertEquals("¥", restored.goals[0].currencySymbol)
  }
}

