package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

open class ConnectDeleteDeviceRequest {

    @SerializedName("connect-publishable-key")
    var connectPublishableKey: String? = null

    @SerializedName("ep-grd-device-uuid")
    var deviceUuid: String? = null
}