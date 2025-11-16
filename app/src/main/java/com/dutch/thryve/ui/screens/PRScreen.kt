package com.dutch.thryve.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.dutch.thryve.ui.viewmodel.FirebaseViewModel
import com.dutch.thryve.ui.viewmodel.PRViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// --- 1. Data Model ---

/**
 * Represents a single Personal Record.
 */
data class PersonalRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val exerciseName: String,
    val weight: Int,
    val reps: Int,
    val date: LocalDate
)



// --- 3. List Item Composable ---

@Composable
fun PRListItem(record: PRViewModel.PersonalRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Exercise Name (Title)
            Text(
                text = record.exerciseName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Weight & Reps
                Text(
                    text = "${record.weight} kg x ${record.reps} reps",
                    style = MaterialTheme.typography.bodyLarge
                )
                // Date
                Text(
                    text = record.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


// --- 4. Input Dialog Composable ---

@Composable
fun ProgressReportDialog(
    onDismissRequest: () -> Unit,
    onSave: (exerciseName: String, weight: Int, reps: Int, date: LocalDate) -> Unit
) {
    var exerciseName by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var repsInput by remember { mutableStateOf("") }
    val date by remember { mutableStateOf(LocalDate.now()) }

    val isValid =
        exerciseName.isNotBlank() && weightInput.toIntOrNull() != null && repsInput.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Log New Personal Record") },
        text = {
            Column {
                OutlinedTextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    label = { Text("Exercise Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
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
                Text(
                    text = "Date: ${date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onSave(exerciseName.trim(), weightInput.toInt(), repsInput.toInt(), date)
                        onDismissRequest()
                    }
                }, enabled = isValid
            ) {
                Text("Save PR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        })
}

// --- 5. Main Screen Composable ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PRScreen(
    navController: NavHostController, viewModel: PRViewModel = hiltViewModel()
) {
    val firebaseViewModel: FirebaseViewModel = hiltViewModel()

    // Collect the list of records from the ViewModel
    val prList by viewModel.prList.collectAsState()

    // State to control the visibility of the input dialog
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Personal Records") })
    }, floatingActionButton = {
        FloatingActionButton(
            onClick = { showDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape
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
                contentPadding = paddingValues,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(prList) { record ->
                    PRListItem(record = record)
                }
            }
        }

        // Show Dialog when state is true
        if (showDialog) {
            ProgressReportDialog(
                onDismissRequest = { showDialog = false },
                onSave = { name, weight, reps, date ->
                    viewModel.logPersonalRecord(name, weight, reps, date)
                    firebaseViewModel.logPersonalRecord(name, weight, reps, date)
                })
        }
    })
}


// --- 6. Preview ---

//@Preview(showBackground = true)
//@Composable
//private fun PreviewProgressReportScreen() {
//    MaterialTheme {
//        // Use the mock implementation for the preview
//        ProgressReportScreen(
//            navController = rememberNavController(),
//            viewModel = MockDailyViewModel() as DailyViewModel
//        )
//    }
//}