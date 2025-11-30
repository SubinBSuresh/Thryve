package com.dutch.thryve.domain.repository

import com.dutch.thryve.domain.model.PersonalRecord
import kotlinx.coroutines.flow.Flow

interface FirebaseRepository {
    fun getPersonalRecords(userId: String): Flow<List<PersonalRecord>>
    suspend fun savePersonalRecord(record: PersonalRecord, userId: String)
    suspend fun updatePersonalRecord(record: PersonalRecord, userId: String)
    suspend fun initializeFirebase()
}