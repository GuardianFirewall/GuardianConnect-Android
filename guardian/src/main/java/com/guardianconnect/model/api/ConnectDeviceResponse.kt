package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

data class ConnectDeviceResponse(

    @SerializedName("ep-grd-device-created-at")
    var epGrdDeviceCreatedAt: Long? = null,

    @SerializedName("ep-grd-device-nickname")
    var epGrdDeviceNickname: String? = null,

    @SerializedName("ep-grd-device-pe-token")
    var epGrdDevicePeToken: String? = null,

    @SerializedName("ep-grd-device-pet-expires")
    var epGrdDevicePetExpires: Long? = null,

    @SerializedName("ep-grd-device-uuid")
    var epGrdDeviceUuid: String? = null,

    var currentDevice: Boolean = false
)