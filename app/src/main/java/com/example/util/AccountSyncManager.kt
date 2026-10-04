package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthlyGoalEntity
import com.example.data.model.UserAccountEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class AccountBackupPayload(
  val email: String,
  val displayName: String,
  val passwordHash: String,
  val createdAt: Long,
  val lastSyncedAt: Long,
  val expenses: List<ExpenseEntity>,
  val budgets: List<CategoryBudgetEntity>,
  val goals: List<MonthlyGoalEntity>
)

object AccountSyncManager {

  fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(password.trim().toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  fun normalizeEmail(email: String): String = email.trim().lowercase()

  fun isFirebaseAvailable(context: Context): Boolean {
    return try {
      FirebaseApp.getApps(context).isNotEmpty()
    } catch (_: Exception) {
      false
    }
  }

  private fun sanitizeDocId(email: String): String {
    return normalizeEmail(email).replace(Regex("[^a-z0-9._-]"), "_")
  }

  suspend fun createCloudAccountIfAvailable(
    context: Context,
    email: String,
    password: String,
    displayName: String
  ): Boolean = withContext(Dispatchers.IO) {
    if (!isFirebaseAvailable(context)) return@withContext false
    return@withContext try {
      val auth = FirebaseAuth.getInstance()
      val result = auth.createUserWithEmailAndPassword(normalizeEmail(email), password).await()
      val profileUpdates = UserProfileChangeRequest.Builder()
        .setDisplayName(displayName.trim())
        .build()
      result.user?.updateProfile(profileUpdates)?.await()
      true
    } catch (_: Exception) {
      false
    }
  }

  suspend fun signInAndFetchCloudBackupIfAvailable(
    context: Context,
    email: String,
    password: String
  ): AccountBackupPayload? = withContext(Dispatchers.IO) {
    val normalizedEmail = normalizeEmail(email)
    // First check if Firebase cloud sync is configured
    if (isFirebaseAvailable(context)) {
      try {
        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(normalizedEmail, password).await()
        val firestore = FirebaseFirestore.getInstance()
        val doc = firestore.collection("user_backups")
          .document(sanitizeDocId(normalizedEmail))
          .get()
          .await()
        val payloadJson = doc.getString("backupJson")
        if (!payloadJson.isNullOrBlank()) {
          parseBackupJson(payloadJson)?.let { return@withContext it }
        }
      } catch (_: Exception) {
        // Fall back to local auto-backup snapshot if offline or not configured
      }
    }

    // Also check local auto-backed-up snapshot file
    readLocalAutoBackupSnapshot(context, normalizedEmail)
  }

  suspend fun syncAccountBackup(
    context: Context,
    payload: AccountBackupPayload
  ): Boolean = withContext(Dispatchers.IO) {
    val jsonString = serializeBackupJson(payload)
    // 1. Always write to auto-backed-up internal snapshot directory
    writeLocalAutoBackupSnapshot(context, payload.email, jsonString)

    // 2. If Firebase is configured, also sync to Cloud Firestore
    if (isFirebaseAvailable(context)) {
      return@withContext try {
        val firestore = FirebaseFirestore.getInstance()
        val data = mapOf(
          "email" to normalizeEmail(payload.email),
          "displayName" to payload.displayName,
          "updatedAt" to System.currentTimeMillis(),
          "expenseCount" to payload.expenses.size,
          "backupJson" to jsonString
        )
        firestore.collection("user_backups")
          .document(sanitizeDocId(payload.email))
          .set(data)
          .await()
        true
      } catch (_: Exception) {
        false
      }
    }
    true
  }

  private fun getBackupDir(context: Context): File {
    val dir = File(context.filesDir, "account_backups")
    if (!dir.exists()) dir.mkdirs()
    return dir
  }

  private fun writeLocalAutoBackupSnapshot(context: Context, email: String, jsonString: String) {
    try {
      val file = File(getBackupDir(context), "${sanitizeDocId(email)}.json")
      file.writeText(jsonString, Charsets.UTF_8)
    } catch (_: Exception) {}
  }

  fun readLocalAutoBackupSnapshot(context: Context, email: String): AccountBackupPayload? {
    return try {
      val file = File(getBackupDir(context), "${sanitizeDocId(email)}.json")
      if (!file.exists()) return null
      parseBackupJson(file.readText(Charsets.UTF_8))
    } catch (_: Exception) {
      null
    }
  }

  suspend fun exportBackupToUri(
    context: Context,
    uri: Uri,
    payload: AccountBackupPayload
  ): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
      val jsonString = serializeBackupJson(payload)
      context.contentResolver.openOutputStream(uri)?.use { out ->
        out.write(jsonString.toByteArray(Charsets.UTF_8))
        out.flush()
      }
      true
    } catch (_: Exception) {
      false
    }
  }

  suspend fun importBackupFromUri(
    context: Context,
    uri: Uri
  ): AccountBackupPayload? = withContext(Dispatchers.IO) {
    return@withContext try {
      val text = context.contentResolver.openInputStream(uri)?.use { input ->
        input.bufferedReader(Charsets.UTF_8).readText()
      } ?: return@withContext null
      parseBackupJson(text)
    } catch (_: Exception) {
      null
    }
  }

  fun serializeBackupJson(payload: AccountBackupPayload): String {
    val root = JSONObject()
    root.put("version", 1)
    root.put("email", normalizeEmail(payload.email))
    root.put("displayName", payload.displayName)
    root.put("passwordHash", payload.passwordHash)
    root.put("createdAt", payload.createdAt)
    root.put("lastSyncedAt", payload.lastSyncedAt)

    val expensesArr = JSONArray()
    payload.expenses.forEach { exp ->
      val obj = JSONObject()
      obj.put("id", exp.id)
      obj.put("title", exp.title)
      obj.put("amount", exp.amount)
      obj.put("category", exp.category)
      obj.put("dateMillis", exp.dateMillis)
      obj.put("note", exp.note)
      obj.put("paymentMethod", exp.paymentMethod)
      obj.put("pictureUri", exp.pictureUri ?: JSONObject.NULL)
      expensesArr.put(obj)
    }
    root.put("expenses", expensesArr)

    val budgetsArr = JSONArray()
    payload.budgets.forEach { b ->
      val obj = JSONObject()
      obj.put("categoryName", b.categoryName)
      obj.put("monthlyLimit", b.monthlyLimit)
      obj.put("iconName", b.iconName)
      obj.put("colorHex", b.colorHex)
      obj.put("pictureUri", b.pictureUri ?: JSONObject.NULL)
      budgetsArr.put(obj)
    }
    root.put("budgets", budgetsArr)

    val goalsArr = JSONArray()
    payload.goals.forEach { g ->
      val obj = JSONObject()
      obj.put("yearMonth", g.yearMonth)
      obj.put("savingsGoal", g.savingsGoal)
      obj.put("monthlyIncome", g.monthlyIncome)
      obj.put("currencySymbol", g.currencySymbol)
      goalsArr.put(obj)
    }
    root.put("goals", goalsArr)

    return root.toString(2)
  }

  fun parseBackupJson(jsonString: String): AccountBackupPayload? {
    return try {
      val root = JSONObject(jsonString)
      val email = normalizeEmail(root.optString("email", ""))
      if (email.isBlank()) return null
      val displayName = root.optString("displayName", email.substringBefore("@"))
      val passwordHash = root.optString("passwordHash", "")
      val createdAt = root.optLong("createdAt", System.currentTimeMillis())
      val lastSyncedAt = root.optLong("lastSyncedAt", System.currentTimeMillis())

      val expensesList = mutableListOf<ExpenseEntity>()
      val expensesArr = root.optJSONArray("expenses") ?: JSONArray()
      for (i in 0 until expensesArr.length()) {
        val obj = expensesArr.getJSONObject(i)
        val pic = if (obj.isNull("pictureUri")) null else obj.optString("pictureUri").takeIf { it.isNotBlank() }
        expensesList.add(
          ExpenseEntity(
            id = 0L, // Let Room auto-generate or preserve cleanly
            title = obj.optString("title", "Expense"),
            amount = obj.optDouble("amount", 0.0),
            category = obj.optString("category", "Other"),
            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
            note = obj.optString("note", ""),
            paymentMethod = obj.optString("paymentMethod", "Card"),
            pictureUri = pic,
            userEmail = email
          )
        )
      }

      val budgetsList = mutableListOf<CategoryBudgetEntity>()
      val budgetsArr = root.optJSONArray("budgets") ?: JSONArray()
      for (i in 0 until budgetsArr.length()) {
        val obj = budgetsArr.getJSONObject(i)
        val pic = if (obj.isNull("pictureUri")) null else obj.optString("pictureUri").takeIf { it.isNotBlank() }
        budgetsList.add(
          CategoryBudgetEntity(
            categoryName = obj.optString("categoryName", "Other"),
            monthlyLimit = obj.optDouble("monthlyLimit", 200.0),
            iconName = obj.optString("iconName", "category"),
            colorHex = obj.optLong("colorHex", 0xFF10B981),
            pictureUri = pic,
            userEmail = email
          )
        )
      }

      val goalsList = mutableListOf<MonthlyGoalEntity>()
      val goalsArr = root.optJSONArray("goals") ?: JSONArray()
      for (i in 0 until goalsArr.length()) {
        val obj = goalsArr.getJSONObject(i)
        goalsList.add(
          MonthlyGoalEntity(
            yearMonth = obj.optString("yearMonth", "DEFAULT"),
            savingsGoal = obj.optDouble("savingsGoal", 500.0),
            monthlyIncome = obj.optDouble("monthlyIncome", 3000.0),
            currencySymbol = obj.optString("currencySymbol", "$"),
            userEmail = email
          )
        )
      }

      AccountBackupPayload(
        email = email,
        displayName = displayName,
        passwordHash = passwordHash,
        createdAt = createdAt,
        lastSyncedAt = lastSyncedAt,
        expenses = expensesList,
        budgets = budgetsList,
        goals = goalsList
      )
    } catch (_: Exception) {
      null
    }
  }
}
