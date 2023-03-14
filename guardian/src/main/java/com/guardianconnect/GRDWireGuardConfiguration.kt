package com.guardianconnect

import android.util.Log
import com.wireguard.config.Config
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.Reader
import java.io.StringReader

/*  A class that implements convenience functions to handle and create WireGuard configuration
    strings and objects for use at runtime or for export */
class GRDWireGuardConfiguration {

    private val TAG = GRDWireGuardConfiguration::class.java.simpleName

    // Create a WireGuard configuration string from a set of function parameters.
    fun getWireGuardConfigString(grdCredential: GRDCredential, dnsServersParam: String?): String {
        var dnsServers = dnsServersParam

        if (dnsServers.isNullOrEmpty()) {
            dnsServers = "1.1.1.1, 1.0.0.1"
        }

        val configString =
            "[Interface]" +
                    "\nPrivateKey = ${grdCredential.devicePrivateKey}" +
                    "\nAddress = ${grdCredential.IPv4Address}" +
                    "\nDNS = $dnsServers" +
                    "\n\n[Peer]" +
                    "\nPublicKey = ${grdCredential.serverPublicKey}" +
                    "\nAllowedIPs = 0.0.0.0/0, ::/0" +
                    "\nEndpoint = ${grdCredential.hostname}:51821"

        Log.d(TAG, "Formatted WireGuard config: \n$configString")
        return configString
    }

    // Create a WireGuard configuration object from a set of function parameters.
    fun getWireGuardConfigObject(grdCredential: GRDCredential, dnsServersParam: String?): Config? {
        var dnsServers = dnsServersParam

        if (dnsServers.isNullOrEmpty()) {
            dnsServers = "1.1.1.1, 1.0.0.1"
        }

        val configString =
            "[Interface]" +
                    "\nPrivateKey = ${grdCredential.devicePrivateKey}" +
                    "\nAddress = ${grdCredential.IPv4Address}" +
                    "\nDNS = $dnsServers" +
                    "\n\n[Peer]" +
                    "\nPublicKey = ${grdCredential.serverPublicKey}" +
                    "\nAllowedIPs = 0.0.0.0/0, ::/0" +
                    "\nEndpoint = ${grdCredential.hostname}:51821"

        Log.d(TAG, "Formatted WireGuard config: \n$configString")

        val inputString: Reader = StringReader(configString)
        val reader = BufferedReader(inputString)
        var config: Config? = null

        try {
            config = Config.parse(reader)
        } catch (e: Exception) {
            GRDConnectManager.getCoroutineScope().launch {
                GRDVPNHelper.grdErrorFlow.emit("Error parsing config! $e")
            }
        }

        return config
    }

}