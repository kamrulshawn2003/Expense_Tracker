package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.CategoryMeta
import com.example.data.model.DefaultCategories
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthlyGoalEntity
import com.example.data.repository.ExpenseRepository
import com.example.notification.BudgetNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategorySpendingSummary(
  val categoryName: String,
  val meta: CategoryMeta,
  val spent: Double,
  val limit: Double,
  val percentageOfLimit: Float,
  val percentageOfTotal: Float,
  val isExceeded: Boolean,
  val isWarning: Boolean
)

data class DaySpendingSummary(
  val dayNumber: Int,
  val dateLabel: String,
  val amount: Double
)

data class MonthBudgetReport(
  val yearMonth: String,
  val monthDisplay: String,
  val totalSpent: Double,
  val monthlyIncome: Double,
  val savingsGoal: Double,
  val netSavings: Double,
  val savingsGoalProgress: Float, // 0.0 to 1.0+
  val isSavingsGoalMet: Boolean,
  val categorySummaries: List<CategorySpendingSummary>,
  val dailyBreakdown: List<DaySpendingSummary>,
  val topSpendingCategory: String?,
  val totalBudgetLimit: Double,
  val overallBudgetRemaining: Double,
  val currencySymbol: String,
  val daysInCycle: Int = 30,
  val daysElapsed: Int = 1,
  val daysRemainingInCycle: Int = 29,
  val isCurrentCycle: Boolean = true,
  val nextCycleResetDate: String = ""
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: ExpenseRepository
  private val context = application.applicationContext

  // Selected date for Daily view (start of day millis)
  private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
  val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

  // Selected Year-Month for Monthly view (e.g. "2026-08")
  private val _selectedYearMonth = MutableStateFlow(
    SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
  )
  val selectedYearMonth: StateFlow<String> = _selectedYearMonth.asStateFlow()

  // In-app alert dismissed tracker
  private val _dismissedAlerts = MutableStateFlow<Set<String>>(emptySet())
  val dismissedAlerts: StateFlow<Set<String>> = _dismissedAlerts.asStateFlow()

  init {
    val database = AppDatabase.getDatabase(context, viewModelScope)
    repository = ExpenseRepository(database.expenseDao(), context)
    viewModelScope.launch {
      repository.ensureDefaultBudgets()
    }
  }

  val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val categoryBudgets: StateFlow<List<CategoryBudgetEntity>> = repository.categoryBudgets
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Dynamic monthly goal flow for selected month
  private val _monthlyGoalData = MutableStateFlow(MonthlyGoalEntity(yearMonth = _selectedYearMonth.value))
  val monthlyGoal: StateFlow<MonthlyGoalEntity> = _monthlyGoalData.asStateFlow()

  init {
    viewModelScope.launch {
      _selectedYearMonth.collect { ym ->
        repository.getMonthlyGoal(ym).collect { goal ->
          if (goal != null) {
            _monthlyGoalData.value = goal
          } else {
            // Default goal for new months
            _monthlyGoalData.value = MonthlyGoalEntity(
              yearMonth = ym,
              savingsGoal = 500.0,
              monthlyIncome = 3000.0,
              currencySymbol = "$"
            )
          }
        }
      }
    }
  }

  // Today's / Selected Day's expenses
  val selectedDayExpenses: StateFlow<List<ExpenseEntity>> = combine(
    allExpenses,
    _selectedDateMillis
  ) { expenses, dateMillis ->
    val cal = Calendar.getInstance().apply {
      timeInMillis = dateMillis
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val startOfDay = cal.timeInMillis
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    val endOfDay = cal.timeInMillis

    expenses.filter { it.dateMillis in startOfDay..endOfDay }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Selected Month's expenses
  val selectedMonthExpenses: StateFlow<List<ExpenseEntity>> = combine(
    allExpenses,
    _selectedYearMonth
  ) { expenses, yearMonth ->
    val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    expenses.filter {
      val expYm = sdf.format(Date(it.dateMillis))
      expYm == yearMonth
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Monthly Budget Report
  val monthlyReport: StateFlow<MonthBudgetReport> = combine(
    selectedMonthExpenses,
    categoryBudgets,
    monthlyGoal,
    _selectedYearMonth
  ) { monthExpensesList, budgetsList, goal, yearMonth ->
    val totalSpent = monthExpensesList.sumOf { it.amount }
    val income = goal.monthlyIncome
    val savingsGoal = goal.savingsGoal
    val netSavings = income - totalSpent
    val currency = goal.currencySymbol

    val goalProgress = if (savingsGoal > 0) {
      ((netSavings / savingsGoal).toFloat()).coerceAtLeast(0f)
    } else 1f

    val isGoalMet = netSavings >= savingsGoal

    // Category Summaries
    val budgetMap = budgetsList.associateBy { it.categoryName }
    val spentByCategory = monthExpensesList.groupBy { it.category }
      .mapValues { entry -> entry.value.sumOf { it.amount } }

    val allCategoryNames = (DefaultCategories.list.map { it.name } + budgetsList.map { it.categoryName } + spentByCategory.keys).distinct()

    val categorySummaries = allCategoryNames.map { catName ->
      val spent = spentByCategory[catName] ?: 0.0
      val limit = budgetMap[catName]?.monthlyLimit
        ?: DefaultCategories.getMeta(catName).defaultLimit
      val meta = DefaultCategories.getMeta(catName)
      val percentOfLimit = if (limit > 0) (spent / limit).toFloat() else 0f
      val percentOfTotal = if (totalSpent > 0) ((spent / totalSpent) * 100).toFloat() else 0f
      val isExceeded = limit > 0 && spent > limit
      val isWarning = limit > 0 && spent >= (limit * 0.8) && !isExceeded

      CategorySpendingSummary(
        categoryName = catName,
        meta = meta,
        spent = spent,
        limit = limit,
        percentageOfLimit = percentOfLimit,
        percentageOfTotal = percentOfTotal,
        isExceeded = isExceeded,
        isWarning = isWarning
      )
    }.sortedByDescending { it.spent }

    // Daily Breakdown for the month
    val cal = Calendar.getInstance()
    try {
      val parsedDate = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(yearMonth)
      if (parsedDate != null) cal.time = parsedDate
    } catch (_: Exception) {}

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dayMap = monthExpensesList.groupBy {
      val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
      c.get(Calendar.DAY_OF_MONTH)
    }.mapValues { entry -> entry.value.sumOf { it.amount } }

    val dailyBreakdown = (1..daysInMonth).map { day ->
      val amt = dayMap[day] ?: 0.0
      DaySpendingSummary(
        dayNumber = day,
        dateLabel = "$day",
        amount = amt
      )
    }

    val topCategory = categorySummaries.firstOrNull { it.spent > 0 }?.categoryName
    val totalBudgetLimit = budgetsList.sumOf { it.monthlyLimit }.let { if (it > 0) it else DefaultCategories.list.sumOf { c -> c.defaultLimit } }
    val remainingBudget = totalBudgetLimit - totalSpent

    val currentCal = Calendar.getInstance()
    val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentCal.time)
    val isCurrentCycle = (yearMonth == currentYearMonth)

    val daysInCycle = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val daysElapsed = if (isCurrentCycle) {
      currentCal.get(Calendar.DAY_OF_MONTH)
    } else {
      daysInCycle
    }
    val daysRemainingInCycle = (daysInCycle - daysElapsed).coerceAtLeast(0)

    val nextResetCal = (cal.clone() as Calendar).apply {
      set(Calendar.DAY_OF_MONTH, 1)
      add(Calendar.MONTH, 1)
    }
    val nextCycleResetDate = SimpleDateFormat("MMM 1, yyyy", Locale.getDefault()).format(nextResetCal.time)

    val monthDisplay = try {
      val d = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(yearMonth)
      SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(d ?: Date())
    } catch (_: Exception) {
      yearMonth
    }

    MonthBudgetReport(
      yearMonth = yearMonth,
      monthDisplay = monthDisplay,
      totalSpent = totalSpent,
      monthlyIncome = income,
      savingsGoal = savingsGoal,
      netSavings = netSavings,
      savingsGoalProgress = goalProgress,
      isSavingsGoalMet = isGoalMet,
      categorySummaries = categorySummaries,
      dailyBreakdown = dailyBreakdown,
      topSpendingCategory = topCategory,
      totalBudgetLimit = totalBudgetLimit,
      overallBudgetRemaining = remainingBudget,
      currencySymbol = currency,
      daysInCycle = daysInCycle,
      daysElapsed = daysElapsed,
      daysRemainingInCycle = daysRemainingInCycle,
      isCurrentCycle = isCurrentCycle,
      nextCycleResetDate = nextCycleResetDate
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    MonthBudgetReport(
      yearMonth = _selectedYearMonth.value,
      monthDisplay = "This Month",
      totalSpent = 0.0,
      monthlyIncome = 3000.0,
      savingsGoal = 500.0,
      netSavings = 3000.0,
      savingsGoalProgress = 1f,
      isSavingsGoalMet = true,
      categorySummaries = emptyList(),
      dailyBreakdown = emptyList(),
      topSpendingCategory = null,
      totalBudgetLimit = 2000.0,
      overallBudgetRemaining = 2000.0,
      currencySymbol = "$",
      daysInCycle = 30,
      daysElapsed = 1,
      daysRemainingInCycle = 29,
      isCurrentCycle = true,
      nextCycleResetDate = "Next 1st"
    )
  )

  // List of active limit exceeded alerts for in-app banner
  val activeAlerts: StateFlow<List<CategorySpendingSummary>> = combine(
    monthlyReport,
    _dismissedAlerts
  ) { report, dismissed ->
    report.categorySummaries.filter { it.isExceeded && !dismissed.contains(it.categoryName) }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun dismissAlert(categoryName: String) {
    _dismissedAlerts.value = _dismissedAlerts.value + categoryName
  }

  fun resetCurrentMonthCycle() {
    viewModelScope.launch {
      repository.resetMonthCycleExpenses(_selectedYearMonth.value)
    }
  }

  fun setSelectedDate(millis: Long) {
    _selectedDateMillis.value = millis
    // Also sync the month view to this date's month
    _selectedYearMonth.value = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(millis))
  }

  fun navigateMonth(delta: Int) {
    try {
      val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
      val date = sdf.parse(_selectedYearMonth.value) ?: Date()
      val cal = Calendar.getInstance().apply {
        time = date
        add(Calendar.MONTH, delta)
      }
      _selectedYearMonth.value = sdf.format(cal.time)
    } catch (_: Exception) {}
  }

  fun setYearMonth(ym: String) {
    _selectedYearMonth.value = ym
  }

  fun addExpense(
    title: String,
    amount: Double,
    category: String,
    dateMillis: Long = _selectedDateMillis.value,
    note: String = "",
    paymentMethod: String = "Card"
  ) {
    if (amount <= 0 || title.isBlank()) return
    viewModelScope.launch {
      repository.insertExpense(
        ExpenseEntity(
          title = title.trim(),
          amount = amount,
          category = category,
          dateMillis = dateMillis,
          note = note.trim(),
          paymentMethod = paymentMethod
        )
      )
    }
  }

  fun updateExpense(expense: ExpenseEntity) {
    viewModelScope.launch {
      repository.updateExpense(expense)
    }
  }

  fun deleteExpense(expense: ExpenseEntity) {
    viewModelScope.launch {
      repository.deleteExpense(expense)
    }
  }

  fun deleteExpenseById(id: Long) {
    viewModelScope.launch {
      repository.deleteExpenseById(id)
    }
  }

  fun updateCategoryLimit(categoryName: String, newLimit: Double) {
    viewModelScope.launch {
      val meta = DefaultCategories.getMeta(categoryName)
      repository.updateCategoryBudget(
        CategoryBudgetEntity(
          categoryName = categoryName,
          monthlyLimit = newLimit,
          iconName = meta.iconName,
          colorHex = meta.color.value.toLong()
        )
      )
    }
  }

  fun updateMonthlyGoal(savingsGoal: Double, monthlyIncome: Double, currency: String = "$") {
    viewModelScope.launch {
      val updated = MonthlyGoalEntity(
        yearMonth = _selectedYearMonth.value,
        savingsGoal = savingsGoal,
        monthlyIncome = monthlyIncome,
        currencySymbol = currency
      )
      repository.updateMonthlyGoal(updated)
      _monthlyGoalData.value = updated
    }
  }

  fun sendTestNotification() {
    BudgetNotificationManager.sendTestNotification(context)
  }

  fun populateSampleData() {
    viewModelScope.launch {
      repository.populateSampleData()
    }
  }
}
