package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.DefaultCategories
import com.example.ui.components.BudgetProgressBar
import com.example.ui.components.CategoryIconBadge
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoalGold
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.CategorySpendingSummary
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.util.ImageStorageHelper
import java.util.Locale

@Composable
fun CategoryBudgetScreen(
  viewModel: ExpenseViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val monthlyReport by viewModel.monthlyReport.collectAsStateWithLifecycle()
  val currency = monthlyReport.currencySymbol

  var editingCategory by remember { mutableStateOf<CategorySpendingSummary?>(null) }
  var deletingCategory by remember { mutableStateOf<CategorySpendingSummary?>(null) }
  var showAddBudgetDialog by remember { mutableStateOf(false) }

  val totalBudget = monthlyReport.totalBudgetLimit
  val totalSpent = monthlyReport.totalSpent
  val exceededCategoriesCount = monthlyReport.categorySummaries.count { it.isExceeded }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { showAddBudgetDialog = true },
        icon = { Icon(Icons.Default.Add, contentDescription = "Add Budget") },
        text = { Text("Add Budget", fontWeight = FontWeight.Bold) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("add_new_budget_fab")
      )
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(paddingValues),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
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
                  text = "Monthly Budgets",
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

            // Cycle reset information pill
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Autorenew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "1-Month Spending Cycle",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = if (monthlyReport.isCurrentCycle) {
                    "Day ${monthlyReport.daysElapsed} of ${monthlyReport.daysInCycle} • Resets to ${currency}0 on ${monthlyReport.nextCycleResetDate}"
                  } else {
                    "Archived cycle for ${monthlyReport.monthDisplay}"
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }

      // 2. Section Title and Add Budget button
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Active Budgets (${monthlyReport.categorySummaries.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )

          Button(
            onClick = { showAddBudgetDialog = true },
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.testTag("top_add_budget_button")
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("New Budget", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          }
        }
      }

      // 3. Category Cards List
      if (monthlyReport.categorySummaries.isEmpty()) {
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
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Text(
                text = "No Budgets Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Create custom category budgets with monthly limits and pictures to monitor spending.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )
              Button(
                onClick = { showAddBudgetDialog = true },
                modifier = Modifier.testTag("empty_state_add_budget_button")
              ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create First Budget")
              }
            }
          }
        }
      } else {
        items(monthlyReport.categorySummaries, key = { it.categoryName }) { catSummary ->
          BudgetCard(
            summary = catSummary,
            currency = currency,
            onEdit = { editingCategory = catSummary },
            onDelete = { deletingCategory = catSummary }
          )
        }
      }
    }
  }

  // Edit Budget Dialog
  if (editingCategory != null) {
    val targetCat = editingCategory!!
    EditBudgetDialog(
      summary = targetCat,
      currency = currency,
      onDismiss = { editingCategory = null },
      onSave = { newLimit, newIcon, newColor, newPicUri ->
        viewModel.updateCategoryLimit(
          categoryName = targetCat.categoryName,
          newLimit = newLimit,
          iconName = newIcon,
          colorHex = newColor,
          pictureUri = newPicUri
        )
        editingCategory = null
      }
    )
  }

  // Create New Budget Dialog
  if (showAddBudgetDialog) {
    CreateBudgetDialog(
      currency = currency,
      onDismiss = { showAddBudgetDialog = false },
      onSave = { name, limit, iconName, colorHex, pictureUri ->
        viewModel.saveCategoryBudget(
          categoryName = name,
          limit = limit,
          iconName = iconName,
          colorHex = colorHex,
          pictureUri = pictureUri
        )
        showAddBudgetDialog = false
      }
    )
  }

  // Delete Budget Confirmation Dialog
  if (deletingCategory != null) {
    val cat = deletingCategory!!
    AlertDialog(
      onDismissRequest = { deletingCategory = null },
      icon = {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = null,
          tint = ExpenseRed,
          modifier = Modifier.size(32.dp)
        )
      },
      title = {
        Text(
          text = "Delete \"${cat.categoryName}\"?",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Text(
          text = "Are you sure you want to delete this budget? All existing expenses under this category will be safely preserved and moved to \"Other\".",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteCategoryBudget(cat.categoryName)
            deletingCategory = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
          modifier = Modifier.testTag("confirm_delete_budget_button")
        ) {
          Text("Delete Budget")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { deletingCategory = null },
          modifier = Modifier.testTag("cancel_delete_budget_button")
        ) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun BudgetCard(
  summary: CategorySpendingSummary,
  currency: String,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val spent = summary.spent
  val limit = summary.limit
  val percent = if (limit > 0) (spent / limit) * 100.0 else 0.0

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .clickable { onEdit() }
      .testTag("category_budget_card_${summary.categoryName}")
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Optional Header Banner Picture
      if (!summary.pictureUri.isNullOrBlank()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
        ) {
          AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
              .data(summary.pictureUri)
              .crossfade(true)
              .build(),
            contentDescription = summary.categoryName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )

          // Gradient overlay for visual polish
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                )
              )
          )

          // Top action buttons (Edit & Delete) over the picture
          Row(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = CircleShape,
              color = Color.Black.copy(alpha = 0.55f),
              modifier = Modifier.size(34.dp)
            ) {
              IconButton(
                onClick = onEdit,
                modifier = Modifier.size(34.dp).testTag("edit_budget_${summary.categoryName}")
              ) {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = "Edit budget",
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
              }
            }

            Surface(
              shape = CircleShape,
              color = Color.Black.copy(alpha = 0.55f),
              modifier = Modifier.size(34.dp)
            ) {
              IconButton(
                onClick = onDelete,
                modifier = Modifier.size(34.dp).testTag("delete_budget_${summary.categoryName}")
              ) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Delete budget",
                  tint = Color(0xFFFF8A80),
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }

          // Bottom label inside banner
          Row(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CategoryIconBadge(meta = summary.meta, size = 32.dp, iconSize = 18.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = summary.categoryName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }
      }

      // Card Body
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // If there was no banner, show top row with icon, title, edit and delete
        if (summary.pictureUri.isNullOrBlank()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CategoryIconBadge(meta = summary.meta)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = summary.categoryName,
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
                  color = if (summary.isExceeded) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = if (summary.isExceeded) FontWeight.Bold else FontWeight.Normal
                )
                Text(text = "/", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Text(
                  text = String.format(Locale.getDefault(), "%s%.2f limit", currency, limit),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Edit & Delete Buttons
            IconButton(
              onClick = onEdit,
              modifier = Modifier.size(34.dp).testTag("edit_budget_${summary.categoryName}")
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit limit",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }

            IconButton(
              onClick = onDelete,
              modifier = Modifier.size(34.dp).testTag("delete_budget_${summary.categoryName}")
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete budget",
                tint = ExpenseRed.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
              )
            }
          }
        } else {
          // Spent details when banner is present
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = String.format(Locale.getDefault(), "%s%.2f spent", currency, spent),
                style = MaterialTheme.typography.bodyMedium,
                color = if (summary.isExceeded) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (summary.isExceeded) FontWeight.Bold else FontWeight.Normal
              )
              Text(text = "/", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
              Text(
                text = String.format(Locale.getDefault(), "%s%.2f limit", currency, limit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            if (summary.isExceeded) {
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
                color = if (summary.isWarning) GoalGold else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }

        // Progress bar
        BudgetProgressBar(spent = spent, limit = limit)
      }
    }
  }
}

