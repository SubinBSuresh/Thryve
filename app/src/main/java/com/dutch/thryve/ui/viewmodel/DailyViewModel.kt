package com.dutch.thryve.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutch.thryve.ai.GeminiService
import com.dutch.thryve.domain.model.DailySummary
import com.dutch.thryve.domain.model.MealLog
import com.dutch.thryve.data.repository.FirebaseRepositoryImpl
import com.dutch.thryve.data.repository.TrackerRepositoryImpl
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
import javax.inject.Inject

@HiltViewModel
class DailyViewModel @Inject constructor(
    private val trackerRepository: TrackerRepositoryImpl,
    private val firebaseRepository: FirebaseRepositoryImpl,
    private val geminiService: GeminiService
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    private val selectedDateFlow = _uiState.map { it.selectedDate }.distinctUntilChanged()

    private fun collectMealLogs() {
        val userId = auth.currentUser?.uid ?: return
        selectedDateFlow.flatMapLatest { date ->
            firebaseRepository.getMealLogsForDate(userId, date)
        }.onEach { logs ->
            val totalCalories = logs.sumOf { it.calories }
            val totalProtein = logs.sumOf { it.protein }
            val totalCarbs = logs.sumOf { it.carbs }
            val totalFat = logs.sumOf { it.fat }

            _uiState.update { currentState ->
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

    init {
        collectMealLogs()
    }

    fun updateMealInputText(text: String) {
        _uiState.update { it.copy(mealInputText = text) }
    }

    fun updateSelectedDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun logMeal(mealDescription: String) {
        val userId = auth.currentUser?.uid ?: return
        if (mealDescription.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isAwaitingAi = true, showInputDialog = false, mealInputText = "")
            }

            try {
                val date = uiState.value.selectedDate
                val mealLog = geminiService.analyzeMeal(mealDescription, date)

                if (mealLog != null) {
                    firebaseRepository.saveMealLog(mealLog.copy(userId = userId), userId)
                } else {
                    _uiState.update { it.copy(error = "Could not analyze meal. Please try again.") }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "An error occurred.") }
                Log.e("dutch", "Error analyzing meal", e)
            } finally {
                _uiState.update { it.copy(isAwaitingAi = false) }
            }
        }
    }

    fun toggleInputDialog(show: Boolean) {
        _uiState.update { it.copy(showInputDialog = show) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val mealLogs: List<MealLog> = emptyList(),
    val totalCalories: Int = 0,
    val mealInputText: String = "",
    val showInputDialog: Boolean = false,
    val isAwaitingAi: Boolean = false,
    val error: String? = null,
    val dailySummary: DailySummary = DailySummary.empty(selectedDate, 4000)
)
