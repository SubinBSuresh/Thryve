package com.dutch.thryve.data.repository

import com.dutch.thryve.data.dao.PRDao
import com.dutch.thryve.data.dao.TrackerDao
import com.dutch.thryve.domain.repository.PRRepository
import com.dutch.thryve.domain.repository.TrackerRepository
import com.dutch.thryve.ui.screens.PersonalRecord
import com.dutch.thryve.ui.viewmodel.PRViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton



@Singleton
class PRRepositoryImpl @Inject constructor(private val prDao: PRDao) {
     fun getPersonalRecords(userId: String): Flow<List<PersonalRecord>> {
        TODO("Not yet implemented")
    }

     suspend fun savePersonalRecord(
        record: PersonalRecord,
        userId: String
    ) {
        TODO("Not yet implemented")
    }
}