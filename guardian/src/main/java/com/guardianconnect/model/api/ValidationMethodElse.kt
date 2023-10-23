package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

data class ValidationMethodElse(

    @SerializedName("validation-method")
    var validationMethod: String? = null,

    var map: HashMap<Any, Any>? = null
)