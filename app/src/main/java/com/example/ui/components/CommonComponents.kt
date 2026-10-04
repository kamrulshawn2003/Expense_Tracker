package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.PieChart
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
import com.example.data.model.CategoryMeta
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoalGold
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.CategorySpendingSummary
import com.example.ui.viewmodel.DaySpendingSummary
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CategoryIconBadge(
  meta: CategoryMeta,
  modifier: Modifier = Modifier,
  size: Dp = 44.dp,
  iconSize: Dp = 22.dp
) {
  val safeColor = if (meta.color.alpha < 0.2f) Color(0xFF10B981) else meta.color.copy(alpha = 1f)
  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(12.dp))
      .background(safeColor.copy(alpha = 0.16f))
      .border(1.dp, safeColor.copy(alpha = 0.32f), RoundedCornerShape(12.dp)),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = meta.icon,
      contentDescription = meta.name,
      tint = safeColor,
      modifier = Modifier.size(iconSize)
    )
  }
}

@Composable
fun LimitAlertBanner(
  alerts: List<CategorySpendingSummary>,
  onDismiss: (String) -> Unit,
  currencySymbol: String = "$",
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
                  "Spent %s%.2f of %s%.2f limit (+%s%.2f over)",
                  currencySymbol, alert.spent,
                  currencySymbol, alert.limit,
                  currencySymbol, alert.spent - alert.limit
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

/**
 * Interactive Pie Chart & Category Breakdown component that visualizes where money is going each month.
 * Supports tapping slices or legend items to inspect individual category spending and share percentages.
 */
@Composable
fun CategoryExpensePieChart(
  categories: List<CategorySpendingSummary>,
  totalSpent: Double,
  currencySymbol: String = "$",
  modifier: Modifier = Modifier,
  showAllCategoriesInList: Boolean = true
) {
  val activeCategories = remember(categories) {
    categories.filter { it.spent > 0 }.sortedByDescending { it.spent }
  }

  var isDonutStyle by remember { mutableStateOf(false) }
  var selectedCategoryIndex by remember(activeCategories) { mutableStateOf<Int?>(null) }
  val separatorColor = MaterialTheme.colorScheme.surface

  val animatedProgress by animateFloatAsState(
    targetValue = if (activeCategories.isNotEmpty() && totalSpent > 0) 1f else 0f,
    animationSpec = tween(durationMillis = 700),
    label = "pieChartSweep"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("category_expense_pie_chart"),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Top controls row: Top spending insight + Pie/Donut toggle
    if (activeCategories.isNotEmpty() && totalSpent > 0) {
      val topCat = activeCategories.first()
      val topColor = if (topCat.meta.color.alpha < 0.2f) Color(0xFF10B981) else topCat.meta.color.copy(alpha = 1f)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = topColor.copy(alpha = 0.14f),
          modifier = Modifier.weight(1f, fill = false)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(topColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = String.format(
                Locale.getDefault(),
                "Top: %s (%.0f%%)",
                topCat.categoryName,
                topCat.percentageOfTotal
              ),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = topColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Style Toggle: Pie vs Donut
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { isDonutStyle = !isDonutStyle }
            .testTag("toggle_chart_style_button")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (isDonutStyle) Icons.Default.PieChart else Icons.Default.DonutLarge,
              contentDescription = "Switch chart style",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isDonutStyle) "Pie View" else "Ring View",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }

    if (activeCategories.isEmpty() || totalSpent <= 0) {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(54.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.PieChart,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp)
            )
          }
          Text(
            text = "No expenses logged for this month yet",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
          )
          Text(
            text = "Add your daily expenses to see a colorful pie chart breakdown of where your money goes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }
      }
      return
    }

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

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val isCompactWidth = maxWidth < 360.dp
      val chartBoxSize = if (isCompactWidth) 180.dp else (maxWidth * 0.46f).coerceIn(170.dp, 210.dp)

      @Composable
      fun PieCanvasSection() {
        Box(
          modifier = Modifier
            .size(chartBoxSize)
            .padding(8.dp)
            .testTag("pie_chart_canvas_box"),
          contentAlignment = Alignment.Center
        ) {
          Canvas(
            modifier = Modifier
              .fillMaxSize()
              .pointerInput(angles) {
                detectTapGestures { offset ->
                  val center = Offset(size.width / 2f, size.height / 2f)
                  val dx = offset.x - center.x
                  val dy = offset.y - center.y
                  val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                  val maxRadius = min(size.width, size.height) / 2f
                  if (dist <= maxRadius * 1.08f) {
                    var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                    if (touchAngle < 0f) touchAngle += 360f
                    if (touchAngle >= 360f) touchAngle -= 360f

                    val clickedIdx = angles.indexOfFirst { (start, sweep) ->
                      touchAngle >= start && touchAngle <= (start + sweep)
                    }
                    selectedCategoryIndex = if (clickedIdx != -1) {
                      if (selectedCategoryIndex == clickedIdx) null else clickedIdx
                    } else {
                      null
                    }
                  }
                }
              }
          ) {
            val explodeOffsetPx = 7.dp.toPx()
            val fullDiameter = min(size.width, size.height) - (explodeOffsetPx * 2f)
            val baseTopLeft = Offset(
              (size.width - fullDiameter) / 2f,
              (size.height - fullDiameter) / 2f
            )
            val arcSize = Size(fullDiameter, fullDiameter)

            angles.forEachIndexed { index, (startAngle, sweepAngle) ->
              val cat = activeCategories[index]
              val isSelected = selectedCategoryIndex == index
              val safeColor = if (cat.meta.color.alpha < 0.2f) Color(0xFF10B981) else cat.meta.color.copy(alpha = 1f)

              val animatedSweep = sweepAngle * animatedProgress
              val midAngleRad = Math.toRadians((startAngle - 90f + sweepAngle / 2f).toDouble())
              val shiftX = if (isSelected && activeCategories.size > 1) (cos(midAngleRad) * explodeOffsetPx).toFloat() else 0f
              val shiftY = if (isSelected && activeCategories.size > 1) (sin(midAngleRad) * explodeOffsetPx).toFloat() else 0f
              val sliceTopLeft = Offset(baseTopLeft.x + shiftX, baseTopLeft.y + shiftY)

              if (!isDonutStyle) {
                // Filled Pie Slice
                drawArc(
                  color = safeColor,
                  startAngle = startAngle - 90f,
                  sweepAngle = animatedSweep.coerceAtLeast(1f),
                  useCenter = true,
                  topLeft = sliceTopLeft,
                  size = arcSize
                )
                // Crisp slice border to separate adjacent slices cleanly
                if (activeCategories.size > 1) {
                  drawArc(
                    color = separatorColor,
                    startAngle = startAngle - 90f,
                    sweepAngle = animatedSweep.coerceAtLeast(1f),
                    useCenter = true,
                    topLeft = sliceTopLeft,
                    size = arcSize,
                    style = Stroke(width = 2.dp.toPx())
                  )
                }
              } else {
                // Ring / Donut Mode
                val strokeWidth = if (isSelected) 30.dp.toPx() else 25.dp.toPx()
                val ringDiameter = fullDiameter - strokeWidth
                val ringTopLeft = Offset(
                  (size.width - ringDiameter) / 2f + shiftX,
                  (size.height - ringDiameter) / 2f + shiftY
                )
                drawArc(
                  color = safeColor,
                  startAngle = startAngle - 90f,
                  sweepAngle = (animatedSweep - 2f).coerceAtLeast(1f),
                  useCenter = false,
                  topLeft = ringTopLeft,
                  size = Size(ringDiameter, ringDiameter),
                  style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
              }
            }
          }

          // Center badge in Donut mode or subtle center pill when tapped
          if (isDonutStyle) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(12.dp)
            ) {
              val sel = selectedCategoryIndex?.let { activeCategories.getOrNull(it) }
              if (sel != null) {
                Text(
                  text = sel.categoryName,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = String.format(Locale.getDefault(), "%.1f%%", sel.percentageOfTotal),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = sel.meta.color.copy(alpha = 1f)
                )
              } else {
                Text(
                  text = "Total",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = String.format(Locale.getDefault(), "%s%.0f", currencySymbol, totalSpent),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1
                )
              }
            }
          }
        }
      }

      @Composable
      fun CompactLegendColumn(modifier: Modifier = Modifier) {
        Column(
          modifier = modifier,
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          activeCategories.take(5).forEachIndexed { index, cat ->
            val isSelected = selectedCategoryIndex == index
            val safeColor = if (cat.meta.color.alpha < 0.2f) Color(0xFF10B981) else cat.meta.color.copy(alpha = 1f)
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) safeColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
              border = if (isSelected) BorderStroke(1.dp, safeColor) else null,
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                  selectedCategoryIndex = if (selectedCategoryIndex == index) null else index
                }
                .testTag("pie_legend_item_${cat.categoryName}")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(safeColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = cat.categoryName,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = String.format(Locale.getDefault(), "%.1f%%", cat.percentageOfTotal),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = safeColor
                )
              }
            }
          }
        }
      }

      if (isCompactWidth) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          PieCanvasSection()
          CompactLegendColumn(modifier = Modifier.fillMaxWidth())
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          PieCanvasSection()
          Spacer(modifier = Modifier.width(14.dp))
          CompactLegendColumn(modifier = Modifier.weight(1f))
        }
      }
    }

    // Selected Slice Spotlight Card (or tap hint)
    val selectedCategory = selectedCategoryIndex?.let { activeCategories.getOrNull(it) }
    if (selectedCategory != null) {
      val selColor = if (selectedCategory.meta.color.alpha < 0.2f) Color(0xFF10B981) else selectedCategory.meta.color.copy(alpha = 1f)
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = selColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, selColor.copy(alpha = 0.4f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("selected_pie_slice_card")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          CategoryIconBadge(meta = selectedCategory.meta, size = 40.dp, iconSize = 20.dp)
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = selectedCategory.categoryName,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = String.format(
                Locale.getDefault(),
                "%.1f%% of monthly spending • Limit: %s%.2f",
                selectedCategory.percentageOfTotal,
                currencySymbol,
                selectedCategory.limit
              ),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, selectedCategory.spent),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = selColor
          )
        }
      }
    }

    // Ranked Category Share Bars (Visual breakdown of where money is going)
    if (showAllCategoriesInList) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Category Share Breakdown",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        activeCategories.forEachIndexed { index, cat ->
          val isSelected = selectedCategoryIndex == index
          val safeColor = if (cat.meta.color.alpha < 0.2f) Color(0xFF10B981) else cat.meta.color.copy(alpha = 1f)
          val shareFraction = (cat.percentageOfTotal / 100.0).toFloat().coerceIn(0.02f, 1f)

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) safeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = if (isSelected) BorderStroke(1.dp, safeColor.copy(alpha = 0.5f)) else null,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .clickable {
                selectedCategoryIndex = if (selectedCategoryIndex == index) null else index
              }
              .testTag("pie_breakdown_row_${cat.categoryName}")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                CategoryIconBadge(meta = cat.meta, size = 34.dp, iconSize = 18.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = cat.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = String.format(
                      Locale.getDefault(),
                      "%.1f%% of total spent",
                      cat.percentageOfTotal
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, cat.spent),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = safeColor.copy(alpha = 0.16f)
                  ) {
                    Text(
                      text = String.format(Locale.getDefault(), "%.1f%%", cat.percentageOfTotal),
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = safeColor,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }

              // Proportional share bar
              LinearProgressIndicator(
                progress = { shareFraction },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(RoundedCornerShape(3.dp)),
                color = safeColor,
                trackColor = safeColor.copy(alpha = 0.15f)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun SpendingDonutChart(
  categories: List<CategorySpendingSummary>,
  totalSpent: Double,
  currencySymbol: String = "$",
  modifier: Modifier = Modifier
) {
  CategoryExpensePieChart(
    categories = categories,
    totalSpent = totalSpent,
    currencySymbol = currencySymbol,
    modifier = modifier,
    showAllCategoriesInList = true
  )
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
        val barWidth = (barSlotWidth * 0.65f).coerceIn(3.dp.toPx(), 14.dp.toPx())

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
