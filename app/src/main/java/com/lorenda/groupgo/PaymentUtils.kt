package com.lorenda.groupgo

import com.lorenda.groupgo.network.PaymentMethodSummary
import com.lorenda.groupgo.network.PaymentMethodsService
import java.net.URLEncoder

/**
 * Helper to fetch cards from backend and return them.
 */
suspend fun fetchPaymentMethodsFromBackend(
    backendBase: String,
    customerId: String? = null,
    uid: String? = null,
    email: String? = null
): Result<List<PaymentMethodSummary>> {
    val url = buildString {
        append(backendBase)
        append("/stripe/payment-methods")
        val params = mutableListOf<String>()
        fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
        if (!customerId.isNullOrBlank()) params.add("customerId=${enc(customerId)}")
        if (!uid.isNullOrBlank()) params.add("uid=${enc(uid)}")
        if (!email.isNullOrBlank()) params.add("email=${enc(email)}")
        if (params.isNotEmpty()) {
            append("?")
            append(params.joinToString("&"))
        }
    }
    return PaymentMethodsService.fetchPaymentMethods(url)
}
