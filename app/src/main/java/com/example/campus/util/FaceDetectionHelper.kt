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
    fun detectFace(bitmap: Bitmap, onComplete: (Boolean, String) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
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
                    onComplete(true, "Face Successfully Registered")
                } else {
                    onComplete(false, "No face detected")
                }
            }
            .addOnFailureListener {
                onComplete(false, "Face detection failed")
            }
    }

    fun loadBitmapFromUri(context: Context, imageUri: Uri): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            val bitmap = ImageDecoder.decodeBitmap(source)

            // Ensure the bitmap is ARGB_8888 format
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        } catch (e: Exception) {
            Log.e("FaceDetectionHelper", "Error loading bitmap", e)
            null
        }
    }

}
