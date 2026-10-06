package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.PieChart
import com.example.data.model.DefaultCategories
import com.example.data.model.ExpenseEntity
import com.example.ui.components.CategoryExpensePieChart
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.LimitAlertBanner
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
  val dbBudgets by viewModel.categoryBudgets.collectAsStateWithLifecycle()

  val totalSpentToday = todayExpenses.sumOf { it.amount }
  val currency = monthlyReport.currencySymbol

  // Check if selected date is today
  val calNow = Calendar.getInstance()
  val calSelected = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
  val isToday = calNow.get(Calendar.YEAR) == calSelected.get(Calendar.YEAR) &&
    calNow.get(Calendar.DAY_OF_YEAR) == calSelected.get(Calendar.DAY_OF_YEAR)

  val dateTitle = if (isToday) "Today" else SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date(selectedDateMillis))
  val fullDateString = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(selectedDateMillis))

  // Recommended daily spending allowance (Spending Budget / Days in Month)
  val dailyAllowance = (monthlyReport.spendingBudget / monthlyReport.daysInCycle.coerceAtLeast(1).toDouble()).coerceAtLeast(1.0)
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
          .padding(bottom = 8.dp)
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
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter
    ) {
      val horizontalPad = if (maxWidth < 360.dp) 12.dp else 16.dp

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 720.dp),
        contentPadding = PaddingValues(start = horizontalPad, end = horizontalPad, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                  .weight(1f)
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
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = dateTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
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
              onDismiss = { catName -> viewModel.dismissAlert(catName) },
              currencySymbol = currency
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
                .padding(18.dp)
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
                      text = "${todayExpenses.size} items",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onPrimary,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                  text = String.format(Locale.getDefault(), "%s%.2f", currency, totalSpentToday),
                  style = MaterialTheme.typography.headlineLarge,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.onPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                    text = "Daily Pace: $currency${String.format(Locale.getDefault(), "%.0f", dailyAllowance)}/day ($currency${String.format(Locale.getDefault(), "%.0f", monthlyReport.spendingBudget)} budget)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = if (isOverDailyAllowance) "Above pace" else "On track",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverDailyAllowance) Color(0xFFFFD1D1) else Color(0xFFA7F3D0)
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
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (todayExpenses.isNotEmpty()) {
              Spacer(modifier = Modifier.width(8.dp))
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
                  .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = "No Expenses",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                  )
                }
                Text(
                  text = "No expenses recorded for $dateTitle",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center
                )
                Text(
                  text = "Tap the + Add Expense button below to record your spending.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
            }
          }
        } else {
          items(todayExpenses, key = { it.id }) { expense ->
            val budgetMatch = dbBudgets.find { it.categoryName.equals(expense.category, ignoreCase = true) }
            val itemMeta = DefaultCategories.getMetaForExpense(
              title = expense.title,
              category = expense.category,
              iconName = budgetMatch?.iconName,
              colorHex = budgetMatch?.colorHex
            )
            val timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(expense.dateMillis))
            val safeColor = if (itemMeta.color.alpha < 0.2f) Color(0xFF10B981) else itemMeta.color.copy(alpha = 1f)

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
                CategoryIconBadge(
                  meta = itemMeta,
                  pictureUri = expense.pictureUri ?: budgetMatch?.pictureUri
                )

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
                      color = safeColor,
                      fontWeight = FontWeight.Medium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(text = "•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                      text = timeFormatted,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1
                    )
                    if (!expense.pictureUri.isNullOrBlank()) {
                      Text(text = "•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                      Text(
                        text = "Receipt",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
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

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                  text = String.format(Locale.getDefault(), "-%s%.2f", currency, expense.amount),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = ExpenseRed,
                  maxLines = 1
                )
              }
            }
          }
        }

        // 6. Monthly Expense Pie Chart Breakdown Card (Where Your Money Is Going This Month)
        item {
          Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("daily_screen_monthly_pie_chart_card")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Box(
                    modifier = Modifier
                      .size(38.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.PieChart,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = "Where Your Money Is Going",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "${monthlyReport.monthDisplay} • Category Breakdown",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                  Text(
                    text = String.format(Locale.getDefault(), "%s%.2f", currency, monthlyReport.totalSpent),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }
              }

              CategoryExpensePieChart(
                categories = monthlyReport.categorySummaries,
                totalSpent = monthlyReport.totalSpent,
                currencySymbol = currency,
                showAllCategoriesInList = true
              )
            }
          }
        }
      }
    }
  }
}

