package com.dutch.thryve.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutch.thryve.data.repository.FirebaseRepositoryImpl
import com.dutch.thryve.domain.model.MealLog
import com.dutch.thryve.domain.model.UserSettings
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    
    private val selectedDateFlow = _uiState.map { it.selectedDate }.distinctUntilChanged()

    init {
        observeData()
    }

    private fun observeData() {
        val userId = auth.currentUser?.uid ?: return
        
        selectedDateFlow.flatMapLatest { selectedDate ->
            val startOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val endOfWeek = startOfWeek.plusDays(6)

            combine(
                firebaseRepository.getMealLogsForDateRange(userId, startOfWeek, endOfWeek),
                firebaseRepository.getUserSettings(userId)
            ) { mealLogs, settings ->
                val userSettings = settings ?: UserSettings()
                val weeklySummary = calculateWeeklySummary(mealLogs, userSettings)
                val dailyData = (0..6).map { i ->
                    val date = startOfWeek.plusDays(i.toLong())
                    val caloriesForDay = mealLogs.filter { 
                        it.date.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == date 
                    }.sumOf { it.calories }
                    DailyCalorieData(date, caloriesForDay)
                }
                
                Triple(weeklySummary, dailyData, userSettings)
            }.onStart { 
                _uiState.update { it.copy(isLoading = true) } 
            }
        }.onEach { (summary, dailyData, settings) ->
            val selectedDate = _uiState.value.selectedDate
            val startOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val endOfWeek = startOfWeek.plusDays(6)
            
            _uiState.update {
                it.copy(
                    isLoading = false,
                    weeklySummary = summary,
                    dailyCalorieData = dailyData,
                    dailyGoal = settings.targetCalories,
                    weekStart = startOfWeek,
                    weekEnd = endOfWeek
                )
            }
        }.launchIn(viewModelScope)
    }

    fun updateSelectedDate(offsetWeeks: Long) {
        _uiState.update { it.copy(selectedDate = it.selectedDate.plusWeeks(offsetWeeks)) }
    }

    fun resetToToday() {
        _uiState.update { it.copy(selectedDate = LocalDate.now()) }
    }

    private fun calculateWeeklySummary(mealLogs: List<MealLog>, settings: UserSettings): WeeklySummary {
        val totalCalories = mealLogs.sumOf { it.calories }
        val totalProtein = mealLogs.sumOf { it.protein }
        val totalCarbs = mealLogs.sumOf { it.carbs }
        val totalFat = mealLogs.sumOf { it.fat }
        
        return WeeklySummary(
            totalCalories = totalCalories,
            totalProtein = totalProtein,
            totalCarbs = totalCarbs,
            totalFat = totalFat,
            weeklyGoal = settings.targetCalories * 7,
            avgCalories = totalCalories / 7,
            avgProtein = totalProtein / 7,
            avgCarbs = totalCarbs / 7,
            avgFat = totalFat / 7
        )
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val weekStart: LocalDate = LocalDate.now(),
    val weekEnd: LocalDate = LocalDate.now(),
    val weeklySummary: WeeklySummary? = null,
    val dailyCalorieData: List<DailyCalorieData> = emptyList(),
    val dailyGoal: Int = 2000,
    val error: String? = null
)

data class WeeklySummary(
    val totalCalories: Int = 0,
    val totalProtein: Int = 0,
    val totalCarbs: Int = 0,
    val totalFat: Int = 0,
    val weeklyGoal: Int = 0,
    val avgCalories: Int = 0,
    val avgProtein: Int = 0,
    val avgCarbs: Int = 0,
    val avgFat: Int = 0
)

data class DailyCalorieData(
    val date: LocalDate,
    val calories: Int
)
