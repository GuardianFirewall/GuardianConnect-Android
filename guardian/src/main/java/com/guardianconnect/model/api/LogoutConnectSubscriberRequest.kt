package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class LogoutConnectSubscriberRequest {

    @SerializedName("connect-publishable-key")
    var connectPublishableKey: String? = null

    @SerializedName("pe-token")
    var peToken: String? = null
}