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

    private val tag = GRDWireGuardConfiguration::class.java.simpleName

    // Create a WireGuard configuration string from a set of function parameters.
    fun getWireGuardConfigString(
        grdCredential: GRDCredential,
        dnsServersParam: String?,
        appExceptionList: ArrayList<String>? = arrayListOf(),
        excludeLANTraffic: Boolean = false
    ): String {
        var dnsServers = dnsServersParam

        if (dnsServers.isNullOrEmpty()) {
            dnsServers = "1.1.1.1, 1.0.0.1"
        }

        val appExceptions: String = if (grdCredential.mainCredential == true && appExceptionList != null) {
            if (appExceptionList.size > 1) {
                appExceptionList.joinToString(", ")
            } else {
                appExceptionList.firstOrNull() ?: "empty list"
            }
        } else {
            "null"
        }

        val allowedIPs = if (excludeLANTraffic) {
            "0.0.0.0/5,8.0.0.0/7,11.0.0.0/8,12.0.0.0/6,16.0.0.0/4,32.0.0.0/3,64.0.0.0/2,128.0.0.0/3," +
                    "160.0.0.0/5,168.0.0.0/6,172.0.0.0/12,172.32.0.0/11,172.64.0.0/10,172.128.0.0/9,173.0.0.0/8," +
                    "174.0.0.0/7,176.0.0.0/4,192.0.0.0/9,192.128.0.0/11,192.160.0.0/13,192.169.0.0/16,192.170.0.0/15," +
                    "192.172.0.0/14,192.176.0.0/12,192.192.0.0/10,193.0.0.0/8,194.0.0.0/7,196.0.0.0/6,200.0.0.0/5," +
                    "208.0.0.0/4,224.0.0.0/3,::/1,8000::/2,c000::/3,e000::/4,f000::/5,f800::/6,fc00::/8,fe00::/7"
        } else {
            "0.0.0.0/0, ::/0"
        }

        val configString =
            "[Interface]" +
                    "\nPrivateKey = ${grdCredential.devicePrivateKey}" +
                    "\nAddress = ${grdCredential.IPv4Address}" +
                    "\nDNS = $dnsServers" +
                    "\nExcludedApplications = $appExceptions" +
                    "\n\n[Peer]" +
                    "\nPublicKey = ${grdCredential.serverPublicKey}" +
                    "\nAllowedIPs = $allowedIPs" +
                    "\nEndpoint = ${grdCredential.hostname}:51821"

        Log.d(tag, "Formatted WireGuard config: \n$configString")
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

        Log.d(tag, "Formatted WireGuard config: \n$configString")

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