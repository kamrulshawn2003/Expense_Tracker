package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoalGold
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountAuthDialog(
  viewModel: ExpenseViewModel,
  initialModeCreateAccount: Boolean = false,
  onDismiss: () -> Unit
) {
  val currentAccount by viewModel.currentUserAccount.collectAsStateWithLifecycle()
  val registeredAccounts by viewModel.registeredAccounts.collectAsStateWithLifecycle()
  val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
  val categoryBudgets by viewModel.categoryBudgets.collectAsStateWithLifecycle()
  val authError by viewModel.authErrorMessage.collectAsStateWithLifecycle()
  val authFeedback by viewModel.authFeedbackMessage.collectAsStateWithLifecycle()
  val isLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()

  var isCreateMode by remember { mutableStateOf(initialModeCreateAccount) }
  var displayName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var claimGuestExpenses by remember { mutableStateOf(true) }

  val exportBackupLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
  ) { uri ->
    if (uri != null) {
      viewModel.exportAccountBackup(uri)
    }
  }

  val importBackupLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      viewModel.importAccountBackup(uri)
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
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Dialog Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (currentAccount != null) Icons.Default.CloudDone else Icons.Default.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = if (currentAccount != null) "Account & Expense Backup" else "Sign In or Create Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (currentAccount != null) "Your expenses are linked to your account"
                else "Keep your expense history safe across reinstalls",
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

        // Feedback / Error Banner
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

          // Active User Profile Card
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
                      MaterialTheme.colorScheme.primary,
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
                      .size(48.dp)
                      .clip(CircleShape)
                      .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = account.displayName.take(1).uppercase(),
                      style = MaterialTheme.typography.titleLarge,
                      fontWeight = FontWeight.ExtraBold,
                      color = Color.White
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = account.displayName,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                    Text(
                      text = account.email,
                      style = MaterialTheme.typography.bodySmall,
                      color = Color.White.copy(alpha = 0.85f)
                    )
                  }
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.2f)
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "Synced",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                      )
                    }
                  }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text(
                      text = "Saved Expenses",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                      text = "${allExpenses.size} records",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                  }
                  Column {
                    Text(
                      text = "Saved Budgets",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                      text = "${categoryBudgets.size} categories",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = "Last Synced",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                      text = lastSyncText,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.SemiBold,
                      color = Color.White
                    )
                  }
                }
              }
            }
          }

          Text(
            text = "Your expenses, custom budgets, and savings goals are automatically linked to ${account.email}. You can also save an external backup file to your device or Google Drive so your data is 100% recoverable after uninstalling.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          // Sync Now Button
          Button(
            onClick = { viewModel.syncAccountNow() },
            enabled = !isLoading,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("sync_now_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sync & Back Up Expenses Now", fontWeight = FontWeight.Bold)
          }

          // Export & Import Backup File Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedButton(
              onClick = {
                val safeName = account.email.substringBefore("@").replace(Regex("[^a-zA-Z0-9]"), "_")
                exportBackupLauncher.launch("expense_backup_${safeName}.json")
              },
              modifier = Modifier
                .weight(1f)
                .testTag("export_backup_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Save Backup", style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
              onClick = {
                importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
              },
              modifier = Modifier
                .weight(1f)
                .testTag("import_backup_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Restore File", style = MaterialTheme.typography.labelMedium)
            }
          }

          HorizontalDivider()

          // Sign Out Button
          OutlinedButton(
            onClick = { viewModel.signOut() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("sign_out_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", fontWeight = FontWeight.Bold)
          }
        } else {
          // Signed Out: Sign In / Create Account Segmented Toggle
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
                text = "Sign In",
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
                text = "Create Account",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isCreateMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp)
              )
            }
          }

          // Quick-fill saved accounts when in Sign-In mode
          if (!isCreateMode && registeredAccounts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Saved Accounts on Device (Tap to fill):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              registeredAccounts.forEach { acc ->
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                      email = acc.email
                      viewModel.clearAuthMessages()
                    }
                    .testTag("saved_account_chip_${acc.email}")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Person,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
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
                  }
                }
              }
            }
          }

          if (isCreateMode) {
            OutlinedTextField(
              value = displayName,
              onValueChange = { displayName = it },
              label = { Text("Full Name") },
              placeholder = { Text("e.g. Alex Chen") },
              leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_name_input"),
              shape = RoundedCornerShape(14.dp)
            )
          }

          OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            placeholder = { Text("you@example.com") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("auth_email_input"),
            shape = RoundedCornerShape(14.dp)
          )

          OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            placeholder = { Text("At least 4 characters") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
              IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (passwordVisible) "Hide Password" else "Show Password"
                )
              }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("auth_password_input"),
            shape = RoundedCornerShape(14.dp)
          )

          if (isCreateMode) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { claimGuestExpenses = !claimGuestExpenses }
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Checkbox(
                checked = claimGuestExpenses,
                onCheckedChange = { claimGuestExpenses = it }
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Save & link existing guest expenses to this new account",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }

          Button(
            onClick = {
              if (isCreateMode) {
                viewModel.createAccount(
                  displayName = displayName,
                  email = email,
                  password = password,
                  claimGuestExpenses = claimGuestExpenses
                )
              } else {
                viewModel.signIn(
                  email = email,
                  password = password
                )
              }
            },
            enabled = !isLoading,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("auth_submit_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
              )
              Spacer(modifier = Modifier.width(8.dp))
            } else {
              Icon(
                imageVector = if (isCreateMode) Icons.Default.PersonAdd else Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
              text = if (isCreateMode) "Create Account & Back Up" else "Sign In & Restore Expenses",
              fontWeight = FontWeight.Bold
            )
          }

          HorizontalDivider()

          // Restore from Backup File option (for users reinstalling the app)
          OutlinedButton(
            onClick = {
              importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("restore_backup_file_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Restore Account from Backup File (.json)")
          }
        }
      }
    }
  }
}
