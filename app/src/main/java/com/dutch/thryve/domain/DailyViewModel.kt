package com.dutch.thryve.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutch.thryve.data.DailySummary
import com.dutch.thryve.data.DailySummary.Companion.empty
import com.dutch.thryve.data.MealLog
import com.dutch.thryve.data.TrackerRepository
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
import java.time.LocalDate
import javax.inject.Inject


private const val CURRENT_USER_ID = "mock_user123"

@HiltViewModel
class DailyViewModel @Inject constructor(private val repository: TrackerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val selectedDateFlow = _uiState.map { it.selectedDate }.distinctUntilChanged()

    private fun collectMealLogs() {
        selectedDateFlow.flatMapLatest { date ->
            repository.getLogsForDate(CURRENT_USER_ID, date)
        }.onEach { logs ->
            _uiState.update { currentState ->
                currentState.copy(
                    mealLogs = logs, totalCalories = logs.sumOf { it.calories })
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


    fun logMeal(mealDescription: String) {}
    fun toggleInputDialog(toggle: Boolean) {

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
    val dailySummary: DailySummary = empty(selectedDate, 4000)
)