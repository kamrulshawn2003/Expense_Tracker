package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DefaultCategories
import com.example.data.model.ExpenseEntity
import com.example.ui.components.CategoryIconBadge
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseBottomSheet(
  viewModel: ExpenseViewModel,
  editingExpense: ExpenseEntity? = null,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current
  val dbBudgets by viewModel.categoryBudgets.collectAsStateWithLifecycle()
  val monthlyGoal by viewModel.monthlyGoal.collectAsStateWithLifecycle()
  val currency = monthlyGoal.currencySymbol

  var title by remember { mutableStateOf(editingExpense?.title ?: "") }
  var amountInput by remember {
    mutableStateOf(
      if (editingExpense != null) String.format(Locale.US, "%.2f", editingExpense.amount) else ""
    )
  }
  var selectedCategory by remember {
    mutableStateOf(editingExpense?.category ?: DefaultCategories.list.first().name)
  }
  var selectedDateMillis by remember {
    mutableLongStateOf(editingExpense?.dateMillis ?: System.currentTimeMillis())
  }
  var note by remember { mutableStateOf(editingExpense?.note ?: "") }

  // Available categories: combine DB budgets and default categories
  val availableCategories = remember(dbBudgets) {
    val fromDb = dbBudgets.map {
      DefaultCategories.getMeta(it.categoryName, it.iconName, it.colorHex)
    }
    if (fromDb.isNotEmpty()) {
      val names = fromDb.map { it.name.lowercase() }.toSet()
      val extras = DefaultCategories.list.filter { it.name.lowercase() !in names }
      fromDb + extras
    } else {
      DefaultCategories.list
    }
  }

  val activeCategoryBudget = dbBudgets.find { it.categoryName.equals(selectedCategory, ignoreCase = true) }
  val liveItemMeta = remember(title, selectedCategory, activeCategoryBudget) {
    DefaultCategories.getMetaForExpense(
      title = title,
      category = selectedCategory,
      iconName = activeCategoryBudget?.iconName,
      colorHex = activeCategoryBudget?.colorHex
    )
  }

  fun evaluateAmount(expr: String): Double? {
    val clean = expr.replace(" ", "").replace(",", ".")
    if (clean.isBlank()) return null
    return try {
      if (clean.contains("+")) {
        clean.split("+").sumOf { it.toDoubleOrNull() ?: 0.0 }
      } else if (clean.contains("-") && !clean.startsWith("-")) {
        val parts = clean.split("-")
        var res = parts[0].toDoubleOrNull() ?: 0.0
        for (i in 1 until parts.size) {
          res -= parts[i].toDoubleOrNull() ?: 0.0
        }
        res
      } else if (clean.contains("*")) {
        clean.split("*").map { it.toDoubleOrNull() ?: 1.0 }.reduce { a, b -> a * b }
      } else if (clean.contains("/")) {
        val parts = clean.split("/")
        if (parts.size == 2) {
          val denom = parts[1].toDoubleOrNull() ?: 1.0
          if (denom != 0.0) (parts[0].toDoubleOrNull() ?: 0.0) / denom else null
        } else clean.toDoubleOrNull()
      } else {
        clean.toDoubleOrNull()
      }
    } catch (_: Exception) {
      null
    }
  }

  val parsedAmount = evaluateAmount(amountInput)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    sheetMaxWidth = 600.dp,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
  ) {
    Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = Alignment.TopCenter
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 600.dp)
          .imePadding()
          .navigationBarsPadding()
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Header with Live Item Logo Badge
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIconBadge(
              meta = liveItemMeta,
              size = 42.dp,
              iconSize = 22.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = if (editingExpense == null) "Add Expense" else "Edit Expense",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Category: $selectedCategory",
                style = MaterialTheme.typography.bodySmall,
                color = liveItemMeta.color.copy(alpha = 1f),
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          if (editingExpense != null) {
            IconButton(
              onClick = {
                viewModel.deleteExpense(editingExpense)
                onDismiss()
              },
              modifier = Modifier.testTag("delete_expense_button")
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Expense",
                tint = ExpenseRed
              )
            }
          }
        }

        // Amount Input
        OutlinedTextField(
          value = amountInput,
          onValueChange = { amountInput = it },
          label = { Text("Amount ($currency)") },
          placeholder = { Text("0.00") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_amount_input"),
          shape = RoundedCornerShape(16.dp),
          trailingIcon = {
            if (parsedAmount != null && (amountInput.contains("+") || amountInput.contains("-") || amountInput.contains("*") || amountInput.contains("/"))) {
              Text(
                text = String.format(Locale.US, "= %s%.2f", currency, parsedAmount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = IncomeGreen,
                modifier = Modifier.padding(end = 12.dp)
              )
            }
          }
        )

        // Expense Title / Item Name with Live Logo Icon
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Expense Item Name") },
          placeholder = { Text("e.g. Lunch, Groceries, Bus") },
          leadingIcon = {
            Icon(
              imageVector = liveItemMeta.icon,
              contentDescription = null,
              tint = liveItemMeta.color.copy(alpha = 1f)
            )
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_title_input"),
          shape = RoundedCornerShape(16.dp)
        )

        // Category Selection Chips with working logos
        Text(
          text = "Select Category",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          availableCategories.forEach { cat ->
            val isSelected = selectedCategory.equals(cat.name, ignoreCase = true)
            val safeColor = if (cat.color.alpha < 0.2f) Color(0xFF10B981) else cat.color.copy(alpha = 1f)
            FilterChip(
              selected = isSelected,
              onClick = { selectedCategory = cat.name },
              label = {
                Text(
                  text = cat.name,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
              },
              leadingIcon = {
                Icon(
                  imageVector = cat.icon,
                  contentDescription = cat.name,
                  tint = safeColor,
                  modifier = Modifier.size(18.dp)
                )
              },
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = isSelected,
                borderColor = safeColor.copy(alpha = 0.35f),
                selectedBorderColor = safeColor,
                selectedBorderWidth = 1.5.dp
              ),
              shape = RoundedCornerShape(12.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = safeColor.copy(alpha = 0.18f),
                selectedLabelColor = MaterialTheme.colorScheme.onSurface
              ),
              modifier = Modifier.testTag("category_chip_${cat.name}")
            )
          }
        }

        // Date Picker
        val dateFormatted = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
        OutlinedButton(
          onClick = {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            DatePickerDialog(
              context,
              { _, y, m, d ->
                val newCal = Calendar.getInstance().apply {
                  set(Calendar.YEAR, y)
                  set(Calendar.MONTH, m)
                  set(Calendar.DAY_OF_MONTH, d)
                }
                selectedDateMillis = newCal.timeInMillis
              },
              cal.get(Calendar.YEAR),
              cal.get(Calendar.MONTH),
              cal.get(Calendar.DAY_OF_MONTH)
            ).show()
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
          Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pick Date", modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "Date: $dateFormatted", style = MaterialTheme.typography.bodyMedium)
        }

        // Optional Note
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Note (Optional)") },
          placeholder = { Text("Additional details") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp)
        )

        // Save Button
        Button(
          onClick = {
            val finalAmount = parsedAmount ?: 0.0
            if (finalAmount > 0 && title.isNotBlank()) {
              if (editingExpense == null) {
                viewModel.addExpense(
                  title = title,
                  amount = finalAmount,
                  category = selectedCategory,
                  dateMillis = selectedDateMillis,
                  note = note,
                  paymentMethod = "",
                  pictureUri = null
                )
              } else {
                viewModel.updateExpense(
                  editingExpense.copy(
                    title = title.trim(),
                    amount = finalAmount,
                    category = selectedCategory,
                    dateMillis = selectedDateMillis,
                    note = note.trim(),
                    paymentMethod = ""
                  )
                )
              }
              onDismiss()
            }
          },
          enabled = (parsedAmount ?: 0.0) > 0 && title.isNotBlank(),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("save_expense_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
          )
        ) {
          Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (editingExpense == null) "Add Expense" else "Save Changes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

