package com.dutch.thryve.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutch.thryve.data.repository.FirebaseRepositoryImpl
import com.dutch.thryve.domain.model.UserSettings
import com.dutch.thryve.domain.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class ProfileScreenState(
    val userSettings: UserSettings = UserSettings(),
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileScreenState())
    val uiState = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    init {
        loadUserSettings()
    }

    private fun loadUserSettings() {
        val userId = auth.currentUser?.uid ?: return
        firebaseRepository.getUserSettings(userId).onEach { settings ->
            if (settings != null) {
                _uiState.update { it.copy(userSettings = settings, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false) } // Stop loading even if no settings found
            }
        }.launchIn(viewModelScope)
    }

    fun onEditToggled(isEditing: Boolean) {
        _uiState.update { it.copy(isEditing = isEditing, isSaved = false) }
    }

    fun onTargetCaloriesChanged(calories: String) {
        _uiState.update { state ->
            val newSettings = state.userSettings.copy(targetCalories = calories.toIntOrNull() ?: 0)
            state.copy(userSettings = newSettings, isSaved = false)
        }
    }

    fun onTargetProteinChanged(protein: String) {
        _uiState.update { state ->
            val newSettings = state.userSettings.copy(targetProtein = protein.toIntOrNull() ?: 0)
            state.copy(userSettings = newSettings, isSaved = false)
        }
    }

    fun onTargetCarbsChanged(carbs: String) {
        _uiState.update { state ->
            val newSettings = state.userSettings.copy(targetCarbs = carbs.toIntOrNull() ?: 0)
            state.copy(userSettings = newSettings, isSaved = false)
        }
    }

    fun onTargetFatChanged(fat: String) {
        _uiState.update { state ->
            val newSettings = state.userSettings.copy(targetFat = fat.toIntOrNull() ?: 0)
            state.copy(userSettings = newSettings, isSaved = false)
        }
    }

    fun onUseGeminiForMacrosChanged(useGemini: Boolean) {
        _uiState.update { state ->
            val newSettings = state.userSettings.copy(useGeminiForMacros = useGemini)
            state.copy(userSettings = newSettings, isSaved = false)
        }
    }


    fun saveUserSettings() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            Log.i("dutch", "save user settings+ ${_uiState.value.userSettings}")
            firebaseRepository.saveUserSettings(_uiState.value.userSettings, userId)
            _uiState.update { it.copy(isSaved = true, isEditing = false) } // Switch back to display mode after saving
        }
    }
}