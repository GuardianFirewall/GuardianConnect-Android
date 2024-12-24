package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName
import com.guardianconnect.GRDRegion

data class GRDSGWServer(

    @SerializedName("hostname")
    var hostname: String? = null,

    @SerializedName("display-name")
    var displayName: String? = null,

    @SerializedName("offline")
    var offline: Boolean? = null,

    @SerializedName("capacity-score")
    var capacityScore: Int? = null,

    @SerializedName("server-feature-environment")
    var serverFeatureEnvironment: Int? = null,

    @SerializedName("beta-capable")
    var betaCapable: Boolean? = null,

    @SerializedName("smart-routing-enabled")
    var smartRoutingEnabled: Boolean? = null,

    @SerializedName("region")
    var region: GRDRegion? = null
)