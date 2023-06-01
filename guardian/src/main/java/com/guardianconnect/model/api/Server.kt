package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class Server {

    @SerializedName("hostname")
    private val hostname: String? = null

    @SerializedName("display-name")
    private val displayName: String? = null

    @SerializedName("offline")
    private val offline: Boolean? = null

    @SerializedName("capacity-score")
    private val capacityScore: Int? = null

    @SerializedName("server-feature-environment")
    private val serverFeatureEnvironment: Int? = null

    @SerializedName("beta-capable")
    private val betaCapable: Boolean? = null

    fun hostname(): String? {
        return hostname
    }

    fun getDisplayName(): String? {
        return displayName
    }

    fun getOffline(): Boolean? {
        return offline
    }

    fun getCapacityScore(): Int? {
        return capacityScore
    }

    fun getServerFeatureEnvironment(): Int? {
        return serverFeatureEnvironment
    }

    fun getBetaCapable(): Boolean? {
        return betaCapable
    }
}