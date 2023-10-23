package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

data class TimeZonesResponse(

    @SerializedName("name")
    var name: String? = null,

    @SerializedName("timezones")
    var timezones: ArrayList<String>? = null
)