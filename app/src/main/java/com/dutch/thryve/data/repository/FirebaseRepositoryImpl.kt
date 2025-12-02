package com.dutch.thryve.data.repository

import android.util.Log
import com.dutch.thryve.domain.model.MealLog
import com.dutch.thryve.domain.model.PersonalRecord
import com.dutch.thryve.domain.model.UserSettings
import com.dutch.thryve.domain.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class FirebaseRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore, private val uid: FirebaseAuth
) : FirebaseRepository {
    override fun getPersonalRecords(userId: String): Flow<List<PersonalRecord>> {

        return db.collection("users").document(userId).collection("personal_records")
            .orderBy("date", Query.Direction.DESCENDING)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { document ->
                    try {
                        val exerciseName = document.getString("exerciseName") ?: ""
                        val weight = document.getLong("weight")?.toInt() ?: 0
                        val reps = document.getLong("reps")?.toInt() ?: 0

                        val dateObject = document.get("date")
                        val date = when (dateObject) {
                            is Timestamp -> dateObject
                            is Map<*, *> -> {
                                val seconds = (dateObject["seconds"] as? Long) ?: 0L
                                val nanoseconds = (dateObject["nanoseconds"] as? Long)?.toInt() ?: 0
                                Timestamp(seconds, nanoseconds)
                            }
                            else -> Timestamp.now() // Or handle error appropriately
                        }

                        PersonalRecord(
                            id = document.id,
                            exerciseName = exerciseName,
                            weight = weight,
                            reps = reps,
                            date = date
                        )
                    } catch (e: Exception) {
                        // Log error or handle document conversion failure
                        null
                    }
                }
            }
    }

    override suspend fun savePersonalRecord(
        record: PersonalRecord, userId: String
    ) {


        db.collection("users").document(userId).collection("personal_records").add(record)
            .await()
    }

    override suspend fun updatePersonalRecord(record: PersonalRecord, userId: String) {
        if (record.id.isBlank()) return
        db.collection("users").document(userId).collection("personal_records").document(record.id)
            .set(record)
            .await()
    }

    override suspend fun deletePersonalRecord(record: PersonalRecord, userId: String) {
        if (record.id.isBlank()) return
        db.collection("users").document(userId).collection("personal_records").document(record.id)
            .delete()
            .await()
    }

    override suspend fun saveMealLog(mealLog: MealLog, userId: String) {
        db.collection("users").document(userId).collection("meal_logs").add(mealLog).await()
    }

    override fun getMealLogsForDate(userId: String, date: LocalDate): Flow<List<MealLog>> {
        Log.i("dutch", "getMealLogsForDate called with userId: $userId, date: $date")

        return db.collection("users").document(userId).collection("meal_logs")
            .whereEqualTo("date.year", date.year)
            .whereEqualTo("date.monthValue", date.monthValue)
            .whereEqualTo("date.dayOfMonth", date.dayOfMonth)
            .snapshots()
            .map { querySnapshot ->
                Log.i("dutch", "Query returned ${querySnapshot.size()} documents.")
                querySnapshot.documents.mapNotNull { document ->
                    try {
                        Log.i("dutch", "Processing document: ${document.id} => ${document.data}")

                        val dateObject = document.get("date")
                        val timestamp = when (dateObject) {
                            is Timestamp -> dateObject
                            is Map<*, *> -> {
                                val year = (dateObject["year"] as? Long)?.toInt() ?: 1970
                                val month = (dateObject["monthValue"] as? Long)?.toInt() ?: 1
                                val day = (dateObject["dayOfMonth"] as? Long)?.toInt() ?: 1
                                val localDate = LocalDate.of(year, month, day)
                                Timestamp(localDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)
                            }
                            else -> {
                                Log.w("dutch", "Unknown date format for document ${document.id}: $dateObject")
                                Timestamp.now()
                            }
                        }

                        MealLog(
                            id = document.id,
                            userId = document.getString("userId") ?: "",
                            date = timestamp,
                            description = document.getString("description") ?: "",
                            calories = document.getLong("calories")?.toInt() ?: 0,
                            protein = document.getLong("protein")?.toInt() ?: 0,
                            fat = document.getLong("fat")?.toInt() ?: 0,
                            carbs = document.getLong("carbs")?.toInt() ?: 0,
                            mealType = document.getString("mealType")?.let { com.dutch.thryve.domain.model.MealType.valueOf(it) }
                        )
                    } catch (e: Exception) {
                        Log.e("dutch", "Failed to parse document ${document.id}", e)
                        null
                    }
                }
            }
    }

    override suspend fun deleteMealLog(mealLogId: String, userId: String) {
        db.collection("users").document(userId).collection("meal_logs").document(mealLogId).delete().await()
    }

    override suspend fun saveUserSettings(userSettings: UserSettings, userId: String) {
        db.collection("users").document(userId).collection("settings").document("goals").set(userSettings).await()
    }

    override fun getUserSettings(userId: String): Flow<UserSettings?> {
        return db.collection("users").document(userId).collection("settings").document("goals").snapshots().map {
            it.toObject(UserSettings::class.java)
        }
    }

    override suspend fun initializeFirebase() {
        TODO("Not yet implemented")
    }
}