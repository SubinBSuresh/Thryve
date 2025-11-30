package com.dutch.thryve.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.dutch.thryve.domain.model.PersonalRecord
import com.dutch.thryve.ui.viewmodel.FirebaseViewModel
import com.dutch.thryve.ui.viewmodel.PRViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


@Composable
fun RankedPRListItem(record: PersonalRecord, rank: Int, onEditClick: () -> Unit) {
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
                imageVector = Icons.Outlined.Star,
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

        if (rank == 1) { // Show edit button only for the top PR
            IconButton(onClick = onEditClick) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Edit PR",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


@Composable
fun ExercisePRsItem(
    exerciseName: String, records: List<PersonalRecord>, onEditRecord: (PersonalRecord) -> Unit
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
                    record = record, rank = index + 1, onEditClick = { onEditRecord(record) })
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressReportDialog(
    recordToEdit: PersonalRecord?,
    onDismissRequest: () -> Unit,
    onSave: (exerciseName: String, weight: Int, reps: Int) -> Unit
) {
    val isEditing = recordToEdit != null
    val title = if (isEditing) "Edit Personal Record" else "Log New Personal Record"

    var exerciseName by remember { mutableStateOf(recordToEdit?.exerciseName ?: "") }
    var weightInput by remember { mutableStateOf(recordToEdit?.weight?.toString() ?: "") }
    var repsInput by remember { mutableStateOf(recordToEdit?.reps?.toString() ?: "") }

    val exercises = listOf(
        "Barbell Squat",
        "Flat Bench Press",
        "Inclined Bench Press",
        "Dumbbell Shoulder Press",
        "Peck Deck Machine",
        "Cable Fly",
        "Deadlift",
        "Overhead Press",
        "Barbell Row",
        "Dumbell Chest Press",
        "Dumbell lateral raise",
        "Cable lateral raise",
        "Face pulls",
        "Concentration curls",
        "Preacher curls dumbell",
        "Dumbell curl",
        "Hammer curls",
        "Cable bicep curls",
        "Trciep pushdown bar",
        "Tricep pushdown rope",
        "Forearm curls",
        "Leg press",
        "Leg curl",
        "Leg extension",
        "Calf raise",
        "Lat pulldown VBar",
        "Lat Pulldown straight bar",
        "Cable machine row VBar",
        "Linear Row",
        "Cable machine row close grip",
        "Dumbell front raises",
        "Barbell upright row",
        "Shrugs",
        "Cable crossovers",
        "Overhear tricep extension bar",
        "Cable single arm kickbacks",
        "Dumbell tricep extension"
    )
    var expanded by remember { mutableStateOf(false) }

    val isValid =
        exerciseName.isNotBlank() && weightInput.toIntOrNull() != null && repsInput.toIntOrNull() != null

    AlertDialog(onDismissRequest = onDismissRequest, title = { Text(title) }, text = {
        Column {
            // Exercise Dropdown
            ExposedDropdownMenuBox(
                expanded = !isEditing && expanded, // Disable dropdown when editing
                onExpandedChange = { if (!isEditing) expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = exerciseName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Exercise Name") },
                    trailingIcon = {
                        if (!isEditing) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    enabled = !isEditing
                )
                ExposedDropdownMenu(
                    expanded = !isEditing && expanded, onDismissRequest = { expanded = false }) {
                    exercises.forEach { selectionOption ->
                        DropdownMenuItem(text = { Text(selectionOption) }, onClick = {
                            exerciseName = selectionOption
                            expanded = false
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
                    onValueChange = { weightInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    text = "Date: ${
                        LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }, confirmButton = {
        Button(
            onClick = {
                if (isValid) {
                    onSave(exerciseName.trim(), weightInput.toInt(), repsInput.toInt())
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PRScreen(
    navController: NavHostController, viewModel: PRViewModel = hiltViewModel()
) {
    val firebaseViewModel: FirebaseViewModel = hiltViewModel()
    val prList by firebaseViewModel.personalRecord.collectAsState()

    val groupedPRs = remember(prList) {
        prList.groupBy { it.exerciseName }.mapValues { (_, records) ->
            records.sortedByDescending { it.weight }.take(3)
        }.entries.sortedBy { it.key } // Sort exercises alphabetically
    }

    var showDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<PersonalRecord?>(null) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Personal Records") })
    }, floatingActionButton = {
        FloatingActionButton(
            onClick = { // For adding a new PR
                recordToEdit = null
                showDialog = true
            }, containerColor = MaterialTheme.colorScheme.primary, shape = CircleShape
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add New PR")
        }
    }, content = { paddingValues ->
        if (prList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No PRs logged yet. Tap '+' to add one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(groupedPRs) { (exerciseName, records) ->
                    ExercisePRsItem(exerciseName = exerciseName, records = records, onEditRecord = {
                        recordToEdit = it
                        showDialog = true
                    })
                }
            }
        }

        if (showDialog) {
            ProgressReportDialog(
                recordToEdit = recordToEdit,
                onDismissRequest = { showDialog = false },
                onSave = { name, weight, reps ->
                    if (recordToEdit == null) {
                        // Create new record
                        firebaseViewModel.logPersonalRecord(name, weight, reps, LocalDate.now())
                    } else {
                        // Update existing record
                        val updatedRecord = recordToEdit!!.copy(weight = weight, reps = reps)
                        firebaseViewModel.updatePersonalRecord(updatedRecord)
                    }
                    showDialog = false
                })
        }
    })
}
