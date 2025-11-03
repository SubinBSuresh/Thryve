package com.dutch.thryve.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.dutch.thryve.data.DailySummary
import com.dutch.thryve.data.MealLog
import com.dutch.thryve.domain.CalendarUiState
import com.dutch.thryve.domain.DailyViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d")
private val DATE_FORMATTER_HEADER = DateTimeFormatter.ofPattern("EEE, MMM d")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen(navController: NavHostController, viewModel: DailyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            // Only show FAB if not awaiting AI response
            if (!uiState.isAwaitingAi) {
                FloatingActionButton(
                    onClick = { viewModel.toggleInputDialog(true) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Add, "Log Meal")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Date Selector (similar to 20% area, but simplified visuals to match image)
            DateSelectorRow(
                selectedDate = uiState.selectedDate,
                onDateSelected = viewModel::updateSelectedDate
            )

            // Main Content Area (80% area, adapted to the card layout)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Calories Card
                CaloriesCard(uiState.dailySummary)
                viewModel.logMeal("10boiled eggs, 2 scoops of whey mixed with water, one scoop oats")

                // Macros Card
                MacrosCard(uiState.dailySummary)

                // Meal List (if any meals are logged) or AI indicator
                AnimatedContent(
                    targetState = uiState.isAwaitingAi,
                    transitionSpec = {
                        (fadeIn() + slideInVertically()).togetherWith(fadeOut() + slideOutVertically())
                    },
                    label = "meal_log_content_animation"
                ) { isAwaitingAi ->
                    if (isAwaitingAi) {
                        AiProcessingIndicator(modifier = Modifier.fillMaxWidth())
                    } else if (uiState.mealLogs.isNotEmpty()) {
                        MealLogList(uiState.mealLogs, modifier = Modifier.weight(1f))
                    } else {
                        // Optional: Show an empty state message if no meals and not processing
                        // EmptyLogMessage(uiState.selectedDate, modifier = Modifier.weight(1f))
                        Spacer(Modifier.weight(1f)) // Just fill space if nothing to show
                    }
                }
            }
        }

        // Log Meal Dialog
        if (uiState.showInputDialog) {
            LogMealDialog(
                uiState = uiState,
                onDismiss = { viewModel.toggleInputDialog(false) },
                onLog = viewModel::logMeal,
                onTextChange = viewModel::updateMealInputText
            )
        }

        // Error Snackbar (optional, but good for user feedback)
        uiState.error?.let { errorMessage ->
            Snackbar(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) { Text(errorMessage) }
            // Once shown, clear the error
            LaunchedEffect(errorMessage) {
//                viewModel.clearError()
            }
        }
    }
}

