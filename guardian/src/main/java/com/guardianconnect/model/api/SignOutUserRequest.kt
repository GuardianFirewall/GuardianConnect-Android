package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

data class SignOutUserRequest(

    @SerializedName("pe-token")
    var peToken: String? = null
)
