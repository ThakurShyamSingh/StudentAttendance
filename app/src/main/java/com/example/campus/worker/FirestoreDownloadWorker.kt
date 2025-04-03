package com.example.campus.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.campus.ui.FirestoreDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirestoreDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                FirestoreDownloader.downloadJSONFromFirestore(context,
                    onDownloading = {},
                    onDownloadComplete = { success ->
                        if (success) {
                            Log.d("FirestoreDownloadWorker", "Student data updated successfully!")
                        } else {
                            Log.e("FirestoreDownloadWorker", "Failed to update student data!")
                        }
                    }
                )
                Result.success()
            } catch (e: Exception) {
                Log.e("FirestoreDownloadWorker", "Error in background sync", e)
                Result.failure()
            }
        }
    }
}