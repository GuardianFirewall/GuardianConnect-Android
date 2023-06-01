package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class TimeZonesResponse {

    @SerializedName("name")
    private val name: String? = null

    @SerializedName("timezones")
    private val timezones: ArrayList<String>? = null

    fun getName(): String? {
        return name
    }

    fun getTimezones(): ArrayList<String>? {
        return timezones
    }

}