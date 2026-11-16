package com.dutch.thryve.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dutch.thryve.ui.viewmodel.DailyCalorieData
import com.dutch.thryve.ui.viewmodel.DashboardViewModel
import com.dutch.thryve.ui.viewmodel.WeeklySummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d")

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Weekly Dashboard",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Week Selector
        WeekSelector(
            weekStart = uiState.weekStart,
            weekEnd = uiState.weekEnd,
            onPreviousWeek = { viewModel.updateSelectedDate(-1) },
            onNextWeek = { viewModel.updateSelectedDate(1) },
            onResetToToday = viewModel::resetToToday
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.weeklySummary != null) {
            val summary = uiState.weeklySummary!!

            // 1. Weekly Calorie Progress
            WeeklyProgressCard(summary.totalCalories, summary.weeklyGoal)

            // 2. Daily Average Section
            DailyAverageCard(summary)

            // 3. Trend Visualization (7-Day Bar Chart)
            WeeklyTrendCard(uiState.dailyCalorieData, uiState.dailyGoal)

            // 4. Macro Distribution (Segmented Bar)
            MacroDistributionCard(summary)
            
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Text("No data available for this week.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun WeekSelector(
    weekStart: LocalDate,
    weekEnd: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onResetToToday: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onPreviousWeek) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Week", tint = MaterialTheme.colorScheme.primary)
                }
                
                Text(
                    text = "${weekStart.format(DATE_FORMATTER)} - ${weekEnd.format(DATE_FORMATTER)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = onNextWeek) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Week", tint = MaterialTheme.colorScheme.primary)
                }
            }
            TextButton(onClick = onResetToToday, modifier = Modifier.height(32.dp)) {
                Text("Current Week", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun WeeklyProgressCard(current: Int, goal: Int) {
    val remaining = goal - current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(76.dp)) {
                CircularProgressIndicator(
                    progress = { if (goal > 0) (current.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
                Text(
                    text = if (goal > 0) "${((current.toFloat() / goal.toFloat()) * 100).toInt()}%" else "0%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column {
                Text(
                    text = "WEEKLY CONSUMPTION",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$current / $goal kcal",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (remaining >= 0) "$remaining kcal remaining" else "${-remaining} kcal over budget",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (remaining >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun DailyAverageCard(summary: WeeklySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "DAILY AVERAGES",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DashboardStatItem("${summary.avgCalories}", "kcal", "Energy")
                DashboardStatItem("${summary.avgProtein}g", "Protein", "Build")
                DashboardStatItem("${summary.avgCarbs}g", "Carbs", "Fuel")
                DashboardStatItem("${summary.avgFat}g", "Fat", "Health")
            }
        }
    }
}

@Composable
fun DashboardStatItem(value: String, label: String, subLabel: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WeeklyTrendCard(dailyData: List<DailyCalorieData>, dailyGoal: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "7-DAY CALORIE TREND",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(20.dp))
            
            val maxCal = (dailyData.maxOfOrNull { it.calories } ?: 0).coerceAtLeast(dailyGoal).toFloat() * 1.2f
            val textMeasurer = rememberTextMeasurer()
            val labelStyle = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val barColor = MaterialTheme.colorScheme.primary
            val errorColor = MaterialTheme.colorScheme.error
            val goalColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val canvasWidth = size.width
                val canvasHeight = size.height - 24.dp.toPx()
                val barWidth = canvasWidth / (dailyData.size * 2f)
                val spaceBetween = canvasWidth / dailyData.size

                val goalY = if (maxCal > 0) canvasHeight - (dailyGoal / maxCal * canvasHeight) else canvasHeight
                drawLine(
                    color = goalColor,
                    start = Offset(0f, goalY),
                    end = Offset(canvasWidth, goalY),
                    strokeWidth = 1.5.dp.toPx()
                )

                dailyData.forEachIndexed { index, data ->
                    val barHeight = if (maxCal > 0) (data.calories / maxCal) * canvasHeight else 0f
                    val x = (index * spaceBetween) + (spaceBetween / 2) - (barWidth / 2)
                    
                    if (barHeight > 0) {
                        drawRoundRect(
                            color = if (data.calories > dailyGoal) errorColor.copy(alpha = 0.8f) else barColor,
                            topLeft = Offset(x, canvasHeight - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    val dayName = data.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    val textResult = textMeasurer.measure(dayName, labelStyle)
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset(x + (barWidth - textResult.size.width) / 2f, canvasHeight + 6.dp.toPx())
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun MacroDistributionCard(summary: WeeklySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PieChart, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "MACRO SPLIT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(16.dp))

            val totalGrams = (summary.totalProtein + summary.totalCarbs + summary.totalFat).toFloat()
            if (totalGrams > 0) {
                val proteinRatio = summary.totalProtein / totalGrams
                val carbsRatio = summary.totalCarbs / totalGrams
                val fatRatio = summary.totalFat / totalGrams

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Box(modifier = Modifier.weight(proteinRatio.coerceAtLeast(0.01f)).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                        Box(modifier = Modifier.weight(carbsRatio.coerceAtLeast(0.01f)).fillMaxHeight().background(MaterialTheme.colorScheme.secondary))
                        Box(modifier = Modifier.weight(fatRatio.coerceAtLeast(0.01f)).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MacroLegendItem("Protein", "${(proteinRatio * 100).toInt()}%", MaterialTheme.colorScheme.primary)
                        MacroLegendItem("Carbs", "${(carbsRatio * 100).toInt()}%", MaterialTheme.colorScheme.secondary)
                        MacroLegendItem("Fats", "${(fatRatio * 100).toInt()}%", MaterialTheme.colorScheme.tertiary)
                    }
                }
            } else {
                Text("No macro data logged for this period", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun MacroLegendItem(label: String, percent: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text("$label ($percent)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
