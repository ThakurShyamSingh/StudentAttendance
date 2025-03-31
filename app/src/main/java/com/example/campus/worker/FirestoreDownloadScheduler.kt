package com.example.campus.worker

import android.content.Context
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import androidx.work.ExistingPeriodicWorkPolicy


object FirestoreDownloadScheduler {
    fun scheduleDailyDownload(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<FirestoreDownloadWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.HOURS) // Starts 1 hour after scheduling
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "dailyFirestoreDownload",
            ExistingPeriodicWorkPolicy.KEEP, // ✅ Use ExistingPeriodicWorkPolicy instead
            workRequest
        )

    }
}
