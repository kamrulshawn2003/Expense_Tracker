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
import com.example.data.model.UserAccountEntity
import com.example.data.repository.ExpenseRepository
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
  val savingsGoalProgress: Float = 1f,
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

  // Active signed-in Gmail user ("guest" when signed out)
  private val _activeUserEmail = MutableStateFlow("guest")
  val activeUserEmail: StateFlow<String> = _activeUserEmail.asStateFlow()

  // Selected date for Daily view (start of day millis)
  private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
  val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

  // Selected Year-Month for Monthly view (e.g. "2026-10")
  private val _selectedYearMonth = MutableStateFlow(
    SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
  )
  val selectedYearMonth: StateFlow<String> = _selectedYearMonth.asStateFlow()

  // In-app alert dismissed tracker
  private val _dismissedAlerts = MutableStateFlow<Set<String>>(emptySet())
  val dismissedAlerts: StateFlow<Set<String>> = _dismissedAlerts.asStateFlow()

  // Auth & Google Drive Backup UI state
  private val _authErrorMessage = MutableStateFlow<String?>(null)
  val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

  private val _authFeedbackMessage = MutableStateFlow<String?>(null)
  val authFeedbackMessage: StateFlow<String?> = _authFeedbackMessage.asStateFlow()

  private val _isAuthLoading = MutableStateFlow(false)
  val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

  private val _backupFrequency = MutableStateFlow("Daily")
  val backupFrequency: StateFlow<String> = _backupFrequency.asStateFlow()

  private val _lastBackupSizeBytes = MutableStateFlow(0)
  val lastBackupSizeBytes: StateFlow<Int> = _lastBackupSizeBytes.asStateFlow()

  val googleOAuthClientId: String
    get() = AccountSyncManager.getGoogleOAuthClientId(context)

  init {
    val database = AppDatabase.getDatabase(context, viewModelScope)
    repository = ExpenseRepository(database.expenseDao(), context)
    val savedEmail = repository.getSavedActiveUserEmail()
    _activeUserEmail.value = savedEmail
    if (savedEmail != "guest") {
      _backupFrequency.value = repository.getBackupFrequency(savedEmail)
      _lastBackupSizeBytes.value = repository.getLastBackupSizeBytes(savedEmail)
    }
    viewModelScope.launch {
      repository.ensureDefaultBudgets(savedEmail)
      if (savedEmail != "guest" && AccountSyncManager.isValidGmailAddress(savedEmail)) {
        // Automatically check & sync with Google Drive on launch
        repository.restoreFromGoogleDrive(savedEmail)
        _lastBackupSizeBytes.value = repository.getLastBackupSizeBytes(savedEmail)
      }
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

    val spendingBudget = (income - savingsGoal).coerceAtLeast(0.0)
    val spendingRemaining = spendingBudget - totalSpent
    val isOverSpendingBudget = spendingBudget > 0 && totalSpent > spendingBudget
    val spendingUsage = if (spendingBudget > 0) (totalSpent / spendingBudget).toFloat() else 0f
    val currency = goal.currencySymbol

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

  /**
   * Connects a user's Gmail (@gmail.com) account, automatically restores any existing
   * WhatsApp-style backup from Google Drive, and backs up current expense history.
   */
  fun connectWithGmail(
    email: String,
    displayName: String = "",
    googleIdToken: String? = null,
    driveAccessToken: String? = null,
    claimGuestExpenses: Boolean = true,
    onSuccess: () -> Unit = {}
  ) {
    viewModelScope.launch {
      _isAuthLoading.value = true
      _authErrorMessage.value = null
      _authFeedbackMessage.value = null

      val result = repository.signInOrSignUpWithGmail(
        email = email,
        displayName = displayName,
        googleIdToken = googleIdToken,
        driveAccessToken = driveAccessToken,
        claimGuestExpenses = claimGuestExpenses
      )

      _isAuthLoading.value = false
      result.fold(
        onSuccess = { syncResult ->
          _activeUserEmail.value = syncResult.account.email
          _backupFrequency.value = repository.getBackupFrequency(syncResult.account.email)
          _lastBackupSizeBytes.value = syncResult.backupSizeBytes
          _authFeedbackMessage.value = if (syncResult.restoredFromGoogleDrive && syncResult.restoredExpenseCount > 0) {
            "Google Drive backup found! Restored ${syncResult.totalExpenseCount} expenses for ${syncResult.account.email}."
          } else {
            "Connected to Google Drive (${syncResult.account.email})! Your expenses are automatically backed up."
          }
          onSuccess()
        },
        onFailure = { err ->
          _authErrorMessage.value = err.message ?: "Could not connect Gmail account."
        }
      )
    }
  }

  /**
   * Triggers an immediate WhatsApp-style backup to Google Drive for the active Gmail account.
   */
  fun syncAccountNow(driveAccessToken: String? = null) {
    val email = _activeUserEmail.value
    if (email == "guest") return
    viewModelScope.launch {
      _isAuthLoading.value = true
      _authErrorMessage.value = null
      repository.syncAccountData(email, driveAccessToken)
      _lastBackupSizeBytes.value = repository.getLastBackupSizeBytes(email)
      _isAuthLoading.value = false
      _authFeedbackMessage.value = "Backed up to Google Drive (${email})!"
    }
  }

  /**
   * Restores the latest expense history backup from Google Drive for the connected Gmail account.
   */
  fun restoreFromGoogleDriveNow(driveAccessToken: String? = null) {
    val email = _activeUserEmail.value
    if (email == "guest") return
    viewModelScope.launch {
      _isAuthLoading.value = true
      _authErrorMessage.value = null
      _authFeedbackMessage.value = null
      val result = repository.restoreFromGoogleDrive(email, driveAccessToken)
      _isAuthLoading.value = false
      result.fold(
        onSuccess = { syncResult ->
          _lastBackupSizeBytes.value = syncResult.backupSizeBytes
          _authFeedbackMessage.value =
            "Restored from Google Drive! ${syncResult.totalExpenseCount} expense records are active for ${syncResult.account.email}."
        },
        onFailure = { err ->
          _authErrorMessage.value = err.message ?: "Could not restore from Google Drive."
        }
      )
    }
  }

  fun updateBackupFrequency(frequency: String) {
    val email = _activeUserEmail.value
    _backupFrequency.value = frequency
    if (email != "guest") {
      repository.setBackupFrequency(email, frequency)
      viewModelScope.launch {
        repository.syncAccountData(email)
        _lastBackupSizeBytes.value = repository.getLastBackupSizeBytes(email)
      }
    }
  }

  fun signOut() {
    viewModelScope.launch {
      repository.signOut()
      _activeUserEmail.value = "guest"
      _authFeedbackMessage.value = "Disconnected Gmail account. Sign in with Gmail anytime to restore from Google Drive."
      _authErrorMessage.value = null
    }
  }

  fun dismissAlert(categoryName: String) {
    _dismissedAlerts.value = _dismissedAlerts.value + categoryName
  }

  fun resetCurrentMonthCycle() {
    viewModelScope.launch {
      repository.resetMonthCycleExpenses(_activeUserEmail.value, _selectedYearMonth.value)
      _lastBackupSizeBytes.value = repository.getLastBackupSizeBytes(_activeUserEmail.value)
    }
  }

  fun setSelectedDate(millis: Long) {
    _selectedDateMillis.value = millis
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
      if (currentEmail != "guest") {
        _lastBackupSizeBytes.value = repository.getLastBackupSizeBytes(currentEmail)
      }
    }
  }

  fun updateExpense(expense: ExpenseEntity) {
    val currentEmail = _activeUserEmail.value
    viewModelScope.launch {
      repository.updateExpense(expense.copy(userEmail = currentEmail))
      if (currentEmail != "guest") {
        _lastBackupSizeBytes.value = repository.getLastBackupSizeBytes(currentEmail)
      }
    }
  }

  fun deleteExpense(expense: ExpenseEntity) {
    val currentEmail = _activeUserEmail.value
    viewModelScope.launch {
      repository.deleteExpense(expense)
      if (currentEmail != "guest") {
        _lastBackupSizeBytes.value = repository.getLastBackupSizeBytes(currentEmail)
      }
    }
  }

  fun deleteExpenseById(id: Long) {
    val currentEmail = _activeUserEmail.value
    viewModelScope.launch {
      repository.deleteExpenseById(id, currentEmail)
      if (currentEmail != "guest") {
        _lastBackupSizeBytes.value = repository.getLastBackupSizeBytes(currentEmail)
      }
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
      if (currentEmail != "guest") {
        _lastBackupSizeBytes.value = repository.getLastBackupSizeBytes(currentEmail)
      }
    }
  }

  fun deleteCategoryBudget(categoryName: String) {
    val currentEmail = _activeUserEmail.value
    viewModelScope.launch {
      repository.deleteCategoryBudget(currentEmail, categoryName)
      if (currentEmail != "guest") {
        _lastBackupSizeBytes.value = repository.getLastBackupSizeBytes(currentEmail)
      }
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
      if (currentEmail != "guest") {
        _lastBackupSizeBytes.value = repository.getLastBackupSizeBytes(currentEmail)
      }
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
      if (currentEmail != "guest") {
        _lastBackupSizeBytes.value = repository.getLastBackupSizeBytes(currentEmail)
      }
    }
  }
}
