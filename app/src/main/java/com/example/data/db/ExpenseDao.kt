package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthlyGoalEntity
import com.example.data.model.UserAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

  // User Accounts
  @Query("SELECT * FROM user_accounts ORDER BY lastSyncedAt DESC")
  fun getAllUserAccounts(): Flow<List<UserAccountEntity>>

  @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
  suspend fun getUserAccountByEmail(email: String): UserAccountEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateUserAccount(account: UserAccountEntity)

  // Expenses
  @Query("SELECT * FROM expenses WHERE userEmail = :userEmail ORDER BY dateMillis DESC, id DESC")
  fun getAllExpensesForUser(userEmail: String): Flow<List<ExpenseEntity>>

  @Query("SELECT * FROM expenses WHERE userEmail = :userEmail ORDER BY dateMillis DESC, id DESC")
  suspend fun getAllExpensesForUserSync(userEmail: String): List<ExpenseEntity>

  @Query("SELECT * FROM expenses WHERE userEmail = :userEmail AND dateMillis >= :startMillis AND dateMillis <= :endMillis ORDER BY dateMillis DESC, id DESC")
  fun getExpensesBetween(userEmail: String, startMillis: Long, endMillis: Long): Flow<List<ExpenseEntity>>

  @Query("SELECT * FROM expenses WHERE userEmail = :userEmail AND category = :category AND dateMillis >= :startMillis AND dateMillis <= :endMillis")
  suspend fun getExpensesForCategoryBetweenSync(
    userEmail: String,
    category: String,
    startMillis: Long,
    endMillis: Long
  ): List<ExpenseEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertExpense(expense: ExpenseEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertExpenses(expenses: List<ExpenseEntity>)

  @Update
  suspend fun updateExpense(expense: ExpenseEntity)

  @Delete
  suspend fun deleteExpense(expense: ExpenseEntity)

  @Query("DELETE FROM expenses WHERE id = :id")
  suspend fun deleteExpenseById(id: Long)

  @Query("DELETE FROM expenses WHERE userEmail = :userEmail AND dateMillis >= :startMillis AND dateMillis <= :endMillis")
  suspend fun deleteExpensesBetween(userEmail: String, startMillis: Long, endMillis: Long)

  // Category Budgets
  @Query("SELECT * FROM category_budgets WHERE userEmail = :userEmail")
  fun getAllCategoryBudgetsForUser(userEmail: String): Flow<List<CategoryBudgetEntity>>

  @Query("SELECT * FROM category_budgets WHERE userEmail = :userEmail")
  suspend fun getAllCategoryBudgetsSync(userEmail: String): List<CategoryBudgetEntity>

  @Query("SELECT * FROM category_budgets WHERE userEmail = :userEmail AND categoryName = :name LIMIT 1")
  suspend fun getCategoryBudgetSync(userEmail: String, name: String): CategoryBudgetEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateCategoryBudget(budget: CategoryBudgetEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertCategoryBudgets(budgets: List<CategoryBudgetEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCategoryBudgets(budgets: List<CategoryBudgetEntity>)

  @Query("DELETE FROM category_budgets WHERE userEmail = :userEmail AND categoryName = :categoryName")
  suspend fun deleteCategoryBudgetByName(userEmail: String, categoryName: String)

  @Query("UPDATE expenses SET category = :newCategory WHERE userEmail = :userEmail AND category = :oldCategory")
  suspend fun updateExpenseCategory(userEmail: String, oldCategory: String, newCategory: String)

  // Monthly Goals
  @Query("SELECT * FROM monthly_goals WHERE userEmail = :userEmail AND yearMonth = :yearMonth LIMIT 1")
  fun getMonthlyGoal(userEmail: String, yearMonth: String): Flow<MonthlyGoalEntity?>

  @Query("SELECT * FROM monthly_goals WHERE userEmail = :userEmail AND yearMonth = :yearMonth LIMIT 1")
  suspend fun getMonthlyGoalSync(userEmail: String, yearMonth: String): MonthlyGoalEntity?

  @Query("SELECT * FROM monthly_goals WHERE userEmail = :userEmail")
  suspend fun getAllMonthlyGoalsSync(userEmail: String): List<MonthlyGoalEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateMonthlyGoal(goal: MonthlyGoalEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMonthlyGoals(goals: List<MonthlyGoalEntity>)
}