@Composable
fun CreateBudgetDialog(
  currency: String,
  onDismiss: () -> Unit,
  onSave: (name: String, limit: Double, iconName: String, colorHex: Long, pictureUri: String?) -> Unit
) {
  val context = LocalContext.current
  var name by remember { mutableStateOf("") }
  var limitInput by remember { mutableStateOf("250") }
  var selectedIconName by remember { mutableStateOf("grocery") }
  var selectedColor by remember { mutableStateOf(DefaultCategories.selectableColors.first()) }
  var pictureUri by remember { mutableStateOf<String?>(null) }
  var showPresetPicker by remember { mutableStateOf(false) }

  // Gallery picker
  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    uri?.let {
      val savedPath = ImageStorageHelper.saveImageFromUri(context, it, "budget_images")
      if (savedPath != null) {
        pictureUri = savedPath
      }
    }
  }

  // Camera capture
  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicturePreview()
  ) { bitmap: Bitmap? ->
    bitmap?.let {
      val savedPath = ImageStorageHelper.saveBitmap(context, it, "budget_images")
      if (savedPath != null) {
        pictureUri = savedPath
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Create New Budget", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Budget Name
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Budget Name") },
          placeholder = { Text("e.g. Dining Out, Pets, Vacation") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("create_budget_name_input"),
          shape = RoundedCornerShape(14.dp)
        )

        // Monthly Target Limit
        OutlinedTextField(
          value = limitInput,
          onValueChange = { limitInput = it },
          label = { Text("Target Limit ($currency)") },
          placeholder = { Text("e.g. 300") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("create_budget_limit_input"),
          shape = RoundedCornerShape(14.dp)
        )

        // Quick limit suggestions
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
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 6.dp)
              )
            }
          }
        }

        // Icon Selection
        Text("Choose Icon", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(DefaultCategories.selectableIcons) { (iconKey, iconVec) ->
            val isSelected = selectedIconName == iconKey
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                  if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                  width = if (isSelected) 2.dp else 0.dp,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                  shape = RoundedCornerShape(12.dp)
                )
                .clickable { selectedIconName = iconKey },
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = iconVec,
                contentDescription = iconKey,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
              )
            }
          }
        }

        // Color Selection
        Text("Accent Color", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(DefaultCategories.selectableColors) { col ->
            val isSelected = selectedColor == col
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(col)
                .clickable { selectedColor = col },
              contentAlignment = Alignment.Center
            ) {
              if (isSelected) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Selected",
                  tint = Color.White,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }

        // Budget Picture Attachment
        Text("Budget Picture (Optional)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

        if (pictureUri != null) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(120.dp)
              .clip(RoundedCornerShape(14.dp))
          ) {
            AsyncImage(
              model = ImageRequest.Builder(context)
                .data(pictureUri)
                .crossfade(true)
                .build(),
              contentDescription = "Budget preview",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )

            // Remove button
            IconButton(
              onClick = { pictureUri = null },
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(30.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
              Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(16.dp))
            }
          }
        }

        // Picture source buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Gallery", fontSize = 12.sp)
          }

          OutlinedButton(
            onClick = { cameraLauncher.launch(null) },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Camera", fontSize = 12.sp)
          }

          OutlinedButton(
            onClick = { showPresetPicker = !showPresetPicker },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Presets", fontSize = 12.sp)
          }
        }

        // Presets selector
        AnimatedVisibility(visible = showPresetPicker) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Select Preset Photo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(DefaultCategories.presetPhotos) { preset ->
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                      pictureUri = preset.url
                      showPresetPicker = false
                    }
                ) {
                  AsyncImage(
                    model = preset.url,
                    contentDescription = preset.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                      .size(60.dp)
                      .clip(RoundedCornerShape(10.dp))
                  )
                  Text(text = preset.label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val limitVal = limitInput.toDoubleOrNull() ?: 100.0
          if (name.isNotBlank() && limitVal > 0) {
            onSave(name.trim(), limitVal, selectedIconName, selectedColor.value.toLong(), pictureUri)
          }
        },
        enabled = name.isNotBlank() && (limitInput.toDoubleOrNull() ?: 0.0) > 0,
        modifier = Modifier.testTag("confirm_create_budget_button")
      ) {
        Text("Save Budget")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun EditBudgetDialog(
  summary: CategorySpendingSummary,
  currency: String,
  onDismiss: () -> Unit,
  onSave: (newLimit: Double, newIcon: String, newColor: Long, newPictureUri: String?) -> Unit
) {
  val context = LocalContext.current
  var limitInput by remember { mutableStateOf(String.format(Locale.US, "%.0f", summary.limit)) }
  var selectedIconName by remember { mutableStateOf(summary.iconName) }
  var selectedColor by remember { mutableStateOf(Color(summary.colorHex)) }
  var pictureUri by remember { mutableStateOf(summary.pictureUri) }
  var showPresetPicker by remember { mutableStateOf(false) }

  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    uri?.let {
      val savedPath = ImageStorageHelper.saveImageFromUri(context, it, "budget_images")
      if (savedPath != null) {
        pictureUri = savedPath
      }
    }
  }

  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicturePreview()
  ) { bitmap: Bitmap? ->
    bitmap?.let {
      val savedPath = ImageStorageHelper.saveBitmap(context, it, "budget_images")
      if (savedPath != null) {
        pictureUri = savedPath
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        CategoryIconBadge(meta = summary.meta, size = 36.dp, iconSize = 18.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = "Edit Budget: ${summary.categoryName}")
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
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

        // Quick limit suggestions
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
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 6.dp)
              )
            }
          }
        }

        // Icon Selection
        Text("Change Icon", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(DefaultCategories.selectableIcons) { (iconKey, iconVec) ->
            val isSelected = selectedIconName == iconKey
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                  if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                  width = if (isSelected) 2.dp else 0.dp,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                  shape = RoundedCornerShape(12.dp)
                )
                .clickable { selectedIconName = iconKey },
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = iconVec,
                contentDescription = iconKey,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
              )
            }
          }
        }

        // Color Selection
        Text("Change Color", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(DefaultCategories.selectableColors) { col ->
            val isSelected = selectedColor == col
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(col)
                .clickable { selectedColor = col },
              contentAlignment = Alignment.Center
            ) {
              if (isSelected) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Selected",
                  tint = Color.White,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }

        // Picture banner
        Text("Budget Picture", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        if (pictureUri != null) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(120.dp)
              .clip(RoundedCornerShape(14.dp))
          ) {
            AsyncImage(
              model = ImageRequest.Builder(context)
                .data(pictureUri)
                .crossfade(true)
                .build(),
              contentDescription = "Budget preview",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )

            IconButton(
              onClick = { pictureUri = null },
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(30.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
              Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(16.dp))
            }
          }
        }

        // Picture source buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Gallery", fontSize = 12.sp)
          }

          OutlinedButton(
            onClick = { cameraLauncher.launch(null) },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Camera", fontSize = 12.sp)
          }

          OutlinedButton(
            onClick = { showPresetPicker = !showPresetPicker },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Presets", fontSize = 12.sp)
          }
        }

        AnimatedVisibility(visible = showPresetPicker) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Select Preset Photo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(DefaultCategories.presetPhotos) { preset ->
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                      pictureUri = preset.url
                      showPresetPicker = false
                    }
                ) {
                  AsyncImage(
                    model = preset.url,
                    contentDescription = preset.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                      .size(60.dp)
                      .clip(RoundedCornerShape(10.dp))
                  )
                  Text(text = preset.label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val newLimit = limitInput.toDoubleOrNull() ?: summary.limit
          if (newLimit >= 0) {
            onSave(newLimit, selectedIconName, selectedColor.value.toLong(), pictureUri)
          }
        },
        modifier = Modifier.testTag("save_category_limit_button")
      ) {
        Text("Save Changes")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
