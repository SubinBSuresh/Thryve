package com.dutch.thryve.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.dutch.thryve.domain.model.DailySummary
import com.dutch.thryve.domain.model.MealLog
import com.dutch.thryve.ui.viewmodel.CalendarUiState
import com.dutch.thryve.ui.viewmodel.DailyViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private val DATE_FORMATTER_HEADER = DateTimeFormatter.ofPattern("EEE, MMM d")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen(navController: NavHostController, viewModel: DailyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.isAwaitingAi) {
                FloatingActionButton(
                    onClick = viewModel::onAddMealClicked,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
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
            DateSelectorRow(
                selectedDate = uiState.selectedDate,
                onDateSelected = viewModel::updateSelectedDate
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DailySummaryCard(uiState.dailySummary)

                AnimatedContent(
                    targetState = uiState.isAwaitingAi,
                    transitionSpec = { (fadeIn() + slideInVertically()).togetherWith(fadeOut() + slideOutVertically()) },
                    label = "meal_log_content_animation"
                ) { isAwaitingAi ->
                    if (isAwaitingAi) {
                        AiProcessingIndicator()
                    } else if (uiState.mealLogs.isNotEmpty()) {
                        MealLogList(
                            logs = uiState.mealLogs,
                            onEdit = viewModel::onEditMealClicked,
                            onDelete = viewModel::onDeleteMealClicked,
                            onToggleFavorite = viewModel::onToggleFavorite
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No meals logged for this day", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (uiState.showInputDialog) {
            LogMealDialog(
                uiState = uiState,
                onDismiss = { viewModel.toggleInputDialog(false) },
                onLog = viewModel::logOrUpdateMeal,
                onTextChange = viewModel::updateMealInputText,
                onCaloriesChanged = viewModel::onManualCaloriesChanged,
                onProteinChanged = viewModel::onManualProteinChanged,
                onCarbsChanged = viewModel::onManualCarbsChanged,
                onFatChanged = viewModel::onManualFatChanged,
                onShowFavorites = { viewModel.onShowFavoritesDialog(true) },
                onUseAiToggled = viewModel::onUseAiToggled,
                onMealTypeSelected = viewModel::onMealTypeSelected
            )
        }

        if (uiState.showFavoritesDialog) {
            FavoriteMealsDialog(
                favorites = uiState.favoriteMeals,
                onDismiss = { viewModel.onShowFavoritesDialog(false) },
                onSelect = { 
                    viewModel.onFavoriteMealSelected(it)
                }
            )
        }

        uiState.mealToDelete?.let { meal ->
            DeleteConfirmationDialog(
                onConfirm = viewModel::onConfirmDelete,
                onDismiss = viewModel::onDismissDeleteDialog,
                mealDescription = meal.description
            )
        }

        uiState.error?.let { errorMessage ->
            LaunchedEffect(errorMessage) {
                snackbarHostState.showSnackbar(errorMessage)
                viewModel.clearError()
            }
        }
    }
}

@Composable
fun DateSelectorRow(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val dates = remember { (-365..365).map { today.plusDays(it.toLong()) } }
    val initialIndex = remember { maxOf(0, dates.indexOf(selectedDate) - 2) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    LaunchedEffect(selectedDate) {
        val index = dates.indexOf(selectedDate)
        if (index != -1) {
            val targetIndex = maxOf(0, index - 2)
            if (abs(listState.firstVisibleItemIndex - targetIndex) > 10) {
                listState.scrollToItem(targetIndex)
            } else {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(dates, key = { it.toString() }) { date ->
            DateSelectionCard(date = date, isSelected = date == selectedDate, onDateSelected = onDateSelected)
        }
    }
}

@Composable
fun DateSelectionCard(date: LocalDate, isSelected: Boolean, onDateSelected: (LocalDate) -> Unit) {
    val isToday = date == LocalDate.now()
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier
            .width(62.dp)
            .height(84.dp)
            .clickable { onDateSelected(date) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            Text(dayName.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = contentColor)
            Spacer(Modifier.height(2.dp))
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (isToday) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
fun DailySummaryCard(dailySummary: DailySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Hero Arc Gauge
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                val primaryColor = if (dailySummary.remainingCalories >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                val trackColor = primaryColor.copy(alpha = 0.12f)
                val progress = if (dailySummary.targetCalories > 0) {
                    (dailySummary.totalFoodCalories.toFloat() / dailySummary.targetCalories.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 10.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)

                    drawArc(
                        color = trackColor,
                        startAngle = 150f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    drawArc(
                        color = primaryColor,
                        startAngle = 150f,
                        sweepAngle = 240f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dailySummary.remainingCalories.toString(),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 36.sp),
                        color = if (dailySummary.remainingCalories >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "REMAINING KCAL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStat(value = "${dailySummary.totalFoodCalories}", label = "Food", color = MaterialTheme.colorScheme.onSurface)
                SummaryStat(value = "${dailySummary.currentProtein}g", label = "Protein", color = MaterialTheme.colorScheme.primary, target = dailySummary.targetProtein)
                SummaryStat(value = "${dailySummary.currentCarbs}g", label = "Carbs", color = MaterialTheme.colorScheme.primary, target = dailySummary.targetCarbs)
                SummaryStat(value = "${dailySummary.currentFat}g", label = "Fats", color = MaterialTheme.colorScheme.primary, target = dailySummary.targetFat)
            }
        }
    }
}

@Composable
fun SummaryStat(value: String, label: String, color: Color, target: Int = 0) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(68.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (target > 0) {
            val currentVal = value.filter { it.isDigit() }.toIntOrNull() ?: 0
            val progress = (currentVal.toFloat() / target.toFloat()).coerceIn(0f, 1f)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
fun MealLogList(logs: List<MealLog>, onEdit: (MealLog) -> Unit, onDelete: (MealLog) -> Unit, onToggleFavorite: (MealLog) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Logged Meals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(logs) { log -> MealLogCard(log, onEdit, onDelete, onToggleFavorite) }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun MealLogCard(log: MealLog, onEdit: (MealLog) -> Unit, onDelete: (MealLog) -> Unit, onToggleFavorite: (MealLog) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.mealType.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(log.description, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                }
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "More") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { onEdit(log); showMenu = false })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { onDelete(log); showMenu = false })
                        DropdownMenuItem(text = { Text(if (log.isFavorite) "Remove from Favorites" else "Save as Favorite") }, onClick = { onToggleFavorite(log); showMenu = false })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MacroStat("Calories", "${log.calories} kcal", MaterialTheme.colorScheme.onSurface)
                MacroStat("Protein", "${log.protein}g", MaterialTheme.colorScheme.primary)
                MacroStat("Carbs", "${log.carbs}g", MaterialTheme.colorScheme.primary)
                MacroStat("Fat", "${log.fat}g", MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun MacroStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogMealDialog(
    uiState: CalendarUiState, 
    onDismiss: () -> Unit, 
    onLog: () -> Unit, 
    onTextChange: (String) -> Unit, 
    onCaloriesChanged: (String) -> Unit, 
    onProteinChanged: (String) -> Unit, 
    onCarbsChanged: (String) -> Unit, 
    onFatChanged: (String) -> Unit, 
    onShowFavorites: () -> Unit, 
    onUseAiToggled: (Boolean) -> Unit,
    onMealTypeSelected: (String) -> Unit
) {
    val isAiEnabled = uiState.userSettings?.useGeminiForMacros == true
    val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack", "Other")
    
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${if (uiState.mealToEdit != null) "Edit" else "Log"} Meal for ${DATE_FORMATTER_HEADER.format(uiState.selectedDate)}", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onShowFavorites, 
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isAiEnabled
                ) { 
                    Text("Choose from Favorites") 
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text("Meal Type", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.Start))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    mealTypes.forEach { type ->
                        FilterChip(
                            selected = uiState.selectedMealType == type,
                            onClick = { onMealTypeSelected(type) },
                            label = { Text(type) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Use AI for calculation", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isAiEnabled, onCheckedChange = onUseAiToggled)
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = uiState.mealInputText, onValueChange = onTextChange, label = { Text("What did you eat?") }, placeholder = { Text("e.g., A large pepperoni pizza") }, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth())
                if (!isAiEnabled) {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = uiState.manualCalories, onValueChange = onCaloriesChanged, label = { Text("Cals") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = uiState.manualProtein, onValueChange = onProteinChanged, label = { Text("Protein") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = uiState.manualCarbs, onValueChange = onCarbsChanged, label = { Text("Carbs") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = uiState.manualFat, onValueChange = onFatChanged, label = { Text("Fats") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = onLog, enabled = uiState.mealInputText.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text(if (uiState.mealToEdit != null) (if (isAiEnabled) "Update & Re-analyze" else "Update") else (if (isAiEnabled) "Analyze & Log" else "Log"))
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    }
}

@Composable
fun FavoriteMealsDialog(favorites: List<MealLog>, onDismiss: () -> Unit, onSelect: (MealLog) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Choose a Favorite", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                if (favorites.isEmpty()) Text("No favorite meals yet.") else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(favorites) { meal -> Text(meal.description, modifier = Modifier.clickable { onSelect(meal) }.padding(8.dp).fillMaxWidth()) }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, mealDescription: String) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Confirm Deletion") }, text = { Text("Are you sure you want to delete \"$mealDescription\"?") }, confirmButton = { Button(onClick = onConfirm) { Text("Delete") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun AiProcessingIndicator(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("Analyzing meal with AI...", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}
