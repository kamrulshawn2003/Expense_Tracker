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
import com.example.data.model.UserAccountEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
  entities = [
    ExpenseEntity::class,
    CategoryBudgetEntity::class,
    MonthlyGoalEntity::class,
    UserAccountEntity::class
  ],
  version = 4,
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
        // Pre-populate default category budgets with proper 32-bit ARGB color hex
        val defaultBudgets = DefaultCategories.list.map {
          CategoryBudgetEntity(
            categoryName = it.name,
            monthlyLimit = it.defaultLimit,
            iconName = it.iconName,
            colorHex = DefaultCategories.colorToHexLong(it.color)
          )
        }
        dao.insertCategoryBudgets(defaultBudgets)

        // Pre-populate current monthly goal
        val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        dao.insertOrUpdateMonthlyGoal(
          MonthlyGoalEntity(
            yearMonth = currentYearMonth,
            savingsGoal = 500.0,
            monthlyIncome = 3000.0
          )
        )
      }
    }
  }
}

