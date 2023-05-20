package com.guardianconnect

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException

class DnsOverHttpsClient(private val dohUrl: String) {
    private val TAG = "DnsOverHttpsClient"

    private val client = OkHttpClient()

    fun resolveDnsOverHttps(request: ByteArray): ByteArray {
        val requestBody = RequestBody.create("application/dns-message".toMediaType(), request)

        val httpRequest = Request.Builder()
            .url(dohUrl)
            .post(requestBody)
            .build()

        try {
            val httpResponse = client.newCall(httpRequest).execute()
            if (httpResponse.isSuccessful) {
                val responseBody = httpResponse.body
                if (responseBody != null) {
                    return responseBody.bytes()
                }
            } else {
                Log.e(TAG, "DNS over HTTPS request failed with code: ${httpResponse.code}")
            }
        } catch (e: IOException) {
            Log.e(TAG, "DNS over HTTPS request failed", e)
        }

        return request // Pass through the original DNS request on failure
    }
}
