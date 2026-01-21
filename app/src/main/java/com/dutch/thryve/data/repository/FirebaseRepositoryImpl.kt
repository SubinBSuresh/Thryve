package com.dutch.thryve.data.repository

import android.util.Log
import com.dutch.thryve.domain.model.MealLog
import com.dutch.thryve.domain.model.PersonalRecord
import com.dutch.thryve.domain.model.Supplement
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
                        val weight = document.get("weight") as? Double ?: (document.get("weight") as? Long)?.toDouble() ?: 0.0
                        val reps = document.getLong("reps")?.toInt() ?: 0

                        val dateObject = document.get("date")
                        val date = when (dateObject) {
                            is Timestamp -> dateObject
                            is Map<*, *> -> {
                                val seconds = (dateObject["seconds"] as? Long) ?: 0L
                                val nanoseconds = (dateObject["nanoseconds"] as? Long)?.toInt() ?: 0
                                Timestamp(seconds, nanoseconds)
                            }
                            else -> Timestamp.now()
                        }

                        PersonalRecord(
                            id = document.id,
                            exerciseName = exerciseName,
                            weight = weight,
                            reps = reps,
                            date = date
                        )
                    } catch (e: Exception) {
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
        // Use set() to either create a new document or overwrite an existing one.
        db.collection("users").document(userId).collection("meal_logs").document(mealLog.id).set(mealLog).await()
    }

    override fun getMealLogsForDate(userId: String, date: LocalDate): Flow<List<MealLog>> {
        val startOfDay = Timestamp(date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)
        val endOfDay = Timestamp(date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond(), 999_999_999)

        return db.collection("users").document(userId).collection("meal_logs")
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThanOrEqualTo("date", endOfDay).orderBy("date", Query.Direction.ASCENDING)
            .snapshots()
            .map { querySnapshot ->
                Log.i("dutch", "Query for date $date returned ${querySnapshot.size()} documents.")
                // Manual mapping to include the document ID
                querySnapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject(MealLog::class.java)?.copy(id = document.id)
                    } catch (e: Exception) {
                        Log.e("dutch", "Failed to parse meal log document ${document.id}", e)
                        null
                    }
                }
            }
    }

    override fun getMealLogsForDateRange(userId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<MealLog>> {
        val startTimestamp = Timestamp(startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)
        val endTimestamp = Timestamp(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond(), 999_999_999)

        return db.collection("users").document(userId).collection("meal_logs")
            .whereGreaterThanOrEqualTo("date", startTimestamp)
            .whereLessThanOrEqualTo("date", endTimestamp)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject(MealLog::class.java)?.copy(id = document.id)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }

    override suspend fun deleteMealLog(mealLogId: String, userId: String) {
        db.collection("users").document(userId).collection("meal_logs").document(mealLogId).delete().await()
    }

    override suspend fun updateMealLog(mealLog: MealLog, userId: String) {
        if (mealLog.id.isBlank()) return
        db.collection("users").document(userId).collection("meal_logs").document(mealLog.id).set(mealLog).await()
    }

    override fun getFavoriteMeals(userId: String): Flow<List<MealLog>> {
        return db.collection("users").document(userId).collection("meal_logs")
            .whereEqualTo("isFavorite", true)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject(MealLog::class.java)?.copy(id = document.id)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }

    override suspend fun saveUserSettings(userSettings: UserSettings, userId: String) {
        db.collection("users").document(userId).collection("settings").document("goals").set(userSettings).await()
    }

    override fun getUserSettings(userId: String): Flow<UserSettings?> {
        return db.collection("users").document(userId).collection("settings").document("goals").snapshots().map {
            it.toObject(UserSettings::class.java)
        }
    }

    override suspend fun saveSupplement(supplement: Supplement, userId: String) {
        db.collection("users").document(userId).collection("supplements").document(supplement.id).set(supplement).await()
    }

    override fun getSupplements(userId: String): Flow<List<Supplement>> {
        return db.collection("users").document(userId).collection("supplements")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject(Supplement::class.java)?.copy(id = document.id)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }

    override suspend fun deleteSupplement(supplementId: String, userId: String) {
        db.collection("users").document(userId).collection("supplements").document(supplementId).delete().await()
    }

    override suspend fun initializeFirebase() {
        TODO("Not yet implemented")
    }
}