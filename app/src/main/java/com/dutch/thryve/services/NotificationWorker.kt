package com.dutch.thryve.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dutch.thryve.R
import com.dutch.thryve.domain.model.Supplement
import com.dutch.thryve.ui.screens.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("THRYVE", "NotificationWorker: Starting work")
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val db = FirebaseFirestore.getInstance()

        // 1. Process Supplement Daily Decay & Check Levels
        val lowStockSupplements = mutableListOf<String>()
        try {
            val supplementsQuery = db.collection("users").document(userId).collection("supplements").get().await()
            val supplements = supplementsQuery.documents.mapNotNull { it.toObject(Supplement::class.java) }

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            for (supplement in supplements) {
                var currentRemaining = supplement.remainingQuantity
                
                // Deduct dosage if not already updated today (to prevent double deduction)
                if (supplement.lastUpdated < today) {
                    currentRemaining = (currentRemaining - supplement.dailyDosage).coerceAtLeast(0.0)
                    
                    // Update the supplement in Firebase
                    db.collection("users").document(userId).collection("supplements")
                        .document(supplement.id)
                        .update(
                            mapOf(
                                "remainingQuantity" to currentRemaining,
                                "lastUpdated" to System.currentTimeMillis()
                            )
                        ).await()
                }

                // Check if days remaining is below threshold
                val daysLeft = if (supplement.dailyDosage > 0) (currentRemaining / supplement.dailyDosage).toInt() else 99
                if (daysLeft <= supplement.daysLeftThreshold) {
                    lowStockSupplements.add("${supplement.name} ($daysLeft days left)")
                }
            }
        } catch (e: Exception) {
            Log.e("THRYVE", "Error processing supplements", e)
        }

        // 2. Prepare Notification Content
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "meal_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Meal Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val baseMessage = "Keep your progress on track by logging your meal for the day."
        val supplementMessage = if (lowStockSupplements.isNotEmpty()) {
            "\n\nLow Stock: ${lowStockSupplements.joinToString(", ")}"
        } else ""

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_meal_notification)
            .setContentTitle("Time to Log Your Meal!")
            .setStyle(NotificationCompat.BigTextStyle().bigText(baseMessage + supplementMessage))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Use unique ID so multiple reminders don't overwrite each other
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        Log.d("THRYVE", "NotificationWorker: Notification sent")

        return Result.success()
    }
}
