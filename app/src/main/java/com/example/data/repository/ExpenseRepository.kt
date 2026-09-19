package com.example.data.repository

import android.content.Context
import com.example.data.db.ExpenseDao
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.DefaultCategories
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthlyGoalEntity
import com.example.notification.BudgetNotificationManager
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseRepository(
  private val expenseDao: ExpenseDao,
  private val context: Context
) {

  val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
  val categoryBudgets: Flow<List<CategoryBudgetEntity>> = expenseDao.getAllCategoryBudgets()

  fun getExpensesForDateRange(startMillis: Long, endMillis: Long): Flow<List<ExpenseEntity>> {
    return expenseDao.getExpensesBetween(startMillis, endMillis)
  }

  fun getMonthlyGoal(yearMonth: String): Flow<MonthlyGoalEntity?> {
    return expenseDao.getMonthlyGoal(yearMonth)
  }

  suspend fun insertExpense(expense: ExpenseEntity): Long {
    val id = expenseDao.insertExpense(expense)

    // Check if adding this expense pushed category over limit for the expense's month
    checkAndNotifyCategoryLimit(expense.category, expense.dateMillis)

    return id
  }

  suspend fun updateExpense(expense: ExpenseEntity) {
    expenseDao.updateExpense(expense)
    checkAndNotifyCategoryLimit(expense.category, expense.dateMillis)
  }

  suspend fun deleteExpense(expense: ExpenseEntity) {
    expenseDao.deleteExpense(expense)
  }

  suspend fun deleteExpenseById(id: Long) {
    expenseDao.deleteExpenseById(id)
  }

  suspend fun resetMonthCycleExpenses(yearMonth: String) {
    val cal = Calendar.getInstance()
    try {
      val parsedDate = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(yearMonth)
      if (parsedDate != null) cal.time = parsedDate
    } catch (_: Exception) {}

    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startOfMonth = cal.timeInMillis

    cal.add(Calendar.MONTH, 1)
    cal.add(Calendar.MILLISECOND, -1)
    val endOfMonth = cal.timeInMillis

    expenseDao.deleteExpensesBetween(startOfMonth, endOfMonth)
  }

  suspend fun updateCategoryBudget(budget: CategoryBudgetEntity) {
    expenseDao.insertOrUpdateCategoryBudget(budget)
  }

  suspend fun updateMonthlyGoal(goal: MonthlyGoalEntity) {
    expenseDao.insertOrUpdateMonthlyGoal(goal)
  }

  private suspend fun checkAndNotifyCategoryLimit(categoryName: String, expenseDateMillis: Long) {
    val budget = expenseDao.getCategoryBudgetSync(categoryName) ?: return
    if (budget.monthlyLimit <= 0) return

    val cal = Calendar.getInstance().apply {
      timeInMillis = expenseDateMillis
      set(Calendar.DAY_OF_MONTH, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val startOfMonth = cal.timeInMillis

    cal.add(Calendar.MONTH, 1)
    cal.add(Calendar.MILLISECOND, -1)
    val endOfMonth = cal.timeInMillis

    val monthExpenses = expenseDao.getExpensesForCategoryBetweenSync(categoryName, startOfMonth, endOfMonth)
    val totalSpent = monthExpenses.sumOf { it.amount }

    if (totalSpent > budget.monthlyLimit) {
      val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(expenseDateMillis))
      val goal = expenseDao.getMonthlyGoalSync(yearMonth)
      val currency = goal?.currencySymbol ?: "$"
      BudgetNotificationManager.sendLimitExceededNotification(
        context = context,
        categoryName = categoryName,
        totalSpent = totalSpent,
        limit = budget.monthlyLimit,
        currencySymbol = currency
      )
    }
  }

  suspend fun ensureDefaultBudgets() {
    val defaults = DefaultCategories.list.map {
      CategoryBudgetEntity(
        categoryName = it.name,
        monthlyLimit = it.defaultLimit,
        iconName = it.iconName,
        colorHex = it.color.value.toLong()
      )
    }
    expenseDao.insertCategoryBudgets(defaults)
  }

  suspend fun populateSampleData() {
    val cal = Calendar.getInstance()
    val currentDay = cal.get(Calendar.DAY_OF_MONTH)

    fun getDayMillis(day: Int, hour: Int, minute: Int): Long {
      val c = Calendar.getInstance()
      val safeDay = day.coerceIn(1, c.getActualMaximum(Calendar.DAY_OF_MONTH))
      c.set(Calendar.DAY_OF_MONTH, safeDay)
      c.set(Calendar.HOUR_OF_DAY, hour)
      c.set(Calendar.MINUTE, minute)
      c.set(Calendar.SECOND, 0)
      c.set(Calendar.MILLISECOND, 0)
      return c.timeInMillis
    }

    val d1 = getDayMillis(currentDay, 9, 30)
    val d2 = getDayMillis(currentDay, 13, 15)
    val d3 = getDayMillis((currentDay - 1).coerceAtLeast(1), 18, 45)
    val d4 = getDayMillis((currentDay - 2).coerceAtLeast(1), 12, 10)
    val d5 = getDayMillis((currentDay - 3).coerceAtLeast(1), 16, 20)
    val d6 = getDayMillis((currentDay - 4).coerceAtLeast(1), 14, 0)
    val d7 = getDayMillis((currentDay - 5).coerceAtLeast(1), 11, 30)

    val samples = listOf(
      ExpenseEntity(title = "Morning Cappuccino", amount = 4.75, category = "Food & Dining", dateMillis = d1, paymentMethod = "Card", note = "Espresso bar"),
      ExpenseEntity(title = "Healthy Lunch Bowl", amount = 14.50, category = "Food & Dining", dateMillis = d2, paymentMethod = "Digital Wallet", note = "Salad stop"),
      ExpenseEntity(title = "Whole Foods Organic Groceries", amount = 112.30, category = "Groceries", dateMillis = d3, paymentMethod = "Card", note = "Weekly veggies & fruits"),
      ExpenseEntity(title = "Subway Card Top-up", amount = 30.00, category = "Transportation", dateMillis = d3, paymentMethod = "Card", note = "Commute"),
      ExpenseEntity(title = "Electric & Water Bill", amount = 85.20, category = "Bills & Utilities", dateMillis = d4, paymentMethod = "Bank Transfer", note = "Monthly utility"),
      ExpenseEntity(title = "Cinema Tickets & Popcorn", amount = 36.00, category = "Entertainment", dateMillis = d4, paymentMethod = "Card", note = "Weekend movie"),
      ExpenseEntity(title = "New Running Shoes", amount = 89.99, category = "Shopping", dateMillis = d5, paymentMethod = "Card", note = "Nike store sale"),
      ExpenseEntity(title = "Pharmacy & Vitamins", amount = 24.50, category = "Healthcare", dateMillis = d6, paymentMethod = "Cash", note = "Multivitamins"),
      ExpenseEntity(title = "Online Programming Course", amount = 29.99, category = "Education", dateMillis = d7, paymentMethod = "Card", note = "Kotlin Masterclass")
    )

    for (s in samples) {
      expenseDao.insertExpense(s)
    }
  }
}
