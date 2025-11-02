package com.dutch.thryve.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackerRepository @Inject constructor(private val trackerDao: TrackerDao){

    private val allLogsFlow = MutableStateFlow(MockData.generateMockLogs())

    fun getLogsForDate(userId: String, date: java.time.LocalDate): Flow<List<MealLog>>{

        return allLogsFlow.map { allLogs ->
            allLogs
                .filter { it.date == date && it.userId == userId }
                .sortedByDescending { it.id } // Simple sorting by ID, or time if time was included
        }
    }



    private object MockData {
        private val today = LocalDate.now()
        private val yesterday = today.minusDays(1)
        private val mockUserId = "mock_user_123"

        fun generateMockLogs(): List<MealLog> {
            return listOf(
                // Logs for TODAY (will show on launch)
                MealLog(
                    userId = mockUserId,
                    date = today,
                    description = "Breakfast: Scrambled eggs (3) with whole-wheat toast (2 slices) and a black coffee.",
                    calories = 450,
                    protein = 35,
                    fat = 20,
                    carbs = 30
                ),
                MealLog(
                    userId = mockUserId,
                    date = today,
                    description = "Lunch: Chicken salad with mixed greens, olive oil dressing, and a small apple.",
                    calories = 380,
                    protein = 45,
                    fat = 15,
                    carbs = 20
                ),
                MealLog(
                    userId = mockUserId,
                    date = today,
                    description = "Snack: Protein shake (whey isolate).",
                    calories = 150,
                    protein = 30,
                    fat = 2,
                    carbs = 5
                ),

                // Logs for YESTERDAY (will show if date is changed)
                MealLog(
                    userId = mockUserId,
                    date = yesterday,
                    description = "Dinner: Salmon fillet with baked sweet potato and steamed asparagus.",
                    calories = 620,
                    protein = 50,
                    fat = 30,
                    carbs = 45
                )
            )
        }
    }

}