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
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

  // Expenses
  @Query("SELECT * FROM expenses ORDER BY dateMillis DESC, id DESC")
  fun getAllExpenses(): Flow<List<ExpenseEntity>>

  @Query("SELECT * FROM expenses WHERE dateMillis >= :startMillis AND dateMillis <= :endMillis ORDER BY dateMillis DESC, id DESC")
  fun getExpensesBetween(startMillis: Long, endMillis: Long): Flow<List<ExpenseEntity>>

  @Query("SELECT * FROM expenses WHERE category = :category AND dateMillis >= :startMillis AND dateMillis <= :endMillis")
  suspend fun getExpensesForCategoryBetweenSync(category: String, startMillis: Long, endMillis: Long): List<ExpenseEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertExpense(expense: ExpenseEntity): Long

  @Update
  suspend fun updateExpense(expense: ExpenseEntity)

  @Delete
  suspend fun deleteExpense(expense: ExpenseEntity)

  @Query("DELETE FROM expenses WHERE id = :id")
  suspend fun deleteExpenseById(id: Long)

  @Query("DELETE FROM expenses WHERE dateMillis >= :startMillis AND dateMillis <= :endMillis")
  suspend fun deleteExpensesBetween(startMillis: Long, endMillis: Long)

  // Category Budgets
  @Query("SELECT * FROM category_budgets")
  fun getAllCategoryBudgets(): Flow<List<CategoryBudgetEntity>>

  @Query("SELECT * FROM category_budgets WHERE categoryName = :name LIMIT 1")
  suspend fun getCategoryBudgetSync(name: String): CategoryBudgetEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateCategoryBudget(budget: CategoryBudgetEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertCategoryBudgets(budgets: List<CategoryBudgetEntity>)

  // Monthly Goals
  @Query("SELECT * FROM monthly_goals WHERE yearMonth = :yearMonth LIMIT 1")
  fun getMonthlyGoal(yearMonth: String): Flow<MonthlyGoalEntity?>

  @Query("SELECT * FROM monthly_goals WHERE yearMonth = :yearMonth LIMIT 1")
  suspend fun getMonthlyGoalSync(yearMonth: String): MonthlyGoalEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateMonthlyGoal(goal: MonthlyGoalEntity)
}
