package com.dutch.thryve.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutch.thryve.ai.GeminiService
import com.dutch.thryve.ai.OpenAIMessage
import com.dutch.thryve.ai.OpenAIService
import com.dutch.thryve.ai.OpenRouterService
import com.dutch.thryve.data.repository.FirebaseRepositoryImpl
import com.dutch.thryve.domain.model.DailySummary
import com.dutch.thryve.domain.model.MealLog
import com.dutch.thryve.domain.model.UserSettings
import com.dutch.thryve.domain.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DailyViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepositoryImpl,
    private val geminiService: GeminiService,
    private val openAIService: OpenAIService,
    private val openRouterService: OpenRouterService
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    private val selectedDateFlow = _uiState.map { it.selectedDate }.distinctUntilChanged()

    init {
        collectUserSettings()
        collectMealLogs()
        collectFavoriteMeals()
    }

    private fun collectUserSettings() {
        val userId = auth.currentUser?.uid ?: return
        firebaseRepository.getUserSettings(userId).onEach { settings ->
            _uiState.update { currentState ->
                val currentSettings = settings ?: UserSettings()
                val newSummary = currentState.dailySummary.copy(
                    targetCalories = currentSettings.targetCalories,
                    targetProtein = currentSettings.targetProtein,
                    targetCarbs = currentSettings.targetCarbs,
                    targetFat = currentSettings.targetFat
                )
                currentState.copy(userSettings = currentSettings, dailySummary = newSummary)
            }
        }.launchIn(viewModelScope)
    }

    private fun collectMealLogs() {
        val userId = auth.currentUser?.uid ?: return
        selectedDateFlow.flatMapLatest { date ->
            firebaseRepository.getMealLogsForDate(userId, date)
        }.onEach { logs ->
            _uiState.update { currentState ->
                val totalCalories = logs.sumOf { it.calories }
                val totalProtein = logs.sumOf { it.protein }
                val totalCarbs = logs.sumOf { it.carbs }
                val totalFat = logs.sumOf { it.fat }

                val newSummary = currentState.dailySummary.copy(
                    totalFoodCalories = totalCalories,
                    currentProtein = totalProtein,
                    currentCarbs = totalCarbs,
                    currentFat = totalFat
                )
                currentState.copy(mealLogs = logs, dailySummary = newSummary)
            }
        }.launchIn(viewModelScope)
    }

    private fun collectFavoriteMeals() {
        val userId = auth.currentUser?.uid ?: return
        firebaseRepository.getFavoriteMeals(userId).onEach { favorites ->
            val distinctFavorites = favorites.distinctBy { it.description.trim().lowercase() }
            _uiState.update { it.copy(favoriteMeals = distinctFavorites) }
        }.launchIn(viewModelScope)
    }

    fun onAddMealClicked() {
        _uiState.update { it.copy(mealToEdit = null, showInputDialog = true, mealInputText = "") }
    }

    fun onEditMealClicked(mealLog: MealLog) {
        _uiState.update { 
            it.copy(
                mealToEdit = mealLog, 
                showInputDialog = true, 
                mealInputText = mealLog.description,
                manualCalories = mealLog.calories.toString(),
                manualProtein = mealLog.protein.toString(),
                manualCarbs = mealLog.carbs.toString(),
                manualFat = mealLog.fat.toString()
            ) 
        }
    }

    fun onDeleteMealClicked(mealLog: MealLog) {
        _uiState.update { it.copy(mealToDelete = mealLog) }
    }

    fun onToggleFavorite(mealLog: MealLog) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val updatedMeal = mealLog.copy(isFavorite = !mealLog.isFavorite)
            try {
                firebaseRepository.updateMealLog(updatedMeal, userId) // network call

                // Update local state on success
                val message = if (updatedMeal.isFavorite) "Added to favorites" else "Removed from favorites"
                _uiState.update { currentState ->
                    val updatedLogs = currentState.mealLogs.map { log ->
                        if (log.id == updatedMeal.id) updatedMeal else log
                    }
                    currentState.copy(mealLogs = updatedLogs, error = message)
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update favorite status.") }
                Log.e("DailyViewModel", "Error toggling favorite", e)
            }
        }
    }

    fun onConfirmDelete() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            _uiState.value.mealToDelete?.let {
                firebaseRepository.deleteMealLog(it.id, userId)
            }
            _uiState.update { it.copy(mealToDelete = null) }
        }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(mealToDelete = null) }
    }
    
    fun onShowFavoritesDialog(show: Boolean) {
        _uiState.update { it.copy(showFavoritesDialog = show) }
    }

    fun onFavoriteMealSelected(meal: MealLog) {
        _uiState.update {
            it.copy(
                showFavoritesDialog = false,
                mealInputText = meal.description,
                manualCalories = meal.calories.toString(),
                manualProtein = meal.protein.toString(),
                manualCarbs = meal.carbs.toString(),
                manualFat = meal.fat.toString()
            )
        }
    }

    // --- Functions for Manual Entry ---
    fun onManualCaloriesChanged(calories: String) {
        _uiState.update { it.copy(manualCalories = calories) }
    }
    fun onManualProteinChanged(protein: String) {
        _uiState.update { it.copy(manualProtein = protein) }
    }
    fun onManualCarbsChanged(carbs: String) {
        _uiState.update { it.copy(manualCarbs = carbs) }
    }
    fun onManualFatChanged(fat: String) {
        _uiState.update { it.copy(manualFat = fat) }
    }
    // --------------------------------
    fun updateMealInputText(text: String) {
        _uiState.update { it.copy(mealInputText = text) }
    }

    fun updateSelectedDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun logOrUpdateMeal() {
        val useGemini = _uiState.value.userSettings?.useGeminiForMacros == true
        if (useGemini) {
            logMealWithGemini()
        } else {
            logMealManually()
        }
    }

    private fun logMealManually() {
        val userId = auth.currentUser?.uid ?: return
        val state = _uiState.value

        val mealLog = MealLog(
            id = state.mealToEdit?.id ?: UUID.randomUUID().toString(),
            userId = userId,
            date = Timestamp(state.selectedDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0),
            description = state.mealInputText,
            calories = state.manualCalories.toIntOrNull() ?: 0,
            protein = state.manualProtein.toIntOrNull() ?: 0,
            carbs = state.manualCarbs.toIntOrNull() ?: 0,
            fat = state.manualFat.toIntOrNull() ?: 0,
            isFavorite = state.mealToEdit?.isFavorite ?: false
        )

        viewModelScope.launch {
            firebaseRepository.saveMealLog(mealLog, userId)
            toggleInputDialog(false) // Reset state
        }
    }

    private fun logMealWithGemini() {
        val userId = auth.currentUser?.uid ?: return
        val mealDescription = _uiState.value.mealInputText
        if (mealDescription.isBlank()) return
        
        val existingMeal = _uiState.value.mealToEdit

        viewModelScope.launch {
            _uiState.update { it.copy(isAwaitingAi = true, showInputDialog = false) }
            try {
                val date = uiState.value.selectedDate
//                val mealLog = geminiService.analyzeMeal(mealDescription, date)
//                val mealLog = openAIService.analyzeMeal(mealDescription, date)
                val mealLog = openRouterService.analyzeMeal(mealDescription, date)

                if (mealLog != null) {
                    val finalMealLog = mealLog.copy(
                        id = existingMeal?.id ?: mealLog.id,
                        userId = userId,
                        isFavorite = existingMeal?.isFavorite ?: false
                    )
                    firebaseRepository.saveMealLog(finalMealLog, userId)
                } else {
                    _uiState.update { it.copy(error = "Could not analyze meal. Please try again.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "An error occurred.") }
                Log.e("dutch", "Error analyzing meal", e)
            } finally {
                _uiState.update { it.copy(isAwaitingAi = false, mealToEdit = null, mealInputText = "") }
            }
        }
    }

    fun toggleInputDialog(show: Boolean) {
        if (!show) {
            _uiState.update { 
                it.copy(
                    showInputDialog = false, 
                    mealToEdit = null, 
                    mealInputText = "",
                    manualCalories = "",
                    manualProtein = "",
                    manualCarbs = "",
                    manualFat = ""
                ) 
            }
        } else {
             _uiState.update { it.copy(showInputDialog = true) }
        }
    }

    fun onUseAiToggled(enabled: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val currentSettings = _uiState.value.userSettings ?: UserSettings()
        val updatedSettings = currentSettings.copy(useGeminiForMacros = enabled)
        viewModelScope.launch {
            firebaseRepository.saveUserSettings(updatedSettings, userId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val mealLogs: List<MealLog> = emptyList(),
    val mealInputText: String = "",
    val showInputDialog: Boolean = false,
    val showFavoritesDialog: Boolean = false,
    val isAwaitingAi: Boolean = false,
    val error: String? = null,
    val dailySummary: DailySummary = DailySummary.empty(selectedDate, 2000),
    val mealToEdit: MealLog? = null,
    val mealToDelete: MealLog? = null,
    val userSettings: UserSettings? = null,
    val favoriteMeals: List<MealLog> = emptyList(),
    // Fields for manual entry
    val manualCalories: String = "",
    val manualProtein: String = "",
    val manualCarbs: String = "",
    val manualFat: String = ""
)
