package com.dutch.thryve.domain.repository

import com.dutch.thryve.domain.model.MealLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TrackerRepository {

    fun getLogsForDate(
        userId: String,
        date: LocalDate
    ): Flow<List<MealLog>>

    suspend fun insertLog(log: MealLog)

    suspend fun deleteLog(id: String)

    suspend fun syncLogs(userId: String): List<MealLog>
}
