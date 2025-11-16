package com.dutch.thryve.domain.repository

import com.dutch.thryve.ui.screens.PersonalRecord
import com.dutch.thryve.ui.viewmodel.PRViewModel
import kotlinx.coroutines.flow.Flow

interface PRRepository {
    fun getPersonalRecords(userId: String): Flow<List<PersonalRecord>>
    suspend fun savePersonalRecord(record: PersonalRecord, userId: String)
}