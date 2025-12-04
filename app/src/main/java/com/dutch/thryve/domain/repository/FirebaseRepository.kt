package com.dutch.thryve.domain.repository

import com.dutch.thryve.domain.model.MealLog
import com.dutch.thryve.domain.model.PersonalRecord
import com.dutch.thryve.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface FirebaseRepository {
    fun getPersonalRecords(userId: String): Flow<List<PersonalRecord>>
    suspend fun savePersonalRecord(record: PersonalRecord, userId: String)
    suspend fun updatePersonalRecord(record: PersonalRecord, userId: String)
    suspend fun deletePersonalRecord(record: PersonalRecord, userId: String)

    suspend fun saveMealLog(mealLog: MealLog, userId: String)
    fun getMealLogsForDate(userId: String, date: LocalDate): Flow<List<MealLog>>
    suspend fun deleteMealLog(mealLogId: String, userId: String)
    suspend fun updateMealLog(mealLog: MealLog, userId: String)
    fun getFavoriteMeals(userId: String): Flow<List<MealLog>>

    suspend fun saveUserSettings(userSettings: UserSettings, userId: String)
    fun getUserSettings(userId: String): Flow<UserSettings?>

    suspend fun initializeFirebase()
}
