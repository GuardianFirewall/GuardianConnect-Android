package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

data class AlertsResponse(

    @SerializedName("timestamp")
    var timestamp: Long? = null,

    @SerializedName("uuid")
    var uuid: String? = null,

    @SerializedName("action")
    var action: String? = null,

    @SerializedName("title")
    var title: String? = null,

    @SerializedName("message")
    var message: String? = null,

    @SerializedName("host")
    var host: String? = null,

    @SerializedName("category")
    var category: String? = null
)