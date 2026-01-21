package com.dutch.thryve.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dutch.thryve.domain.model.Supplement
import com.dutch.thryve.ui.viewmodel.SupplementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementsScreen(viewModel: SupplementViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var supplementToEdit by remember { mutableStateOf<Supplement?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Supplements", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Supplement")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.supplements.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No supplements added yet.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.supplements) { supplement ->
                        SupplementCard(
                            supplement = supplement,
                            onEdit = { supplementToEdit = supplement },
                            onDelete = { viewModel.deleteSupplement(supplement.id) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            SupplementDialog(
                onDismiss = { showAddDialog = false },
                onSave = { 
                    viewModel.saveSupplement(it)
                    showAddDialog = false
                }
            )
        }

        supplementToEdit?.let { supplement ->
            SupplementDialog(
                supplement = supplement,
                onDismiss = { supplementToEdit = null },
                onSave = { 
                    viewModel.saveSupplement(it)
                    supplementToEdit = null
                }
            )
        }
    }
}

@Composable
fun SupplementCard(supplement: Supplement, onEdit: () -> Unit, onDelete: () -> Unit) {
    val daysRemaining = supplement.estimatedDaysRemaining
    val isLow = daysRemaining <= supplement.daysLeftThreshold

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLow) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = supplement.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${supplement.remainingQuantity} ${supplement.unit} remaining",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Daily Dosage: ${supplement.dailyDosage} ${supplement.unit}",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLow) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "Est. $daysRemaining days left",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isLow) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = if (isLow) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = if (isLow) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SupplementDialog(supplement: Supplement? = null, onDismiss: () -> Unit, onSave: (Supplement) -> Unit) {
    var name by remember { mutableStateOf(supplement?.name ?: "") }
    var remainingQuantity by remember { mutableStateOf(supplement?.remainingQuantity?.toString() ?: "") }
    var dailyDosage by remember { mutableStateOf(supplement?.dailyDosage?.toString() ?: "") }
    var unit by remember { mutableStateOf(supplement?.unit ?: "pcs") }
    var threshold by remember { mutableStateOf(supplement?.daysLeftThreshold?.toString() ?: "7") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (supplement == null) "Add Supplement" else "Edit Supplement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = remainingQuantity, 
                    onValueChange = { remainingQuantity = it }, 
                    label = { Text("Remaining Quantity") }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dailyDosage, 
                    onValueChange = { dailyDosage = it }, 
                    label = { Text("Daily Dosage") }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit (e.g., pcs, g, scoops)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = threshold, 
                    onValueChange = { threshold = it }, 
                    label = { Text("Notify when X days left") }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedSupplement = (supplement ?: Supplement()).copy(
                        name = name,
                        remainingQuantity = remainingQuantity.toDoubleOrNull() ?: 0.0,
                        dailyDosage = dailyDosage.toDoubleOrNull() ?: 0.0,
                        unit = unit,
                        daysLeftThreshold = threshold.toIntOrNull() ?: 7,
                        lastUpdated = System.currentTimeMillis() // Reset timer on edit
                    )
                    onSave(updatedSupplement)
                },
                enabled = name.isNotBlank() && remainingQuantity.toDoubleOrNull() != null && dailyDosage.toDoubleOrNull() != null
            ) {
                Text(if (supplement == null) "Add" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
