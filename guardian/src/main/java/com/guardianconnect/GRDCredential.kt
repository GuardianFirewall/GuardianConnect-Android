package com.guardianconnect

import com.guardianconnect.api.Repository
import com.guardianconnect.api.IOnApiResponse
import com.guardianconnect.managers.GRDConnectManager
import com.guardianconnect.model.api.GRDSGWServer
import com.guardianconnect.util.Constants.Companion.GRD_MAIN
import com.guardianconnect.util.Constants.Companion.GRD_WIREGUARD_PRETTY
import com.wireguard.crypto.KeyPair
import java.util.*


class GRDCredential {
	var name: 					String? = null
	var identifier: 			String? = null
	var mainCredential: 		Boolean? = null
	var transportProtocol: 		GRDTransportProtocol.GRDTransportProtocolType? = null
	var expirationDate: 		Long? = null
	var server:					GRDSGWServer? = null
	var hostname: 				String? = null
	var hostnameDisplayValue: 	String? = null
	var clientId: 				String? = null
	var apiAuthToken: 			String? = null

	// WireGuard credential related properties
    var devicePublicKey: 		String? = null
    var devicePrivateKey: 		String? = null
    var serverPublicKey: 		String? = null
    var IPv4Address: 			String? = null
    var IPv6Address: 			String? = null

	companion object {
		fun initGRDCredential(transportProtocolType: GRDTransportProtocol.GRDTransportProtocolType, validForDays: Long, mainCreds: Boolean, credentialData: Map<String, Any>, grdServer: GRDSGWServer, keyPair: KeyPair): GRDCredential {
			val credential = GRDCredential()
			credential.name = GRD_WIREGUARD_PRETTY + " " + credential.truncatedHost(grdServer)
			credential.mainCredential = mainCreds
			credential.identifier = UUID.randomUUID().toString()
			if (mainCreds) {
				credential.identifier = GRD_MAIN
			}

			credential.apiAuthToken = credentialData["api-auth-token"] as String
			credential.server = grdServer
			credential.hostname = grdServer.hostname
			credential.hostnameDisplayValue = grdServer.displayName
			credential.expirationDate = System.currentTimeMillis() + validForDays * 86400000
			credential.transportProtocol = transportProtocolType

			if (transportProtocolType == GRDTransportProtocol.GRDTransportProtocolType.GRD_TP_WIREGUARD) {
				credential.devicePublicKey = keyPair.publicKey.toBase64()
				credential.devicePrivateKey = keyPair.privateKey.toBase64()
				credential.serverPublicKey = credentialData["server-public-key"] as String
				credential.IPv4Address = credentialData["mapped-ipv4-address"] as String
				credential.IPv6Address = credentialData["mapped-ipv6-address"] as String
				credential.clientId = credentialData["client-id"] as String
			}

			return credential
		}
	}

	fun canSendSGWAPIRequests(): Boolean {
		if (this.hostname.isNullOrEmpty()) {
			return false

		} else if (this.clientId.isNullOrEmpty()) {
			return false

		} else if (this.apiAuthToken.isNullOrEmpty()) {
			return false
		}

		return true
	}

	fun downloadAlerts(iOnApiResponse: IOnApiResponse) {
		if (!canSendSGWAPIRequests()) {
			iOnApiResponse.onError("SGW credential can't send SGW API requests")
			return
		}

		val requestData = mutableMapOf<String, Any>()
		requestData["api-auth-token"] = this.apiAuthToken.toString()

		if (this.mainCredential == true) {
			// Note from CJ 2026-05-21
			// Last download timestamps are being automatically stored server side now
			// remove the storing of the timestamp below and don't send it in the API
			// request anymore
			val timestamp = GRDConnectManager.getSharedPrefs().getLong("GRD_ALERTS_DOWNLOAD_TIMESTAMP", 0)
			requestData["timestamp"] = timestamp
		}

		Repository.instance.downloadAlerts(this.clientId.toString(), requestData, object: IOnApiResponse {
			override fun onSuccess(any: Any?) {
				@Suppress("UNCHECKED_CAST")
				val responseData = any as ArrayList<Map<String, Any>>
				val alerts = mutableListOf<GRDAlert>()
				for (alertData: Map<String, Any> in responseData) {
					val newAlert = GRDAlert.alertFromMap(alertData)
					alerts.add(newAlert)
				}

				iOnApiResponse.onSuccess(alerts.toList())
			}

			override fun onError(error: String?) {
				iOnApiResponse.onError(error)
			}
		})
	}

    /* A function to return a truncated version of the complete hostname to show in the user interface */
    fun truncatedHost(server: GRDSGWServer): String? {
        return server.hostname?.split(".")?.get(0)
    }
}
