package com.example.data.repository

import android.content.Context
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

data class GmailAuthSyncResult(
  val account: UserAccountEntity,
  val restoredFromGoogleDrive: Boolean,
  val restoredExpenseCount: Int,
  val totalExpenseCount: Int,
  val backupSizeBytes: Int,
  val lastBackupTimeMillis: Long
)

class ExpenseRepository(
  private val expenseDao: ExpenseDao,
  private val context: Context
) {

  private val authPrefs = context.getSharedPreferences("expense_tracker_auth_prefs", Context.MODE_PRIVATE)

  val allUserAccounts: Flow<List<UserAccountEntity>> = expenseDao.getAllUserAccounts()

  init {
    // Ensure Firebase cloud backup backend is initialized on startup
    AccountSyncManager.isFirebaseAvailable(context)
  }

  fun getSavedActiveUserEmail(): String {
    return authPrefs.getString("active_user_email", "guest") ?: "guest"
  }

  fun setSavedActiveUserEmail(email: String) {
    authPrefs.edit().putString("active_user_email", email).apply()
  }

  fun getSavedDriveAccessToken(email: String): String? {
    val key = "drive_token_${AccountSyncManager.normalizeEmail(email)}"
    return authPrefs.getString(key, null)
  }

  fun setSavedDriveAccessToken(email: String, token: String?) {
    val key = "drive_token_${AccountSyncManager.normalizeEmail(email)}"
    if (token.isNullOrBlank()) {
      authPrefs.edit().remove(key).apply()
    } else {
      authPrefs.edit().putString(key, token).apply()
    }
  }

  fun getBackupFrequency(email: String): String {
    val key = "backup_freq_${AccountSyncManager.normalizeEmail(email)}"
    return authPrefs.getString(key, "Daily") ?: "Daily"
  }

  fun setBackupFrequency(email: String, frequency: String) {
    val key = "backup_freq_${AccountSyncManager.normalizeEmail(email)}"
    authPrefs.edit().putString(key, frequency).apply()
  }

  fun getLastBackupSizeBytes(email: String): Int {
    val key = "backup_size_${AccountSyncManager.normalizeEmail(email)}"
    return authPrefs.getInt(key, 0)
  }

  private fun setLastBackupSizeBytes(email: String, sizeBytes: Int) {
    val key = "backup_size_${AccountSyncManager.normalizeEmail(email)}"
    authPrefs.edit().putInt(key, sizeBytes).apply()
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
    if (expense.userEmail != "guest" && getBackupFrequency(expense.userEmail) != "Only when I tap Back Up") {
      syncAccountData(expense.userEmail)
    }
    return id
  }

  suspend fun updateExpense(expense: ExpenseEntity) {
    expenseDao.updateExpense(expense)
    checkAndNotifyCategoryLimit(expense.userEmail, expense.category, expense.dateMillis)
    if (expense.userEmail != "guest" && getBackupFrequency(expense.userEmail) != "Only when I tap Back Up") {
      syncAccountData(expense.userEmail)
    }
  }

  suspend fun deleteExpense(expense: ExpenseEntity) {
    expenseDao.deleteExpense(expense)
    if (expense.userEmail != "guest" && getBackupFrequency(expense.userEmail) != "Only when I tap Back Up") {
      syncAccountData(expense.userEmail)
    }
  }

  suspend fun deleteExpenseById(id: Long, userEmail: String = "guest") {
    expenseDao.deleteExpenseById(id)
    if (userEmail != "guest" && getBackupFrequency(userEmail) != "Only when I tap Back Up") {
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
    if (budget.userEmail != "guest" && getBackupFrequency(budget.userEmail) != "Only when I tap Back Up") {
      syncAccountData(budget.userEmail)
    }
  }

  suspend fun saveCategoryBudget(budget: CategoryBudgetEntity) {
    expenseDao.insertOrUpdateCategoryBudget(budget)
    if (budget.userEmail != "guest" && getBackupFrequency(budget.userEmail) != "Only when I tap Back Up") {
      syncAccountData(budget.userEmail)
    }
  }

  suspend fun deleteCategoryBudget(userEmail: String, categoryName: String) {
    expenseDao.updateExpenseCategory(userEmail, categoryName, "Other")
    expenseDao.deleteCategoryBudgetByName(userEmail, categoryName)
    if (userEmail != "guest" && getBackupFrequency(userEmail) != "Only when I tap Back Up") {
      syncAccountData(userEmail)
    }
  }

  suspend fun updateMonthlyGoal(goal: MonthlyGoalEntity) {
    expenseDao.insertOrUpdateMonthlyGoal(goal)
    if (goal.userEmail != "guest" && getBackupFrequency(goal.userEmail) != "Only when I tap Back Up") {
      syncAccountData(goal.userEmail)
    }
  }

  /**
   * Unified WhatsApp-style Gmail Sign-In / Sign-Up & Automatic Google Drive Restore.
   * Strictly requires a valid @gmail.com address.
   * When a user signs in with their Gmail (even after uninstalling and reinstalling the app),
   * it automatically checks Google Drive & Cloud for their saved backup, restores all expenses,
   * budgets, and savings goals, merges any guest expenses if requested, and syncs back to Google Drive.
   */
  suspend fun signInOrSignUpWithGmail(
    email: String,
    displayName: String = "",
    googleIdToken: String? = null,
    driveAccessToken: String? = null,
    claimGuestExpenses: Boolean = true
  ): Result<GmailAuthSyncResult> {
    val normalizedEmail = AccountSyncManager.normalizeEmail(email)
    if (!AccountSyncManager.isValidGmailAddress(normalizedEmail)) {
      return Result.failure(
        IllegalArgumentException("Please enter a valid Gmail address (ending with @gmail.com) to use Google Drive backup.")
      )
    }

    if (!driveAccessToken.isNullOrBlank()) {
      setSavedDriveAccessToken(normalizedEmail, driveAccessToken)
    }
    val effectiveDriveToken = driveAccessToken ?: getSavedDriveAccessToken(normalizedEmail)

    if (!googleIdToken.isNullOrBlank()) {
      AccountSyncManager.signInWithGoogleIdTokenIfAvailable(context, googleIdToken)
    }

    // 1. Check Google Drive & Cloud for an existing WhatsApp-style backup for this Gmail
    val remoteBackup = AccountSyncManager.fetchGmailCloudBackup(
      context = context,
      email = normalizedEmail,
      driveAccessToken = effectiveDriveToken
    )

    val localAccount = expenseDao.getUserAccountByEmail(normalizedEmail)
    val resolvedName = displayName.trim()
      .ifBlank { remoteBackup?.displayName.orEmpty() }
      .ifBlank { localAccount?.displayName.orEmpty() }
      .ifBlank {
        normalizedEmail.substringBefore("@")
          .replace(".", " ")
          .replace("_", " ")
          .split(" ")
          .filter { it.isNotBlank() }
          .joinToString(" ") { part ->
            part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
          }
          .ifBlank { normalizedEmail.substringBefore("@") }
      }

    val beforeCount = expenseDao.getAllExpensesForUserSync(normalizedEmail).size
    var restoredFromDrive = false

    // 2. If a Google Drive / Cloud backup exists, restore and merge its records into the local database!
    if (remoteBackup != null) {
      restoreBackupIntoDatabase(remoteBackup.copy(displayName = resolvedName))
      if (remoteBackup.expenses.isNotEmpty() || remoteBackup.budgets.isNotEmpty()) {
        restoredFromDrive = true
      }
      if (remoteBackup.backupFrequency.isNotBlank()) {
        setBackupFrequency(normalizedEmail, remoteBackup.backupFrequency)
      }
    }

    // 3. Also copy/merge any guest expenses recorded prior to signing in if requested
    if (claimGuestExpenses) {
      mergeGuestDataIntoGmailAccount(normalizedEmail)
    }

    val now = System.currentTimeMillis()
    val finalAccount = UserAccountEntity(
      email = normalizedEmail,
      displayName = resolvedName,
      passwordHash = localAccount?.passwordHash ?: remoteBackup?.passwordHash.orEmpty(),
      createdAt = localAccount?.createdAt ?: remoteBackup?.createdAt ?: now,
      lastSyncedAt = now
    )
    expenseDao.insertOrUpdateUserAccount(finalAccount)

    ensureDefaultBudgets(normalizedEmail)
    setSavedActiveUserEmail(normalizedEmail)

    // 4. Immediately sync the complete merged state back to Google Drive
    syncAccountData(normalizedEmail, effectiveDriveToken)

    val afterCount = expenseDao.getAllExpensesForUserSync(normalizedEmail).size
    val restoredCount = (afterCount - beforeCount).coerceAtLeast(if (restoredFromDrive) remoteBackup?.expenses?.size ?: 0 else 0)
    val sizeBytes = getLastBackupSizeBytes(normalizedEmail)

    return Result.success(
      GmailAuthSyncResult(
        account = finalAccount,
        restoredFromGoogleDrive = restoredFromDrive,
        restoredExpenseCount = restoredCount,
        totalExpenseCount = afterCount,
        backupSizeBytes = sizeBytes,
        lastBackupTimeMillis = now
      )
    )
  }

  /**
   * Explicitly fetches and restores the user's expense history from Google Drive for their active Gmail.
   */
  suspend fun restoreFromGoogleDrive(
    userEmail: String,
    driveAccessToken: String? = null
  ): Result<GmailAuthSyncResult> {
    val normalizedEmail = AccountSyncManager.normalizeEmail(userEmail)
    if (!AccountSyncManager.isValidGmailAddress(normalizedEmail)) {
      return Result.failure(IllegalArgumentException("Please sign in with a Gmail (@gmail.com) account first."))
    }

    if (!driveAccessToken.isNullOrBlank()) {
      setSavedDriveAccessToken(normalizedEmail, driveAccessToken)
    }
    val effectiveToken = driveAccessToken ?: getSavedDriveAccessToken(normalizedEmail)

    val remoteBackup = AccountSyncManager.fetchGmailCloudBackup(
      context = context,
      email = normalizedEmail,
      driveAccessToken = effectiveToken
    ) ?: return Result.failure(
      IllegalArgumentException("No Google Drive backup found yet for $normalizedEmail. Tap 'Back Up Now' to create your first backup.")
    )

    val beforeCount = expenseDao.getAllExpensesForUserSync(normalizedEmail).size
    val account = restoreBackupIntoDatabase(remoteBackup)
    val afterCount = expenseDao.getAllExpensesForUserSync(normalizedEmail).size
    val newlyAdded = (afterCount - beforeCount).coerceAtLeast(0)

    setLastBackupSizeBytes(normalizedEmail, remoteBackup.backupSizeBytes)

    return Result.success(
      GmailAuthSyncResult(
        account = account,
        restoredFromGoogleDrive = true,
        restoredExpenseCount = if (newlyAdded > 0) newlyAdded else remoteBackup.expenses.size,
        totalExpenseCount = afterCount,
        backupSizeBytes = remoteBackup.backupSizeBytes,
        lastBackupTimeMillis = remoteBackup.lastSyncedAt
      )
    )
  }

  private suspend fun mergeGuestDataIntoGmailAccount(gmailAddress: String) {
    val guestExpenses = expenseDao.getAllExpensesForUserSync("guest")
    if (guestExpenses.isNotEmpty()) {
      val existingUserExpenses = expenseDao.getAllExpensesForUserSync(gmailAddress)
      val existingSignatures = existingUserExpenses.map {
        "${it.title}|${it.amount}|${it.category}|${it.dateMillis}"
      }.toSet()

      val newGuestExpenses = guestExpenses.filter { exp ->
        val sig = "${exp.title}|${exp.amount}|${exp.category}|${exp.dateMillis}"
        sig !in existingSignatures
      }.map {
        it.copy(id = 0L, userEmail = gmailAddress)
      }

      if (newGuestExpenses.isNotEmpty()) {
        expenseDao.insertExpenses(newGuestExpenses)
      }
    }

    val guestBudgets = expenseDao.getAllCategoryBudgetsSync("guest")
    val existingBudgets = expenseDao.getAllCategoryBudgetsSync(gmailAddress)
    if (guestBudgets.isNotEmpty() && existingBudgets.isEmpty()) {
      expenseDao.upsertCategoryBudgets(guestBudgets.map { it.copy(userEmail = gmailAddress) })
    }

    val guestGoals = expenseDao.getAllMonthlyGoalsSync("guest")
    val existingGoals = expenseDao.getAllMonthlyGoalsSync(gmailAddress)
    if (guestGoals.isNotEmpty() && existingGoals.isEmpty()) {
      expenseDao.insertMonthlyGoals(guestGoals.map { it.copy(userEmail = gmailAddress) })
    }
  }

  suspend fun signOut() {
    setSavedActiveUserEmail("guest")
    ensureDefaultBudgets("guest")
  }

  suspend fun syncAccountData(
    userEmail: String,
    driveAccessToken: String? = null
  ): Boolean {
    val normalizedEmail = AccountSyncManager.normalizeEmail(userEmail)
    if (normalizedEmail == "guest" || normalizedEmail.isBlank()) return false
    val account = expenseDao.getUserAccountByEmail(normalizedEmail) ?: return false
    val expenses = expenseDao.getAllExpensesForUserSync(normalizedEmail)
    val budgets = expenseDao.getAllCategoryBudgetsSync(normalizedEmail)
    val goals = expenseDao.getAllMonthlyGoalsSync(normalizedEmail)
    val now = System.currentTimeMillis()
    val frequency = getBackupFrequency(normalizedEmail)

    if (!driveAccessToken.isNullOrBlank()) {
      setSavedDriveAccessToken(normalizedEmail, driveAccessToken)
    }
    val effectiveToken = driveAccessToken ?: getSavedDriveAccessToken(normalizedEmail)

    val payload = AccountBackupPayload(
      email = normalizedEmail,
      displayName = account.displayName,
      passwordHash = account.passwordHash,
      createdAt = account.createdAt,
      lastSyncedAt = now,
      expenses = expenses,
      budgets = budgets,
      goals = goals,
      backupFrequency = frequency
    )

    val jsonBytes = AccountSyncManager.serializeBackupJson(payload).toByteArray(Charsets.UTF_8).size
    setLastBackupSizeBytes(normalizedEmail, jsonBytes)

    val synced = AccountSyncManager.syncAccountBackup(
      context = context,
      payload = payload.copy(backupSizeBytes = jsonBytes),
      driveAccessToken = effectiveToken
    )
    expenseDao.insertOrUpdateUserAccount(account.copy(lastSyncedAt = now))
    return synced
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
