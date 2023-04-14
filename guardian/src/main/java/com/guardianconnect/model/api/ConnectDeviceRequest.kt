package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class ConnectDeviceRequest {

    @SerializedName("connect-publishable-key")
    var connectPublicKey: String? = null

    @SerializedName("pe-token")
    var peToken: String? = null

    @SerializedName("ep-grd-device-nickname")
    var epGrdDeviceNickname: String? = null

    @SerializedName("ep-grd-device-accepted-tos")
    var epGrdDeviceAcceptedTos: Boolean? = null

}