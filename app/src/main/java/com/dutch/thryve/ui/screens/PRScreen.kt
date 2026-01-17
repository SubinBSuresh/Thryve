package com.dutch.thryve.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.dutch.thryve.domain.model.Exercise
import com.dutch.thryve.domain.model.ExerciseProvider
import com.dutch.thryve.domain.model.PersonalRecord
import com.dutch.thryve.ui.viewmodel.FirebaseViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RankedPRListItem(
    record: PersonalRecord, rank: Int, onEditClick: () -> Unit, onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val (medalColor, textStyle) = when (rank) {
        1 -> Pair(Color(0xFFFFD700), MaterialTheme.typography.titleSmall)
        2 -> Pair(Color(0xFFC0C0C0), MaterialTheme.typography.bodyMedium)
        3 -> Pair(Color(0xFFCD7F32), MaterialTheme.typography.bodySmall)
        else -> Pair(Color.Transparent, MaterialTheme.typography.bodyLarge)
    }

    val formattedDate = remember(record.date) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(record.date.toDate())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank in 1..3) {
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = "Medal",
                tint = medalColor,
                modifier = Modifier.size(if (rank == 1) 20.dp else 16.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${record.weight} kg x ${record.reps} reps",
                style = textStyle,
                color = if (rank == 1) MaterialTheme.colorScheme.primary else Color.Unspecified
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (rank == 1) { // Show actions only for the top PR
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = {
                        onEditClick()
                        showMenu = false
                    })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = {
                        onDeleteClick()
                        showMenu = false
                    })
                }
            }
        }
    }
}


