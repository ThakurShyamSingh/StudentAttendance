package com.example.campus.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.File
import java.io.FileWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FaceNetHelper(private val context: Context) : AutoCloseable {

    private val modelPath = "facenet.tflite"
    private var interpreter: Interpreter? = null
    private val csvFile = File(context.filesDir, "registered_faces.csv")

    init {
        try {
            val model = FileUtil.loadMappedFile(context, modelPath)
            interpreter = Interpreter(model)
        } catch (e: Exception) {
            Log.e("FaceNetHelper", "Error loading FaceNet model", e)
            interpreter = null
        }
    }

    fun registerFace(name: String, bitmap: Bitmap, imagePath: String, onResult: (Boolean, String) -> Unit) {
        detectFace(bitmap) { faceBitmap ->
            if (faceBitmap == null) {
                onResult(false, "No face detected")
                return@detectFace
            }
            try {
                getFaceEmbedding(faceBitmap) { embedding ->
                    if (embedding == null) {
                        onResult(false, "Failed to generate embedding")
                        return@getFaceEmbedding
                    }
                    saveEmbeddingToCSV(name, imagePath, embedding)
                    onResult(true, "Face Successfully Registered")
                }
            } catch (e: Exception) {
                Log.e("FaceNetHelper", "Error processing face", e)
                onResult(false, "Error processing face")
            }
        }
    }

    private fun detectFace(bitmap: Bitmap, onFaceDetected: (Bitmap?) -> Unit) {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .build()

        val detector = FaceDetection.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val faceBitmap = cropFace(bitmap, face)?.let { ensureBitmapFormat(it) }
                    onFaceDetected(faceBitmap)
                } else {
                    onFaceDetected(null)
                }
            }
            .addOnFailureListener {
                Log.e("FaceNetHelper", "Face detection failed", it)
                onFaceDetected(null)
            }
    }

    private fun getFaceEmbedding(bitmap: Bitmap, onResult: (FloatArray?) -> Unit) {
        if (interpreter == null) {
            Log.e("FaceNetHelper", "FaceNet model is not loaded")
            onResult(null)
            return
        }

        try {
            val processedBitmap = preprocessImage(bitmap)
            val inputBuffer = convertBitmapToBuffer(processedBitmap)
            val outputArray = Array(1) { FloatArray(512) }
            interpreter?.run(inputBuffer, outputArray)
            onResult(outputArray[0])
        } catch (e: Exception) {
            Log.e("FaceNetHelper", "Failed to generate embedding", e)
            onResult(null)
        }
    }

    private fun cropFace(bitmap: Bitmap, face: Face): Bitmap? {
        val bounds = face.boundingBox
        val x = bounds.left.coerceAtLeast(0)
        val y = bounds.top.coerceAtLeast(0)
        val width = bounds.width().coerceAtMost(bitmap.width - x)
        val height = bounds.height().coerceAtMost(bitmap.height - y)

        return try {
            Bitmap.createBitmap(bitmap, x, y, width, height)
        } catch (e: Exception) {
            Log.e("FaceNetHelper", "Failed to crop face", e)
            null
        }
    }

    private fun ensureBitmapFormat(bitmap: Bitmap): Bitmap {
        return if (bitmap.config == Bitmap.Config.ARGB_8888) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(160, 160, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        val tensorImage = TensorImage(org.tensorflow.lite.DataType.FLOAT32)
        tensorImage.load(ensureBitmapFormat(bitmap))
        return imageProcessor.process(tensorImage).bitmap
    }

    private fun convertBitmapToBuffer(bitmap: Bitmap): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(1 * 160 * 160 * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(160 * 160)
        bitmap.getPixels(intValues, 0, 160, 0, 0, 160, 160)

        for (pixelValue in intValues) {
            val r = (pixelValue shr 16 and 0xFF) / 255.0f
            val g = (pixelValue shr 8 and 0xFF) / 255.0f
            val b = (pixelValue and 0xFF) / 255.0f
            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }
        return inputBuffer
    }

    private fun saveEmbeddingToCSV(name: String, imagePath: String, embedding: FloatArray) {
        try {
            if (!csvFile.exists()) {
                csvFile.createNewFile()
            }
            val writer = FileWriter(csvFile, true)
            Log.i("Captured Details ","$name,$imagePath,${embedding.joinToString(",")}\n")
            writer.append("$name,$imagePath,${embedding.joinToString(",")}\n")
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            Log.e("FaceNetHelper", "Error saving embedding to CSV", e)
        }
    }

    override fun close() {
        interpreter?.close()
    }
}
