package com.dutch.thryve.domain.model

import com.google.firebase.Timestamp

enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK
}
data class MealLog(
    val id: String = "", // Used for Firestore/Database operations
    val userId: String = "",
    val date: Timestamp = Timestamp.now(),
    val mealType: MealType? = null, // New field
    val description: String = "",
    val calories: Int = 0,
    val protein: Int = 0, // grams
    val fat: Int = 0,     // grams
    val carbs: Int = 0    // grams
)
