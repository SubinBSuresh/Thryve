package com.dutch.thryve.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
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
    val statusColor = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val statusText = if (isLow) "Re-stock Needed" else "In Stock"

    val supplyRatio = if (supplement.dailyDosage > 0) {
        (daysRemaining.toFloat() / (supplement.daysLeftThreshold * 3f)).coerceIn(0f, 1f)
    } else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isLow) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = supplement.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${supplement.remainingQuantity} ${supplement.unit} remaining  •  Dosage: ${supplement.dailyDosage} ${supplement.unit}/day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Est. $daysRemaining days supply left",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(supplyRatio * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { supplyRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.15f)
            )
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
                        lastUpdated = System.currentTimeMillis()
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
