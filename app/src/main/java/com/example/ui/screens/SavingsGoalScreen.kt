package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

  var isSavedNoticeVisible by remember { mutableStateOf(false) }

  val targetSavings = (savingsGoalInput.toDoubleOrNull() ?: monthlyGoal.savingsGoal).coerceAtLeast(0.0)
  val currentIncome = (incomeInput.toDoubleOrNull() ?: monthlyGoal.monthlyIncome).coerceAtLeast(0.0)
  val restCountedForSpendings = (currentIncome - targetSavings).coerceAtLeast(0.0)
  val totalSpentThisMonth = monthlyReport.totalSpent
  val remainingForSpendings = restCountedForSpendings - totalSpentThisMonth
  val spendingRatio = if (restCountedForSpendings > 0) (totalSpentThisMonth / restCountedForSpendings).toFloat() else 0f
  val dailySpendingAllowance = if (monthlyReport.daysInCycle > 0) (restCountedForSpendings / monthlyReport.daysInCycle) else (restCountedForSpendings / 30.0)
  val savingsPercentage = if (currentIncome > 0) ((targetSavings / currentIncome) * 100).coerceIn(0.0, 100.0) else 0.0
  val spendingsPercentage = if (currentIncome > 0) ((restCountedForSpendings / currentIncome) * 100).coerceIn(0.0, 100.0) else 0.0
  val sixMonthProjection = targetSavings * 6
  val oneYearProjection = targetSavings * 12

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    val isCompact = maxWidth < 360.dp
    val horizontalPad = if (isCompact) 12.dp else 16.dp

    LazyColumn(
      modifier = Modifier
        .widthIn(max = 720.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(
        start = horizontalPad,
        end = horizontalPad,
        top = 12.dp,
        bottom = 88.dp
      ),
      verticalArrangement = Arrangement.spacedBy(14.dp)
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
              .padding(if (isCompact) 16.dp else 20.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Text(
                    text = "Monthly Savings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.25f)
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                      )
                      Spacer(modifier = Modifier.width(3.dp))
                      Text(
                        text = "Protected",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }

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
                    modifier = Modifier.size(22.dp)
                  )
                }
              }

              Text(
                text = String.format(Locale.getDefault(), "%s%.2f / month", currency, targetSavings),
                style = if (isCompact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Spendable Balance:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = String.format(Locale.getDefault(), "%s%.2f", currency, restCountedForSpendings),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                  )
                }
              }

              Text(
                text = "Your fixed monthly savings is kept separate from your spendable income.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f)
              )
            }
          }
        }
      }

      // 2. Savings & Spendings Allocation Card
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(if (isCompact) 14.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(IncomeGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Payments,
                  contentDescription = null,
                  tint = IncomeGreen,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Income & Spendings Allocation",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "Rest of income counted for spendings after savings",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(
                    text = "Income",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = String.format(Locale.getDefault(), "%s%.0f", currency, currentIncome),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = GoalGold.copy(alpha = 0.12f),
                modifier = Modifier.weight(1f)
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(
                    text = "(-) Savings",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoalGold,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = String.format(Locale.getDefault(), "%s%.0f", currency, targetSavings),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoalGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = IncomeGreen.copy(alpha = 0.15f),
                modifier = Modifier.weight(1.1f)
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(
                    text = "(=) Spendings",
                    style = MaterialTheme.typography.labelSmall,
                    color = IncomeGreen,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = String.format(Locale.getDefault(), "%s%.0f", currency, restCountedForSpendings),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = IncomeGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = String.format(Locale.getDefault(), "Savings: %.0f%%", savingsPercentage),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = GoalGold
                )
                Text(
                  text = String.format(Locale.getDefault(), "Spendings: %.0f%%", spendingsPercentage),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = IncomeGreen
                )
              }

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(10.dp)
                  .clip(RoundedCornerShape(5.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant)
              ) {
                val savingsWeight = (targetSavings / currentIncome.coerceAtLeast(1.0)).toFloat().coerceIn(0.01f, 0.99f)
                val spendingsWeight = 1f - savingsWeight

                Box(
                  modifier = Modifier
                    .weight(savingsWeight)
                    .fillMaxSize()
                    .background(GoalGold)
                )
                Box(
                  modifier = Modifier
                    .weight(spendingsWeight)
                    .fillMaxSize()
                    .background(IncomeGreen)
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Spent This Month:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = String.format(
                      Locale.getDefault(),
                      "%s%.2f / %s%.2f",
                      currency, totalSpentThisMonth,
                      currency, restCountedForSpendings
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                  )
                }

                LinearProgressIndicator(
                  progress = { spendingRatio.coerceIn(0f, 1f) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                  color = if (remainingForSpendings < 0) ExpenseRed else if (spendingRatio >= 0.8f) GoalGold else IncomeGreen,
                  trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = if (remainingForSpendings >= 0) {
                      String.format(Locale.getDefault(), "Remaining: %s%.2f", currency, remainingForSpendings)
                    } else {
                      String.format(Locale.getDefault(), "Over by: %s%.2f", currency, -remainingForSpendings)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (remainingForSpendings >= 0) IncomeGreen else ExpenseRed
                  )
                  Text(
                    text = String.format(Locale.getDefault(), "Daily pace: %s%.2f", currency, dailySpendingAllowance),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(GoalGold.copy(alpha = 0.1f))
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = GoalGold,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Savings ($currency${String.format(Locale.getDefault(), "%.0f", targetSavings)}) is protected. Daily expenses count against the remaining $currency${String.format(Locale.getDefault(), "%.0f", restCountedForSpendings)}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
              )
            }
          }
        }
      }

      // 3. Goal & Income Settings Form
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(if (isCompact) 16.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Text(
              text = "Savings, Income & Currency Settings",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedTextField(
                value = savingsGoalInput,
                onValueChange = { savingsGoalInput = it },
                label = { Text("Monthly Savings ($currency)") },
                placeholder = { Text("500") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("savings_goal_input"),
                shape = RoundedCornerShape(14.dp)
              )

              Text(
                text = "Spendable budget after savings: $currency${String.format(Locale.getDefault(), "%.2f", restCountedForSpendings)}",
                style = MaterialTheme.typography.labelSmall,
                color = IncomeGreen,
                fontWeight = FontWeight.SemiBold
              )
            }

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
                    textAlign = TextAlign.Center,
                    color = if (savingsGoalInput == goalPreset) GoalGold else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
                  )
                }
              }
            }

            OutlinedTextField(
              value = incomeInput,
              onValueChange = { incomeInput = it },
              label = { Text("Monthly Income ($currency)") },
              placeholder = { Text("3000") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("monthly_income_input"),
              shape = RoundedCornerShape(14.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Currency Symbol",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                listOf(
                  "¥" to "¥ (Chinese Yuan / CNY)",
                  "元" to "元 (RMB)",
                  "$" to "$ (USD)",
                  "€" to "€ (EUR)",
                  "£" to "£ (GBP)",
                  "₹" to "₹ (INR)",
                  "HK$" to "HK$ (HKD)",
                  "NT$" to "NT$ (TWD)",
                  "C$" to "C$ (CAD)",
                  "A$" to "A$ (AUD)"
                ).forEach { (symbol, label) ->
                  val isSelected = currencyInput == symbol
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) GoalGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) BorderStroke(1.5.dp, GoalGold) else null,
                    modifier = Modifier
                      .clip(RoundedCornerShape(12.dp))
                      .clickable { currencyInput = symbol }
                      .testTag("currency_chip_${symbol.replace("$", "usd")}")
                  ) {
                    Text(
                      text = label,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      color = if (isSelected) GoalGold else MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                  }
                }
              }

              OutlinedTextField(
                value = currencyInput,
                onValueChange = { if (it.length <= 5) currencyInput = it },
                label = { Text("Selected Currency Symbol (e.g. ¥, 元, CNY, $)") },
                placeholder = { Text("¥") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("currency_symbol_input"),
                shape = RoundedCornerShape(14.dp)
              )
            }

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
              Text("Save Settings", fontWeight = FontWeight.Bold)
            }

            if (isSavedNoticeVisible) {
              Text(
                text = "✓ Settings saved! Savings: $currency${String.format(Locale.getDefault(), "%.0f", targetSavings)} • Spendable: $currency${String.format(Locale.getDefault(), "%.0f", restCountedForSpendings)}",
                style = MaterialTheme.typography.bodySmall,
                color = IncomeGreen,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }

      // 4. Long Term Growth Projection Card
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth()
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
                    color = IncomeGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                    color = IncomeGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
