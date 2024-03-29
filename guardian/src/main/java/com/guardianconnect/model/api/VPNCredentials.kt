package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

data class VPNCredentials(

    @SerializedName("subscriber-credential")
    var subscriberCredential: String? = null,

    @SerializedName("api-auth-token")
    var apiAuthToken: String? = null
)