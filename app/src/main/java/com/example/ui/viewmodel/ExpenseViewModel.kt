package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.CategoryMeta
import com.example.data.model.DefaultCategories
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthlyGoalEntity
import com.example.data.model.UserAccountEntity
import com.example.data.repository.ExpenseRepository
import com.example.notification.BudgetNotificationManager
import com.example.util.AccountSyncManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
  val isWarning: Boolean,
  val pictureUri: String? = null,
  val iconName: String = "category",
  val colorHex: Long = 0xFF10B981
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
  val savingsAmount: Double = savingsGoal,
  val spendingBudget: Double = (monthlyIncome - savingsGoal).coerceAtLeast(0.0),
  val spendingRemaining: Double = spendingBudget - totalSpent,
  val spendingUsagePercentage: Float = if (spendingBudget > 0) (totalSpent / spendingBudget).toFloat() else 0f,
  val isOverSpendingBudget: Boolean = spendingBudget > 0 && totalSpent > spendingBudget,
  val netSavings: Double = savingsGoal,
  val savingsGoalProgress: Float = 1f, // 0.0 to 1.0+
  val isSavingsGoalMet: Boolean = true,
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

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: ExpenseRepository
  private val context = application.applicationContext

  // Active signed-in user email ("guest" when signed out)
  private val _activeUserEmail = MutableStateFlow("guest")
  val activeUserEmail: StateFlow<String> = _activeUserEmail.asStateFlow()

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

  // Auth UI state
  private val _authErrorMessage = MutableStateFlow<String?>(null)
  val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

  private val _authFeedbackMessage = MutableStateFlow<String?>(null)
  val authFeedbackMessage: StateFlow<String?> = _authFeedbackMessage.asStateFlow()

  private val _isAuthLoading = MutableStateFlow(false)
  val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

  val isFirebaseConfigured: Boolean
    get() = AccountSyncManager.isFirebaseAvailable(context)

  init {
    val database = AppDatabase.getDatabase(context, viewModelScope)
    repository = ExpenseRepository(database.expenseDao(), context)
    val savedEmail = repository.getSavedActiveUserEmail()
    _activeUserEmail.value = savedEmail
    viewModelScope.launch {
      repository.ensureDefaultBudgets(savedEmail)
    }
  }

  val registeredAccounts: StateFlow<List<UserAccountEntity>> = repository.allUserAccounts
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val currentUserAccount: StateFlow<UserAccountEntity?> = combine(
    registeredAccounts,
    _activeUserEmail
  ) { accounts, email ->
    if (email == "guest" || email.isBlank()) null
    else accounts.find { it.email.equals(email, ignoreCase = true) }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val allExpenses: StateFlow<List<ExpenseEntity>> = _activeUserEmail
    .flatMapLatest { email -> repository.getExpensesForUser(email) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val categoryBudgets: StateFlow<List<CategoryBudgetEntity>> = _activeUserEmail
    .flatMapLatest { email -> repository.getCategoryBudgetsForUser(email) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Dynamic monthly goal flow for selected month and active user
  private val _monthlyGoalData = MutableStateFlow(MonthlyGoalEntity(yearMonth = _selectedYearMonth.value))
  val monthlyGoal: StateFlow<MonthlyGoalEntity> = _monthlyGoalData.asStateFlow()

  init {
    viewModelScope.launch {
      combine(_activeUserEmail, _selectedYearMonth) { email, ym -> email to ym }
        .flatMapLatest { (email, ym) ->
          repository.getMonthlyGoal(email, ym)
        }
        .collect { goal ->
          val ym = _selectedYearMonth.value
          val email = _activeUserEmail.value
          if (goal != null) {
            _monthlyGoalData.value = goal
          } else {
            _monthlyGoalData.value = MonthlyGoalEntity(
              yearMonth = ym,
              savingsGoal = 500.0,
              monthlyIncome = 3000.0,
              currencySymbol = "$",
              userEmail = email
            )
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

    // Pay-Yourself-First / Savings rule:
    // After adding the saving amount, always count the rest of the amount for spendings.
    // Savings amount does not have any relation with the spending amount.
    val spendingBudget = (income - savingsGoal).coerceAtLeast(0.0)
    val spendingRemaining = spendingBudget - totalSpent
    val isOverSpendingBudget = spendingBudget > 0 && totalSpent > spendingBudget
    val spendingUsage = if (spendingBudget > 0) (totalSpent / spendingBudget).toFloat() else 0f
    val currency = goal.currencySymbol

    // Savings amount is dedicated and protected, never reduced by spendings
    val netSavings = savingsGoal
    val goalProgress = 1f
    val isGoalMet = true

    // Category Summaries
    val budgetMap = budgetsList.associateBy { it.categoryName }
    val spentByCategory = monthExpensesList.groupBy { it.category }
      .mapValues { entry -> entry.value.sumOf { it.amount } }

    val allCategoryNames = (budgetsList.map { it.categoryName } + spentByCategory.keys).distinct()

    val categorySummaries = allCategoryNames.map { catName ->
      val spent = spentByCategory[catName] ?: 0.0
      val budgetItem = budgetMap[catName]
      val limit = budgetItem?.monthlyLimit
        ?: DefaultCategories.getMeta(catName).defaultLimit
      val meta = DefaultCategories.getMeta(
        name = catName,
        iconName = budgetItem?.iconName,
        colorHex = budgetItem?.colorHex
      )
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
        isWarning = isWarning,
        pictureUri = budgetItem?.pictureUri,
        iconName = budgetItem?.iconName ?: meta.iconName,
        colorHex = DefaultCategories.colorToHexLong(meta.color)
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
    // Total monthly spending limit is the rest of income after savings
    val totalBudgetLimit = if (spendingBudget > 0) spendingBudget else budgetsList.sumOf { it.monthlyLimit }.let { if (it > 0) it else DefaultCategories.list.sumOf { c -> c.defaultLimit } }
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
      savingsAmount = savingsGoal,
      spendingBudget = spendingBudget,
      spendingRemaining = spendingRemaining,
      spendingUsagePercentage = spendingUsage,
      isOverSpendingBudget = isOverSpendingBudget,
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
      savingsAmount = 500.0,
      spendingBudget = 2500.0,
      spendingRemaining = 2500.0,
      spendingUsagePercentage = 0f,
      isOverSpendingBudget = false,
      netSavings = 500.0,
      savingsGoalProgress = 1f,
      isSavingsGoalMet = true,
      categorySummaries = emptyList(),
      dailyBreakdown = emptyList(),
      topSpendingCategory = null,
      totalBudgetLimit = 2500.0,
      overallBudgetRemaining = 2500.0,
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

  fun clearAuthMessages() {
    _authErrorMessage.value = null
    _authFeedbackMessage.value = null
  }

  fun createAccount(
    displayName: String,
    email: String,
    password: String,
    claimGuestExpenses: Boolean = true,
    onSuccess: () -> Unit = {}
  ) {
    viewModelScope.launch {
      _isAuthLoading.value = true
      _authErrorMessage.value = null
      _authFeedbackMessage.value = null
      val result = repository.createAccount(displayName, email, password, claimGuestExpenses)
      _isAuthLoading.value = false
      result.fold(
        onSuccess = { account ->
          _activeUserEmail.value = account.email
          _authFeedbackMessage.value = "Account created & expenses backed up for ${account.displayName}!"
          onSuccess()
        },
        onFailure = { err ->
          _authErrorMessage.value = err.message ?: "Could not create account."
        }
      )
    }
  }

  fun signIn(
    email: String,
    password: String,
    onSuccess: () -> Unit = {}
  ) {
    viewModelScope.launch {
      _isAuthLoading.value = true
      _authErrorMessage.value = null
      _authFeedbackMessage.value = null
      val result = repository.signIn(email, password)
      _isAuthLoading.value = false
      result.fold(
        onSuccess = { account ->
          _activeUserEmail.value = account.email
          _authFeedbackMessage.value = "Welcome back, ${account.displayName}! Your previous expenses are restored."
          onSuccess()
        },
        onFailure = { err ->
          _authErrorMessage.value = err.message ?: "Sign in failed."
        }
      )
    }
  }

  fun signOut() {
    viewModelScope.launch {
      repository.signOut()
      _activeUserEmail.value = "guest"
      _authFeedbackMessage.value = "Signed out. Sign in anytime to view your saved account expenses."
      _authErrorMessage.value = null
    }
  }

  fun syncAccountNow() {
    val email = _activeUserEmail.value
    if (email == "guest") return
    viewModelScope.launch {
      _isAuthLoading.value = true
      repository.syncAccountData(email)
      _isAuthLoading.value = false
      _authFeedbackMessage.value = "All expenses & budgets synced and backed up!"
    }
  }

  fun exportAccountBackup(uri: Uri) {
    val email = _activeUserEmail.value
    viewModelScope.launch {
      _isAuthLoading.value = true
      val ok = repository.exportUserAccountBackup(email, uri)
      _isAuthLoading.value = false
      if (ok) {
        _authFeedbackMessage.value = "Backup file saved! You can restore this file even after reinstalling the app."
      } else {
        _authErrorMessage.value = "Failed to export backup file."
      }
    }
  }

  fun importAccountBackup(uri: Uri, onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      _isAuthLoading.value = true
      _authErrorMessage.value = null
      val result = repository.importAndRestoreAccountBackup(uri)
      _isAuthLoading.value = false
      result.fold(
        onSuccess = { account ->
          _activeUserEmail.value = account.email
          _authFeedbackMessage.value = "Restored account & expenses for ${account.displayName}!"
          onSuccess()
        },
        onFailure = { err ->
          _authErrorMessage.value = err.message ?: "Could not restore backup file."
        }
      )
    }
  }

  fun dismissAlert(categoryName: String) {
    _dismissedAlerts.value = _dismissedAlerts.value + categoryName
  }

  fun resetCurrentMonthCycle() {
    viewModelScope.launch {
      repository.resetMonthCycleExpenses(_activeUserEmail.value, _selectedYearMonth.value)
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
    paymentMethod: String = "",
    pictureUri: String? = null
  ) {
    if (amount <= 0 || title.isBlank()) return
    val currentEmail = _activeUserEmail.value
    viewModelScope.launch {
      repository.insertExpense(
        ExpenseEntity(
          title = title.trim(),
          amount = amount,
          category = category,
          dateMillis = dateMillis,
          note = note.trim(),
          paymentMethod = paymentMethod,
          pictureUri = pictureUri,
          userEmail = currentEmail
        )
      )
    }
  }

  fun updateExpense(expense: ExpenseEntity) {
    viewModelScope.launch {
      repository.updateExpense(expense.copy(userEmail = _activeUserEmail.value))
    }
  }

  fun deleteExpense(expense: ExpenseEntity) {
    viewModelScope.launch {
      repository.deleteExpense(expense)
    }
  }

  fun deleteExpenseById(id: Long) {
    viewModelScope.launch {
      repository.deleteExpenseById(id, _activeUserEmail.value)
    }
  }

  fun saveCategoryBudget(
    categoryName: String,
    limit: Double,
    iconName: String = "other",
    colorHex: Long = 0xFF10B981,
    pictureUri: String? = null
  ) {
    val currentEmail = _activeUserEmail.value
    viewModelScope.launch {
      repository.saveCategoryBudget(
        CategoryBudgetEntity(
          categoryName = categoryName.trim(),
          monthlyLimit = limit,
          iconName = iconName,
          colorHex = colorHex,
          pictureUri = pictureUri,
          userEmail = currentEmail
        )
      )
    }
  }

  fun deleteCategoryBudget(categoryName: String) {
    val currentEmail = _activeUserEmail.value
    viewModelScope.launch {
      repository.deleteCategoryBudget(currentEmail, categoryName)
    }
  }

  fun updateCategoryLimit(
    categoryName: String,
    newLimit: Double,
    iconName: String? = null,
    colorHex: Long? = null,
    pictureUri: String? = null
  ) {
    val currentEmail = _activeUserEmail.value
    viewModelScope.launch {
      val existing = categoryBudgets.value.find { it.categoryName == categoryName }
      val meta = DefaultCategories.getMeta(categoryName, existing?.iconName, existing?.colorHex)
      repository.updateCategoryBudget(
        CategoryBudgetEntity(
          categoryName = categoryName,
          monthlyLimit = newLimit,
          iconName = iconName ?: existing?.iconName ?: meta.iconName,
          colorHex = colorHex ?: DefaultCategories.colorToHexLong(meta.color),
          pictureUri = if (pictureUri != null) pictureUri else existing?.pictureUri,
          userEmail = currentEmail
        )
      )
    }
  }

  fun updateMonthlyGoal(savingsGoal: Double, monthlyIncome: Double, currency: String = "$") {
    val currentEmail = _activeUserEmail.value
    viewModelScope.launch {
      val updated = MonthlyGoalEntity(
        yearMonth = _selectedYearMonth.value,
        savingsGoal = savingsGoal,
        monthlyIncome = monthlyIncome,
        currencySymbol = currency,
        userEmail = currentEmail
      )
      repository.updateMonthlyGoal(updated)
      _monthlyGoalData.value = updated
    }
  }
}
