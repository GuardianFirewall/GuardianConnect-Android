package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class NewVPNDeviceResponse {

    @SerializedName("server-public-key")
    private val serverPublicKey: String? = null

    @SerializedName("mapped-ipv4-address")
    private val mappedIpv4Address: String? = null

    @SerializedName("mapped-ipv6-address")
    private val mappedIpv6Address: String? = null

    @SerializedName("client-id")
    var clientId: String? = null

    @SerializedName("api-auth-token")
    private val apiAuthToken: String? = null

    fun getServerPublicKey(): String? {
        return serverPublicKey
    }

    fun getMappedIpv4Address(): String? {
        return mappedIpv4Address
    }

    fun getMappedIpv6Address(): String? {
        return mappedIpv6Address
    }

    fun getApiAuthToken(): String? {
        return apiAuthToken
    }

}