package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.DefaultCategories
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthlyGoalEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Database(
  entities = [ExpenseEntity::class, CategoryBudgetEntity::class, MonthlyGoalEntity::class],
  version = 1,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun expenseDao(): ExpenseDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "expense_tracker_db"
        )
          .addCallback(DatabaseCallback(scope))
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }

    private class DatabaseCallback(
      private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        INSTANCE?.let { database ->
          scope.launch(Dispatchers.IO) {
            populateInitialData(database.expenseDao())
          }
        }
      }

      suspend fun populateInitialData(dao: ExpenseDao) {
        // Pre-populate default category budgets
        val defaultBudgets = DefaultCategories.list.map {
          CategoryBudgetEntity(
            categoryName = it.name,
            monthlyLimit = it.defaultLimit,
            iconName = it.iconName,
            colorHex = it.color.value.toLong()
          )
        }
        dao.insertCategoryBudgets(defaultBudgets)

        // Pre-populate current monthly goal
        val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        dao.insertOrUpdateMonthlyGoal(
          MonthlyGoalEntity(
            yearMonth = currentYearMonth,
            savingsGoal = 600.0,
            monthlyIncome = 3200.0
          )
        )

        // Pre-populate a few realistic expenses for the current month so user sees immediate data
        val cal = Calendar.getInstance()
        val today = cal.timeInMillis

        cal.add(Calendar.HOUR_OF_DAY, -4)
        val coffeeTime = cal.timeInMillis

        cal.add(Calendar.DAY_OF_MONTH, -1)
        val yesterday = cal.timeInMillis

        cal.add(Calendar.DAY_OF_MONTH, -2)
        val twoDaysAgo = cal.timeInMillis

        val sampleExpenses = listOf(
          ExpenseEntity(title = "Morning Coffee & Croissant", amount = 8.50, category = "Food & Dining", dateMillis = coffeeTime, paymentMethod = "Card", note = "Café Latte"),
          ExpenseEntity(title = "Supermarket Weekly Grocery", amount = 94.20, category = "Groceries", dateMillis = yesterday, paymentMethod = "Card", note = "Fruits, milk, bread"),
          ExpenseEntity(title = "Metro Transit Pass", amount = 45.00, category = "Transportation", dateMillis = twoDaysAgo, paymentMethod = "Digital Wallet", note = "Monthly reload"),
          ExpenseEntity(title = "Dinner with Friends", amount = 42.80, category = "Food & Dining", dateMillis = today, paymentMethod = "Card", note = "Pizzeria"),
          ExpenseEntity(title = "Home Internet Bill", amount = 60.00, category = "Bills & Utilities", dateMillis = twoDaysAgo, paymentMethod = "Bank Transfer", note = "Fibre 500Mbps"),
          ExpenseEntity(title = "Pharmacy Essentials", amount = 19.40, category = "Healthcare", dateMillis = yesterday, paymentMethod = "Cash", note = "Vitamins")
        )

        for (expense in sampleExpenses) {
          dao.insertExpense(expense)
        }
      }
    }
  }
}
