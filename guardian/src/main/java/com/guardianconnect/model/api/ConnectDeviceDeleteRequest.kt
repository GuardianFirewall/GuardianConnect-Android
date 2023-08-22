package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

open class ConnectDeviceDeleteRequest : ConnectDeleteDeviceRequest() {

    @SerializedName("pe-token")
    var peToken: String? = null

}