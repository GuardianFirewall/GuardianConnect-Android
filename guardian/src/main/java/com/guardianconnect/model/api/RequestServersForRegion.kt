package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class RequestServersForRegion {

    @SerializedName("region")
    var region: String? = null

    @SerializedName("paid")
    val paid: Int? = null

    @SerializedName("feature-environment")
    val featureEnvironment: Int? = null

    @SerializedName("beta-capable")
    val betaCapable: Int? = null

}