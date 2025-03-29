package com.example.campus.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object CameraHelper {
    fun setupCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onImageCaptureReady: (ImageCapture) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            try {
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)

                // Pass ImageCapture instance back to the caller
                onImageCaptureReady(imageCapture)
            } catch (exc: Exception) {
                Log.e("CameraHelper", "Camera binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun captureAndProcessFace(
        context: Context,
        imageCapture: ImageCapture,
        name: String, // User input name for the registered face
        onComplete: (Boolean, String) -> Unit
    ) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        val fileName = "face_$timeStamp.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Campus")
            }
        }

        val imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        imageUri?.let { uri ->
            val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri)
            if (outputStream == null) {
                onComplete(false, "Failed to open output stream")
                return
            }
            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputStream).build()

            imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val bitmap = FaceDetectionHelper.loadBitmapFromUri(context, uri)
                        if (bitmap != null) {
                            // Detect face first before proceeding
                            FaceDetectionHelper.detectFace(bitmap) { faceDetected, detectionMessage ->
                                if (faceDetected) {
                                    processCapturedFace(context, name, bitmap, uri, onComplete)
                                } else {
                                    onComplete(false, detectionMessage)
                                }
                            }
                        } else {
                            onComplete(false, "Failed to load image")
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CameraHelper", "Image capture failed", exception)
                        onComplete(false, "Image capture failed")
                    }
                }
            )
        } ?: onComplete(false, "Failed to create media store entry")
    }


    private fun processCapturedFace(
        context: Context,
        name: String,
        bitmap: Bitmap,
        uri: Uri,
        onComplete: (Boolean, String) -> Unit
    ) {
        val faceNetHelper = FaceNetHelper(context)
        faceNetHelper.registerFace(name, bitmap, uri.toString()) { success, message ->
        // Close the interpreter after use
            onComplete(success, message)
        }
    }
}
