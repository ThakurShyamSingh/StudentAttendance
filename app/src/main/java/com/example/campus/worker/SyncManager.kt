package com.example.campus.worker

import android.content.Context
import android.util.Log
import com.example.campus.ui.FirestoreDownloader
import kotlinx.coroutines.*
import com.example.campus.ui.FirestoreUploader

object SyncManager {
    private var syncJob: Job? = null

    fun startAutoSync(context: Context) {
        if (syncJob?.isActive == true) return

        syncJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {

//                FirestoreDownloader.downloadJSONFromFirestore(
//                    context = context,
//                    onProgress = {}, // You can show sync progress in UI if needed
//                    onDownloading = {}, // Optional UI hook
//                    onDownloadComplete = {
//
//                    } // Optional UI hook
//                )
//                FirestoreUploader.uploadJSONToFirestore(
//                    context = context,
//                    onProgress = {}, // You can show sync progress in UI if needed
//                    onUploading = {}, // Optional UI hook
//                    onUploadComplete = {} // Optional UI hook
//                )


                Log.d("SyncManager", "Auto sync completed")
                delay(1 * 60 * 1000L) // sync every 5 minutes
            }
        }
    }

    fun stopAutoSync() {
        syncJob?.cancel()
        syncJob = null
    }
}
