package com.example.campus.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

object FaceDetectionHelper {

    /**
     * Detects a face in the given bitmap and returns the result through a callback.
     */
    fun isFaceDetected(bitmap: Bitmap, rotationDegrees: Int, onComplete: (Boolean, String) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, rotationDegrees)

        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    Log.d("FaceDetectionHelper", "Face detected: true")
                    onComplete(true, "Face Successfully Registered")
                } else {
                    Log.d("FaceDetectionHelper", "Face detected: false")
                    onComplete(false, "No face detected")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FaceDetectionHelper", "Face detection failed", e)
                onComplete(false, "Face detection failed")
            }
    }

    /**
     * Loads a bitmap from a given URI.
     */
    fun loadBitmapFromUri(context: Context, imageUri: Uri): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            val bitmap = ImageDecoder.decodeBitmap(source)

            // Ensure the bitmap is in ARGB_8888 format
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        } catch (e: Exception) {
            Log.e("FaceDetectionHelper", "Error loading bitmap", e)
            null
        }
    }
}