@Composable
fun ExercisePRsItem(
    exerciseName: String,
    records: List<PersonalRecord>,
    onEditRecord: (PersonalRecord) -> Unit,
    onDeleteRecord: (PersonalRecord) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
            records.forEachIndexed { index, record ->
                RankedPRListItem(
                    record = record,
                    rank = index + 1,
                    onEditClick = { onEditRecord(record) },
                    onDeleteClick = { onDeleteRecord(record) })
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressReportDialog(
    recordToEdit: PersonalRecord?,
    onDismissRequest: () -> Unit,
    onSave: (exercise: Exercise, weight: Double, reps: Int) -> Unit
) {
    val isEditing = recordToEdit != null
    val title = if (isEditing) "Edit Personal Record" else "Log New Personal Record"

    val initialExercise = remember(recordToEdit) {
        if (recordToEdit != null) {
            ExerciseProvider.exercises.find { it.id == recordToEdit.exerciseId }
                ?: ExerciseProvider.exercises.find { it.name == recordToEdit.exerciseName }
        } else {
            null
        }
    }

    var selectedCategory by remember { mutableStateOf(initialExercise?.category) }
    var selectedExercise by remember { mutableStateOf(initialExercise) }
    var weightInput by remember { mutableStateOf(recordToEdit?.weight?.toString() ?: "") }
    var repsInput by remember { mutableStateOf(recordToEdit?.reps?.toString() ?: "") }

    val categories = remember { ExerciseProvider.exercises.map { it.category }.distinct().sorted() }
    var filteredExercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }

    LaunchedEffect(selectedCategory) {
        filteredExercises = if (selectedCategory != null) {
            ExerciseProvider.exercises.filter { it.category == selectedCategory }
        } else {
            emptyList()
        }
    }

    var categoryExpanded by remember { mutableStateOf(false) }
    var exerciseExpanded by remember { mutableStateOf(false) }

    val isValid = selectedExercise != null && weightInput.toDoubleOrNull() != null && repsInput.toIntOrNull() != null

    AlertDialog(onDismissRequest = onDismissRequest, title = { Text(title) }, text = {
        Column {
            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = !isEditing && categoryExpanded,
                onExpandedChange = { if (!isEditing) categoryExpanded = !categoryExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { if (!isEditing) ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    enabled = !isEditing
                )
                ExposedDropdownMenu(
                    expanded = !isEditing && categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(text = { Text(category) }, onClick = {
                            selectedCategory = category
                            selectedExercise = null // Reset exercise when category changes
                            categoryExpanded = false
                        })
                    }
                }
            }

            // Exercise Dropdown
            ExposedDropdownMenuBox(
                expanded = !isEditing && exerciseExpanded && selectedCategory != null,
                onExpandedChange = { if (!isEditing && selectedCategory != null) exerciseExpanded = !exerciseExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedExercise?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Exercise") },
                    trailingIcon = { if (!isEditing && selectedCategory != null) ExposedDropdownMenuDefaults.TrailingIcon(expanded = exerciseExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    enabled = !isEditing && selectedCategory != null
                )
                ExposedDropdownMenu(
                    expanded = !isEditing && exerciseExpanded && selectedCategory != null,
                    onDismissRequest = { exerciseExpanded = false }
                ) {
                    filteredExercises.forEach { exercise ->
                        DropdownMenuItem(text = { Text(exercise.name) }, onClick = {
                            selectedExercise = exercise
                            exerciseExpanded = false
                        })
                    }
                }
            }

            // Weight and Reps Inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,1}$"""))) {
                            weightInput = input
                        }
                    },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { repsInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            if (!isEditing) {
                Text(
                    text = "Date: ${LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }, confirmButton = {
        Button(
            onClick = {
                if (isValid) {
                    onSave(selectedExercise!!, weightInput.toDouble(), repsInput.toInt())
                }
            }, enabled = isValid
        ) {
            Text("Save")
        }
    }, dismissButton = {
        TextButton(onClick = onDismissRequest) {
            Text("Cancel")
        }
    })
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Deletion") },
        text = { Text("Are you sure you want to delete this personal record?") },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        })
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PRScreen(
    navController: NavHostController, firebaseViewModel: FirebaseViewModel = hiltViewModel()
) {
    val prList by firebaseViewModel.personalRecord.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }

    val exercisesById = remember { ExerciseProvider.exercises.associateBy { it.id } }
    val exercisesByName = remember { ExerciseProvider.exercises.associateBy { it.name } }

    val groupedPRs = remember(prList, selectedCategory) {
        prList
            .mapNotNull { record ->
                val exercise = if (record.exerciseId.isNotEmpty()) {
                    exercisesById[record.exerciseId]
                } else {
                    exercisesByName[record.exerciseName]
                }
                exercise?.let { Pair(record, it) }
            }
            .filter { (_, exercise) ->
                selectedCategory == "All" || exercise.category == selectedCategory
            }
            .groupBy { (_, exercise) -> exercise }
            .map { (exercise, pairs) ->
                val topRecords = pairs.map { it.first }.sortedByDescending { it.weight }.take(3)
                exercise to topRecords
            }
            .sortedBy { (exercise, _) -> exercise.name }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<PersonalRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<PersonalRecord?>(null) }
    val categories = remember { listOf("All") + ExerciseProvider.exercises.map { it.category }.distinct().sorted() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { // For adding a new PR
                    recordToEdit = null
                    showEditDialog = true
                }, containerColor = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add New PR")
            }
        }, content = { innerPadding ->

            Column(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)) {
                // Custom Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Personal Records",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Category Filter Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) })
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(groupedPRs) { (exercise, records) ->
                        ExercisePRsItem(
                            exerciseName = exercise.name,
                            records = records,
                            onEditRecord = { record ->
                                recordToEdit = record
                                showEditDialog = true
                            },
                            onDeleteRecord = { record ->
                                recordToDelete = record
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }

            if (showEditDialog) {
                val currentRecordToEdit = recordToEdit
                ProgressReportDialog(recordToEdit = currentRecordToEdit, onDismissRequest = { showEditDialog = false }, onSave = { exercise, weight, reps ->
                    if (currentRecordToEdit != null) {
                        val updatedRecord = currentRecordToEdit.copy(weight = weight, reps = reps)
                        firebaseViewModel.updatePersonalRecord(updatedRecord)
                    } else {
                        val newRecord = PersonalRecord(
                            exerciseId = exercise.id,
                            exerciseName = exercise.name,
                            weight = weight,
                            reps = reps
                        )
                        firebaseViewModel.logPersonalRecord(newRecord)
                    }
                    showEditDialog = false
                })
            }

            if (showDeleteDialog) {
                val currentRecordToDelete = recordToDelete
                if (currentRecordToDelete != null) {
                    DeleteConfirmationDialog(onConfirm = {
                        firebaseViewModel.deletePersonalRecord(currentRecordToDelete)
                        showDeleteDialog = false
                    }, onDismiss = { showDeleteDialog = false })
                }
            }
        })
}
