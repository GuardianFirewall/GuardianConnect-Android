package com.guardianconnect

import android.util.Log
import com.guardianconnect.util.Constants.Companion.GRD_TRANSPORT_PROTOCOL
import com.guardianconnect.util.Constants.Companion.GRD_UNKNOWN
import com.guardianconnect.util.Constants.Companion.GRD_UNKNOWN_PRETTY
import com.guardianconnect.util.Constants.Companion.GRD_WIREGUARD
import com.guardianconnect.util.Constants.Companion.GRD_WIREGUARD_PRETTY

class GRDTransportProtocol {

    val TAG = GRDTransportProtocol::class.java.simpleName

    enum class GRDTransportProtocolType(val tp: String) {
        GRD_TP_UNKNOWN("GRDTransportProtocolUnknown"),
        GRD_TP_WIREGUARD("GRDTransportProtocolWireGuard")
    }

    // Store the preferred transport protocol in SharedPreferences
    fun setUserPreferredTransportProtocol(transportProtocol: GRDTransportProtocolType) {
        if (transportProtocol == GRDTransportProtocolType.GRD_TP_UNKNOWN) {
            GRDConnectManager.getSharedPrefsEditor()
                ?.putString(GRD_TRANSPORT_PROTOCOL, GRD_UNKNOWN)
        } else {
            GRDConnectManager.getSharedPrefsEditor()
                ?.putString(GRD_TRANSPORT_PROTOCOL, GRD_WIREGUARD)
        }
        GRDConnectManager.getSharedPrefsEditor()?.apply()
        Log.d(TAG, "Setting user preferred transport protocol to: $transportProtocol")
    }

    // Read the currently preferred transport protocol from the SharedPreferences and return it
    // as the type GRDTransportProtocol
    fun getUserPreferredTransportProtocol(): GRDTransportProtocolType {
        val transportProtocolString =
            GRDConnectManager.getSharedPrefs()?.getString(GRD_TRANSPORT_PROTOCOL, "")
        if (!transportProtocolString.isNullOrEmpty()) {
            if (transportProtocolString == GRDTransportProtocolType.GRD_TP_UNKNOWN.tp) {
                return GRDTransportProtocolType.GRD_TP_UNKNOWN
            }
        }
        return GRDTransportProtocolType.GRD_TP_WIREGUARD

    }

    // Return transport protocol as string
    fun transportProtocolStringFor(transportProtocol: GRDTransportProtocolType): String {
        return if (transportProtocol == GRDTransportProtocolType.GRD_TP_WIREGUARD) {
            GRD_WIREGUARD
        } else {
            GRD_UNKNOWN
        }
    }

    // Return transport protocol as formatted string
    fun prettyTransportProtocolStringFor(transportProtocol: GRDTransportProtocolType): String {
        return if (transportProtocol == GRDTransportProtocolType.GRD_TP_WIREGUARD) {
            GRD_WIREGUARD_PRETTY
        } else {
            GRD_UNKNOWN_PRETTY
        }
    }

    //  Return GRDTransportProtocol type for string
    fun transportProtocolFromString(protocolString: String): GRDTransportProtocolType {
        return if (protocolString == GRDTransportProtocolType.GRD_TP_WIREGUARD.name) {
            GRDTransportProtocolType.GRD_TP_WIREGUARD
        } else {
            GRDTransportProtocolType.GRD_TP_UNKNOWN
        }
    }
}