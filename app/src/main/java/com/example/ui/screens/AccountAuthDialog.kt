package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.util.AccountSyncManager
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountAuthDialog(
  viewModel: ExpenseViewModel,
  initialModeCreateAccount: Boolean = false,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  val currentAccount by viewModel.currentUserAccount.collectAsStateWithLifecycle()
  val registeredAccounts by viewModel.registeredAccounts.collectAsStateWithLifecycle()
  val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
  val categoryBudgets by viewModel.categoryBudgets.collectAsStateWithLifecycle()
  val authError by viewModel.authErrorMessage.collectAsStateWithLifecycle()
  val authFeedback by viewModel.authFeedbackMessage.collectAsStateWithLifecycle()
  val isLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
  val backupFrequency by viewModel.backupFrequency.collectAsStateWithLifecycle()
  val lastBackupSizeBytes by viewModel.lastBackupSizeBytes.collectAsStateWithLifecycle()

  var isCreateMode by remember { mutableStateOf(initialModeCreateAccount) }
  var displayName by remember { mutableStateOf("") }
  var gmailInput by remember { mutableStateOf("") }
  var claimGuestExpenses by remember { mutableStateOf(true) }

  // Pending Google Sign-In state while requesting Drive OAuth consent
  var pendingGmail by remember { mutableStateOf<String?>(null) }
  var pendingDisplayName by remember { mutableStateOf("") }
  var pendingIdToken by remember { mutableStateOf<String?>(null) }

  val driveAuthLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartIntentSenderForResult()
  ) { result ->
    try {
      val authResult = Identity.getAuthorizationClient(context)
        .getAuthorizationResultFromIntent(result.data)
      val accessToken = authResult.accessToken
      val targetEmail = pendingGmail ?: currentAccount?.email
      if (!targetEmail.isNullOrBlank()) {
        if (currentAccount == null) {
          viewModel.connectWithGmail(
            email = targetEmail,
            displayName = pendingDisplayName,
            googleIdToken = pendingIdToken,
            driveAccessToken = accessToken,
            claimGuestExpenses = claimGuestExpenses
          )
        } else {
          viewModel.syncAccountNow(driveAccessToken = accessToken)
        }
      }
    } catch (_: Exception) {
      val targetEmail = pendingGmail
      if (!targetEmail.isNullOrBlank() && currentAccount == null) {
        viewModel.connectWithGmail(
          email = targetEmail,
          displayName = pendingDisplayName,
          googleIdToken = pendingIdToken,
          driveAccessToken = null,
          claimGuestExpenses = claimGuestExpenses
        )
      }
    }
  }

  fun requestDriveAuthorizationAndConnect(
    email: String,
    name: String,
    idToken: String? = null
  ) {
    pendingGmail = email
    pendingDisplayName = name
    pendingIdToken = idToken

    coroutineScope.launch {
      try {
        val authRequest = AuthorizationRequest.builder()
          .setRequestedScopes(
            listOf(
              Scope(AccountSyncManager.DRIVE_APPDATA_SCOPE),
              Scope(AccountSyncManager.DRIVE_FILE_SCOPE)
            )
          )
          .build()

        val authResult = Identity.getAuthorizationClient(context)
          .authorize(authRequest)
          .await()

        if (authResult.hasResolution()) {
          val pendingIntent = authResult.pendingIntent
          if (pendingIntent != null && resultLauncherAvailable(context)) {
            driveAuthLauncher.launch(
              IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            )
            return@launch
          }
        }

        viewModel.connectWithGmail(
          email = email,
          displayName = name,
          googleIdToken = idToken,
          driveAccessToken = authResult.accessToken,
          claimGuestExpenses = claimGuestExpenses
        )
      } catch (_: Exception) {
        // Connect and sync via cloud backup backend even if Play Services Drive prompt is unavailable on emulator
        viewModel.connectWithGmail(
          email = email,
          displayName = name,
          googleIdToken = idToken,
          driveAccessToken = null,
          claimGuestExpenses = claimGuestExpenses
        )
      }
    }
  }

  fun launchGoogleOneTapSignIn() {
    val clientId = viewModel.googleOAuthClientId
    if (clientId.isBlank()) {
      if (gmailInput.isNotBlank()) {
        val normalized = normalizeGmailWithDomain(gmailInput)
        requestDriveAuthorizationAndConnect(normalized, displayName)
      }
      return
    }

    coroutineScope.launch {
      try {
        val credentialManager = CredentialManager.create(context)
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder()
          .addCredentialOption(signInWithGoogleOption)
          .build()

        val result = credentialManager.getCredential(
          request = request,
          context = context
        )
        val credential = result.credential
        if (credential is CustomCredential &&
          credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
          val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
          val googleEmail = googleIdTokenCredential.id
          val googleName = googleIdTokenCredential.displayName.orEmpty()
          gmailInput = googleEmail
          if (googleName.isNotBlank()) {
            displayName = googleName
          }
          requestDriveAuthorizationAndConnect(
            email = googleEmail,
            name = googleName,
            idToken = googleIdTokenCredential.idToken
          )
        }
      } catch (_: Exception) {
        // If emulator has no OS Google account configured in CredentialManager,
        // connect using the entered @gmail.com address if provided
        if (gmailInput.isNotBlank()) {
          val normalized = normalizeGmailWithDomain(gmailInput)
          requestDriveAuthorizationAndConnect(normalized, displayName)
        }
      }
    }
  }

  Dialog(
    onDismissRequest = {
      viewModel.clearAuthMessages()
      onDismiss()
    },
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .widthIn(max = 540.dp)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Dialog Header (WhatsApp Google Drive Backup style)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(IncomeGreen.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (currentAccount != null) Icons.Default.CloudDone else Icons.Default.Backup,
                contentDescription = null,
                tint = IncomeGreen,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = if (currentAccount != null) "Google Drive Backup" else "Gmail Sign In & Backup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (currentAccount != null) "Automatic Google Drive backup via Gmail"
                else "Sign in with Gmail to back up & restore on any device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(
            onClick = {
              viewModel.clearAuthMessages()
              onDismiss()
            },
            modifier = Modifier.testTag("close_auth_dialog_button")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        // Loading Progress Indicator (WhatsApp backup style)
        if (isLoading) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = IncomeGreen.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp,
                  color = IncomeGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Syncing expense history with Google Drive...",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = IncomeGreen
                )
              }
              LinearProgressIndicator(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp)),
                color = IncomeGreen,
                trackColor = IncomeGreen.copy(alpha = 0.2f)
              )
            }
          }
        }

        // Error Banner
        if (!authError.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = ExpenseRed.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = authError ?: "",
              style = MaterialTheme.typography.bodySmall,
              color = ExpenseRed,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(12.dp)
            )
          }
        }

        // Success / Restore Feedback Banner
        if (!authFeedback.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = IncomeGreen.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = IncomeGreen,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = authFeedback ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = IncomeGreen,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        if (currentAccount != null) {
          val account = currentAccount!!
          val lastSyncText = remember(account.lastSyncedAt) {
            SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()).format(Date(account.lastSyncedAt))
          }
          val formattedSize = remember(lastBackupSizeBytes, allExpenses.size) {
            val estimatedBytes = if (lastBackupSizeBytes > 0) lastBackupSizeBytes else (allExpenses.size * 220 + 640)
            AccountSyncManager.formatBackupSize(estimatedBytes)
          }

          // WhatsApp-Style "Last Backup" Summary Card
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(
                  brush = Brush.linearGradient(
                    listOf(
                      Color(0xFF059669),
                      Color(0xFF0F766E)
                    )
                  )
                )
                .padding(18.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(46.dp)
                      .clip(CircleShape)
                      .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.CloudDone,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(24.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Last Backup to Google Drive",
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                    Text(
                      text = lastSyncText,
                      style = MaterialTheme.typography.bodySmall,
                      color = Color.White.copy(alpha = 0.9f)
                    )
                  }
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.22f)
                  ) {
                    Text(
                      text = formattedSize,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text(
                      text = "Gmail Account",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                      text = account.email,
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = "Backed Up History",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                      text = "${allExpenses.size} expenses • ${categoryBudgets.size} budgets",
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                  }
                }
              }
            }
          }

          Text(
            text = "Back up your expense history and budgets to Google Drive. You can restore them anytime when you reinstall the app and sign in with ${account.email}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          // WhatsApp Green "BACK UP" Button
          Button(
            onClick = { viewModel.syncAccountNow() },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("sync_now_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back Up to Google Drive Now", fontWeight = FontWeight.Bold, color = Color.White)
          }

          // Restore from Google Drive Button
          OutlinedButton(
            onClick = { viewModel.restoreFromGoogleDriveNow() },
            enabled = !isLoading,
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp)
              .testTag("restore_drive_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Restore Expense History from Google Drive", fontWeight = FontWeight.SemiBold)
          }

          // WhatsApp-Style "Back up to Google Drive" Frequency Settings
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Schedule,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Auto-Backup to Google Drive",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold
                )
              }

              val frequencies = listOf("Daily", "Weekly", "Monthly", "Only when I tap Back Up")
              FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                frequencies.forEach { freq ->
                  val selected = backupFrequency == freq
                  FilterChip(
                    selected = selected,
                    onClick = { viewModel.updateBackupFrequency(freq) },
                    label = {
                      Text(
                        text = if (freq == "Daily") "Daily (Auto)" else freq,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                      )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = IncomeGreen.copy(alpha = 0.18f),
                      selectedLabelColor = IncomeGreen
                    )
                  )
                }
              }
            }
          }

          HorizontalDivider()

          // Sign Out / Switch Gmail Account Button
          OutlinedButton(
            onClick = { viewModel.signOut() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp)
              .testTag("sign_out_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Disconnect Gmail Account", fontWeight = FontWeight.Bold)
          }
        } else {
          // Signed Out: Sign In with Gmail / Sign Up with Gmail Toggle
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (!isCreateMode) MaterialTheme.colorScheme.primary else Color.Transparent,
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                  isCreateMode = false
                  viewModel.clearAuthMessages()
                }
                .testTag("tab_auth_signin")
            ) {
              Text(
                text = "Login with Gmail",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (!isCreateMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp)
              )
            }

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isCreateMode) MaterialTheme.colorScheme.primary else Color.Transparent,
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                  isCreateMode = true
                  viewModel.clearAuthMessages()
                }
                .testTag("tab_auth_signup")
            ) {
              Text(
                text = "Sign Up with Gmail",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isCreateMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp)
              )
            }
          }

          // Info card explaining how Gmail + Google Drive backup works
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = IncomeGreen.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Backup,
                contentDescription = null,
                tint = IncomeGreen,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Sign in with your @gmail.com account to save your expense history to Google Drive and automatically restore it whenever you reinstall the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }

          // One-Tap Official Google Sign-In Button
          OutlinedButton(
            onClick = { launchGoogleOneTapSignIn() },
            enabled = !isLoading,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("google_onetap_signin_button")
          ) {
            Icon(
              imageVector = Icons.Default.AccountCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Choose Google Account on Device",
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }

          // Previously connected Gmail accounts on this device (tap to connect)
          val gmailAccounts = remember(registeredAccounts) {
            registeredAccounts.filter { AccountSyncManager.isValidGmailAddress(it.email) }
          }
          if (gmailAccounts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Saved Gmail Accounts (Tap to sign in & restore):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              gmailAccounts.forEach { acc ->
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                      gmailInput = acc.email
                      displayName = acc.displayName
                      requestDriveAuthorizationAndConnect(acc.email, acc.displayName)
                    }
                    .testTag("saved_account_chip_${acc.email}")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Email,
                      contentDescription = null,
                      tint = IncomeGreen,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = acc.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                      )
                      Text(
                        text = acc.email,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                    Text(
                      text = "Restore",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = IncomeGreen
                    )
                  }
                }
              }
            }
          }

          if (isCreateMode) {
            OutlinedTextField(
              value = displayName,
              onValueChange = { displayName = it },
              label = { Text("Your Name (Optional)") },
              placeholder = { Text("e.g. Kamrul Islam") },
              leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_name_input"),
              shape = RoundedCornerShape(14.dp)
            )
          }

          OutlinedTextField(
            value = gmailInput,
            onValueChange = { gmailInput = it },
            label = { Text("Gmail Address (@gmail.com)") },
            placeholder = { Text("yourname@gmail.com") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            trailingIcon = {
              if (gmailInput.isNotBlank() && !gmailInput.contains("@")) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { gmailInput = "${gmailInput.trim()}@gmail.com" }
                ) {
                  Text(
                    text = "+ @gmail.com",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("auth_email_input"),
            shape = RoundedCornerShape(14.dp)
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable { claimGuestExpenses = !claimGuestExpenses }
              .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = claimGuestExpenses,
              onCheckedChange = { claimGuestExpenses = it }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Include existing device expenses in Google Drive backup",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Button(
            onClick = {
              val finalEmail = normalizeGmailWithDomain(gmailInput)
              gmailInput = finalEmail
              requestDriveAuthorizationAndConnect(
                email = finalEmail,
                name = displayName
              )
            },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("auth_submit_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CloudDone,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isCreateMode) "Sign Up with Gmail & Back Up to Drive"
              else "Login with Gmail & Restore from Drive",
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }
      }
    }
  }
}

private fun normalizeGmailWithDomain(rawInput: String): String {
  val trimmed = rawInput.trim().lowercase(Locale.ROOT)
  if (trimmed.isBlank()) return ""
  return if (!trimmed.contains("@")) {
    "$trimmed@gmail.com"
  } else {
    trimmed
  }
}

private fun resultLauncherAvailable(context: android.content.Context): Boolean {
  return context is Activity
}
