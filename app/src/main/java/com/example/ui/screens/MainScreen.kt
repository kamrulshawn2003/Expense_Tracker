package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ExpenseEntity
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.ExpenseViewModel

sealed class AppTab(
  val index: Int,
  val title: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String
) {
  data object Daily : AppTab(0, "Daily", Icons.Filled.DateRange, Icons.Outlined.DateRange, "tab_daily")
  data object Categories : AppTab(1, "Budgets", Icons.Filled.Category, Icons.Outlined.Category, "tab_categories")
  data object Reports : AppTab(2, "Reports", Icons.Filled.Assessment, Icons.Outlined.Assessment, "tab_reports")
  data object Goals : AppTab(3, "Savings", Icons.Filled.Savings, Icons.Outlined.Savings, "tab_savings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  viewModel: ExpenseViewModel = viewModel()
) {
  var selectedTabIndex by remember { mutableIntStateOf(0) }
  val tabs = listOf(AppTab.Daily, AppTab.Categories, AppTab.Reports, AppTab.Goals)

  var showAddExpenseSheet by remember { mutableStateOf(false) }
  var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

  var showAuthDialog by remember { mutableStateOf(false) }
  var authDialogInitialCreateMode by remember { mutableStateOf(false) }

  val currentUserAccount by viewModel.currentUserAccount.collectAsStateWithLifecycle()

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = when (selectedTabIndex) {
              0 -> "Daily Expenses"
              1 -> "Category Budgets"
              2 -> "Monthly Report"
              3 -> "Savings & Currency"
              else -> "Expense Tracker"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        },
        actions = {
          val account = currentUserAccount
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (account != null) IncomeGreen.copy(alpha = 0.14f)
            else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
              .padding(end = 12.dp)
              .clip(RoundedCornerShape(20.dp))
              .clickable {
                authDialogInitialCreateMode = false
                showAuthDialog = true
              }
              .testTag("top_bar_account_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (account != null) {
                Box(
                  modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(IncomeGreen),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = account.displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = account.displayName.split(" ").firstOrNull() ?: account.displayName,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = IncomeGreen,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  imageVector = Icons.Default.CloudDone,
                  contentDescription = "Synced",
                  tint = IncomeGreen,
                  modifier = Modifier.size(15.dp)
                )
              } else {
                Icon(
                  imageVector = Icons.Default.AccountCircle,
                  contentDescription = "Login with Gmail",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Gmail Backup",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          titleContentColor = MaterialTheme.colorScheme.onBackground
        )
      )
    },
    bottomBar = {
      NavigationBar(
        modifier = Modifier
          .windowInsetsPadding(WindowInsets.navigationBars)
          .testTag("bottom_navigation_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
      ) {
        tabs.forEach { tab ->
          val isSelected = selectedTabIndex == tab.index
          NavigationBarItem(
            selected = isSelected,
            onClick = { selectedTabIndex = tab.index },
            icon = {
              Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.title
              )
            },
            label = {
              Text(
                text = tab.title,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag(tab.testTag)
          )
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
      val isCompact = maxWidth < 400.dp
      val horizontalPad = if (maxWidth < 360.dp) 12.dp else 16.dp

      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Account Backup / Sign-In Banner when user is not signed in
        if (currentUserAccount == null) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .widthIn(max = 720.dp)
              .fillMaxWidth()
              .padding(horizontal = horizontalPad, vertical = 6.dp)
              .testTag("guest_account_banner")
          ) {
            if (isCompact) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Google Drive Backup via Gmail",
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "Sign in with Gmail to back up & restore after reinstall",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  OutlinedButton(
                    onClick = {
                      authDialogInitialCreateMode = false
                      showAuthDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(34.dp).testTag("banner_signin_button")
                  ) {
                    Text("Gmail Login", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                  }
                  Button(
                    onClick = {
                      authDialogInitialCreateMode = true
                      showAuthDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(34.dp).testTag("banner_create_account_button")
                  ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gmail Sign Up", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                  }
                }
              }
            } else {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = IncomeGreen,
                    modifier = Modifier.size(22.dp)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = "Google Drive Backup via Gmail",
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = "Sign in with Gmail to back up & restore anytime after reinstall",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  OutlinedButton(
                    onClick = {
                      authDialogInitialCreateMode = false
                      showAuthDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("banner_signin_button")
                  ) {
                    Text("Gmail Login", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                  }
                  Button(
                    onClick = {
                      authDialogInitialCreateMode = true
                      showAuthDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("banner_create_account_button")
                  ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gmail Sign Up", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                  }
                }
              }
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          Crossfade(targetState = selectedTabIndex, label = "tab_crossfade") { tabIdx ->
            when (tabIdx) {
              0 -> DailyExpenseScreen(
                viewModel = viewModel,
                onOpenAddExpense = {
                  editingExpense = null
                  showAddExpenseSheet = true
                },
                onEditExpense = { expense ->
                  editingExpense = expense
                  showAddExpenseSheet = true
                }
              )
              1 -> CategoryBudgetScreen(viewModel = viewModel)
              2 -> MonthlyReportScreen(viewModel = viewModel)
              3 -> SavingsGoalScreen(viewModel = viewModel)
            }
          }
        }
      }
    }
  }

  // Add / Edit Expense Bottom Sheet
  if (showAddExpenseSheet) {
    AddExpenseBottomSheet(
      viewModel = viewModel,
      editingExpense = editingExpense,
      onDismiss = {
        showAddExpenseSheet = false
        editingExpense = null
      }
    )
  }

  // Sign In / Create Account Dialog
  if (showAuthDialog) {
    AccountAuthDialog(
      viewModel = viewModel,
      initialModeCreateAccount = authDialogInitialCreateMode,
      onDismiss = { showAuthDialog = false }
    )
  }
}
