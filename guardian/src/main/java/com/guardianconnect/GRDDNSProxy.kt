import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

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
                .addAddress("10.0.0.2", 24)
                .addDnsServer("1.1.1.1")
                .addDnsServer("1.0.0.1")
                .establish()
            Log.d(TAG, "VPN connection established")
            startVpnConnection()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVpnConnection() {
        val protectFd = parcelFileDescriptor?.fd
        val tunnel = DatagramChannel.open()
        tunnel.connect(InetSocketAddress("10.0.0.2", 8080)) // TODO: Replace with our VPN server address and port
        tunnel.configureBlocking(false)
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
            tunnel.write(relayBuffer)
            relayBuffer.clear()

            relayBuffer.clear()
            val bytesWritten = tunnel.read(relayBuffer)
            if (bytesWritten <= 0) {
                break
            }
            relayBuffer.flip()

            // Intercept DNS resolution requests
            val dnsRequest = ByteArray(bytesWritten)
            relayBuffer.get(dnsRequest)
            val dnsResponse = interceptDnsRequest(dnsRequest)
            relayBuffer.clear()
            relayBuffer.put(dnsResponse)
            relayBuffer.flip()

            vpnOutput.write(relayBuffer)
            relayBuffer.clear()
        }

        closeVpnConnection()
    }

    private fun interceptDnsRequest(request: ByteArray): ByteArray {
        if (request.size >= 12 && (request[2].toInt() and 0x80) == 0) {
            val hostname = extractHostnameFromDnsRequest(request)

            if (blockedHostnames.contains(hostname)) {
                return createDnsResponse(request, "127.0.0.1")
            }
        }

        return request
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

        // Set the DNS response flags (bit 15: QR = 1 for response, opcode = 0 for standard query)
        response[2] = (response[2].toInt() or 0x80).toByte()

        // Set the DNS response answer count to 1
        response[6] = 0x00
        response[7] = 0x01

        // Set the DNS response answer TTL to 0 (no caching)
        response[12] = 0x00
        response[13] = 0x00
        response[14] = 0x00
        response[15] = 0x00

        // Set the DNS response answer data length to 4 (IPv4 address length)
        response[24] = 0x00
        response[25] = 0x04

        // Set the DNS response answer data to the specified IP address
        val ipBytes = InetAddress.getByName(ipAddress).address
        response[26] = ipBytes[0]
        response[27] = ipBytes[1]
        response[28] = ipBytes[2]
        response[29] = ipBytes[3]

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
