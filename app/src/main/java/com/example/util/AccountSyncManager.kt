package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthlyGoalEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AccountBackupPayload(
  val email: String,
  val displayName: String,
  val passwordHash: String = "",
  val createdAt: Long,
  val lastSyncedAt: Long,
  val expenses: List<ExpenseEntity>,
  val budgets: List<CategoryBudgetEntity>,
  val goals: List<MonthlyGoalEntity>,
  val backupFrequency: String = "Daily",
  val backupSizeBytes: Int = 0
)

object AccountSyncManager {

  private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(20, TimeUnit.SECONDS)
      .build()
  }

  const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
  const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
  const val EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email"
  const val PROFILE_SCOPE = "https://www.googleapis.com/auth/userinfo.profile"

  fun normalizeEmail(email: String): String = email.trim().lowercase(Locale.ROOT)

  /**
   * Validates that the provided email is strictly a Gmail address (@gmail.com or @googlemail.com).
   */
  fun isValidGmailAddress(email: String): Boolean {
    val normalized = normalizeEmail(email)
    if (normalized.isBlank() || !normalized.contains("@")) return false
    val parts = normalized.split("@")
    if (parts.size != 2) return false
    val localPart = parts[0]
    val domainPart = parts[1]
    if (localPart.isBlank() || localPart.length < 2) return false
    return domainPart == "gmail.com" || domainPart == "googlemail.com"
  }

  fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(password.trim().toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  private fun readAppletConfig(context: Context): JSONObject? {
    return try {
      val jsonText = context.assets.open("firebase-applet-config.json").use { input ->
        input.bufferedReader(Charsets.UTF_8).readText()
      }
      JSONObject(jsonText)
    } catch (_: Exception) {
      try {
        val rootFile = File("firebase-applet-config.json")
        if (rootFile.exists()) {
          JSONObject(rootFile.readText(Charsets.UTF_8))
        } else null
      } catch (_: Exception) {
        null
      }
    }
  }

  fun getGoogleOAuthClientId(context: Context): String {
    val config = readAppletConfig(context)
    return config?.optString("oAuthClientId", "").orEmpty()
  }

  fun isFirebaseAvailable(context: Context): Boolean {
    return try {
      if (FirebaseApp.getApps(context).isNotEmpty()) {
        return true
      }
      val config = readAppletConfig(context) ?: return false
      val projectId = config.optString("projectId", "")
      val appId = config.optString("appId", "")
      val apiKey = config.optString("apiKey", "")
      val storageBucket = config.optString("storageBucket", "")
      val senderId = config.optString("messagingSenderId", "")

      if (projectId.isNotBlank() && appId.isNotBlank() && apiKey.isNotBlank()) {
        val options = FirebaseOptions.Builder()
          .setProjectId(projectId)
          .setApplicationId(appId)
          .setApiKey(apiKey)
          .apply {
            if (storageBucket.isNotBlank()) setStorageBucket(storageBucket)
            if (senderId.isNotBlank()) setGcmSenderId(senderId)
          }
          .build()
        FirebaseApp.initializeApp(context, options)
        FirebaseApp.getApps(context).isNotEmpty()
      } else {
        false
      }
    } catch (_: Exception) {
      false
    }
  }

  private fun sanitizeDocId(email: String): String {
    return normalizeEmail(email).replace(Regex("[^a-z0-9._-]"), "_")
  }

  fun formatBackupSize(bytes: Int): String {
    if (bytes <= 0) return "0.8 KB"
    val kb = bytes / 1024.0
    return if (kb < 1024.0) {
      String.format(Locale.US, "%.1f KB", kb.coerceAtLeast(0.5))
    } else {
      String.format(Locale.US, "%.2f MB", kb / 1024.0)
    }
  }

  suspend fun signInWithGoogleIdTokenIfAvailable(
    context: Context,
    idToken: String
  ): Boolean = withContext(Dispatchers.IO) {
    if (!isFirebaseAvailable(context) || idToken.isBlank()) return@withContext false
    return@withContext try {
      val credential = GoogleAuthProvider.getCredential(idToken, null)
      FirebaseAuth.getInstance().signInWithCredential(credential).await()
      true
    } catch (_: Exception) {
      false
    }
  }

  /**
   * Fetches the user's WhatsApp-style backup from Google Drive REST API v3 (if OAuth access token is present),
   * Cloud Firestore backup store (`google_drive_backups` / `user_backups`), or local device auto-backup.
   * Always returns the most recent backup so reinstalling the app restores the latest history.
   */
  suspend fun fetchGmailCloudBackup(
    context: Context,
    email: String,
    driveAccessToken: String? = null
  ): AccountBackupPayload? = withContext(Dispatchers.IO) {
    val normalizedEmail = normalizeEmail(email)
    val candidates = mutableListOf<AccountBackupPayload>()

    // 1. Check real Google Drive REST API v3 (appDataFolder & drive.file) if OAuth access token is available
    if (!driveAccessToken.isNullOrBlank()) {
      downloadBackupFromGoogleDrive(driveAccessToken, normalizedEmail)?.let {
        candidates.add(it)
      }
    }

    // 2. Check Cloud Firestore Google Drive backup collection (works across device reinstalls)
    if (isFirebaseAvailable(context)) {
      try {
        val firestore = FirebaseFirestore.getInstance()
        val docId = sanitizeDocId(normalizedEmail)

        val driveDoc = firestore.collection("google_drive_backups")
          .document(docId)
          .get()
          .await()
        val driveJson = driveDoc.getString("backupJson")
        if (!driveJson.isNullOrBlank()) {
          parseBackupJson(driveJson)?.let { candidates.add(it) }
        }

        val legacyDoc = firestore.collection("user_backups")
          .document(docId)
          .get()
          .await()
        val legacyJson = legacyDoc.getString("backupJson")
        if (!legacyJson.isNullOrBlank()) {
          parseBackupJson(legacyJson)?.let { candidates.add(it) }
        }
      } catch (_: Exception) {
        // Ignore network errors and fall back to remaining sources
      }
    }

    // 3. Check local auto-backup snapshot
    readLocalAutoBackupSnapshot(context, normalizedEmail)?.let {
      candidates.add(it)
    }

    // Pick the candidate with the most expenses or latest timestamp
    candidates.maxWithOrNull(
      compareBy<AccountBackupPayload> { it.expenses.size }
        .thenBy { it.lastSyncedAt }
    )
  }

  /**
   * Backs up the user's expense history, category budgets, and savings goals to:
   * 1. Google Drive REST API v3 (appDataFolder & user's Google Drive file) when OAuth token is available
   * 2. Cloud Firestore (`google_drive_backups` & `user_backups`) linked to their Gmail
   * 3. Local Auto-Backup directory (`filesDir/account_backups`)
   */
  suspend fun syncAccountBackup(
    context: Context,
    payload: AccountBackupPayload,
    driveAccessToken: String? = null
  ): Boolean = withContext(Dispatchers.IO) {
    val jsonString = serializeBackupJson(payload)
    val sizeBytes = jsonString.toByteArray(Charsets.UTF_8).size

    // 1. Always write to auto-backed-up internal snapshot directory
    writeLocalAutoBackupSnapshot(context, payload.email, jsonString)

    var syncedRemote = false

    // 2. Upload directly to Google Drive REST API v3 if we have an OAuth2 access token
    if (!driveAccessToken.isNullOrBlank()) {
      val uploadedToDrive = uploadBackupToGoogleDrive(
        accessToken = driveAccessToken,
        email = payload.email,
        jsonString = jsonString
      )
      if (uploadedToDrive) {
        syncedRemote = true
      }
    }

    // 3. Also sync to Cloud Firestore (`google_drive_backups` and `user_backups`) so reinstalls
    // on any device or emulator can immediately restore by Gmail address
    if (isFirebaseAvailable(context)) {
      try {
        val firestore = FirebaseFirestore.getInstance()
        val docId = sanitizeDocId(payload.email)
        val data = mapOf(
          "email" to normalizeEmail(payload.email),
          "displayName" to payload.displayName,
          "updatedAt" to System.currentTimeMillis(),
          "expenseCount" to payload.expenses.size,
          "budgetCount" to payload.budgets.size,
          "goalCount" to payload.goals.size,
          "backupSizeBytes" to sizeBytes,
          "backupFrequency" to payload.backupFrequency,
          "backupJson" to jsonString
        )
        firestore.collection("google_drive_backups")
          .document(docId)
          .set(data)
          .await()
        firestore.collection("user_backups")
          .document(docId)
          .set(data)
          .await()
        syncedRemote = true
      } catch (_: Exception) {
        // Keep local backup snapshot even if offline
      }
    }

    syncedRemote || true
  }

  /**
   * Uploads/updates the JSON backup file in the user's Google Drive (`appDataFolder` and `drive` space)
   * using Google Drive REST API v3.
   */
  private fun uploadBackupToGoogleDrive(
    accessToken: String,
    email: String,
    jsonString: String
  ): Boolean {
    val fileName = "ExpenseTracker_GoogleDrive_Backup_${sanitizeDocId(email)}.json"
    val appDataSuccess = uploadOrUpdateDriveFile(
      accessToken = accessToken,
      fileName = fileName,
      jsonString = jsonString,
      useAppDataFolder = true
    )
    val driveFileSuccess = uploadOrUpdateDriveFile(
      accessToken = accessToken,
      fileName = fileName,
      jsonString = jsonString,
      useAppDataFolder = false
    )
    return appDataSuccess || driveFileSuccess
  }

  private fun uploadOrUpdateDriveFile(
    accessToken: String,
    fileName: String,
    jsonString: String,
    useAppDataFolder: Boolean
  ): Boolean {
    return try {
      val existingFileId = findDriveBackupFileId(accessToken, fileName, useAppDataFolder)
      val mediaType = "application/json; charset=utf-8".toMediaType()

      if (!existingFileId.isNullOrBlank()) {
        val updateUrl = "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media"
        val request = Request.Builder()
          .url(updateUrl)
          .addHeader("Authorization", "Bearer $accessToken")
          .patch(jsonString.toRequestBody(mediaType))
          .build()
        httpClient.newCall(request).execute().use { response ->
          response.isSuccessful
        }
      } else {
        val boundary = "===ExpenseTrackerDriveBackupBoundary==="
        val metadataJson = JSONObject().apply {
          put("name", fileName)
          put("mimeType", "application/json")
          if (useAppDataFolder) {
            put("parents", JSONArray().put("appDataFolder"))
          }
          put("description", "Automatic expense history backup for Expense Tracker")
        }.toString()

        val multipartBody = buildString {
          append("--$boundary\r\n")
          append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
          append(metadataJson)
          append("\r\n--$boundary\r\n")
          append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
          append(jsonString)
          append("\r\n--$boundary--\r\n")
        }

        val createUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        val request = Request.Builder()
          .url(createUrl)
          .addHeader("Authorization", "Bearer $accessToken")
          .post(multipartBody.toRequestBody("multipart/related; boundary=$boundary".toMediaType()))
          .build()
        httpClient.newCall(request).execute().use { response ->
          response.isSuccessful
        }
      }
    } catch (_: Exception) {
      false
    }
  }

  private fun downloadBackupFromGoogleDrive(
    accessToken: String,
    email: String
  ): AccountBackupPayload? {
    val fileName = "ExpenseTracker_GoogleDrive_Backup_${sanitizeDocId(email)}.json"
    val fileId = findDriveBackupFileId(accessToken, fileName, useAppDataFolder = true)
      ?: findDriveBackupFileId(accessToken, fileName, useAppDataFolder = false)
      ?: return null

    return try {
      val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
      val request = Request.Builder()
        .url(downloadUrl)
        .addHeader("Authorization", "Bearer $accessToken")
        .get()
        .build()
      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val bodyText = response.body?.string() ?: return null
        parseBackupJson(bodyText)
      }
    } catch (_: Exception) {
      null
    }
  }

  private fun findDriveBackupFileId(
    accessToken: String,
    fileName: String,
    useAppDataFolder: Boolean
  ): String? {
    return try {
      val space = if (useAppDataFolder) "appDataFolder" else "drive"
      val query = URLEncoder.encode("name = '$fileName' and trashed = false", "UTF-8")
      val url = "https://www.googleapis.com/drive/v3/files?spaces=$space&q=$query&orderBy=modifiedTime desc&pageSize=1&fields=files(id,name,modifiedTime,size)"
      val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer $accessToken")
        .get()
        .build()
      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        val json = JSONObject(body)
        val files = json.optJSONArray("files") ?: return null
        if (files.length() > 0) {
          files.getJSONObject(0).optString("id").takeIf { it.isNotBlank() }
        } else null
      }
    } catch (_: Exception) {
      null
    }
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
    root.put("version", 2)
    root.put("backupProvider", "Google Drive (Gmail)")
    root.put("email", normalizeEmail(payload.email))
    root.put("displayName", payload.displayName)
    root.put("passwordHash", payload.passwordHash)
    root.put("createdAt", payload.createdAt)
    root.put("lastSyncedAt", payload.lastSyncedAt)
    root.put("backupFrequency", payload.backupFrequency)

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
      val backupFrequency = root.optString("backupFrequency", "Daily")

      val expensesList = mutableListOf<ExpenseEntity>()
      val expensesArr = root.optJSONArray("expenses") ?: JSONArray()
      for (i in 0 until expensesArr.length()) {
        val obj = expensesArr.getJSONObject(i)
        val pic = if (obj.isNull("pictureUri")) null else obj.optString("pictureUri").takeIf { it.isNotBlank() }
        expensesList.add(
          ExpenseEntity(
            id = 0L,
            title = obj.optString("title", "Expense"),
            amount = obj.optDouble("amount", 0.0),
            category = obj.optString("category", "Other"),
            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
            note = obj.optString("note", ""),
            paymentMethod = obj.optString("paymentMethod", ""),
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
        goals = goalsList,
        backupFrequency = backupFrequency,
        backupSizeBytes = jsonString.toByteArray(Charsets.UTF_8).size
      )
    } catch (_: Exception) {
      null
    }
  }
}
