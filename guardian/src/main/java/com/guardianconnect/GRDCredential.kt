package com.guardianconnect

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
	var region: 				GRDRegion? = null
	var clientId: 				String? = null
	var apiAuthToken: 			String? = null

	// WireGuard credential related properties
    var devicePublicKey: 		String? = null
    var devicePrivateKey: 		String? = null
    var serverPublicKey: 		String? = null
    var IPv4Address: 			String? = null
    var IPv6Address: 			String? = null

	companion object {
		// TODO: populate region object
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

    /* A function to return a truncated version of the complete hostname to show in the user interface */
	// TODO: this needs to live in the GRDSGWServer class!
    fun truncatedHost(server: GRDSGWServer): String? {
        return server.hostname?.split(".")?.get(0)
    }
}
