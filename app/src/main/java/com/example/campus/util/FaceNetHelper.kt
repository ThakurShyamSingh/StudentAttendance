package com.example.campus.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FaceNetHelper(private val context: Context) : AutoCloseable {

    private val modelPath = "facenet.tflite"
    private var interpreter: Interpreter? = null
    private val jsonFile = File(context.filesDir, "registered_faces.json")
    private val faceMatchThreshold = 0.6f

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

    fun recognizeFace(embedding: FloatArray): Pair<String, String>? {
        val registeredFaces = loadRegisteredFaces()
        var bestMatch: Pair<String, String>? = null
        var bestDistance = Float.MAX_VALUE

        for (faceData in registeredFaces) {
            val distance = calculateEuclideanDistance(embedding, faceData.embedding)
            Log.d("FaceNetHelper", "Comparing with ${faceData.name} (${faceData.rollNumber}), Distance: $distance")

            if (distance < bestDistance && distance < faceMatchThreshold) {
                bestDistance = distance
                bestMatch = Pair(faceData.name, faceData.rollNumber)
            }
        }

        return if (bestMatch != null) {
            Log.d("FaceNetHelper", "Face match found: ${bestMatch.first} (${bestMatch.second})")
            bestMatch
        } else {
            Log.d("FaceNetHelper", "No matching face found")
            null
        }
    }

    private fun loadRegisteredFaces(): List<FaceData> {
        val faceList = mutableListOf<FaceData>()

        if (!jsonFile.exists()) return faceList

        return try {
            val jsonString = jsonFile.readText()
            val jsonObject = JSONObject(jsonString)
            val studentArray = jsonObject.getJSONArray("StudentDetails")

            for (i in 0 until studentArray.length()) {
                val student = studentArray.getJSONObject(i)
                val name = student.getString("name")
                val rollNumber = student.getString("rollNumber")
                val embeddingArray = student.getJSONArray("embedding")
                val embedding = FloatArray(embeddingArray.length()) { j -> embeddingArray.getDouble(j).toFloat() }

                if (embedding.size == 512) {
                    faceList.add(FaceData(name, rollNumber, embedding))
                } else {
                    Log.e("FaceNetHelper", "Invalid embedding format, skipping: $student")
                }
            }

            Log.d("FaceNetHelper", "Loaded ${faceList.size} registered faces from JSON")
            faceList
        } catch (e: Exception) {
            Log.e("FaceNetHelper", "Error reading JSON file", e)
            faceList
        }
    }

    private fun calculateEuclideanDistance(embedding1: FloatArray, embedding2: FloatArray): Float {
        return embedding1.zip(embedding2)
            .sumOf { (a, b) -> ((a - b) * (a - b)).toDouble() }
            .toFloat()
    }

    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(160, 160, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        val tensorImage = TensorImage(org.tensorflow.lite.DataType.FLOAT32)
        tensorImage.load(bitmap)
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
 * Data class to store registered face embeddings along with name and roll number.
 */
data class FaceData(val name: String, val rollNumber: String, val embedding: FloatArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceData) return false
        return name == other.name && rollNumber == other.rollNumber && embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int {
        return 31 * (31 * name.hashCode() + rollNumber.hashCode()) + embedding.contentHashCode()
    }
}
