package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

open class ConnectDevicesAllDevicesRequest {

    // includes in all requests
    @SerializedName("connect-publishable-key")
    var connectPublishableKey: String? = null

    // subscriber-identifier & subscriber-secret are used for requests
    // originating from devices in the Connect Subscriber role
    @SerializedName("ep-grd-subscriber-identifier")
    var epGrdSubscriberIdentifier: String? = null

    @SerializedName("ep-grd-subscriber-secret")
    var epGrdSubscriberSecret: String? = null


    // PE-Tokens are used from devices in the Connect Device role
    @SerializedName("pe-token")
    var peToken: String? = null

}