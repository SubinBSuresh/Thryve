package com.dutch.thryve.data.repository

import com.dutch.thryve.domain.model.PersonalRecord
import com.dutch.thryve.domain.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
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

    override suspend fun initializeFirebase() {
        TODO("Not yet implemented")
    }
}