package com.dutch.thryve.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutch.thryve.data.repository.FirebaseRepositoryImpl
import com.dutch.thryve.domain.model.MealLog
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    init {
        fetchWeeklySummary()
    }

    private fun fetchWeeklySummary() {
        val userId = auth.currentUser?.uid ?: return
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(6)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            firebaseRepository.getMealLogsForDateRange(userId, startDate, endDate)
                .collect { mealLogs ->
                    val weeklySummary = calculateWeeklySummary(mealLogs)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            weeklySummary = weeklySummary,
                            error = null
                        )
                    }
                }
        }
    }

    private fun calculateWeeklySummary(mealLogs: List<MealLog>): WeeklySummary {
        val totalCalories = mealLogs.sumOf { it.calories }
        val totalProtein = mealLogs.sumOf { it.protein }
        val totalCarbs = mealLogs.sumOf { it.carbs }
        val totalFat = mealLogs.sumOf { it.fat }
        return WeeklySummary(totalCalories, totalProtein, totalCarbs, totalFat)
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val weeklySummary: WeeklySummary? = null,
    val error: String? = null
)

data class WeeklySummary(
    val totalCalories: Int = 0,
    val totalProtein: Int = 0,
    val totalCarbs: Int = 0,
    val totalFat: Int = 0
)
