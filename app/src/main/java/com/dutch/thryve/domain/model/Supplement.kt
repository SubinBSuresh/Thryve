package com.dutch.thryve.domain.model

import java.util.UUID

data class Supplement(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val totalQuantity: Double = 0.0,
    val remainingQuantity: Double = 0.0,
    val dailyDosage: Double = 0.0,
    val unit: String = "pcs", // capsules, grams, scoops
    val daysLeftThreshold: Int = 7,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val estimatedDaysRemaining: Int
        get() = if (dailyDosage > 0) (remainingQuantity / dailyDosage).toInt() else 0
}
