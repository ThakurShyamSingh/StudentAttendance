package com.example.campus.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.File
//import java.io.FileWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FaceNetHelper(private val context: Context) : AutoCloseable {

    private val modelPath = "facenet.tflite"
    private var interpreter: Interpreter? = null
    private val csvFile = File(context.filesDir, "registered_faces.csv")
    private val faceMatchThreshold = 0.6f  // Lower value = stricter matching

    init {
        try {
            val model = FileUtil.loadMappedFile(context, modelPath)
            interpreter = Interpreter(model)
            Log.d("FaceNetHelper", "FaceNet model loaded successfully")
        } catch (e: Exception) {
            Log.e("FaceNetHelper", "Error loading FaceNet model", e)
            interpreter = null
        }
    }

    /**
     * Generates a 512-dimensional face embedding from the given bitmap.
     */
    fun generateEmbedding(bitmap: Bitmap): FloatArray? {
        if (interpreter == null) {
            Log.e("FaceNetHelper", "FaceNet model is not loaded")
            return null
        }

        return try {
            val processedBitmap = preprocessImage(bitmap)
            val inputBuffer = convertBitmapToBuffer(processedBitmap)
            val outputArray = Array(1) { FloatArray(512) }
            interpreter?.run(inputBuffer, outputArray)
            Log.d("FaceNetHelper", "Embedding generated successfully")
            outputArray[0]
        } catch (e: Exception) {
            Log.e("FaceNetHelper", "Failed to generate embedding", e)
            null
        }
    }

    /**
     * Matches the given embedding with registered faces and returns the matched name, or null if no match is found.
     */
    fun recognizeFace(embedding: FloatArray): String? {
        val registeredFaces = loadRegisteredFaces()
        var bestMatch: String? = null
        var bestDistance = Float.MAX_VALUE

        for (faceData in registeredFaces) {
            val distance = calculateEuclideanDistance(embedding, faceData.embedding)
            Log.d("FaceNetHelper", "Comparing with ${faceData.name}, Distance: $distance")

            if (distance < bestDistance && distance < faceMatchThreshold) {
                bestDistance = distance
                bestMatch = faceData.name
            }
        }

        return if (bestMatch != null) {
            Log.d("FaceNetHelper", "Face match found: $bestMatch")
            bestMatch
        } else {
            Log.d("FaceNetHelper", "No matching face found")
            null
        }
    }

    /**
     * Loads registered face embeddings from CSV.
     */
    private fun loadRegisteredFaces(): List<FaceData> {
        val faceList = mutableListOf<FaceData>()

        if (!csvFile.exists()) return faceList

        csvFile.forEachLine { line ->
            val parts = line.split(",")
            if (parts.size > 2) {
                val name = parts[0]
                val embedding = parts.drop(1).map { it.toFloat() }.toFloatArray()
                faceList.add(FaceData(name, embedding))
            }
        }

        Log.d("FaceNetHelper", "Loaded ${faceList.size} registered faces")
        return faceList
    }

    /**
     * Calculates Euclidean distance between two embeddings.
     */
    private fun calculateEuclideanDistance(embedding1: FloatArray, embedding2: FloatArray): Float {
        return embedding1.zip(embedding2)
            .sumOf { (a, b) -> ((a - b) * (a - b)).toDouble() }
            .toFloat()
    }

    /**
     * Preprocesses an image for FaceNet.
     */
    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(160, 160, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        val tensorImage = TensorImage(org.tensorflow.lite.DataType.FLOAT32)
        tensorImage.load(bitmap)
        return imageProcessor.process(tensorImage).bitmap
    }

    /**
     * Converts a Bitmap to a ByteBuffer for FaceNet input.
     */
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

    override fun close() {
        try {
            interpreter?.close()
            Log.d("FaceNetHelper", "Interpreter closed successfully")
        } catch (e: Exception) {
            Log.e("FaceNetHelper", "Error closing FaceNet Interpreter", e)
        }
    }

}

/**
 * Data class to store registered face embeddings.
 */
data class FaceData(val name: String, val embedding: FloatArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceData) return false
        return name == other.name && embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int {
        return 31 * name.hashCode() + embedding.contentHashCode()
    }
}

