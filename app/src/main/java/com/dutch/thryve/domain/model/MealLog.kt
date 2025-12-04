package com.dutch.thryve.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName


data class MealLog(
    val id: String = "",
    val userId: String = "",
    val date: Timestamp = Timestamp.now(),
    val description: String = "",
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    @get:PropertyName("isFavorite") @set:PropertyName("isFavorite")
    var isFavorite: Boolean = false
)
