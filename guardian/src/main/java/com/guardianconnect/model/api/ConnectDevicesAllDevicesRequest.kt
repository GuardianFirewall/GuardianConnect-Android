package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

open class ConnectDevicesAllDevicesRequest {

    @SerializedName("pe-token")
    var peToken: String? = null

    @SerializedName("connect-publishable-key")
    var connectPublishableKey: String? = null

}