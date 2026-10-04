package com.example.data.model

import androidx.room.Entity

@Entity(tableName = "monthly_goals", primaryKeys = ["userEmail", "yearMonth"])
data class MonthlyGoalEntity(
  val yearMonth: String, // e.g. "2026-08" or "DEFAULT"
  val savingsGoal: Double = 500.0,
  val monthlyIncome: Double = 3000.0,
  val currencySymbol: String = "$",
  val userEmail: String = "guest"
)
