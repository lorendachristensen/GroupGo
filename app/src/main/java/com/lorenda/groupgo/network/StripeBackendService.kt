package com.lorenda.groupgo.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class SetupIntentResponse(
    val customerId: String,
    val setupIntentClientSecret: String,
    val ephemeralKey: String,
    val publishableKey: String
)

object StripeBackendService {
    private const val TAG = "StripeBackendService"

    suspend fun createSetupIntent(
        backendUrl: String,
        uid: String,
        email: String
    ): Result<SetupIntentResponse> {
        return try {
            // Perform network I/O off the main thread
            withContext(Dispatchers.IO) {
                val url = URL(backendUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }

                val payload = JSONObject().apply {
                    put("uid", uid)
                    put("email", email)
                }.toString()

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(payload)
                }

                val status = conn.responseCode
                val responseText = if (status in 200..299) {
                    conn.inputStream.bufferedReader().use(BufferedReader::readText)
                } else {
                    conn.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                }

                if (status !in 200..299) {
                    Log.e(TAG, "Backend error: $status $responseText")
                    return@withContext Result.failure(Exception("Backend error: $status"))
                }

                val json = JSONObject(responseText)
                val data = SetupIntentResponse(
                    customerId = json.getString("customerId"),
                    setupIntentClientSecret = json.getString("setupIntentClientSecret"),
                    ephemeralKey = json.getString("ephemeralKey"),
                    publishableKey = json.getString("publishableKey")
                )
                Result.success(data)
            }
        } catch (e: Exception) {
            Log.e(TAG, "createSetupIntent failed", e)
            Result.failure(e)
        }
    }
}
