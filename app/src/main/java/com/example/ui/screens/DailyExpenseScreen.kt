package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DefaultCategories
import com.example.data.model.ExpenseEntity
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.LimitAlertBanner
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRed
import com.example.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DailyExpenseScreen(
  viewModel: ExpenseViewModel,
  onOpenAddExpense: () -> Unit,
  onEditExpense: (ExpenseEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val selectedDateMillis by viewModel.selectedDateMillis.collectAsStateWithLifecycle()
  val todayExpenses by viewModel.selectedDayExpenses.collectAsStateWithLifecycle()
  val monthlyReport by viewModel.monthlyReport.collectAsStateWithLifecycle()
  val activeAlerts by viewModel.activeAlerts.collectAsStateWithLifecycle()

  val totalSpentToday = todayExpenses.sumOf { it.amount }
  val currency = monthlyReport.currencySymbol

  // Check if selected date is today
  val calNow = Calendar.getInstance()
  val calSelected = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
  val isToday = calNow.get(Calendar.YEAR) == calSelected.get(Calendar.YEAR) &&
    calNow.get(Calendar.DAY_OF_YEAR) == calSelected.get(Calendar.DAY_OF_YEAR)

  val dateTitle = if (isToday) "Today" else SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date(selectedDateMillis))
  val fullDateString = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(selectedDateMillis))

  // Recommended daily spending allowance (Total Budget / 30)
  val dailyAllowance = (monthlyReport.totalBudgetLimit / 30.0).coerceAtLeast(1.0)
  val isOverDailyAllowance = totalSpentToday > dailyAllowance

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      FloatingActionButton(
        onClick = onOpenAddExpense,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .padding(bottom = 16.dp)
          .testTag("add_expense_fab")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Add Expense", fontWeight = FontWeight.Bold)
        }
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Date Navigation Bar
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = {
                val c = Calendar.getInstance().apply {
                  timeInMillis = selectedDateMillis
                  add(Calendar.DAY_OF_YEAR, -1)
                }
                viewModel.setSelectedDate(c.timeInMillis)
              }
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
            }

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                  val c = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                  DatePickerDialog(
                    context,
                    { _, y, m, d ->
                      val newC = Calendar.getInstance().apply {
                        set(Calendar.YEAR, y)
                        set(Calendar.MONTH, m)
                        set(Calendar.DAY_OF_MONTH, d)
                      }
                      viewModel.setSelectedDate(newC.timeInMillis)
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
                  ).show()
                }
                .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = dateTitle,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  imageVector = Icons.Default.CalendarMonth,
                  contentDescription = "Pick Date",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
              }
              Text(
                text = fullDateString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            IconButton(
              onClick = {
                val c = Calendar.getInstance().apply {
                  timeInMillis = selectedDateMillis
                  add(Calendar.DAY_OF_YEAR, 1)
                }
                viewModel.setSelectedDate(c.timeInMillis)
              }
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
            }
          }
        }
      }

      // 2. Active Exceed Alerts (if any)
      if (activeAlerts.isNotEmpty()) {
        item {
          LimitAlertBanner(
            alerts = activeAlerts,
            onDismiss = { catName -> viewModel.dismissAlert(catName) }
          )
        }
      }

      // 3. Daily Summary Hero Banner
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
                  colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary
                  )
                )
              )
              .padding(20.dp)
          ) {
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Daily Total",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                  fontWeight = FontWeight.Medium
                )
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                ) {
                  Text(
                    text = "${todayExpenses.size} expenses",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = String.format(Locale.getDefault(), "%s%.2f", currency, totalSpentToday),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary
              )

              Spacer(modifier = Modifier.height(14.dp))

              // Daily allowance comparison pill
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Daily Target: $currency${String.format(Locale.getDefault(), "%.0f", dailyAllowance)}/day",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
                Text(
                  text = if (isOverDailyAllowance) "⚠️ Above daily pace" else "✓ Within pace",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (isOverDailyAllowance) Color(0xFFFFD1D1) else Color(0xFFA7F3D0)
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              // 1-Month Count Cycle Information Pill
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Autorenew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "1-Month Cycle: Day ${monthlyReport.daysElapsed}/${monthlyReport.daysInCycle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.95f),
                    fontWeight = FontWeight.Medium
                  )
                }
                Text(
                  text = if (monthlyReport.daysRemainingInCycle > 0) "Resets in ${monthlyReport.daysRemainingInCycle}d to $0" else "Cycle ends today",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
              }
            }
          }
        }
      }

      // 4. Section Title
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Expenses ($dateTitle)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          if (todayExpenses.isNotEmpty()) {
            Text(
              text = String.format(Locale.getDefault(), "Total: %s%.2f", currency, totalSpentToday),
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      // 5. Expense Items or Empty State
      if (todayExpenses.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(36.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(64.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.ReceiptLong,
                  contentDescription = "No Expenses",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(32.dp)
                )
              }
              Text(
                text = "No expenses recorded today",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Calculate and log your daily spending using the + button below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }
      } else {
        items(todayExpenses, key = { it.id }) { expense ->
          val meta = DefaultCategories.getMeta(expense.category)
          val timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(expense.dateMillis))

          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .clickable { onEditExpense(expense) }
              .testTag("expense_card_${expense.id}")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              CategoryIconBadge(meta = meta)

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = expense.title,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text(
                    text = expense.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = meta.color,
                    fontWeight = FontWeight.Medium
                  )
                  Text(text = "•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                  Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  if (expense.paymentMethod.isNotBlank()) {
                    Text(text = "•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                      text = expense.paymentMethod,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                if (expense.note.isNotBlank()) {
                  Text(
                    text = expense.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = String.format(Locale.getDefault(), "-%s%.2f", currency, expense.amount),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = ExpenseRed
                )
              }
            }
          }
        }
      }
    }
  }
}
