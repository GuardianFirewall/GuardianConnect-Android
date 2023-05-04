import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel


class GRDDNSProxy : VpnService() {
    private val TAG = "GRDDNSProxy"
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

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
            vpnOutput.write(relayBuffer)
            relayBuffer.clear()
        }

        closeVpnConnection()
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
