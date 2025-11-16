package com.dutch.thryve.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutch.thryve.data.repository.FirebaseRepositoryImpl
import com.dutch.thryve.ui.screens.PersonalRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class FirebaseViewModel @Inject constructor(private val repository: FirebaseRepositoryImpl) :
    ViewModel() {


    fun logPersonalRecord(exerciseName: String, weight: Int, reps: Int, date: LocalDate) {
        viewModelScope.launch {
            repository.savePersonalRecord(
                PersonalRecord("1", exerciseName, weight, reps, date)
            )
        }
    }
}

