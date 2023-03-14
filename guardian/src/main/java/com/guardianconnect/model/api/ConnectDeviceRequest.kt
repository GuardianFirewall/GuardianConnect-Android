package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class ConnectDeviceRequest {

    @SerializedName("connect-public-key")
    var connectPublicKey: String? = null

    @SerializedName("pe-token")
    var peToken: String? = null

    @SerializedName("device-nickname")
    var deviceNickname: String? = null

    @SerializedName("accepted-tos")
    var acceptedTos: Boolean? = null

}