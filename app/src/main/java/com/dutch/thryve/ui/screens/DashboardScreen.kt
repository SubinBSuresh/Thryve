package com.dutch.thryve.ui.screens

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dutch.thryve.ui.viewmodel.DailyCalorieData
import com.dutch.thryve.ui.viewmodel.DashboardViewModel
import com.dutch.thryve.ui.viewmodel.WeeklySummary
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Weekly Dashboard",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
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
            Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
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
    weekStart: java.time.LocalDate,
    weekEnd: java.time.LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onResetToToday: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousWeek) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Week")
            }
            
            Text(
                text = "${weekStart.format(DATE_FORMATTER)} - ${weekEnd.format(DATE_FORMATTER)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            IconButton(onClick = onNextWeek) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Week")
            }
        }
        TextButton(onClick = onResetToToday) {
            Text("Reset to Current Week")
        }
    }
}

@Composable
fun WeeklyProgressCard(current: Int, goal: Int) {
    val remaining = goal - current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                CircularProgressIndicator(
                    progress = if (goal > 0) (current.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                )
                Text(
                    text = if (goal > 0) "${((current.toFloat() / goal.toFloat()) * 100).toInt()}%" else "0%",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            Column {
                Text("Weekly Total", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "$current / $goal",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text("kcal consumed", style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (remaining >= 0) "$remaining kcal left" else "${-remaining} kcal over",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (remaining >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun DailyAverageCard(summary: WeeklySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Daily Averages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DashboardStatItem("${summary.avgCalories}", "kcal", "Energy")
                DashboardStatItem("${summary.avgProtein}g", "Protein", "Build")
                DashboardStatItem("${summary.avgCarbs}g", "Carbs", "Fuel")
                DashboardStatItem("${summary.avgFat}g", "Fats", "Health")
            }
        }
    }
}

@Composable
fun DashboardStatItem(value: String, label: String, subLabel: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(subLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
    }
}

@Composable
fun WeeklyTrendCard(dailyData: List<DailyCalorieData>, dailyGoal: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("7-Day Calorie Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            
            val maxCal = (dailyData.maxOfOrNull { it.calories } ?: 0).coerceAtLeast(dailyGoal).toFloat() * 1.2f
            val density = LocalDensity.current
            val labelTextSize = with(density) { 10.sp.toPx() }
            
            val barColor = MaterialTheme.colorScheme.primary
            val errorColor = MaterialTheme.colorScheme.error
            val goalColor = errorColor.copy(alpha = 0.3f)

            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barWidth = canvasWidth / (dailyData.size * 2f)
                val spaceBetween = canvasWidth / dailyData.size

                // Draw Daily Goal Line
                val goalY = if (maxCal > 0) canvasHeight - (dailyGoal / maxCal * canvasHeight) else canvasHeight
                drawLine(
                    color = goalColor,
                    start = Offset(0f, goalY),
                    end = Offset(canvasWidth, goalY),
                    strokeWidth = 2.dp.toPx()
                )

                dailyData.forEachIndexed { index, data ->
                    val barHeight = if (maxCal > 0) (data.calories / maxCal) * canvasHeight else 0f
                    val x = (index * spaceBetween) + (spaceBetween / 2) - (barWidth / 2)
                    
                    // Draw Bar
                    drawRect(
                        color = if (data.calories > dailyGoal) errorColor.copy(alpha = 0.7f) else barColor,
                        topLeft = Offset(x, canvasHeight - barHeight),
                        size = Size(barWidth, barHeight)
                    )

                    // Draw Day Label
                    drawContext.canvas.nativeCanvas.drawText(
                        data.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        x + (barWidth / 2),
                        canvasHeight + 20.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = labelTextSize
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun MacroDistributionCard(summary: WeeklySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PieChart, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Macro Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))

            val totalGrams = (summary.totalProtein + summary.totalCarbs + summary.totalFat).toFloat()
            if (totalGrams > 0) {
                val proteinRatio = summary.totalProtein / totalGrams
                val carbsRatio = summary.totalCarbs / totalGrams
                val fatRatio = summary.totalFat / totalGrams

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Segmented Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Box(modifier = Modifier.weight(proteinRatio.coerceAtLeast(0.01f)).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
                        Box(modifier = Modifier.weight(carbsRatio.coerceAtLeast(0.01f)).fillMaxHeight().background(MaterialTheme.colorScheme.secondary))
                        Box(modifier = Modifier.weight(fatRatio.coerceAtLeast(0.01f)).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                    }

                    // Legend
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MacroLegendItem("Protein", "${(proteinRatio * 100).toInt()}%", MaterialTheme.colorScheme.tertiary)
                        MacroLegendItem("Carbs", "${(carbsRatio * 100).toInt()}%", MaterialTheme.colorScheme.secondary)
                        MacroLegendItem("Fats", "${(fatRatio * 100).toInt()}%", MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                Text("No macros logged yet", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun MacroLegendItem(label: String, percent: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text("$label ($percent)", style = MaterialTheme.typography.labelSmall)
    }
}
