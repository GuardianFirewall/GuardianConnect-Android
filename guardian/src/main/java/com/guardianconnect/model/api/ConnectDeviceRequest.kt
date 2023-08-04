package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class ConnectDeviceRequest: ConnectDevicesAllDevicesRequest() {

    @SerializedName("ep-grd-device-nickname")
    var epGrdDeviceNickname: String? = null

    @SerializedName("ep-grd-device-accepted-tos")
    var epGrdDeviceAcceptedTos: Boolean? = null

}