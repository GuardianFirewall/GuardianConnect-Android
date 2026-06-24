package com.guardianconnect

import com.guardianconnect.model.api.NewVPNDeviceResponse
import com.guardianconnect.model.api.GRDSGWServer
import com.guardianconnect.util.Constants.Companion.GRD_MAIN
import com.guardianconnect.util.Constants.Companion.GRD_Main_Credential_WG_Private_Key
import com.guardianconnect.util.Constants.Companion.GRD_Main_Credential_WG_Public_Key
import com.guardianconnect.util.Constants.Companion.GRD_WIREGUARD_PRETTY
import com.guardianconnect.util.GRDKeystore
import com.wireguard.crypto.KeyPair
import java.util.*


class GRDCredential {
    var checkedExpiration: 		Boolean? = null
    var expired: 				Boolean? = null
    var name: 					String? = null
    var identifier: 			String? = null
    var mainCredential: 		Boolean? = null
    var transportProtocol: 		GRDTransportProtocol.GRDTransportProtocolType? = null
    var expirationDate: 		Long? = null
    var hostname: 				String? = null
    var hostnameDisplayValue: 	String? = null
    var clientId: 				String? = null
    var apiAuthToken: 			String? = null
    var devicePublicKey: 		String? = null
    var devicePrivateKey: 		String? = null
    var serverPublicKey: 		String? = null
    var IPv4Address: 			String? = null
    var IPv6Address: 			String? = null
    var region: 				GRDRegion? = null

    fun initGRDCredential(transportProtocolType: GRDTransportProtocol.GRDTransportProtocolType, validForDays: Long, mainCreds: Boolean, vpnDeviceResponse: NewVPNDeviceResponse, server: GRDSGWServer, keyPair: KeyPair) {
        identifier = UUID.randomUUID().toString()
        name = GRD_WIREGUARD_PRETTY + " " + truncatedHost(server)
        mainCredential = mainCreds
        if (mainCreds) {
            identifier = GRD_MAIN
        }
        apiAuthToken = vpnDeviceResponse.apiAuthToken
        hostname = server.hostname
        region = server.region
        expirationDate = System.currentTimeMillis() + validForDays * 86400000
        hostnameDisplayValue = server.displayName
        checkedExpiration = false
        expired = false
        transportProtocol = transportProtocolType
        if (transportProtocolType == GRDTransportProtocol.GRDTransportProtocolType.GRD_TP_WIREGUARD) {
            devicePublicKey = keyPair.publicKey.toBase64()
            devicePrivateKey = keyPair.privateKey.toBase64()
            serverPublicKey = vpnDeviceResponse.serverPublicKey
            IPv4Address = vpnDeviceResponse.mappedIpv4Address
            IPv6Address = vpnDeviceResponse.mappedIpv6Address
            clientId = vpnDeviceResponse.clientId
        }
    }

    /* A function to return a truncated version of the complete hostname to show in the user interface */
    fun truncatedHost(server: GRDSGWServer): String? {
        return server.hostname?.split(".")?.get(0)
    }
}