package com.dutch.thryve.data.repository

import android.util.Log
import com.dutch.thryve.domain.repository.FirebaseRepository
import com.dutch.thryve.ui.screens.PersonalRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate


class FirebasePRRepositoryImpl :  FirebaseRepository{
    // Mock data storage for real-time flow demonstration
    private val mockRecords = MutableStateFlow(
        listOf(
            PersonalRecord("1", "Bench Press", 100, 5, LocalDate.now()),
            PersonalRecord("2", "Squat", 140, 3, LocalDate.now().minusDays(5))
        )
    )

    override fun getPersonalRecords(userId: String): Flow<List<PersonalRecord>> {
        // REAL IMPLEMENTATION: Uses onSnapshot listener on the Firestore collection
//        Log.d(LOG_TAG, "Fetching records for user: $userId")
        return mockRecords.asStateFlow()
    }

    override suspend fun savePersonalRecord(record: PersonalRecord, userId: String) {
        // REAL IMPLEMENTATION: Uses firestore.collection(...).addDoc()
//        Log.i(LOG_TAG, "Saving record for user $userId: ${record.exerciseName}")
        mockRecords.update { it + record }
    }
}