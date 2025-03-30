package com.example.campus.util

import android.content.Context
import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.net.Uri
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors

class CameraHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {
    private var imageCapture: ImageCapture? = null
//    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            try {
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("CameraHelper", "Camera binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun captureAndProcessFace(onComplete: (Bitmap?) -> Unit) {
        val imageCapture = imageCapture ?: run {
            onComplete(null)
            return
        }

        imageCapture.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = image.toBitmap().copy(Bitmap.Config.ARGB_8888, true)
                image.close()

                FaceDetectionHelper.isFaceDetected(bitmap, image.imageInfo.rotationDegrees) { faceDetected, _ ->
                    if (faceDetected) {
                        onComplete(bitmap)
                    } else {
                        onComplete(null)
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraHelper", "Image capture failed", exception)
                onComplete(null)
            }
        })
    }

//    private fun ImageProxy.toBitmap(): Bitmap {
//        val buffer = planes[0].buffer
//        val bytes = ByteArray(buffer.remaining())
//        buffer.get(bytes)
//        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//    }
}
