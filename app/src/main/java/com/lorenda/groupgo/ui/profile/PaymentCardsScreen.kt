package com.lorenda.groupgo.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentCardsScreen(
    onBackClick: () -> Unit = {},
    uid: String,
    email: String
) {
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var paymentMethods by remember { mutableStateOf<List<PaymentMethodDisplay>>(emptyList()) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val paymentSheet = rememberPaymentSheet { result ->
        when (result) {
            is PaymentSheetResult.Completed -> {
                statusMessage = "Payment method saved"
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val cards = PaymentCardsService().listPaymentMethods(uid, email)
                        withContext(Dispatchers.Main) {
                            paymentMethods = cards
                            ensureDefault(cards, uid, email, coroutineScope)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            statusMessage = "Error refreshing cards: ${e.message}"
                        }
                    }
                }
            }
            is PaymentSheetResult.Canceled -> statusMessage = "Canceled"
            is PaymentSheetResult.Failed -> statusMessage = "Failed: ${result.error.localizedMessage}"
            else -> statusMessage = "Unknown result"
        }
    }

    fun launchPaymentSheet(
        customerId: String,
        ephemeralKey: String,
        clientSecret: String,
        publishableKey: String
    ) {
        PaymentConfiguration.init(context, publishableKey)
        paymentSheet.presentWithSetupIntent(
            clientSecret,
            PaymentSheet.Configuration(
                merchantDisplayName = "GroupGo",
                customer = PaymentSheet.CustomerConfiguration(
                    id = customerId,
                    ephemeralKeySecret = ephemeralKey
                )
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Cards") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Link a card to your account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    isLoading = true
                    statusMessage = null
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val response = PaymentCardsService().createSetupIntent(uid, email)
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                launchPaymentSheet(
                                    customerId = response.customerId,
                                    ephemeralKey = response.ephemeralKey,
                                    clientSecret = response.setupIntentClientSecret,
                                    publishableKey = response.publishableKey
                                )
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                statusMessage = "Error: ${e.message}"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Link Payment Method")
                }
            }
            statusMessage?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            Divider()
            Text(
                text = "Linked Cards",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (paymentMethods.isEmpty()) {
                Text("No linked cards yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(paymentMethods) { card ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text("${card.brand.uppercase()} •••• ${card.last4}", fontWeight = FontWeight.SemiBold)
                                        Text("Exp: ${card.expMonth}/${card.expYear}", style = MaterialTheme.typography.bodySmall)
                                        if (card.isDefault) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Default",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("Default", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        if (!card.isDefault) {
                                            OutlinedButton(
                                                onClick = {
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        try {
                                                            PaymentCardsService().setDefaultPaymentMethod(uid, email, card.id)
                                                            val cards = PaymentCardsService().listPaymentMethods(uid, email)
                                                            withContext(Dispatchers.Main) {
                                                                paymentMethods = cards
                                                                statusMessage = "Default updated"
                                                                ensureDefault(cards, uid, email, coroutineScope)
                                                            }
                                                        } catch (e: Exception) {
                                                            withContext(Dispatchers.Main) {
                                                                statusMessage = "Error setting default: ${e.message}"
                                                            }
                                                        }
                                                    }
                                                }
                                            ) {
                                                Text("Make Default")
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    try {
                                                        PaymentCardsService().deletePaymentMethod(uid, email, card.id)
                                                        val cards = PaymentCardsService().listPaymentMethods(uid, email)
                                                        withContext(Dispatchers.Main) {
                                                            paymentMethods = cards
                                                            statusMessage = "Card deleted"
                                                            ensureDefault(cards, uid, email, coroutineScope)
                                                        }
                                                    } catch (e: Exception) {
                                                        withContext(Dispatchers.Main) {
                                                            statusMessage = "Error deleting: ${e.message}"
                                                        }
                                                    }
                                                }
                                            }
                                        ) {
                                            Text("Delete")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(uid, email) {
        isLoading = true
        statusMessage = null
        try {
            val cards = withContext(Dispatchers.IO) {
                PaymentCardsService().listPaymentMethods(uid, email)
            }
            paymentMethods = cards
            ensureDefault(cards, uid, email, coroutineScope)
        } catch (e: Exception) {
            statusMessage = "Error loading cards: ${e.message}"
        } finally {
            isLoading = false
        }
    }
}

private data class SetupIntentResponse(
    val customerId: String,
    val ephemeralKey: String,
    val setupIntentClientSecret: String,
    val publishableKey: String
)

private data class PaymentMethodDisplay(
    val id: String,
    val brand: String,
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
    val isDefault: Boolean
)

private class PaymentCardsService(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun createSetupIntent(uid: String, email: String): SetupIntentResponse {
        val json = JSONObject()
            .put("uid", uid)
            .put("email", email)
        val body = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://10.0.2.2:4242/stripe/setup-intent")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
            val respBody = response.body?.string() ?: throw IllegalStateException("Empty body")
            val obj = JSONObject(respBody)
            return SetupIntentResponse(
                customerId = obj.getString("customerId"),
                ephemeralKey = obj.getString("ephemeralKey"),
                setupIntentClientSecret = obj.getString("setupIntentClientSecret"),
                publishableKey = obj.getString("publishableKey")
            )
        }
    }

    fun listPaymentMethods(uid: String, email: String): List<PaymentMethodDisplay> {
        val json = JSONObject()
            .put("uid", uid)
            .put("email", email)
        val body = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://10.0.2.2:4242/stripe/payment-methods")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
            val respBody = response.body?.string() ?: throw IllegalStateException("Empty body")
            val obj = JSONObject(respBody)
            val defaultId = obj.optString("defaultPaymentMethod", "")
            val list = obj.getJSONArray("paymentMethods")
            return List(list.length()) { idx ->
                val pm = list.getJSONObject(idx)
                val id = pm.getString("id")
                PaymentMethodDisplay(
                    id = id,
                    brand = pm.optString("brand"),
                    last4 = pm.optString("last4"),
                    expMonth = pm.optInt("expMonth"),
                    expYear = pm.optInt("expYear"),
                    isDefault = id == defaultId || pm.optBoolean("isDefault", false)
                )
            }
        }
    }

    fun setDefaultPaymentMethod(uid: String, email: String, paymentMethodId: String) {
        val json = JSONObject()
            .put("uid", uid)
            .put("email", email)
            .put("paymentMethodId", paymentMethodId)
        val body = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://10.0.2.2:4242/stripe/payment-methods/default")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
        }
    }

    fun deletePaymentMethod(uid: String, email: String, paymentMethodId: String) {
        val json = JSONObject()
            .put("uid", uid)
            .put("email", email)
            .put("paymentMethodId", paymentMethodId)
        val body = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://10.0.2.2:4242/stripe/payment-methods/delete")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
        }
    }
}

private fun ensureDefault(
    cards: List<PaymentMethodDisplay>,
    uid: String,
    email: String,
    scope: CoroutineScope
) {
    val hasDefault = cards.any { it.isDefault }
    if (cards.size == 1 && !hasDefault) {
        val only = cards.first()
        scope.launch(Dispatchers.IO) {
            try {
                PaymentCardsService().setDefaultPaymentMethod(uid, email, only.id)
            } catch (_: Exception) {
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PaymentCardsScreenPreview() {
    MaterialTheme {
        PaymentCardsScreen(uid = "uid123", email = "test@example.com")
    }
}
