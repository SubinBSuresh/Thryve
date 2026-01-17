package com.dutch.thryve.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.dutch.thryve.ui.viewmodel.ExportDuration
import com.dutch.thryve.ui.viewmodel.ExportOptions
import com.dutch.thryve.ui.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen(navController: NavHostController, viewModel: ProfileViewModel = hiltViewModel()) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Goals saved")
        }
    }

    LaunchedEffect(uiState.exportMessage) {
        uiState.exportMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearExportMessage()
        }
    }

    LaunchedEffect(uiState.dataToCopy) {
        uiState.dataToCopy?.let { data ->
            clipboardManager.setText(AnnotatedString(data))
            viewModel.onDataCopied()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentUser?.displayName ?: "User",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = uiState.userSettings.targetCalories.toString(),
                    onValueChange = viewModel::onTargetCaloriesChanged,
                    label = { Text("Target Calories") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.isEditing,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.userSettings.targetProtein.toString(),
                        onValueChange = viewModel::onTargetProteinChanged,
                        label = { Text("Protein (g)") },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.isEditing,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = uiState.userSettings.targetCarbs.toString(),
                        onValueChange = viewModel::onTargetCarbsChanged,
                        label = { Text("Carbs (g)") },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.isEditing,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = uiState.userSettings.targetFat.toString(),
                        onValueChange = viewModel::onTargetFatChanged,
                        label = { Text("Fat (g)") },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.isEditing,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (uiState.isEditing) {
                            viewModel.saveUserSettings()
                        } else {
                            viewModel.onEditToggled(true)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.isEditing) "Save goals" else "Edit goals")
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { viewModel.setShowExportDialog(true) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Export Data to Clipboard")
                }

                Spacer(modifier = Modifier.height(32.dp))

                IconButton(onClick = { viewModel.logout() }) {
                    Icon(Icons.Filled.Logout, contentDescription = "Logout")
                }
            }
        }

        if (uiState.showExportDialog) {
            ExportDataDialog(
                onDismiss = { viewModel.setShowExportDialog(false) },
                onExport = { viewModel.exportData(it) }
            )
        }
    }
}

@Composable
fun ExportDataDialog(onDismiss: () -> Unit, onExport: (ExportOptions) -> Unit) {
    var includeMeals by remember { mutableStateOf(true) }
    var includePRs by remember { mutableStateOf(true) }
    var selectedDuration by remember { mutableStateOf(ExportDuration.LAST_7_DAYS) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Your Data") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select data to include:", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeMeals, onCheckedChange = { includeMeals = it })
                    Text("Meal Logs")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includePRs, onCheckedChange = { includePRs = it })
                    Text("Personal Records")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Duration:", style = MaterialTheme.typography.titleSmall)
                ExportDuration.values().forEach { duration ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (duration == selectedDuration),
                                onClick = { selectedDuration = duration }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (duration == selectedDuration),
                            onClick = { selectedDuration = duration }
                        )
                        Text(text = duration.label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onExport(ExportOptions(includeMeals, includePRs, selectedDuration))
                },
                enabled = includeMeals || includePRs
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
