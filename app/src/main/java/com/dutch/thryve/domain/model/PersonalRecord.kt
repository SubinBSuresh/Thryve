package com.dutch.thryve.domain.model

import com.google.firebase.Timestamp
import java.util.UUID


data class PersonalRecord(
    val id: String = UUID.randomUUID().toString(),
    val exerciseName: String = "  ",
    val weight: Int = 100,
    val reps: Int = 100,
    val date: Timestamp = Timestamp.now()
) {
    constructor() :this("" , "", 0, 0, Timestamp.now())
}