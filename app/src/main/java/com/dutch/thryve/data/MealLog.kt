package com.dutch.thryve.data

import java.time.LocalDate
enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK
}
data class MealLog(
    val id: String = "", // Used for Firestore/Database operations
    val userId: String = "",
    val date: LocalDate,
    val mealType: MealType? = null, // New field
    val description: String,
    val calories: Int,
    val protein: Int, // grams
    val fat: Int,     // grams
    val carbs: Int    // grams
)

