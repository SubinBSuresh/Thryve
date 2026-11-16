package com.dutch.thryve.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutch.thryve.data.repository.FirebaseRepositoryImpl
import com.dutch.thryve.domain.model.MealLog
import com.dutch.thryve.domain.model.PersonalRecord
import com.dutch.thryve.domain.model.Supplement
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class DeveloperUiState(
    val statusMessage: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class DeveloperViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeveloperUiState())
    val uiState: StateFlow<DeveloperUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    fun seedSampleMeals() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Seeding sample meals...") }
            try {
                val today = LocalDate.now()
                val sampleMeals = listOf(
                    Triple("Oatmeal with Berries & Whey Protein", 450, Pair(35, Pair(55, 10))),
                    Triple("Grilled Chicken Breast with Brown Rice & Broccoli", 650, Pair(52, Pair(65, 12))),
                    Triple("Salmon Fillet with Sweet Potato", 580, Pair(42, Pair(40, 22))),
                    Triple("Greek Yogurt with Almonds & Honey", 320, Pair(24, Pair(28, 12)))
                )

                for (dayOffset in 0..6) {
                    val date = today.minusDays(dayOffset.toLong())
                    val timestamp = Timestamp(date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)

                    sampleMeals.forEachIndexed { index, meal ->
                        val mealType = when (index) {
                            0 -> "Breakfast"
                            1 -> "Lunch"
                            2 -> "Dinner"
                            else -> "Snack"
                        }
                        val mealLog = MealLog(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            date = timestamp,
                            description = meal.first,
                            calories = meal.second,
                            protein = meal.third.first,
                            carbs = meal.third.second.first,
                            fat = meal.third.second.second,
                            mealType = mealType
                        )
                        firebaseRepository.saveMealLog(mealLog, userId)
                    }
                }
                _uiState.update { it.copy(isLoading = false, statusMessage = "Successfully seeded 7 days of sample meals!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, statusMessage = "Failed to seed meals: ${e.message}") }
            }
        }
    }

    fun seedSamplePRs() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Seeding sample PRs...") }
            try {
                val samplePRs = listOf(
                    PersonalRecord(id = UUID.randomUUID().toString(), exerciseName = "Barbell Bench Press", weight = 100.0, reps = 5),
                    PersonalRecord(id = UUID.randomUUID().toString(), exerciseName = "Barbell Squat", weight = 140.0, reps = 3),
                    PersonalRecord(id = UUID.randomUUID().toString(), exerciseName = "Barbell Deadlift", weight = 180.0, reps = 1),
                    PersonalRecord(id = UUID.randomUUID().toString(), exerciseName = "Overhead Press", weight = 65.0, reps = 5)
                )
                samplePRs.forEach { record ->
                    firebaseRepository.savePersonalRecord(record, userId)
                }
                _uiState.update { it.copy(isLoading = false, statusMessage = "Successfully seeded sample PRs!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, statusMessage = "Failed to seed PRs: ${e.message}") }
            }
        }
    }

    fun seedSampleSupplements() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Seeding sample supplements...") }
            try {
                val sampleSupplements = listOf(
                    Supplement(name = "Whey Protein Isolate", totalQuantity = 1000.0, remainingQuantity = 750.0, dailyDosage = 30.0, unit = "g", daysLeftThreshold = 7),
                    Supplement(name = "Creatine Monohydrate", totalQuantity = 300.0, remainingQuantity = 220.0, dailyDosage = 5.0, unit = "g", daysLeftThreshold = 7),
                    Supplement(name = "Omega-3 Fish Oil", totalQuantity = 120.0, remainingQuantity = 90.0, dailyDosage = 2.0, unit = "capsules", daysLeftThreshold = 10),
                    Supplement(name = "Magnesium Glycinate", totalQuantity = 90.0, remainingQuantity = 12.0, dailyDosage = 2.0, unit = "capsules", daysLeftThreshold = 10)
                )
                sampleSupplements.forEach { supplement ->
                    firebaseRepository.saveSupplement(supplement, userId)
                }
                _uiState.update { it.copy(isLoading = false, statusMessage = "Successfully seeded sample supplements!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, statusMessage = "Failed to seed supplements: ${e.message}") }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
