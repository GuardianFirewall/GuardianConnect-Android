package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

data class BaseRequest(

    @SerializedName("api-auth-token")
    var apiAuthToken: String? = null
)