package com.dutch.thryve.data

import java.time.LocalDate

data class DailySummary(
    val date: LocalDate,
    val totalFoodCalories: Int,
    val totalExerciseCalories: Int, // Placeholder for future exercise logging
    val remainingCalories: Int,

    // Current macros
    val currentCarbs: Int,
    val currentProtein: Int,
    val currentFat: Int,

    // Target macros (these would come from user settings/goals)
    val targetCarbs: Int,
    val targetProtein: Int,
    val targetFat: Int
) {
    companion object {
        fun empty(date: LocalDate, targetCalories: Int): DailySummary {
            return DailySummary(
                date = date,
                totalFoodCalories = 0,
                totalExerciseCalories = 0,
                remainingCalories = targetCalories, // Initially all remaining
                currentCarbs = 0,
                currentProtein = 0,
                currentFat = 0,
                targetCarbs = 0, // Placeholder, will be set by ViewModel
                targetProtein = 0,
                targetFat = 0
            )
        }
    }
}
