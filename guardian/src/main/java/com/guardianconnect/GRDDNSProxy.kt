import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.guardianconnect.DnsOverHttpsClient
import com.guardianconnect.DnsOverTlsClient
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress
import java.nio.ByteBuffer

// TODO: To be tested
class GRDDNSProxy : VpnService() {
    private val TAG = "GRDDNSProxy"
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    private val blockedHostnames = listOf(
        "reddit.com",
        "github.com",
        "guardianapp.com",
        "apple.com",
        "google.com"
    )

    private val dnsOverHttpsUrl =
        "https://dns.google/dns-query" // Default DoH server URL (can be changed as needed)
    private val dnsOverTlsAddress =
        "1.1.1.1" // Default DoT server address (can be changed as needed)
    private val dnsOverTlsPort = 853 // Default DoT server port (can be changed as needed)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        establishVpnConnection()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        closeVpnConnection()
    }

    private fun establishVpnConnection() {
        try {
            Log.d(TAG, "Establishing VPN connection...")
            parcelFileDescriptor = Builder()
                .setMtu(1500)
                .addAddress("10.0.0.2", 24) // TODO: Replace with our VPN server address and port
                .addRoute("0.0.0.0", 0)
                .establish()
            Log.d(TAG, "VPN connection established")
            startVpnConnection()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVpnConnection() {
        val protectFd = parcelFileDescriptor?.fd
        val vpnInput = FileInputStream(parcelFileDescriptor?.fileDescriptor).channel
        val vpnOutput = FileOutputStream(parcelFileDescriptor?.fileDescriptor).channel
        val relayBuffer = ByteBuffer.allocate(1024)

        while (true) {
            relayBuffer.clear()
            val bytesRead = vpnInput.read(relayBuffer)
            if (bytesRead <= 0) {
                break
            }
            relayBuffer.flip()

            val dnsRequest = ByteArray(bytesRead)
            relayBuffer.get(dnsRequest)

            val dnsResponse = when {
                isPlainTextDns() -> performPlainTextDnsResolution(dnsRequest)
                isDnsOverHttps() -> performDnsOverHttpsResolution(dnsRequest)
                isDnsOverTls() -> performDnsOverTlsResolution(dnsRequest)
                else -> dnsRequest // Pass through the original DNS request
            }

            relayBuffer.clear()
            relayBuffer.put(dnsResponse)
            relayBuffer.flip()

            vpnOutput.write(relayBuffer)
            relayBuffer.clear()
        }

        closeVpnConnection()
    }

    private fun isPlainTextDns(): Boolean {
        // If plain text DNS should be used
        return true
    }

    private fun isDnsOverHttps(): Boolean {
        // If DNS over HTTPS should be used
        return true
    }

    private fun isDnsOverTls(): Boolean {
        // If DNS over TLS should be used
        return true
    }

    private fun performPlainTextDnsResolution(request: ByteArray): ByteArray {
        val hostname = extractHostnameFromDnsRequest(request)

        if (blockedHostnames.contains(hostname)) {
            // Create a DNS response with 127.0.0.1 as the resolved IP address
            return createDnsResponse(request, "127.0.0.1")
        }
        return request
    }

    private fun performDnsOverHttpsResolution(request: ByteArray): ByteArray {
        val dnsOverHttpsClient = DnsOverHttpsClient(dnsOverHttpsUrl)

        val hostname = extractHostnameFromDnsRequest(request)

        if (blockedHostnames.contains(hostname)) {
            // Create a DNS response with 127.0.0.1 as the resolved IP address
            return createDnsResponse(request, "127.0.0.1")
        }

        // Perform DNS over HTTPS resolution
        return try {
            dnsOverHttpsClient.resolveDnsOverHttps(request)
        } catch (e: Exception) {
            Log.e(TAG, "DNS over HTTPS resolution failed", e)
            request // Pass through the original DNS request on failure
        }
    }

    private fun performDnsOverTlsResolution(request: ByteArray): ByteArray {
        val dnsOverTlsClient = DnsOverTlsClient(dnsOverTlsAddress, dnsOverTlsPort)

        val hostname = extractHostnameFromDnsRequest(request)

        if (blockedHostnames.contains(hostname)) {
            // Create a DNS response with 127.0.0.1 as the resolved IP address
            return createDnsResponse(request, "127.0.0.1")
        }

        // Perform DNS over TLS resolution
        return try {
            dnsOverTlsClient.resolveDnsOverTls(request)
        } catch (e: Exception) {
            Log.e(TAG, "DNS over TLS resolution failed", e)
            request // Pass through the original DNS request on failure
        }
    }

    private fun extractHostnameFromDnsRequest(request: ByteArray): String {
        // Skip the DNS header (12 bytes)
        var index = 12
        val hostnameBuilder = StringBuilder()

        // Loop through the request to extract the hostname
        while (index < request.size) {
            val labelLength = request[index].toInt()

            if (labelLength == 0) {
                // End of the hostname
                break
            }

            if (index + labelLength >= request.size) {
                // Malformed request, label length exceeds the request size
                break
            }

            if (hostnameBuilder.isNotEmpty()) {
                // Append a dot before each label
                hostnameBuilder.append('.')
            }

            hostnameBuilder.append(String(request, index + 1, labelLength))
            index += labelLength + 1
        }

        return hostnameBuilder.toString()
    }

    private fun createDnsResponse(request: ByteArray, ipAddress: String): ByteArray {
        // Create a DNS response with the specified IP address

        val response = ByteArray(request.size)
        System.arraycopy(request, 0, response, 0, request.size)

        // Modify the DNS response to include the specified IP address
        response[2] = (response[2].toInt() or 0x80).toByte() // Set the response flag (bit 15)
        response[3] = (response[3].toInt() or 0x80).toByte() // Set the answer count to 1

        // Replace the answer section with the specified IP address
        response[request.size] = 0.toByte() // Label
        response[request.size + 1] = 1.toByte() // Type: A (IPv4 address)
        response[request.size + 2] = 0.toByte() // Class: IN (Internet)
        response[request.size + 3] = 0.toByte() // Time to Live (TTL)
        response[request.size + 4] = 0.toByte() // TTL
        response[request.size + 5] = 0.toByte() // TTL
        response[request.size + 6] = 0.toByte() // TTL
        response[request.size + 7] = 60.toByte() // TTL
        response[request.size + 8] = 0.toByte() // Data Length
        response[request.size + 9] = 4.toByte() // Data Length
        val ipBytes = InetAddress.getByName(ipAddress).address
        System.arraycopy(ipBytes, 0, response, request.size + 10, 4) // IP address

        return response
    }

    private fun closeVpnConnection() {
        try {
            parcelFileDescriptor?.close()
            parcelFileDescriptor = null
            Log.d(TAG, "VPN connection closed")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
