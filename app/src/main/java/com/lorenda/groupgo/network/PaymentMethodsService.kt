package com.lorenda.groupgo.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

data class PaymentMethodSummary(
    val id: String,
    val brand: String?,
    val last4: String?,
    val expMonth: Int?,
    val expYear: Int?
)

object PaymentMethodsService {
    private const val TAG = "PaymentMethodsService"

    suspend fun fetchPaymentMethods(
        backendUrl: String
    ): Result<List<PaymentMethodSummary>> {
        return try {
            withContext(Dispatchers.IO) {
                val url = URL(backendUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
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
                val pmArray = json.optJSONArray("paymentMethods") ?: return@withContext Result.success(emptyList())
                val list = mutableListOf<PaymentMethodSummary>()
                for (i in 0 until pmArray.length()) {
                    val pm = pmArray.getJSONObject(i)
                    list.add(
                        PaymentMethodSummary(
                            id = pm.optString("id"),
                            brand = pm.optString("brand", null),
                            last4 = pm.optString("last4", null),
                            expMonth = if (pm.has("exp_month")) pm.optInt("exp_month") else null,
                            expYear = if (pm.has("exp_year")) pm.optInt("exp_year") else null
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchPaymentMethods failed", e)
            Result.failure(e)
        }
    }
}

