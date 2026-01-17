package com.dutch.thryve.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutch.thryve.data.repository.FirebaseRepositoryImpl
import com.dutch.thryve.domain.model.UserSettings
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject


data class ProfileScreenState(
    val userSettings: UserSettings = UserSettings(),
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val isSaved: Boolean = false,
    val showExportDialog: Boolean = false,
    val exportMessage: String? = null,
    val dataToCopy: String? = null
)

enum class ExportDuration(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    ALL_TIME("All Time")
}

data class ExportOptions(
    val includeMeals: Boolean = true,
    val includePRs: Boolean = true,
    val duration: ExportDuration = ExportDuration.LAST_7_DAYS
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
                _uiState.update { it.copy(isLoading = false) }
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
            firebaseRepository.saveUserSettings(_uiState.value.userSettings, userId)
            _uiState.update { it.copy(isSaved = true, isEditing = false) }
        }
    }

    fun setShowExportDialog(show: Boolean) {
        _uiState.update { it.copy(showExportDialog = show) }
    }

    fun exportData(options: ExportOptions) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showExportDialog = false) }
            
            val today = LocalDate.now()
            val (startDate, endDate) = when (options.duration) {
                ExportDuration.TODAY -> today to today
                ExportDuration.YESTERDAY -> today.minusDays(1) to today.minusDays(1)
                ExportDuration.LAST_7_DAYS -> today.minusDays(7) to today
                ExportDuration.LAST_30_DAYS -> today.minusDays(30) to today
                ExportDuration.ALL_TIME -> LocalDate.of(2000, 1, 1) to today
            }

            val result = StringBuilder()
            result.append("Thryve Data Export - Generated on ${today}\n\n")

            if (options.includeMeals) {
                val meals = firebaseRepository.getMealLogsForDateRange(userId, startDate, endDate).first()
                result.append("--- MEAL LOGS ---\n")
                result.append("Date,Description,Calories,Protein,Carbs,Fat\n")
                meals.forEach { 
                    result.append("${it.date.toDate()},${it.description},${it.calories},${it.protein},${it.carbs},${it.fat}\n")
                }
                result.append("\n")
            }

            if (options.includePRs) {
                val prs = firebaseRepository.getPersonalRecords(userId).first()
                val filteredPrs = if (options.duration == ExportDuration.ALL_TIME) prs else {
                    prs.filter { 
                        val prDate = it.date.toDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        !prDate.isBefore(startDate) && !prDate.isAfter(endDate)
                    }
                }
                result.append("--- PERSONAL RECORDS ---\n")
                result.append("Date,Exercise,Weight,Reps\n")
                filteredPrs.forEach {
                    result.append("${it.date.toDate()},${it.exerciseName},${it.weight},${it.reps}\n")
                }
            }

            _uiState.update { it.copy(isLoading = false, dataToCopy = result.toString()) }
        }
    }

    fun onDataCopied() {
        _uiState.update { it.copy(dataToCopy = null, exportMessage = "Data copied to clipboard") }
    }

    fun clearExportMessage() {
        _uiState.update { it.copy(exportMessage = null) }
    }

    fun logout() {
        auth.signOut()
    }
}
