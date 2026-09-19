package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoalGold
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.ExpenseViewModel
import java.util.Locale

@Composable
fun SavingsGoalScreen(
  viewModel: ExpenseViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val monthlyGoal by viewModel.monthlyGoal.collectAsStateWithLifecycle()
  val monthlyReport by viewModel.monthlyReport.collectAsStateWithLifecycle()
  val currency = monthlyGoal.currencySymbol

  var savingsGoalInput by remember(monthlyGoal.savingsGoal) {
    mutableStateOf(String.format(Locale.US, "%.0f", monthlyGoal.savingsGoal))
  }
  var incomeInput by remember(monthlyGoal.monthlyIncome) {
    mutableStateOf(String.format(Locale.US, "%.0f", monthlyGoal.monthlyIncome))
  }
  var currencyInput by remember(monthlyGoal.currencySymbol) {
    mutableStateOf(monthlyGoal.currencySymbol)
  }

  var hasNotificationPermission by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasNotificationPermission = isGranted
  }

  var isSavedNoticeVisible by remember { mutableStateOf(false) }
  var showResetCycleDialog by remember { mutableStateOf(false) }

  val targetSavings = savingsGoalInput.toDoubleOrNull() ?: monthlyGoal.savingsGoal
  val currentIncome = incomeInput.toDoubleOrNull() ?: monthlyGoal.monthlyIncome
  val sixMonthProjection = targetSavings * 6
  val oneYearProjection = targetSavings * 12

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Hero Goal Banner
      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = Color.Transparent),
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                brush = Brush.linearGradient(
                  listOf(
                    GoalGold,
                    Color(0xFFD97706)
                  )
                )
              )
              .padding(22.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Monthly Savings Goal",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                  )
                }
              }

              Text(
                text = String.format(Locale.getDefault(), "%s%.2f / month", currency, targetSavings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
              )

              Text(
                text = "Target to set aside each month before allocating category budgets.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f)
              )
            }
          }
        }
      }

      // 2. Goal & Income Settings Form
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Text(
              text = "Adjust Fixed Monthly Goal & Income",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )

            // Savings Goal Input
            OutlinedTextField(
              value = savingsGoalInput,
              onValueChange = { savingsGoalInput = it },
              label = { Text("Fixed Monthly Savings Goal ($currency)") },
              placeholder = { Text("500") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("savings_goal_input"),
              shape = RoundedCornerShape(14.dp)
            )

            // Preset chips for quick goal setting
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              listOf("300", "500", "750", "1000").forEach { goalPreset ->
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (savingsGoalInput == goalPreset) GoalGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { savingsGoalInput = goalPreset }
                ) {
                  Text(
                    text = "$currency$goalPreset",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = if (savingsGoalInput == goalPreset) GoalGold else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
                  )
                }
              }
            }

            // Monthly Income Input
            OutlinedTextField(
              value = incomeInput,
              onValueChange = { incomeInput = it },
              label = { Text("Expected Monthly Income ($currency)") },
              placeholder = { Text("3000") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("monthly_income_input"),
              shape = RoundedCornerShape(14.dp)
            )

            // Currency Symbol
            OutlinedTextField(
              value = currencyInput,
              onValueChange = { if (it.length <= 3) currencyInput = it },
              label = { Text("Currency Symbol") },
              placeholder = { Text("$") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("currency_symbol_input"),
              shape = RoundedCornerShape(14.dp)
            )

            Button(
              onClick = {
                val goalVal = savingsGoalInput.toDoubleOrNull() ?: 500.0
                val incomeVal = incomeInput.toDoubleOrNull() ?: 3000.0
                viewModel.updateMonthlyGoal(
                  savingsGoal = goalVal,
                  monthlyIncome = incomeVal,
                  currency = if (currencyInput.isNotBlank()) currencyInput else "$"
                )
                isSavedNoticeVisible = true
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_goals_button"),
              shape = RoundedCornerShape(14.dp)
            ) {
              Icon(imageVector = Icons.Default.Check, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Save Financial Goal", fontWeight = FontWeight.Bold)
            }

            if (isSavedNoticeVisible) {
              Text(
                text = "✓ Savings goal & income updated successfully!",
                style = MaterialTheme.typography.bodySmall,
                color = IncomeGreen,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }

      // 3. Long Term Growth Projection Card
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.AutoGraph,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Savings Projection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = "In 6 Months",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = String.format(Locale.getDefault(), "%s%.0f", currency, sixMonthProjection),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = "In 1 Year",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = String.format(Locale.getDefault(), "%s%.0f", currency, oneYearProjection),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen
                  )
                }
              }
            }
          }
        }
      }

      // 4. Notifications Center & Test Trigger
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.NotificationsActive,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Limit Exceed Notifications",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = if (hasNotificationPermission) "System notifications active" else "Permission required",
                  style = MaterialTheme.typography.bodySmall,
                  color = if (hasNotificationPermission) IncomeGreen else GoalGold
                )
              }
            }

            Text(
              text = "The app immediately monitors every daily expense logged and fires an alert notification whenever a category reaches or exceeds its set budget.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                Button(
                  onClick = {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                  },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Enable Notifications")
                }
              }

              OutlinedButton(
                onClick = {
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                  }
                  viewModel.sendTestNotification()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).testTag("test_notification_button")
              ) {
                Icon(imageVector = Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Alert", style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }

      // 5. 1-Month Count Cycle Engine Control
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Autorenew,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "1-Month Count Cycle Engine",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Day ${monthlyReport.daysElapsed} of ${monthlyReport.daysInCycle} • ${monthlyReport.daysRemainingInCycle} days left",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            Text(
              text = "Expenses, category consumption, and budget limits operate on a strict 1-month count cycle. After each month concludes, calculations automatically start over from $0.00 for the next cycle.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Next Cycle Auto-Reset Date:",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = monthlyReport.nextCycleResetDate,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold
                )
              }

              OutlinedButton(
                onClick = { showResetCycleDialog = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                modifier = Modifier.testTag("manual_reset_cycle_button")
              ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restart from $0", style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }
      }

      // 6. Sample Data Generator
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Load Sample Expenses",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Populate realistic sample expenses inside the active 1-month cycle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
              onClick = { viewModel.populateSampleData() },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("load_sample_data_button")
            ) {
              Text("Load Data", style = MaterialTheme.typography.labelMedium)
            }
          }
        }
      }
    }

    if (showResetCycleDialog) {
      AlertDialog(
        onDismissRequest = { showResetCycleDialog = false },
        icon = {
          Icon(
            imageVector = Icons.Default.RestartAlt,
            contentDescription = null,
            tint = ExpenseRed
          )
        },
        title = { Text("Restart 1-Month Cycle from $0?") },
        text = {
          Text("This will clear all expenses recorded in the current 1-month cycle and reset spending counters back to $0.00.")
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.resetCurrentMonthCycle()
              showResetCycleDialog = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
          ) {
            Text("Reset Cycle to $0")
          }
        },
        dismissButton = {
          TextButton(onClick = { showResetCycleDialog = false }) {
            Text("Cancel")
          }
        }
      )
    }
  }
}
