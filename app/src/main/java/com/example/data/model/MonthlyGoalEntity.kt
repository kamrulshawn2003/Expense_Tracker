package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_goals")
data class MonthlyGoalEntity(
  @PrimaryKey val yearMonth: String, // e.g. "2026-08" or "DEFAULT"
  val savingsGoal: Double = 500.0,
  val monthlyIncome: Double = 3000.0,
  val currencySymbol: String = "$"
)
