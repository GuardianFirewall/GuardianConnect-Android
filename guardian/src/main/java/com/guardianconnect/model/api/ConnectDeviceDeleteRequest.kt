package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class ConnectDeviceDeleteRequest {

    @SerializedName("connect-publishable-key")
    var connectPublishableKey: String? = null

    @SerializedName("pe-token")
    var peToken: String? = null

    @SerializedName("ep-grd-device-uuid")
    var deviceUuid: String? = null
}