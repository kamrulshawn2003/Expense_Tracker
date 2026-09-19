package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryMeta
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoalGold
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.CategorySpendingSummary
import com.example.ui.viewmodel.DaySpendingSummary
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CategoryIconBadge(
  meta: CategoryMeta,
  modifier: Modifier = Modifier,
  size: Dp = 44.dp,
  iconSize: Dp = 22.dp
) {
  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(12.dp))
      .background(meta.color.copy(alpha = 0.15f)),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = meta.icon,
      contentDescription = meta.name,
      tint = meta.color,
      modifier = Modifier.size(iconSize)
    )
  }
}

@Composable
fun LimitAlertBanner(
  alerts: List<CategorySpendingSummary>,
  onDismiss: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  if (alerts.isEmpty()) return

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    alerts.forEach { alert ->
      var visible by remember(alert.categoryName) { mutableStateOf(true) }

      AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("alert_banner_${alert.categoryName}"),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = ExpenseRed.copy(alpha = 0.12f)
          ),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
              listOf(ExpenseRed.copy(alpha = 0.5f), ExpenseRed.copy(alpha = 0.8f))
            )
          )
        ) {
          Row(
            modifier = Modifier
              .padding(horizontal = 14.dp, vertical = 10.dp)
              .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ExpenseRed.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = ExpenseRed,
                modifier = Modifier.size(20.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Limit Exceeded: ${alert.categoryName}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ExpenseRed
              )
              Text(
                text = String.format(
                  Locale.getDefault(),
                  "Spent $%.2f of $%.2f limit (+$%.2f over)",
                  alert.spent, alert.limit, alert.spent - alert.limit
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            IconButton(
              onClick = {
                visible = false
                onDismiss(alert.categoryName)
              },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun BudgetProgressBar(
  spent: Double,
  limit: Double,
  modifier: Modifier = Modifier
) {
  val ratio = if (limit > 0) (spent / limit).toFloat() else 0f
  val progress = ratio.coerceIn(0f, 1f)
  val isExceeded = limit > 0 && spent > limit
  val isWarning = limit > 0 && spent >= (limit * 0.8) && !isExceeded

  val animatedProgress by animateFloatAsState(
    targetValue = progress,
    animationSpec = tween(durationMillis = 600),
    label = "progress"
  )

  val barColor by animateColorAsState(
    targetValue = when {
      isExceeded -> ExpenseRed
      isWarning -> GoalGold
      else -> IncomeGreen
    },
    label = "barColor"
  )

  Column(modifier = modifier.fillMaxWidth()) {
    LinearProgressIndicator(
      progress = { animatedProgress },
      modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp)),
      color = barColor,
      trackColor = barColor.copy(alpha = 0.15f),
    )
  }
}

@Composable
fun SpendingDonutChart(
  categories: List<CategorySpendingSummary>,
  totalSpent: Double,
  currencySymbol: String = "$",
  modifier: Modifier = Modifier
) {
  val activeCategories = categories.filter { it.spent > 0 }

  if (activeCategories.isEmpty() || totalSpent <= 0) {
    Box(
      modifier = modifier
        .height(200.dp)
        .fillMaxWidth(),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "No spending recorded for this period",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    return
  }

  var selectedCategoryIndex by remember { mutableStateOf<Int?>(null) }

  val total = totalSpent.toFloat()
  val angles = remember(activeCategories, totalSpent) {
    var accumulated = 0f
    activeCategories.map { cat ->
      val sweep = (cat.spent.toFloat() / total) * 360f
      val start = accumulated
      accumulated += sweep
      start to sweep
    }
  }

  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Canvas Donut
    Box(
      modifier = Modifier
        .size(180.dp)
        .padding(8.dp),
      contentAlignment = Alignment.Center
    ) {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(Unit) {
            detectTapGestures { offset ->
              val center = Offset(size.width / 2f, size.height / 2f)
              val dx = offset.x - center.x
              val dy = offset.y - center.y
              var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
              if (touchAngle < 0) touchAngle += 360f

              val clickedIdx = angles.indexOfFirst { (start, sweep) ->
                touchAngle >= start && touchAngle < (start + sweep)
              }
              selectedCategoryIndex = if (clickedIdx != -1) clickedIdx else null
            }
          }
      ) {
        val strokeWidth = 32.dp.toPx()
        val diameter = min(size.width, size.height) - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)

        angles.forEachIndexed { index, (startAngle, sweepAngle) ->
          val cat = activeCategories[index]
          val isSelected = selectedCategoryIndex == index
          val currentStroke = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth

          drawArc(
            color = cat.meta.color,
            startAngle = startAngle - 90f,
            sweepAngle = (sweepAngle - 2f).coerceAtLeast(1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = currentStroke, cap = StrokeCap.Round)
          )
        }
      }

      // Center summary
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(12.dp)
      ) {
        val displayCategory = selectedCategoryIndex?.let { activeCategories.getOrNull(it) }
        if (displayCategory != null) {
          Text(
            text = displayCategory.categoryName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = displayCategory.meta.color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = String.format(Locale.getDefault(), "%s%.1f", currencySymbol, displayCategory.spent),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = String.format(Locale.getDefault(), "%.0f%%", displayCategory.percentageOfTotal),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        } else {
          Text(
            text = "Total Spent",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, totalSpent),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    // Legend items (Top 4)
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      activeCategories.take(4).forEachIndexed { index, cat ->
        val isSelected = selectedCategoryIndex == index
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (isSelected) cat.meta.color.copy(alpha = 0.15f) else Color.Transparent,
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              selectedCategoryIndex = if (selectedCategoryIndex == index) null else index
            }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(cat.meta.color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = cat.categoryName,
              style = MaterialTheme.typography.bodySmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f)
            )
            Text(
              text = String.format(Locale.getDefault(), "%.0f%%", cat.percentageOfTotal),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

@Composable
fun DailySpendingBarChart(
  dailyBreakdown: List<DaySpendingSummary>,
  currencySymbol: String = "$",
  modifier: Modifier = Modifier
) {
  val maxAmount = (dailyBreakdown.maxOfOrNull { it.amount } ?: 0.0).coerceAtLeast(50.0)

  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = "Daily Spending Trend",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(130.dp)
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height - 20.dp.toPx()
        val count = dailyBreakdown.size
        if (count == 0) return@Canvas

        val barSlotWidth = width / count
        val barWidth = (barSlotWidth * 0.65f).coerceIn(4.dp.toPx(), 14.dp.toPx())

        // Average line
        val total = dailyBreakdown.sumOf { it.amount }
        val avg = (total / count).toFloat()
        val avgY = height - ((avg / maxAmount.toFloat()) * height)
        drawLine(
          color = Color.Gray.copy(alpha = 0.35f),
          start = Offset(0f, avgY),
          end = Offset(width, avgY),
          strokeWidth = 1.dp.toPx()
        )

        dailyBreakdown.forEachIndexed { index, day ->
          val barHeight = if (maxAmount > 0) ((day.amount / maxAmount).toFloat() * height) else 0f
          val x = (index * barSlotWidth) + (barSlotWidth - barWidth) / 2
          val y = height - barHeight

          val barColor = if (day.amount > 0) {
            if (day.amount > (avg * 1.5f)) ExpenseRed else IncomeGreen
          } else {
            Color.LightGray.copy(alpha = 0.25f)
          }

          // Draw bar
          drawRoundRect(
            color = barColor,
            topLeft = Offset(x, if (day.amount > 0) y else height - 4.dp.toPx()),
            size = Size(barWidth, if (day.amount > 0) barHeight else 4.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
          )
        }
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(text = "Day 1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(text = "Day 15", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(text = "Day ${dailyBreakdown.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
