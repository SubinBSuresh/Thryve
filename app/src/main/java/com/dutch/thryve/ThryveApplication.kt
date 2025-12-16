package com.dutch.thryve

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dutch.thryve.services.NotificationWorker
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class ThryveApplication: Application() {
    companion object {
        val LOG_TAG = "THRYVE"
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        createNotificationChannel()
        scheduleMealReminders()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Meal Reminders"
            val descriptionText = "Reminders to log your meals"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("meal_reminder_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleMealReminders() {
        val workManager = WorkManager.getInstance(this)

        // Define constraints (optional)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        // Schedule for 8 AM
        scheduleWork(workManager, constraints, "MealReminder8am", 9, 50)

        // Schedule for 1 PM
        scheduleWork(workManager, constraints, "MealReminder1pm", 13, 50)

        // Schedule for 7 PM
        scheduleWork(workManager, constraints, "MealReminder7pm", 19, 50)
        scheduleWork(workManager, constraints, "MealReminder7pm", 11,50)
    }

    private fun scheduleWork(workManager: WorkManager, constraints: Constraints, uniqueWorkName: String, hourOfDay: Int, minuteOfDay: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minuteOfDay)
            set(Calendar.SECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val initialDelay = target.timeInMillis - now.timeInMillis

        val periodicWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            24, TimeUnit.HOURS
        )
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .setConstraints(constraints)
        .build()

        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.REPLACE, // or KEEP, depending on desired behavior
            periodicWorkRequest
        )
    }
}
