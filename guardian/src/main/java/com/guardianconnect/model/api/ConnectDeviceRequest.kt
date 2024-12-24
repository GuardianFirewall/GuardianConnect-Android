package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

data class ConnectDeviceRequest(

    @SerializedName("ep-grd-device-nickname")
    var epGrdDeviceNickname: String? = null,

    @SerializedName("ep-grd-device-accepted-tos")
    var epGrdDeviceAcceptedTos: Boolean? = null
) : ConnectDevicesAllDevicesRequest()