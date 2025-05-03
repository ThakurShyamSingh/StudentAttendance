package com.example.campus.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

class CameraHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {
    private var imageCapture: ImageCapture? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    private var cameraProvider: ProcessCameraProvider? = null

    fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        try {
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (exc: Exception) {
            Log.e("CameraHelper", "Camera binding failed", exc)
        }
    }

    fun toggleCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        bindCameraUseCases()
    }

    fun captureAndProcessFace(onComplete: (Bitmap?) -> Unit) {
        val imageCapture = imageCapture ?: run {
            onComplete(null)
            return
        }

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
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
            }
        )
    }

    fun captureMultipleImages(
        imageCount: Int,
        onImageCaptured: (Bitmap?) -> Unit,
        onComplete: () -> Unit
    ) {
        var capturedCount = 0

        fun captureNext() {
            captureAndProcessFace { bitmap ->
                onImageCaptured(bitmap)
                capturedCount++
                if (capturedCount < imageCount) {
                    captureNext()
                } else {
                    onComplete()
                }
            }
        }

        captureNext()
    }
}