@Composable
fun DateSelectorRow(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val today = LocalDate.now()
    val dates = remember {
        (-7..7).map { today.plusDays(it.toLong()) } // Last 7 days, today, next 7 days
    }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        // Scroll to today's date on initial composition
        val todayIndex = dates.indexOf(today)
        if (todayIndex != -1) {
            coroutineScope.launch {
                // Scroll to center today, or adjust as needed
                listState.scrollToItem(todayIndex - 2) // Show a few days before today
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(dates) { date ->
            DateSelectionCard(
                date = date,
                isSelected = date == selectedDate,
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
fun DateSelectionCard(
    date: LocalDate,
    isSelected: Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier
            .width(60.dp)
            .height(80.dp)
            .clickable { onDateSelected(date) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}


@Composable
fun DailyLogContent(uiState: CalendarUiState) {
//    TODO("Not yet implemented")
}

@Composable
fun DateSummaryRow(
    selectionDate: LocalDate, onDateSelected: (LocalDate) -> Unit
) {


    val coroutineScope = rememberCoroutineScope()


    val today = LocalDate.now()
    val startDay = today.minusDays(7)
    val dates = (0L..14L).map { startDay.plusDays(it) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val todayIndex = dates.indexOf(today)
        if (todayIndex != -1) {
            coroutineScope.launch {
                listState.scrollToItem(todayIndex - 2)
            }
        }
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {


        items(dates) { date ->
            DateCard(
                date = date, isSelected = date == selectionDate, onDateSelected = onDateSelected
            )

        }
    }
}


@Composable
fun DateCard(date: LocalDate, isSelected: Boolean, onDateSelected: (LocalDate) -> Unit) {
    val isToday = date == LocalDate.now()
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier
            .width(80.dp)
            .height(100.dp)
            .clickable { onDateSelected(date) }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfWeek.getDisplayName(
                    TextStyle.SHORT, Locale.getDefault()
                ), color = contentColor.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )

            Text(
                text = date.month.getDisplayName(
                    TextStyle.SHORT, Locale.getDefault(),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}


@Composable
fun MacroSummaryHeader(selectionDate: LocalDate, totalCalories: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Activity log for ",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Place,
                contentDescription = "total calories",
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "**$totalCalories** kcal logged",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}


@Composable
fun CaloriesCard(dailySummary: DailySummary) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = "Calories",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(Modifier.width(9.dp))

                Text(
                    text = "Calories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalorieStat(
                    value = dailySummary.totalFoodCalories.toString(),
                    label = "Food",
                    color = MaterialTheme.colorScheme.error
                )
                Divider(
                    modifier = Modifier
                        .height(60.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
                CalorieStat(
                    value = dailySummary.totalExerciseCalories.toString(), // Mocked as 0 for now
                    label = "Exercise",
                    color = MaterialTheme.colorScheme.tertiary
                )
                Divider(
                    modifier = Modifier
                        .height(60.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
                CalorieStat(
                    value = dailySummary.remainingCalories.toString(),
                    label = "Remaining",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

    }
}


@Composable
fun CalorieStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun MacrosCard(dailySummary: DailySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Place, // Using fitness icon for macros
                    contentDescription = "Macros",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Macros",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MacroGoalStat(
                    label = "Carbs (g)",
                    current = dailySummary.currentCarbs,
                    target = dailySummary.targetCarbs,
                    color = MaterialTheme.colorScheme.secondary
                )
                Divider(
                    modifier = Modifier
                        .height(60.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
                MacroGoalStat(
                    label = "Protein (g)",
                    current = dailySummary.currentProtein,
                    target = dailySummary.targetProtein,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Divider(
                    modifier = Modifier
                        .height(60.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
                MacroGoalStat(
                    label = "Fat (g)",
                    current = dailySummary.currentFat,
                    target = dailySummary.targetFat,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


@Composable
fun MacroGoalStat(label: String, current: Int, target: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                    append(current.toString())
                }
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                ) {
                    append("/$target")
                }
            }, style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun AiProcessingIndicator(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
//        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Analyzing meal with Gemini AI...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}


@Composable
fun MealLogList(logs: List<MealLog>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Meal Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(logs) { log ->
                MealLogCard(log = log)
            }
        }
    }
}

@Composable
fun MealLogCard(log: MealLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = log.description,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MacroStat("Cal", log.calories.toString(), MaterialTheme.colorScheme.error)
                MacroStat("Prt", log.protein.toString(), MaterialTheme.colorScheme.tertiary)
                MacroStat("Carb", log.carbs.toString(), MaterialTheme.colorScheme.secondary)
                MacroStat("Fat", log.fat.toString(), MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun MacroStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LogMealDialog(
    uiState: CalendarUiState,
    onDismiss: () -> Unit,
    onLog: (String) -> Unit,
    onTextChange: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Log Meal for ${DATE_FORMATTER_HEADER.format(uiState.selectedDate)}",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = uiState.mealInputText,
                    onValueChange = onTextChange,
                    label = { Text("What did you eat?") },
                    placeholder = { Text("e.g., A large pepperoni pizza and a soda") },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = "Meal") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onLog(uiState.mealInputText) },
                    enabled = uiState.mealInputText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Analyze & Log")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    }
}








