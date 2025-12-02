package com.dutch.thryve.domain.model

import java.time.LocalDate

data class DailySummary(
    val date: LocalDate,
    val totalFoodCalories: Int,
    val totalExerciseCalories: Int, // Placeholder for future exercise logging

    // Current macros
    val currentCarbs: Int,
    val currentProtein: Int,
    val currentFat: Int,

    // Target macros (these would come from user settings/goals)
    val targetCalories:Int,
    val targetCarbs: Int,
    val targetProtein: Int,
    val targetFat: Int,

) {
    // Calculated property for remaining calories
    val remainingCalories: Int
        get() = targetCalories - totalFoodCalories
        
    companion object {
        fun empty(date: LocalDate, targetCalories: Int): DailySummary {
            return DailySummary(
                date = date,
                totalFoodCalories = 0,
                totalExerciseCalories = 0,
                currentCarbs = 0,
                currentProtein = 0,
                currentFat = 0,
                targetCalories = targetCalories, // Correctly set the target
                targetCarbs = 0, 
                targetProtein = 0,
                targetFat = 0
            )
        }
    }
}