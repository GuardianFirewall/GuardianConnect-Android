package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class ConnectSubscriberAllDevicesRequest: ConnectDevicesAllDevicesRequest() {

    @SerializedName("ep-grd-subscriber-identifier")
    var epGrdSubscriberIdentifier: String? = null

    @SerializedName("ep-grd-subscriber-secret")
    var epGrdSubscriberSecret: Boolean? = null
}