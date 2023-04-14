package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class ConnectDevicesAllRequest {

    @SerializedName("pe-token")
    var peToken: String? = null

    @SerializedName("connect-publishable-key")
    var connectPublishableKey: String? = null

}