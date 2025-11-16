package com.dutch.thryve.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "meal")
data class MealData(
    @PrimaryKey val id: String,
    val userId: String,
    val date: String,
    val time: String,
    val text: String,
    val calories: Double,
    val protein: Double,
    val fats: Double,
    val carbs: Double,
    val timeStamp: Long,
    val isAwaitingAi: Boolean
) {


    companion object {
        fun createInitialMeal(
            userId: String,
            date: LocalDate,
            time: String,
            text: String
        ): MealData {

            return MealData(
                id = UUID.randomUUID().toString(),
                userId = userId,
                date = date.toString(),
                time = time,
                text = text,
                calories = 0.0,
                protein = 0.0,
                carbs = 0.0,
                fats = 0.0,
                isAwaitingAi = true, // Critical: Mark this for immediate AI processing
                timeStamp = System.currentTimeMillis()
            )
        }
    }
}