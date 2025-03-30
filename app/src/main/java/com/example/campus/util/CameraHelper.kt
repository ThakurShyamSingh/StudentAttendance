package com.example.campus.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.util.Log
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private lateinit var imageCapture: ImageCapture
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * Initializes the camera and binds it to the lifecycle.
     */
    fun setupCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture?.addListener({
            try {
                val cameraProvider = cameraProviderFuture?.get()
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                imageCapture = ImageCapture.Builder()
                    .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                    .build()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Bind camera lifecycle
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)

                Log.d("CameraHelper", "Camera setup successful")
            } catch (e: Exception) {
                Log.e("CameraHelper", "Error setting up camera", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Captures an image, detects a face, and returns the face bitmap if detected.
     */
    fun captureAndProcessFace(onResult: (Bitmap?) -> Unit) {
        val captureInstance = imageCapture ?: run {
            Log.e("CameraHelper", "ImageCapture not initialized")
            onResult(null)
            return
        }

        captureInstance.takePicture(
            executorService,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val rotationDegrees = image.imageInfo.rotationDegrees
                    val bitmap = imageProxyToBitmap(image)
                    image.close()

                    FaceDetectionHelper.isFaceDetected(bitmap, rotationDegrees) { isDetected, message ->
                        if (isDetected) {
                            Log.d("CameraHelper", "Face detected in image: $message")
                            onResult(bitmap)
                        } else {
                            Log.d("CameraHelper", "No face detected in image: $message")
                            onResult(null)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraHelper", "Error capturing image", exception)
                    onResult(null)
                }
            }
        )
    }

    /**
     * Converts ImageProxy to Bitmap.
     */
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        if (image.format != ImageFormat.YUV_420_888 || image.planes.size < 3) {
            Log.e("CameraHelper", "Invalid image format: Expected YUV_420_888 but got ${image.format}")
            return Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888) // Return an empty bitmap to avoid crash
        }

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copy Y, U, and V planes into NV21 array
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val outputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, outputStream)
        val byteArray = outputStream.toByteArray()

        var bitmap = android.graphics.BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

        // Rotate the bitmap based on `rotationDegrees`
        val matrix = android.graphics.Matrix()
        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        return bitmap
    }



    /**
     * Shuts down the camera executor.
     */
    fun shutdown() {
        executorService.shutdown()
    }
}
