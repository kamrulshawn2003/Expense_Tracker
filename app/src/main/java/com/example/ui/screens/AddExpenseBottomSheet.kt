package com.example.ui.screens

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.DefaultCategories
import com.example.data.model.ExpenseEntity
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.util.ImageStorageHelper
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
  var selectedPaymentMethod by remember {
    mutableStateOf(editingExpense?.paymentMethod ?: "Card")
  }
  var note by remember { mutableStateOf(editingExpense?.note ?: "") }
  var pictureUri by remember { mutableStateOf(editingExpense?.pictureUri) }
  var showFullScreenPreview by remember { mutableStateOf(false) }

  // Gallery photo picker launcher
  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    uri?.let {
      val savedPath = ImageStorageHelper.saveImageFromUri(context, it, "receipts")
      if (savedPath != null) {
        pictureUri = savedPath
      }
    }
  }

  // Camera capture launcher
  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicturePreview()
  ) { bitmap: Bitmap? ->
    bitmap?.let {
      val savedPath = ImageStorageHelper.saveBitmap(context, it, "receipts")
      if (savedPath != null) {
        pictureUri = savedPath
      }
    }
  }

  // Quick title presets
  val quickPresets = listOf(
    "Coffee", "Lunch", "Dinner", "Groceries", "Uber / Ride", "Gas",
    "Pharmacy", "Cinema", "Shopping", "Utility Bill", "Snacks"
  )

  val paymentMethods = listOf("Card", "Cash", "Digital Wallet", "Bank Transfer")

  // Available categories: combine DB budgets and default categories
  val availableCategories = remember(dbBudgets) {
    val fromDb = dbBudgets.map {
      DefaultCategories.getMeta(it.categoryName, it.iconName, it.colorHex)
    }
    if (fromDb.isNotEmpty()) {
      val names = fromDb.map { it.name }.toSet()
      val extras = DefaultCategories.list.filter { it.name !in names }
      fromDb + extras
    } else {
      DefaultCategories.list
    }
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
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (editingExpense == null) "Add Daily Expense" else "Edit Expense",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )

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

      // Amount Input with Calculator support
      OutlinedTextField(
        value = amountInput,
        onValueChange = { amountInput = it },
        label = { Text("Amount ($ / Calculator: e.g. 15+4.50)") },
        placeholder = { Text("0.00") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("expense_amount_input"),
        shape = RoundedCornerShape(16.dp),
        trailingIcon = {
          if (parsedAmount != null && (amountInput.contains("+") || amountInput.contains("-") || amountInput.contains("*") || amountInput.contains("/"))) {
            Text(
              text = String.format(Locale.US, "= $%.2f", parsedAmount),
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = IncomeGreen,
              modifier = Modifier.padding(end = 12.dp)
            )
          }
        }
      )

      // Quick Calculator Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf("+5", "+10", "+20", "+50", "+100").forEach { addVal ->
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .clickable {
                val cur = parsedAmount ?: 0.0
                val add = addVal.replace("+", "").toDoubleOrNull() ?: 0.0
                amountInput = String.format(Locale.US, "%.2f", cur + add)
              }
          ) {
            Text(
              text = addVal,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(vertical = 8.dp)
            )
          }
        }
      }

      // Expense Title
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Expense Title / Description") },
        placeholder = { Text("e.g. Lunch at Cafe") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("expense_title_input"),
        shape = RoundedCornerShape(16.dp)
      )

      // Quick Presets Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        quickPresets.forEach { preset ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (title == preset) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                title = preset
                when (preset) {
                  "Coffee", "Lunch", "Dinner", "Snacks" -> selectedCategory = "Food & Dining"
                  "Groceries" -> selectedCategory = "Groceries"
                  "Uber / Ride", "Gas" -> selectedCategory = "Transportation"
                  "Cinema" -> selectedCategory = "Entertainment"
                  "Shopping" -> selectedCategory = "Shopping"
                  "Pharmacy" -> selectedCategory = "Healthcare"
                  "Utility Bill" -> selectedCategory = "Bills & Utilities"
                }
              }
          ) {
            Text(
              text = preset,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              color = if (title == preset) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
          }
        }
      }

      // Category Selection Chips
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
          FilterChip(
            selected = isSelected,
            onClick = { selectedCategory = cat.name },
            label = {
              Text(text = cat.name, style = MaterialTheme.typography.bodySmall)
            },
            leadingIcon = {
              Icon(
                imageVector = cat.icon,
                contentDescription = cat.name,
                tint = if (isSelected) cat.color else cat.color.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
              )
            },
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = cat.color.copy(alpha = 0.2f),
              selectedLabelColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.testTag("category_chip_${cat.name}")
          )
        }
      }

      // Date Picker
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        val dateFormatted = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
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
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(14.dp)
        ) {
          Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pick Date", modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = dateFormatted, style = MaterialTheme.typography.bodySmall)
        }
      }

      // Payment Method Chips
      Text(
        text = "Payment Method",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        paymentMethods.forEach { method ->
          val isSelected = selectedPaymentMethod == method
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable { selectedPaymentMethod = method }
          ) {
            Text(
              text = method,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
          }
        }
      }

      // Receipt / Picture Attachment Section
      Text(
        text = "Receipt / Attachment",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )

      if (pictureUri != null) {
        // Preview Card
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
          AsyncImage(
            model = ImageRequest.Builder(context)
              .data(pictureUri)
              .crossfade(true)
              .build(),
            contentDescription = "Receipt Attachment",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxSize()
              .clickable { showFullScreenPreview = true }
          )

          // Click to view prompt
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.5f),
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(8.dp)
              .clickable { showFullScreenPreview = true }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.ZoomIn, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Tap to view full receipt", color = Color.White, fontSize = 11.sp)
            }
          }

          // Top action buttons: Change & Remove
          Row(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = CircleShape,
              color = Color.Black.copy(alpha = 0.6f),
              modifier = Modifier.size(32.dp)
            ) {
              IconButton(
                onClick = {
                  galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Change photo", tint = Color.White, modifier = Modifier.size(16.dp))
              }
            }

            Surface(
              shape = CircleShape,
              color = Color.Black.copy(alpha = 0.6f),
              modifier = Modifier.size(32.dp)
            ) {
              IconButton(
                onClick = { pictureUri = null },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      } else {
        // Attachment Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = { cameraLauncher.launch(null) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("expense_camera_button"),
            contentPadding = PaddingValues(vertical = 12.dp)
          ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Take Photo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          }

          OutlinedButton(
            onClick = {
              galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("expense_gallery_button"),
            contentPadding = PaddingValues(vertical = 12.dp)
          ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Receipt", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          }
        }
      }

      // Optional Note
      OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("Note (Optional)") },
        placeholder = { Text("Additional details or invoice ref") },
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
                paymentMethod = selectedPaymentMethod,
                pictureUri = pictureUri
              )
            } else {
              viewModel.updateExpense(
                editingExpense.copy(
                  title = title.trim(),
                  amount = finalAmount,
                  category = selectedCategory,
                  dateMillis = selectedDateMillis,
                  note = note.trim(),
                  paymentMethod = selectedPaymentMethod,
                  pictureUri = pictureUri
                )
              )
            }
            onDismiss()
          }
        },
        enabled = (parsedAmount ?: 0.0) > 0 && title.isNotBlank(),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
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

  // Full Screen Preview Dialog
  if (showFullScreenPreview && pictureUri != null) {
    Dialog(
      onDismissRequest = { showFullScreenPreview = false },
      properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.9f))
      ) {
        AsyncImage(
          model = ImageRequest.Builder(context)
            .data(pictureUri)
            .crossfade(true)
            .build(),
          contentDescription = "Full Receipt",
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize()
        )

        IconButton(
          onClick = { showFullScreenPreview = false },
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(16.dp)
            .size(40.dp)
            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
      }
    }
  }
}
