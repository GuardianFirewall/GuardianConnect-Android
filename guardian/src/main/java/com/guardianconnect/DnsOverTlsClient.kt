package com.guardianconnect

import android.util.Log
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

class DnsOverTlsClient(private val dotAddress: String, private val dotPort: Int) {
    private val TAG = "DnsOverTlsClient"

    private val client = OkHttpClient.Builder()
        .sslSocketFactory(DefaultSslSocketFactory(), DefaultTrustManager())
        .build()

    fun resolveDnsOverTls(request: ByteArray): ByteArray {
        val socketFactory = client.sslSocketFactory as DefaultSslSocketFactory
        val trustManager = client.x509TrustManager as DefaultTrustManager

        val socket = socketFactory.createSocket(dotAddress, dotPort) as SSLSocket
        socket.startHandshake()

        val outputStream = socket.outputStream
        val inputStream = socket.inputStream

        try {
            outputStream.write(request)
            outputStream.flush()

            val response = inputStream.readBytes()
            socket.close()

            return response
        } catch (e: IOException) {
            Log.e(TAG, "DNS over TLS request failed", e)
        }

        return request // Pass through the original DNS request on failure
    }

    private class DefaultSslSocketFactory : SSLSocketFactory() {
        override fun getDefaultCipherSuites(): Array<String> = arrayOf()
        override fun getSupportedCipherSuites(): Array<String> = arrayOf()
        override fun createSocket(
            s: Socket?,
            host: String?,
            port: Int,
            autoClose: Boolean
        ): Socket = throw UnsupportedOperationException()

        override fun createSocket(host: String?, port: Int): Socket =
            throw UnsupportedOperationException()

        override fun createSocket(
            host: String?,
            port: Int,
            localHost: InetAddress?,
            localPort: Int
        ): Socket = throw UnsupportedOperationException()

        override fun createSocket(host: InetAddress?, port: Int): Socket =
            throw UnsupportedOperationException()

        override fun createSocket(
            address: InetAddress?,
            port: Int,
            localAddress: InetAddress?,
            localPort: Int
        ): Socket = throw UnsupportedOperationException()
    }

    private class DefaultTrustManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}