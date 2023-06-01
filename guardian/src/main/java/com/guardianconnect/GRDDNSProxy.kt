package com.guardianconnect

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.dnsoverhttps.DnsOverHttps
import org.json.JSONObject
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer

class GRDDNSProxy : VpnService() {
    private val TAG = "GRDDNSProxy"
    private val dnsResolutionMethod = DnsResolutionMethod.PLAIN_TEXT

    private enum class DnsResolutionMethod {
        PLAIN_TEXT,
        DNS_OVER_HTTPS,
        DNS_OVER_TLS
    }

    private val blockedHostnames = listOf(
        "reddit.com",
        "github.com",
        "guardianapp.com",
        "apple.com",
        "google.com"
    )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        if (!isVpnRunning) {
            establishVpnConnection()
            isVpnRunning = true
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        stopVpnConnection()
    }

    private val handler = Handler(Looper.getMainLooper())

    private fun establishVpnConnection() {
        Thread {
            try {
                Log.d(TAG, "Establishing VPN connection...")
                val vpnBuilder = getDNSviaSysProps(applicationContext)?.get(0)?.let {
                    Builder()
                        .setMtu(3000)
                        .addAddress("10.0.2.15", 24)
                        .addDnsServer(it)
                }

                for (hostname in blockedHostnames) {
                    try {
                        val ipAddress = resolveDns(hostname)
                        ipAddress?.let {
                            if (isIPv4(ipAddress))
                                ipAddress.let { it1 -> vpnBuilder?.addRoute(it1, 32) }
                            else if (isIPv6(ipAddress))
                                ipAddress.let { it1 -> vpnBuilder?.addRoute(it1, 128) }
                            else
                                return@let
                        }
                    } catch (e: Exception) {
                        e.message?.let { errorMessage ->
                            handler.post {
                                Log.e(TAG, errorMessage)
                            }
                        }
                    }
                }

                parcelFileDescriptor = vpnBuilder?.establish()

                handler.post {
                    Log.d(TAG, "VPN connection established")
                    startVpnConnection()
                }
            } catch (e: Exception) {
                e.message?.let { errorMessage ->
                    handler.post {
                        Log.e(TAG, errorMessage)
                    }
                }
            }
        }.start()
    }

    fun isIPv4(address: String): Boolean {
        return try {
            InetAddress.getByName(address).isReachable(1000) && InetAddress.getByName(address) is Inet4Address
        } catch (e: UnknownHostException) {
            false
        }
    }

    fun isIPv6(address: String): Boolean {
        return try {
            InetAddress.getByName(address).isReachable(1000) && InetAddress.getByName(address) is Inet6Address
        } catch (e: UnknownHostException) {
            false
        }
    }


    private fun getDNSviaSysProps(context: Context): Array<String?>? {
        return try {
            val result = HashSet<String?>()
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val dnsServers = connectivityManager.getLinkProperties(connectivityManager.activeNetwork)
                ?.dnsServers
            dnsServers?.let {
                for (dnsServer in it) {
                    val value = dnsServer.hostAddress
                    if (value != null && value != "") {
                        result.add(value)
                    }
                }
            }
            result.toTypedArray()
        } catch (e: Exception) {
            arrayOfNulls(0)
        }
    }


    private fun startVpnConnection() {
        val protectFd = parcelFileDescriptor?.fd
        val vpnInput = FileInputStream(parcelFileDescriptor?.fileDescriptor)
        val vpnOutput = FileOutputStream(parcelFileDescriptor?.fileDescriptor)
        val relayBuffer = ByteBuffer.allocate(1024)
        while (true) {
            relayBuffer.clear()
            val bytesRead: Int = try {
                vpnInput.channel.read(relayBuffer)
            } catch (e: IOException) {
                break
            }
            if (bytesRead <= 0) {
                break
            }
            relayBuffer.flip()
            val dnsRequest = ByteArray(bytesRead)
            relayBuffer[dnsRequest]
            val dnsResponse: ByteArray = dnsRequest
            relayBuffer.clear()
            dnsResponse.let { relayBuffer.put(it) }
            relayBuffer.flip()
            try {
                vpnOutput.channel.write(relayBuffer)
            } catch (e: IOException) {
                break
            }
            relayBuffer.clear()
        }
    }

    companion object {
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        var isVpnRunning = false

        private fun closeVpnConnection() {
            try {
                parcelFileDescriptor?.close()
                parcelFileDescriptor = null
                Log.d("GRDDNSProxy", "VPN connection closed")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun stopVpnConnection() {
            isVpnRunning = false
            closeVpnConnection()
        }
    }

    private fun resolveDns(hostname: String): String? {
        return when (dnsResolutionMethod) {
            DnsResolutionMethod.PLAIN_TEXT -> resolveDnsPlainText(hostname)
            DnsResolutionMethod.DNS_OVER_HTTPS -> resolveDnsOverHttps(hostname)
            DnsResolutionMethod.DNS_OVER_TLS -> resolveDnsOverTls(hostname)
        }
    }

    private fun resolveDnsPlainText(hostname: String): String? {
        return InetAddress.getLoopbackAddress().hostAddress
    }

    private fun resolveDnsOverHttps(hostname: String): String? {
        val client = OkHttpClient()

        val dnsQuery = "https://dns.google/resolve?name=$hostname"
        val request = Request.Builder()
            .url(dnsQuery)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                responseBody?.let { body ->
                    val jsonObject = JSONObject(body)
                    val answersArray = jsonObject.getJSONArray("Answer")
                    if (answersArray.length() > 0) {
                        val answer = answersArray.getJSONObject(0)
                        Log.d(TAG, hostname + " " + answer.getString("data"))
                        return answer.getString("data")
                    }
                }
            }
        }
        return InetAddress.getLoopbackAddress().hostAddress
    }

    private fun resolveDnsOverTls(hostname: String): String? {
        val client = OkHttpClient()

        val dnsRequest = "https://dns.google/dns-query".toHttpUrlOrNull()?.let {
            DnsOverHttps.Builder()
                .client(client)
                .url(it)
                .build()
        }

        val dnsResponse = dnsRequest?.lookup(hostname)

        if (!dnsResponse.isNullOrEmpty()) {
            for (address in dnsResponse) {
                Log.d(TAG, "Resolved IP Address: $address")
            }
        } else {
            Log.d(TAG, "DNS lookup failed.")
        }
        return InetAddress.getLoopbackAddress().hostAddress
    }
}
