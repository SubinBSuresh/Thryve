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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.dutch.thryve.ui.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen(navController: NavHostController, viewModel: ProfileViewModel = hiltViewModel()) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Goals saved")
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
                    label = { Text("TargetCalories") },
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
                        label = { Text("TargetProtein") },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.isEditing,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )



                    OutlinedTextField(
                        value = uiState.userSettings.targetCarbs.toString(),
                        onValueChange = viewModel::onTargetCarbsChanged,
                        label = { Text("TargetCarbs") },
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = "Use gemini for macro calculation?",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Switch(
                        checked = uiState.userSettings.useGeminiForMacros,
                        onCheckedChange = viewModel::onUseGeminiForMacrosChanged,
                        enabled = uiState.isEditing
                    )

                }
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
                    Text(
                        if (uiState.isEditing) {
                            "Save goal"
                        } else {
                            "Edit goals"
                        }
                    )
                }


            }
        }


    }

}
