package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class RequestServersForRegion {

    @SerializedName("region")
    var region: String? = null

    @SerializedName("paid")
    var paid: Int? = null

    @SerializedName("feature-environment")
    var featureEnvironment: Int? = null

    @SerializedName("beta-capable")
    var betaCapable: Int? = null

}