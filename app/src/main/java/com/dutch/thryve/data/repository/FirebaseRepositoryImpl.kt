package com.dutch.thryve.data.repository

import com.dutch.thryve.domain.repository.FirebaseRepository
import com.dutch.thryve.ui.screens.PersonalRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class FirebaseRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore?, private val uid: FirebaseAuth
) : FirebaseRepository {
    override fun getPersonalRecords(userId: String): Flow<List<PersonalRecord>> {
        TODO("Not yet implemented")
    }

    override suspend fun savePersonalRecord(
        record: PersonalRecord
    ) {


        db?.collection("users")?.document(record.id)?.collection("personal_records")?.add(record)?.await()
    }

    override suspend fun initializeFirebase() {
        TODO("Not yet implemented")
    }
}