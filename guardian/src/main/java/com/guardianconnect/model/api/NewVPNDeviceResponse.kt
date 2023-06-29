package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class NewVPNDeviceResponse {

    @SerializedName("server-public-key")
    var serverPublicKey: String? = null

    @SerializedName("mapped-ipv4-address")
    var mappedIpv4Address: String? = null

    @SerializedName("mapped-ipv6-address")
    var mappedIpv6Address: String? = null

    @SerializedName("client-id")
    var clientId: String? = null

    @SerializedName("api-auth-token")
    var apiAuthToken: String? = null

}