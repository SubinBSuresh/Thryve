package com.dutch.thryve.domain.model

import com.google.firebase.Timestamp

data class PersonalRecord(
    val id: String = "",
    val exerciseId: String = "",
    val exerciseName: String = "", // Keep for display purposes, but don't rely on it for logic
    val weight: Int = 0,
    val reps: Int = 0,
    val date: Timestamp = Timestamp.now()
)
