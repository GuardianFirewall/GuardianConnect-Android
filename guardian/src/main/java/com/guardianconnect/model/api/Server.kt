package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class Server {

    @SerializedName("hostname")
    var hostname: String? = null

    @SerializedName("display-name")
    var displayName: String? = null

    @SerializedName("offline")
    var offline: Boolean? = null

    @SerializedName("capacity-score")
    var capacityScore: Int? = null

    @SerializedName("server-feature-environment")
    var serverFeatureEnvironment: Int? = null

    @SerializedName("beta-capable")
    var betaCapable: Boolean? = null

}