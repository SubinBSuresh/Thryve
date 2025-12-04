package com.dutch.thryve.ui.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutch.thryve.data.repository.FirebaseRepositoryImpl
import com.dutch.thryve.domain.model.PersonalRecord
import com.dutch.thryve.domain.repository.FirebaseRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class FirebaseViewModel @Inject constructor(private val repository: FirebaseRepositoryImpl) :
    ViewModel() {
    private val TAG = "Dutch__FirebaseViewModel"
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _personalRecords = MutableStateFlow<List<PersonalRecord>>(emptyList())
    val personalRecord: StateFlow<List<PersonalRecord>> = _personalRecords.asStateFlow()

    init {
        fetchPersonalRecords()
    }

    private fun fetchPersonalRecords() {
        val userId = auth.currentUser?.uid
        if (userId == null) return


        Log.i(TAG, "userid :: $userId")
        viewModelScope.launch {
            // 3. The entire chain must be inside the launch block.
            repository.getPersonalRecords(userId).catch { e ->
                // This will catch any exceptions from the upstream Flow (e.g., network issues)
                Log.e(TAG, "Error fetching personal records", e)
            }.collect { records ->
                // 4. Update the StateFlow's value. The UI will automatically react to this change.
                _personalRecords.value = records
                Log.d(TAG, "Fetched and updated ${records.size} personal records.")
            }
        } // The launch block closes here.
    }


    fun logPersonalRecord(personalRecord: PersonalRecord) {

        val userId = auth.currentUser?.uid
        if (userId == null) return


        viewModelScope.launch {
            repository.savePersonalRecord(
                personalRecord, userId
            )
        }
    }

    fun updatePersonalRecord(record: PersonalRecord) {
        val userId = auth.currentUser?.uid
        if (userId == null) return

        viewModelScope.launch {
            repository.updatePersonalRecord(record, userId)
        }
    }

    fun deletePersonalRecord(record: PersonalRecord) {
        val userId = auth.currentUser?.uid
        if (userId == null) return

        viewModelScope.launch {
            repository.deletePersonalRecord(record, userId)
        }
    }

    fun logout() {
        auth.signOut()
    }
}

