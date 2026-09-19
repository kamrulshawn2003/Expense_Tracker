package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Locale

object BudgetNotificationManager {
  const val CHANNEL_ID = "budget_limit_channel"
  private const val CHANNEL_NAME = "Budget & Category Limits"
  private const val CHANNEL_DESC = "Notifications when category spending exceeds set limits"

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val importance = NotificationManager.IMPORTANCE_HIGH
      val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
        description = CHANNEL_DESC
        enableLights(true)
        enableVibration(true)
      }
      val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }

  fun sendLimitExceededNotification(
    context: Context,
    categoryName: String,
    totalSpent: Double,
    limit: Double,
    currencySymbol: String = "$"
  ) {
    createNotificationChannel(context)

    // Check notification permission for Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          context,
          android.Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        // Cannot post system notification without permission, in-app banner will still notify
        return
      }
    }

    val excess = totalSpent - limit
    val percent = (totalSpent / limit) * 100.0

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      categoryName.hashCode(),
      intent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val title = "⚠️ Budget Limit Exceeded: $categoryName"
    val shortText = String.format(
      Locale.getDefault(),
      "Spent %s%.2f of %s%.2f budget (%.0f%%)!",
      currencySymbol, totalSpent, currencySymbol, limit, percent
    )
    val bigText = String.format(
      Locale.getDefault(),
      "You have exceeded your monthly limit for %s by %s%.2f! Total spent this month: %s%.2f / Limit: %s%.2f.",
      categoryName, currencySymbol, excess, currencySymbol, totalSpent, currencySymbol, limit
    )

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_app_icon)
      .setContentTitle(title)
      .setContentText(shortText)
      .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)

    try {
      with(NotificationManagerCompat.from(context)) {
        notify(categoryName.hashCode(), builder.build())
      }
    } catch (_: SecurityException) {
      // Permission caught gracefully
    }
  }

  fun sendTestNotification(context: Context) {
    sendLimitExceededNotification(
      context = context,
      categoryName = "Food & Dining",
      totalSpent = 385.50,
      limit = 350.00,
      currencySymbol = "$"
    )
  }
}
