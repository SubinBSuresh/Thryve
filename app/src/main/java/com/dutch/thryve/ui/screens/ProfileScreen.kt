package com.dutch.thryve.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.dutch.thryve.ui.viewmodel.ExportDuration
import com.dutch.thryve.ui.viewmodel.ExportOptions
import com.dutch.thryve.ui.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

private val EMOJI_AVATARS = listOf(
    "🦁", "⚡", "🏋️‍♂️", "🎯", "🔥", "🥗", "🏆", "🚀", "🦍", "🥑", "🦾", "🥇"
)

@Composable
fun ProfileScreen(navController: NavHostController, viewModel: ProfileViewModel = hiltViewModel()) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    
    var selectedEmoji by remember { mutableStateOf("⚡") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var profileTapCount by remember { mutableStateOf(0) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Goals saved successfully")
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Profile Avatar with Emoji Picker
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            profileTapCount++
                            if (profileTapCount >= 8) {
                                viewModel.setShowApiKeyDialog(true)
                                profileTapCount = 0
                            } else {
                                showEmojiPicker = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedEmoji,
                        fontSize = 54.sp,
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change Avatar",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                var nameTapCount by remember { mutableStateOf(0) }
                Text(
                    text = currentUser?.displayName ?: "Athlete",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable {
                        nameTapCount++
                        if (nameTapCount >= 5) {
                            nameTapCount = 0
                            navController.navigate(Screen.Developer.route)
                        }
                    }
                )
                Text(
                    text = currentUser?.email ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Daily Nutrition Goals Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DAILY NUTRITION GOALS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = {
                                    if (uiState.isEditing) {
                                        viewModel.saveUserSettings()
                                    } else {
                                        viewModel.onEditToggled(true)
                                    }
                                }
                            ) {
                                Text(if (uiState.isEditing) "Save" else "Edit")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = uiState.userSettings.targetCalories.toString(),
                            onValueChange = viewModel::onTargetCaloriesChanged,
                            label = { Text("Target Calories (kcal)") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState.isEditing,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = uiState.userSettings.targetCarbs.toString(),
                                onValueChange = viewModel::onTargetCarbsChanged,
                                label = { Text("Carbs (g)") },
                                modifier = Modifier.weight(1f),
                                enabled = uiState.isEditing,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = uiState.userSettings.targetFat.toString(),
                                onValueChange = viewModel::onTargetFatChanged,
                                label = { Text("Fat (g)") },
                                modifier = Modifier.weight(1f),
                                enabled = uiState.isEditing,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { viewModel.setShowExportDialog(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export Data to Clipboard", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Log Out", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        if (showEmojiPicker) {
            EmojiPickerDialog(
                onDismiss = { showEmojiPicker = false },
                onSelect = {
                    selectedEmoji = it
                    showEmojiPicker = false
                }
            )
        }

        if (uiState.showExportDialog) {
            ExportDataDialog(
                onDismiss = { viewModel.setShowExportDialog(false) },
                onExport = { viewModel.exportData(it) }
            )
        }

        if (uiState.showApiKeyDialog) {
            ApiKeyDialog(
                onDismiss = { viewModel.setShowApiKeyDialog(false) },
                onSave = { viewModel.saveApiKey(it) }
            )
        }
    }
}

@Composable
fun EmojiPickerDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Profile Emoji", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(EMOJI_AVATARS) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onSelect(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 28.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ApiKeyDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var apiKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Developer: Set API Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the API key to use for AI services.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(apiKey) },
                enabled = apiKey.isNotBlank()
            ) {
                Text("Save Key")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
                ExportDuration.entries.forEach { duration ->
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
