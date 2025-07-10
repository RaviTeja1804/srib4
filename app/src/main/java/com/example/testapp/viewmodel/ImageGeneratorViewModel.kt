package com.example.testapp.viewmodel

import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testapp.data.model.JigsawImagePart
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ImageGeneratorViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val month: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    private val monthName: String = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())

    var imageBase64: String? = null
        private set
    var imagePrompt: String = ""

    fun getOrGenerateMonthlyImage(onResult: (imageBase64: String?, prompt: String) -> Unit) {
        firestore.collection("jigsaw_images")
            .whereEqualTo("month", month)
            .get()
            .addOnSuccessListener { snapshot ->
                val parts = snapshot.documents
                    .mapNotNull { it.toObject(JigsawImagePart::class.java) }
                    .sortedBy { it.id }

                if (parts.isNotEmpty() && parts.size >= 10) {
                    imageBase64 = parts.joinToString("") { it.base64Part }
                    imagePrompt = parts.first().prompt
                    onResult(imageBase64, imagePrompt)
                } else {
                    generateImageFromAI { base64, prompt ->
                        if (base64 != null) {
                            saveImageInParts(base64, prompt) {
                                imageBase64 = base64
                                imagePrompt = prompt
                                onResult(base64, prompt)
                            }
                        } else {
                            onResult(null, "")
                        }
                    }
                }
            }
            .addOnFailureListener {
                onResult(null, "")
            }
    }

    private fun generateImageFromAI(onComplete: (base64: String?, prompt: String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Step 1: Get prompt from Gemini
                val geminiPrompt = "Give me a short, descriptive prompt (max 10 words ONLY) for a festival or general occasion celebrated mainly in India or worldwide during the month of $monthName."

                val geminiRequestBody = JSONObject().apply {
                    put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().put(
                            JSONObject().put("text", geminiPrompt)
                        ))
                    ))
                }

                val geminiRequest = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=AIzaSyCMOtM9Y0bjx8iWG8JOlU-gT-vDpLUwr_U")
                    .post(RequestBody.create("application/json".toMediaTypeOrNull(), geminiRequestBody.toString()))
                    .addHeader("Content-Type", "application/json")
                    .build()

                val geminiClient = OkHttpClient()
                val geminiResponse = geminiClient.newCall(geminiRequest).execute()

                if (!geminiResponse.isSuccessful) {
                    Log.e("GeminiResponse", "Error: ${geminiResponse.code} - ${geminiResponse.message}")
                    val errorBody = geminiResponse.body?.string()
                    Log.e("GeminiResponse", "Error body: $errorBody")
                    onComplete(null, "")
                    return@launch
                }

                val responseBody = geminiResponse.body?.string()
                Log.d("GeminiResponse", "Success: $responseBody")

                val responseJson = JSONObject(responseBody ?: "")

                // Check if candidates array exists and has content
                if (!responseJson.has("candidates") || responseJson.getJSONArray("candidates").length() == 0) {
                    Log.e("GeminiResponse", "No candidates found in response")
                    onComplete(null, "")
                    return@launch
                }

                val prompt = responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()

                Log.d("GeminiResponse", "Generated prompt: $prompt")

                // Step 2: Generate Image from ClipDrop
                val clipdropBody = JSONObject().put("prompt", prompt)
                val clipdropRequest = Request.Builder()
                    .url("https://clipdrop-api.co/text-to-image/v1")
                    .addHeader("x-api-key", "a686623782aea1f5a02f2fbe4d69d6233d490386a562a7afac5ad07f047012b74dc60ed193f9e8746f25bc8c23ce1fbd")
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create("application/json".toMediaTypeOrNull(), clipdropBody.toString()))
                    .build()

                val clipdropClient = OkHttpClient()
                val clipdropResponse = clipdropClient.newCall(clipdropRequest).execute()

                if (!clipdropResponse.isSuccessful) {
                    Log.e("ClipDropResponse", "Error: ${clipdropResponse.code} - ${clipdropResponse.message}")
                    val errorBody = clipdropResponse.body?.string()
                    Log.e("ClipDropResponse", "Error body: $errorBody")
                    onComplete(null, "")
                    return@launch
                }

                val imageBytes = clipdropResponse.body?.bytes()
                if (imageBytes == null) {
                    Log.e("ClipDropResponse", "No image data received")
                    onComplete(null, "")
                    return@launch
                }

                val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                onComplete(base64Image, prompt)

            } catch (e: Exception) {
                Log.e("IMAGE_GEN_ERROR", "Exception: ${e.message}", e)
                onComplete(null, "")
            }
        }
    }

    private fun saveImageInParts(base64: String, prompt: String, onDone: () -> Unit) {
        val parts = splitBase64(base64, 10)
        val batch = firestore.batch()
        val collection = firestore.collection("jigsaw_images")

        parts.forEachIndexed { index, part ->
            val partData = JigsawImagePart(
                id = index + 1,
                base64Part = part,
                prompt = prompt,
                month = month
            )
            val docRef = collection.document("${month}_part${index + 1}")
            batch.set(docRef, partData)
        }

        batch.commit()
            .addOnSuccessListener { onDone() }
            .addOnFailureListener { e -> Log.e("SAVE_PARTS_ERROR", e.message ?: "Failed to save parts") }
    }

    private fun splitBase64(base64: String, parts: Int): List<String> {
        val size = base64.length
        val chunkSize = size / parts
        return (0 until parts).map { i ->
            val start = i * chunkSize
            val end = if (i == parts - 1) size else (i + 1) * chunkSize
            base64.substring(start, end)
        }
    }
}