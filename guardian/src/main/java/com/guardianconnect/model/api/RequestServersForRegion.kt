package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName
import com.guardianconnect.enumeration.GRDServerFeatureEnvironment

class RequestServersForRegion {

    @SerializedName("region")
    var region: String? = null

    @SerializedName("paid")
    var paid: Int? = null

    @SerializedName("feature-environment")
    var featureEnvironment: GRDServerFeatureEnvironment? = null

    @SerializedName("beta-capable")
    var betaCapable: Boolean? = null

    override fun toString(): String {
        return "region:$region, featureEnvironment:$featureEnvironment, betaCapable:$betaCapable, paid:$paid"
    }
}