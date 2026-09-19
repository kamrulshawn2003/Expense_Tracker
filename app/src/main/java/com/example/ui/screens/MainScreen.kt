package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ExpenseEntity
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

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = when (selectedTabIndex) {
              0 -> "Daily Expense Calculator"
              1 -> "Category Spending & Limits"
              2 -> "Monthly Budget Report"
              3 -> "Monthly Savings Goal"
              else -> "Expense Tracker"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
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
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
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
}
