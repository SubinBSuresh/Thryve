package com.dutch.thryve.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutch.thryve.data.repository.PRRepositoryImpl
import com.dutch.thryve.data.repository.TrackerRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PRViewModel @Inject constructor(private val repository: PRRepositoryImpl) : ViewModel(){
    private val _prList = MutableStateFlow(loadInitialRecords())
    val prList: StateFlow<List<PersonalRecord>> = _prList.asStateFlow()


    open fun logPersonalRecord(exerciseName: String, weight: Int, reps: Int, date: LocalDate) {
        // Run the state update in the ViewModelScope
        viewModelScope.launch {
            val newRecord = PersonalRecord(
                exerciseName = exerciseName,
                weight = weight,
                reps = reps,
                date = date
            )

            // Atomically update the StateFlow by creating a new list with the new record added
            _prList.update { currentList ->
                (currentList + newRecord).sortedByDescending { it.date }
            }

            // NOTE: In a real app, you would call a repository here:
//             repository.savePersonalRecord(newRecord)
        }
    }

    private fun loadInitialRecords(): List<PersonalRecord> {
        return listOf(
            PersonalRecord("1", "Bench Press", 100, 5, LocalDate.now()),
            PersonalRecord("2", "Squat", 140, 3, LocalDate.now().minusDays(5)),
            PersonalRecord("3", "Deadlift", 185, 1, LocalDate.now().minusDays(10))
        ).sortedByDescending { it.date }
    }

    data class PersonalRecord(
        val id: String = UUID.randomUUID().toString(),
        val exerciseName: String = "  ",
        val weight: Int = 100,
        val reps: Int = 100,
        val date: LocalDate = LocalDate.now()
    )
}