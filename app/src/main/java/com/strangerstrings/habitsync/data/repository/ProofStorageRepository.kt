package com.strangerstrings.habitsync.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.strangerstrings.habitsync.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ProofStorageRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val apiBaseUrl: String = BuildConfig.SIGNED_UPLOAD_API_BASE_URL,
) {
    suspend fun uploadProofImage(
        habitId: String,
        proofImageBytes: ByteArray,
    ): String = withContext(Dispatchers.IO) {
        val user = firebaseAuth.currentUser ?: error("No authenticated user found.")
        val idToken = user.getIdToken(true).await().token
            ?: error("Unable to acquire Firebase ID token.")

        val signRequestBody = JSONObject()
            .put("habitId", habitId)
            .put("contentType", JPEG_MIME_TYPE)
            .toString()
            .toRequestBody(JSON_MIME_TYPE.toMediaType())

        val signRequest = Request.Builder()
            .url("${apiBaseUrl.trimEnd('/')}/proof-upload-url")
            .addHeader("Authorization", "Bearer $idToken")
            .post(signRequestBody)
            .build()

        val signResponse = httpClient.newCall(signRequest).execute()
        if (!signResponse.isSuccessful) {
            error("Failed to request upload URL: ${signResponse.code}")
        }
        val signBody = signResponse.body?.string().orEmpty()
        val signPayload = JSONObject(signBody)
        val uploadUrl = signPayload.optString("uploadUrl")
        val fileUrl = signPayload.optString("fileUrl")
        if (uploadUrl.isBlank() || fileUrl.isBlank()) {
            error("Invalid upload response from server.")
        }

        val uploadRequest = Request.Builder()
            .url(uploadUrl)
            .put(proofImageBytes.toRequestBody(JPEG_MIME_TYPE.toMediaType()))
            .build()

        val uploadResponse = httpClient.newCall(uploadRequest).execute()
        if (!uploadResponse.isSuccessful) {
            error("Failed to upload proof image: ${uploadResponse.code}")
        }

        fileUrl
    }

    private companion object {
        const val JSON_MIME_TYPE = "application/json"
        const val JPEG_MIME_TYPE = "image/jpeg"
    }
}
