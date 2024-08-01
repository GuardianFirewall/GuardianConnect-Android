package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

data class TimeZonesResponse(

    @SerializedName("name")
    var name: String? = null,

    @SerializedName("timezones")
    var timezones: ArrayList<String>? = null,

    @SerializedName("name-pretty")
    var namePretty: String? = null,

    @SerializedName("continent")
    var continent: String? = null,

    @SerializedName("country-iso-code")
    var countryISOCode: String? = null
)