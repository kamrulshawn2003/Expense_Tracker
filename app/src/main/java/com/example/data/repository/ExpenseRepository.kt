package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.db.ExpenseDao
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.DefaultCategories
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthlyGoalEntity
import com.example.data.model.UserAccountEntity
import com.example.notification.BudgetNotificationManager
import com.example.util.AccountBackupPayload
import com.example.util.AccountSyncManager
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseRepository(
  private val expenseDao: ExpenseDao,
  private val context: Context
) {

  private val authPrefs = context.getSharedPreferences("expense_tracker_auth_prefs", Context.MODE_PRIVATE)

  val allUserAccounts: Flow<List<UserAccountEntity>> = expenseDao.getAllUserAccounts()

  fun getSavedActiveUserEmail(): String {
    return authPrefs.getString("active_user_email", "guest") ?: "guest"
  }

  fun setSavedActiveUserEmail(email: String) {
    authPrefs.edit().putString("active_user_email", email).apply()
  }

  suspend fun getUserAccount(email: String): UserAccountEntity? {
    if (email == "guest" || email.isBlank()) return null
    return expenseDao.getUserAccountByEmail(AccountSyncManager.normalizeEmail(email))
  }

  fun getExpensesForUser(userEmail: String): Flow<List<ExpenseEntity>> {
    return expenseDao.getAllExpensesForUser(userEmail)
  }

  fun getCategoryBudgetsForUser(userEmail: String): Flow<List<CategoryBudgetEntity>> {
    return expenseDao.getAllCategoryBudgetsForUser(userEmail)
  }

  fun getExpensesForDateRange(userEmail: String, startMillis: Long, endMillis: Long): Flow<List<ExpenseEntity>> {
    return expenseDao.getExpensesBetween(userEmail, startMillis, endMillis)
  }

  fun getMonthlyGoal(userEmail: String, yearMonth: String): Flow<MonthlyGoalEntity?> {
    return expenseDao.getMonthlyGoal(userEmail, yearMonth)
  }

  suspend fun insertExpense(expense: ExpenseEntity): Long {
    val id = expenseDao.insertExpense(expense)
    checkAndNotifyCategoryLimit(expense.userEmail, expense.category, expense.dateMillis)
    if (expense.userEmail != "guest") {
      syncAccountData(expense.userEmail)
    }
    return id
  }

  suspend fun updateExpense(expense: ExpenseEntity) {
    expenseDao.updateExpense(expense)
    checkAndNotifyCategoryLimit(expense.userEmail, expense.category, expense.dateMillis)
    if (expense.userEmail != "guest") {
      syncAccountData(expense.userEmail)
    }
  }

  suspend fun deleteExpense(expense: ExpenseEntity) {
    expenseDao.deleteExpense(expense)
    if (expense.userEmail != "guest") {
      syncAccountData(expense.userEmail)
    }
  }

  suspend fun deleteExpenseById(id: Long, userEmail: String = "guest") {
    expenseDao.deleteExpenseById(id)
    if (userEmail != "guest") {
      syncAccountData(userEmail)
    }
  }

  suspend fun resetMonthCycleExpenses(userEmail: String, yearMonth: String) {
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

    expenseDao.deleteExpensesBetween(userEmail, startOfMonth, endOfMonth)
    if (userEmail != "guest") {
      syncAccountData(userEmail)
    }
  }

  suspend fun updateCategoryBudget(budget: CategoryBudgetEntity) {
    expenseDao.insertOrUpdateCategoryBudget(budget)
    if (budget.userEmail != "guest") {
      syncAccountData(budget.userEmail)
    }
  }

  suspend fun saveCategoryBudget(budget: CategoryBudgetEntity) {
    expenseDao.insertOrUpdateCategoryBudget(budget)
    if (budget.userEmail != "guest") {
      syncAccountData(budget.userEmail)
    }
  }

  suspend fun deleteCategoryBudget(userEmail: String, categoryName: String) {
    expenseDao.updateExpenseCategory(userEmail, categoryName, "Other")
    expenseDao.deleteCategoryBudgetByName(userEmail, categoryName)
    if (userEmail != "guest") {
      syncAccountData(userEmail)
    }
  }

  suspend fun updateMonthlyGoal(goal: MonthlyGoalEntity) {
    expenseDao.insertOrUpdateMonthlyGoal(goal)
    if (goal.userEmail != "guest") {
      syncAccountData(goal.userEmail)
    }
  }

  suspend fun createAccount(
    displayName: String,
    email: String,
    password: String,
    claimGuestExpenses: Boolean = true
  ): Result<UserAccountEntity> {
    val normalizedEmail = AccountSyncManager.normalizeEmail(email)
    val trimmedName = displayName.trim().ifBlank { normalizedEmail.substringBefore("@") }

    if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
      return Result.failure(IllegalArgumentException("Please enter a valid email address."))
    }
    if (password.length < 4) {
      return Result.failure(IllegalArgumentException("Password must be at least 4 characters."))
    }

    val existingLocal = expenseDao.getUserAccountByEmail(normalizedEmail)
    if (existingLocal != null) {
      return Result.failure(IllegalArgumentException("An account with this email already exists. Please sign in instead."))
    }

    val passwordHash = AccountSyncManager.hashPassword(password)
    val now = System.currentTimeMillis()
    val newAccount = UserAccountEntity(
      email = normalizedEmail,
      displayName = trimmedName,
      passwordHash = passwordHash,
      createdAt = now,
      lastSyncedAt = now
    )

    expenseDao.insertOrUpdateUserAccount(newAccount)

    // Attempt cloud registration if Firebase is configured
    AccountSyncManager.createCloudAccountIfAvailable(context, normalizedEmail, password, trimmedName)

    // Copy/claim guest expenses, budgets, and goals into this new account if requested
    val existingUserExpenses = expenseDao.getAllExpensesForUserSync(normalizedEmail)
    if (claimGuestExpenses && existingUserExpenses.isEmpty()) {
      val guestExpenses = expenseDao.getAllExpensesForUserSync("guest")
      if (guestExpenses.isNotEmpty()) {
        val copiedExpenses = guestExpenses.map {
          it.copy(id = 0L, userEmail = normalizedEmail)
        }
        expenseDao.insertExpenses(copiedExpenses)
      }

      val guestBudgets = expenseDao.getAllCategoryBudgetsSync("guest")
      if (guestBudgets.isNotEmpty()) {
        val copiedBudgets = guestBudgets.map {
          it.copy(userEmail = normalizedEmail)
        }
        expenseDao.upsertCategoryBudgets(copiedBudgets)
      }

      val guestGoals = expenseDao.getAllMonthlyGoalsSync("guest")
      if (guestGoals.isNotEmpty()) {
        val copiedGoals = guestGoals.map {
          it.copy(userEmail = normalizedEmail)
        }
        expenseDao.insertMonthlyGoals(copiedGoals)
      }
    }

    ensureDefaultBudgets(normalizedEmail)
    setSavedActiveUserEmail(normalizedEmail)
    syncAccountData(normalizedEmail)

    return Result.success(newAccount)
  }

  suspend fun signIn(
    email: String,
    password: String
  ): Result<UserAccountEntity> {
    val normalizedEmail = AccountSyncManager.normalizeEmail(email)
    if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
      return Result.failure(IllegalArgumentException("Please enter a valid email address."))
    }
    if (password.isBlank()) {
      return Result.failure(IllegalArgumentException("Please enter your password."))
    }

    val passwordHash = AccountSyncManager.hashPassword(password)

    // 1. Check if cloud or local backup snapshot exists (e.g., after app reinstall)
    val remoteOrSnapshotBackup = AccountSyncManager.signInAndFetchCloudBackupIfAvailable(
      context = context,
      email = normalizedEmail,
      password = password
    )

    val localAccount = expenseDao.getUserAccountByEmail(normalizedEmail)

    if (localAccount == null && remoteOrSnapshotBackup == null) {
      return Result.failure(
        IllegalArgumentException("No account found for $normalizedEmail. Please create an account or restore from a backup file.")
      )
    }

    // Verify password hash if available
    val expectedHash = localAccount?.passwordHash ?: remoteOrSnapshotBackup?.passwordHash.orEmpty()
    if (expectedHash.isNotBlank() && expectedHash != passwordHash) {
      return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
    }

    // If we have a remote/snapshot backup and local expenses for this user are empty (e.g. fresh reinstall), restore them!
    val currentLocalExpenses = expenseDao.getAllExpensesForUserSync(normalizedEmail)
    if (remoteOrSnapshotBackup != null && currentLocalExpenses.isEmpty()) {
      restoreBackupIntoDatabase(remoteOrSnapshotBackup)
    }

    val finalAccount = expenseDao.getUserAccountByEmail(normalizedEmail) ?: UserAccountEntity(
      email = normalizedEmail,
      displayName = remoteOrSnapshotBackup?.displayName ?: normalizedEmail.substringBefore("@"),
      passwordHash = passwordHash,
      createdAt = remoteOrSnapshotBackup?.createdAt ?: System.currentTimeMillis(),
      lastSyncedAt = System.currentTimeMillis()
    )

    val updatedAccount = finalAccount.copy(lastSyncedAt = System.currentTimeMillis())
    expenseDao.insertOrUpdateUserAccount(updatedAccount)
    ensureDefaultBudgets(normalizedEmail)
    setSavedActiveUserEmail(normalizedEmail)
    syncAccountData(normalizedEmail)

    return Result.success(updatedAccount)
  }

  suspend fun signOut() {
    setSavedActiveUserEmail("guest")
    ensureDefaultBudgets("guest")
  }

  suspend fun syncAccountData(userEmail: String): Boolean {
    val normalizedEmail = AccountSyncManager.normalizeEmail(userEmail)
    if (normalizedEmail == "guest" || normalizedEmail.isBlank()) return false
    val account = expenseDao.getUserAccountByEmail(normalizedEmail) ?: return false
    val expenses = expenseDao.getAllExpensesForUserSync(normalizedEmail)
    val budgets = expenseDao.getAllCategoryBudgetsSync(normalizedEmail)
    val goals = expenseDao.getAllMonthlyGoalsSync(normalizedEmail)
    val now = System.currentTimeMillis()

    val payload = AccountBackupPayload(
      email = normalizedEmail,
      displayName = account.displayName,
      passwordHash = account.passwordHash,
      createdAt = account.createdAt,
      lastSyncedAt = now,
      expenses = expenses,
      budgets = budgets,
      goals = goals
    )

    val synced = AccountSyncManager.syncAccountBackup(context, payload)
    expenseDao.insertOrUpdateUserAccount(account.copy(lastSyncedAt = now))
    return synced
  }

  suspend fun exportUserAccountBackup(userEmail: String, uri: Uri): Boolean {
    val normalizedEmail = AccountSyncManager.normalizeEmail(userEmail)
    val account = expenseDao.getUserAccountByEmail(normalizedEmail)
    val expenses = expenseDao.getAllExpensesForUserSync(normalizedEmail)
    val budgets = expenseDao.getAllCategoryBudgetsSync(normalizedEmail)
    val goals = expenseDao.getAllMonthlyGoalsSync(normalizedEmail)
    val now = System.currentTimeMillis()

    val payload = AccountBackupPayload(
      email = normalizedEmail,
      displayName = account?.displayName ?: normalizedEmail.substringBefore("@"),
      passwordHash = account?.passwordHash ?: "",
      createdAt = account?.createdAt ?: now,
      lastSyncedAt = now,
      expenses = expenses,
      budgets = budgets,
      goals = goals
    )
    return AccountSyncManager.exportBackupToUri(context, uri, payload)
  }

  suspend fun importAndRestoreAccountBackup(uri: Uri): Result<UserAccountEntity> {
    val payload = AccountSyncManager.importBackupFromUri(context, uri)
      ?: return Result.failure(IllegalArgumentException("Invalid or corrupted backup file."))

    val restoredAccount = restoreBackupIntoDatabase(payload)
    setSavedActiveUserEmail(restoredAccount.email)
    syncAccountData(restoredAccount.email)
    return Result.success(restoredAccount)
  }

  private suspend fun restoreBackupIntoDatabase(payload: AccountBackupPayload): UserAccountEntity {
    val normalizedEmail = AccountSyncManager.normalizeEmail(payload.email)
    val now = System.currentTimeMillis()
    val existingAccount = expenseDao.getUserAccountByEmail(normalizedEmail)

    val account = UserAccountEntity(
      email = normalizedEmail,
      displayName = payload.displayName.ifBlank { existingAccount?.displayName ?: normalizedEmail.substringBefore("@") },
      passwordHash = payload.passwordHash.ifBlank { existingAccount?.passwordHash ?: "" },
      createdAt = existingAccount?.createdAt ?: payload.createdAt,
      lastSyncedAt = now
    )
    expenseDao.insertOrUpdateUserAccount(account)

    if (payload.budgets.isNotEmpty()) {
      expenseDao.upsertCategoryBudgets(
        payload.budgets.map { it.copy(userEmail = normalizedEmail) }
      )
    } else {
      ensureDefaultBudgets(normalizedEmail)
    }

    if (payload.goals.isNotEmpty()) {
      expenseDao.insertMonthlyGoals(
        payload.goals.map { it.copy(userEmail = normalizedEmail) }
      )
    }

    // Merge expenses without duplicating identical entries (matching title, amount, category, dateMillis)
    val existingExpenses = expenseDao.getAllExpensesForUserSync(normalizedEmail)
    val existingSignatures = existingExpenses.map {
      "${it.title}|${it.amount}|${it.category}|${it.dateMillis}"
    }.toSet()

    val newExpenses = payload.expenses.filter { exp ->
      val sig = "${exp.title}|${exp.amount}|${exp.category}|${exp.dateMillis}"
      sig !in existingSignatures
    }.map {
      it.copy(id = 0L, userEmail = normalizedEmail)
    }

    if (newExpenses.isNotEmpty()) {
      expenseDao.insertExpenses(newExpenses)
    }

    return account
  }

  private suspend fun checkAndNotifyCategoryLimit(
    userEmail: String,
    categoryName: String,
    expenseDateMillis: Long
  ) {
    val budget = expenseDao.getCategoryBudgetSync(userEmail, categoryName) ?: return
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

    val monthExpenses = expenseDao.getExpensesForCategoryBetweenSync(userEmail, categoryName, startOfMonth, endOfMonth)
    val totalSpent = monthExpenses.sumOf { it.amount }

    if (totalSpent > budget.monthlyLimit) {
      val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(expenseDateMillis))
      val goal = expenseDao.getMonthlyGoalSync(userEmail, yearMonth)
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

  suspend fun ensureDefaultBudgets(userEmail: String = "guest") {
    val existing = expenseDao.getAllCategoryBudgetsSync(userEmail)
    if (existing.isEmpty()) {
      val defaults = DefaultCategories.list.map {
        CategoryBudgetEntity(
          categoryName = it.name,
          monthlyLimit = it.defaultLimit,
          iconName = it.iconName,
          colorHex = DefaultCategories.colorToHexLong(it.color),
          pictureUri = null,
          userEmail = userEmail
        )
      }
      expenseDao.insertCategoryBudgets(defaults)
    } else {
      // Repair any previously saved shifted 64-bit colorHex values so logos are always visible
      val repaired = existing.mapNotNull { item ->
        val safeColor = DefaultCategories.hexLongToColor(
          item.colorHex,
          DefaultCategories.list.find { it.name.equals(item.categoryName, ignoreCase = true) }?.color
            ?: DefaultCategories.selectableColors.first()
        )
        val expectedHex = DefaultCategories.colorToHexLong(safeColor)
        if (item.colorHex != expectedHex) {
          item.copy(colorHex = expectedHex)
        } else {
          null
        }
      }
      if (repaired.isNotEmpty()) {
        expenseDao.upsertCategoryBudgets(repaired)
      }
    }
  }
}

