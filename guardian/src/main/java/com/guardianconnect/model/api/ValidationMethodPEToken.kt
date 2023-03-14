package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class ValidationMethodPEToken {

    @SerializedName("validation-method")
    var validationMethod: String? = null

    @SerializedName("pe-token")
    var peToken: String? = null
}