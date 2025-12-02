package com.dutch.thryve.domain.model

data class UserSettings(
    val targetCalories: Int = 2000,
    val targetProtein: Int = 150,
    val targetCarbs: Int = 200,
    val targetFat: Int = 60,
    val useGeminiForMacros: Boolean = true
) {
    // Add a no-argument constructor for Firestore deserialization
    constructor() : this(2000, 150, 200, 60, true)
}