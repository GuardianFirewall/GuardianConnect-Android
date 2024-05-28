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


/* The class is a representation of how we return newly created credentials in JSON from our
    VPN servers and any on device computed metadata
 */
class GRDCredential {

    var checkedExpiration: Boolean? = null

    var expired: Boolean? = null

    var name: String? = null

    var identifier: String? = null

    var mainCredential: Boolean? = null

    var transportProtocol: GRDTransportProtocol.GRDTransportProtocolType? = null

    var expirationDate: Long? = null

    var hostname: String? = null

    var hostnameDisplayValue: String? = null

    var clientId: String? = null

    var apiAuthToken: String? = null

    var devicePublicKey: String? = null

    var devicePrivateKey: String? = null

    var serverPublicKey: String? = null

    var IPv4Address: String? = null

    var IPv6Address: String? = null

    var region: GRDRegion? = null

    /*  An init function to create a instance variable of the GRDCredential class given some data
        from the Android Keystore or the API
         Functions starting the VPN connection or generating new VPN credentials without starting
         a connection should always take a GRDTransportProtocol type.
    */
    fun createGRDCredential(
        grdTransportProtocolType: GRDTransportProtocol.GRDTransportProtocolType,
        validForDays: Long,
        mainCreds: Boolean,
        vpnDeviceResponse: NewVPNDeviceResponse,
        grdSgwServer: GRDSGWServer,
        keyPair: KeyPair
    ) {
        identifier = UUID.randomUUID().toString()
        name = GRD_WIREGUARD_PRETTY + " " + truncatedHost(grdSgwServer)
        mainCredential = mainCreds
        if (mainCreds) {
            identifier = GRD_MAIN
        }
        apiAuthToken = vpnDeviceResponse.apiAuthToken
        hostname = grdSgwServer.hostname
        region = grdSgwServer.region
        expirationDate =
            System.currentTimeMillis() + validForDays * 86400000
        hostnameDisplayValue = grdSgwServer.displayName
        checkedExpiration = false
        expired = false
        transportProtocol = grdTransportProtocolType
        if (grdTransportProtocolType == GRDTransportProtocol.GRDTransportProtocolType.GRD_TP_WIREGUARD) {
            devicePublicKey = keyPair.publicKey.toBase64()
            devicePrivateKey = keyPair.privateKey.toBase64()
            serverPublicKey = vpnDeviceResponse.serverPublicKey
            IPv4Address = vpnDeviceResponse.mappedIpv4Address
            IPv6Address = vpnDeviceResponse.mappedIpv6Address
            clientId = vpnDeviceResponse.clientId

            // Store the keypair
            devicePublicKey?.let {
                GRDKeystore.instance.saveToKeyStore(GRD_Main_Credential_WG_Public_Key, it)
            }
            devicePrivateKey?.let {
                GRDKeystore.instance.saveToKeyStore(GRD_Main_Credential_WG_Private_Key, it)
            }
        }
    }

    /* A function to return a truncated version of the complete hostname to show in the user interface */
    fun truncatedHost(grdSgwServer: GRDSGWServer): String? {
        return grdSgwServer.hostname?.split(".")?.get(0)
    }
}