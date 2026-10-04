package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
  @PrimaryKey val email: String,
  val displayName: String,
  val passwordHash: String,
  val createdAt: Long = System.currentTimeMillis(),
  val lastSyncedAt: Long = System.currentTimeMillis()
)
