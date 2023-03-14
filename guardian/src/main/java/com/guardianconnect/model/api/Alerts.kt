package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class Alerts {

    @SerializedName("api-auth-token")
    var apiAuthToken: String? = null

    @SerializedName("timestamp")
    var timestamp: Long? = null

}