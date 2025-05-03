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

class ArcFaceHelper(private val context: Context) : AutoCloseable {

    private val modelPath = "arcface.tflite"
    private var interpreter: Interpreter? = null
    private val csvFile = File(context.filesDir, "registered_faces.csv")

    init {
        try {
            val model = FileUtil.loadMappedFile(context, modelPath)
            interpreter = Interpreter(model)
        } catch (e: Exception) {
            Log.e("ArcFaceHelper", "Error loading ArcFace model", e)
            interpreter = null
        }
    }

    fun registerFaceMultipleImages(
        name: String,
        bitmaps: List<Bitmap>,
        imagePaths: List<String>,
        onResult: (Boolean, String) -> Unit
    ) {
        if (bitmaps.isEmpty() || imagePaths.isEmpty() || bitmaps.size != imagePaths.size) {
            onResult(false, "Invalid input: Ensure bitmaps and imagePaths are non-empty and of equal size")
            return
        }

        val embeddingsList = mutableListOf<FloatArray>()

        for (bitmap in bitmaps) {
            detectFace(bitmap) { faceBitmap ->
                if (faceBitmap == null) {
                    onResult(false, "No face detected in one or more images")
                    return@detectFace
                }

                getFaceEmbedding(faceBitmap) { embedding ->
                    if (embedding == null) {
                        onResult(false, "Failed to generate embedding for one or more images")
                        return@getFaceEmbedding
                    }
                    embeddingsList.add(embedding)

                    // If all embeddings are generated, average them
                    if (embeddingsList.size == bitmaps.size) {
                        val averagedEmbedding = averageEmbeddings(embeddingsList)
                        saveEmbeddingToCSV(name, imagePaths.joinToString(";"), averagedEmbedding)
                        onResult(true, "Face Successfully Registered with Multiple Images")
                    }
                }
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
                Log.e("ArcFaceHelper", "Face detection failed", it)
                onFaceDetected(null)
            }
    }

    private fun getFaceEmbedding(bitmap: Bitmap, onResult: (FloatArray?) -> Unit) {
        if (interpreter == null) {
            Log.e("ArcFaceHelper", "ArcFace model is not loaded")
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
            Log.e("ArcFaceHelper", "Failed to generate embedding", e)
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
            Log.e("ArcFaceHelper", "Failed to crop face", e)
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
            .add(ResizeOp(112, 112, ResizeOp.ResizeMethod.BILINEAR)) // ArcFace typically uses 112x112 input
            .build()

        val tensorImage = TensorImage(org.tensorflow.lite.DataType.FLOAT32)
        tensorImage.load(ensureBitmapFormat(bitmap))
        return imageProcessor.process(tensorImage).bitmap
    }

    private fun convertBitmapToBuffer(bitmap: Bitmap): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(1 * 112 * 112 * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(112 * 112)
        bitmap.getPixels(intValues, 0, 112, 0, 0, 112, 112)

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

    private fun averageEmbeddings(embeddingsList: List<FloatArray>): FloatArray {
        val embeddingSize = embeddingsList[0].size
        val averagedEmbedding = FloatArray(embeddingSize)

        for (embedding in embeddingsList) {
            for (i in embedding.indices) {
                averagedEmbedding[i] += embedding[i]
            }
        }

        // Divide by the number of embeddings to calculate the average
        for (i in averagedEmbedding.indices) {
            averagedEmbedding[i] /= embeddingsList.size
        }

        return averagedEmbedding
    }

    private fun saveEmbeddingToCSV(name: String, imagePaths: String, embedding: FloatArray) {
        try {
            if (!csvFile.exists()) {
                csvFile.createNewFile()
            }
            val writer = FileWriter(csvFile, true)
            writer.append("$name,$imagePaths,${embedding.joinToString(",")}\n")
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            Log.e("ArcFaceHelper", "Error saving embedding to CSV", e)
        }
    }

    override fun close() {
        interpreter?.close()
    }
}