package com.example.campus.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class RetinaFaceHelper(private val context: Context) {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .build()

    private val detector = FaceDetection.getClient(options)

    fun detectFace(bitmap: Bitmap, onFaceDetected: (Bitmap?) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val croppedBitmap = cropFace(bitmap, face)
                    onFaceDetected(croppedBitmap)
                } else {
                    Log.e("RetinaFaceHelper", "No face detected")
                    onFaceDetected(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("RetinaFaceHelper", "Face detection failed", e)
                onFaceDetected(null)
            }
    }

    private fun cropFace(bitmap: Bitmap, face: Face): Bitmap? {
        val boundingBox = face.boundingBox
        return try {
            Bitmap.createBitmap(
                bitmap,
                boundingBox.left.coerceAtLeast(0),
                boundingBox.top.coerceAtLeast(0),
                boundingBox.width().coerceAtMost(bitmap.width - boundingBox.left),
                boundingBox.height().coerceAtMost(bitmap.height - boundingBox.top)
            )
        } catch (e: Exception) {
            Log.e("RetinaFaceHelper", "Error cropping face", e)
            null
        }
    }
}