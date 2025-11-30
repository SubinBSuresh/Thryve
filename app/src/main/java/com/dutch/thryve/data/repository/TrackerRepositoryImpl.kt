package com.dutch.thryve.data.repository

import com.dutch.thryve.domain.model.MealLog
import com.dutch.thryve.data.dao.TrackerDao
import com.dutch.thryve.domain.repository.TrackerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackerRepositoryImpl @Inject constructor(private val trackerDao: TrackerDao) :
    TrackerRepository {
        private val _logs = MutableStateFlow<List<MealLog>>(emptyList())

    override fun getLogsForDate(userId: String, date: LocalDate): Flow<List<MealLog>> {

return _logs
    }

    override suspend fun insertLog(log: MealLog) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteLog(id: String) {
        TODO("Not yet implemented")
    }

    override suspend fun syncLogs(userId: String): List<MealLog> {
        TODO("Not yet implemented")
    }




}