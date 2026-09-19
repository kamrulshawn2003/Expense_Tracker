package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DefaultCategories
import com.example.ui.components.BudgetProgressBar
import com.example.ui.components.CategoryIconBadge
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoalGold
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.CategorySpendingSummary
import com.example.ui.viewmodel.ExpenseViewModel
import java.util.Locale

@Composable
fun CategoryBudgetScreen(
  viewModel: ExpenseViewModel,
  modifier: Modifier = Modifier
) {
  val monthlyReport by viewModel.monthlyReport.collectAsStateWithLifecycle()
  val currency = monthlyReport.currencySymbol

  var editingCategory by remember { mutableStateOf<CategorySpendingSummary?>(null) }
  var showAddCategoryDialog by remember { mutableStateOf(false) }

  val totalBudget = monthlyReport.totalBudgetLimit
  val totalSpent = monthlyReport.totalSpent
  val overallProgress = if (totalBudget > 0) (totalSpent / totalBudget).toFloat() else 0f
  val exceededCategoriesCount = monthlyReport.categorySummaries.count { it.isExceeded }

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
      // 1. Header Overview Card
      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Monthly Category Budgets",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = monthlyReport.monthDisplay,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              if (exceededCategoriesCount > 0) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = ExpenseRed.copy(alpha = 0.15f)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Warning,
                      contentDescription = null,
                      tint = ExpenseRed,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "$exceededCategoriesCount exceeded",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = ExpenseRed
                    )
                  }
                }
              }
            }

            // Overall budget bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = String.format(Locale.getDefault(), "Total Spent: %s%.2f", currency, totalSpent),
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = String.format(Locale.getDefault(), "Limit: %s%.2f", currency, totalBudget),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              BudgetProgressBar(spent = totalSpent, limit = totalBudget)
            }

            // Notification Info notice
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Notifications are automatically dispatched to your device when any category exceeds its monthly limit.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            // 1-Month Cycle Reset pill
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Autorenew,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "1-Month Cycle Limit",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold
                )
              }
              Text(
                text = "Resets to $0 on ${monthlyReport.nextCycleResetDate}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }

      // 2. Action row
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Category Limits (${monthlyReport.categorySummaries.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )

          OutlinedButton(
            onClick = { showAddCategoryDialog = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("add_custom_category_button")
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Category", style = MaterialTheme.typography.labelMedium)
          }
        }
      }

      // 3. Category Cards List
      items(monthlyReport.categorySummaries, key = { it.categoryName }) { catSummary ->
        val spent = catSummary.spent
        val limit = catSummary.limit
        val percent = if (limit > 0) (spent / limit) * 100.0 else 0.0

        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { editingCategory = catSummary }
            .testTag("category_budget_card_${catSummary.categoryName}")
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              CategoryIconBadge(meta = catSummary.meta)

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = catSummary.categoryName,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text(
                    text = String.format(Locale.getDefault(), "%s%.2f spent", currency, spent),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (catSummary.isExceeded) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (catSummary.isExceeded) FontWeight.Bold else FontWeight.Normal
                  )
                  Text(text = "/", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                  Text(
                    text = String.format(Locale.getDefault(), "%s%.2f limit", currency, limit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              // Status badge
              if (catSummary.isExceeded) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = ExpenseRed.copy(alpha = 0.15f)
                ) {
                  Text(
                    text = String.format(Locale.getDefault(), "+%s%.0f OVER", currency, spent - limit),
                    style = MaterialTheme.typography.labelSmall,
                    color = ExpenseRed,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              } else {
                Text(
                  text = String.format(Locale.getDefault(), "%.0f%%", percent),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (catSummary.isWarning) GoalGold else MaterialTheme.colorScheme.onSurface
                )
              }

              Spacer(modifier = Modifier.width(6.dp))

              IconButton(
                onClick = { editingCategory = catSummary },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = "Edit limit",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
              }
            }

            // Progress bar
            BudgetProgressBar(spent = spent, limit = limit)
          }
        }
      }
    }
  }

  // Edit Category Limit Dialog
  if (editingCategory != null) {
    val targetCat = editingCategory!!
    var limitInput by remember { mutableStateOf(String.format(Locale.US, "%.0f", targetCat.limit)) }

    AlertDialog(
      onDismissRequest = { editingCategory = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          CategoryIconBadge(meta = targetCat.meta, size = 36.dp, iconSize = 18.dp)
          Spacer(modifier = Modifier.width(10.dp))
          Text(text = "Edit Budget: ${targetCat.categoryName}")
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "Set the maximum monthly spending limit for this category. You'll receive a notification if you exceed it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          OutlinedTextField(
            value = limitInput,
            onValueChange = { limitInput = it },
            label = { Text("Monthly Limit ($currency)") },
            placeholder = { Text("e.g. 350") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("edit_category_limit_input"),
            shape = RoundedCornerShape(14.dp)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("100", "250", "400", "600").forEach { quickVal ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { limitInput = quickVal }
              ) {
                Text(
                  text = "$currency$quickVal",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                  modifier = Modifier.padding(vertical = 6.dp)
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val newLimit = limitInput.toDoubleOrNull() ?: targetCat.limit
            if (newLimit >= 0) {
              viewModel.updateCategoryLimit(targetCat.categoryName, newLimit)
              editingCategory = null
            }
          },
          modifier = Modifier.testTag("save_category_limit_button")
        ) {
          Text("Save Limit")
        }
      },
      dismissButton = {
        TextButton(onClick = { editingCategory = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Add Custom Category Dialog
  if (showAddCategoryDialog) {
    var newCatName by remember { mutableStateOf("") }
    var newCatLimit by remember { mutableStateOf("150") }

    AlertDialog(
      onDismissRequest = { showAddCategoryDialog = false },
      title = { Text(text = "Add Custom Category") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlinedTextField(
            value = newCatName,
            onValueChange = { newCatName = it },
            label = { Text("Category Name") },
            placeholder = { Text("e.g. Fitness, Pets, Books") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("new_category_name_input"),
            shape = RoundedCornerShape(14.dp)
          )
          OutlinedTextField(
            value = newCatLimit,
            onValueChange = { newCatLimit = it },
            label = { Text("Monthly Budget Limit ($currency)") },
            placeholder = { Text("e.g. 150") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("new_category_limit_input"),
            shape = RoundedCornerShape(14.dp)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newCatName.isNotBlank()) {
              val limitVal = newCatLimit.toDoubleOrNull() ?: 100.0
              viewModel.updateCategoryLimit(newCatName.trim(), limitVal)
              showAddCategoryDialog = false
            }
          },
          enabled = newCatName.isNotBlank(),
          modifier = Modifier.testTag("confirm_add_category_button")
        ) {
          Text("Create Category")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddCategoryDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
