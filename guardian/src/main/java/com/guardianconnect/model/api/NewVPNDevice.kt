package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class NewVPNDevice {

    @SerializedName("subscriber-credential")
    var subscriberCredential: String? = null

    @SerializedName("transport-protocol")
    var transportProtocol: String? = null

    @SerializedName("public-key")
    var publicKey: String? = null

}