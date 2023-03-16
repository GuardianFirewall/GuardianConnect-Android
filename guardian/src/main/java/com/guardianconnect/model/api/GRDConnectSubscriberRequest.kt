package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class GRDConnectSubscriberRequest {

    @SerializedName("ep-grd-subscriber-identifier")
    var epGrdSubscriberIdentifier: String? = null

    @SerializedName("ep-grd-subscriber-secret")
    var epGrdSubscriberSecret: String? = null

    @SerializedName("ep-grd-subscriber-accepted-tos")
    var acceptedTos: Boolean? = null

    @SerializedName("ep-grd-subscriber-email")
    var epGrdSubscriberEmail: String? = null

    @SerializedName("connect-public-key")
    var connectPublicKey: String? = null

}