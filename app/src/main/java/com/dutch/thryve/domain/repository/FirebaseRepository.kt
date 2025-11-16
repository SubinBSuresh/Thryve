package com.dutch.thryve.domain.repository

import com.dutch.thryve.ui.screens.PersonalRecord
import kotlinx.coroutines.flow.Flow

interface FirebaseRepository {
    fun getPersonalRecords(userId: String): Flow<List<PersonalRecord>>
    suspend fun savePersonalRecord(record: PersonalRecord)
    suspend fun initializeFirebase()
}