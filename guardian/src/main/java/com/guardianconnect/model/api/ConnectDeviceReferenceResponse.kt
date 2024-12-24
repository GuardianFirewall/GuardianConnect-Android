package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

data class ConnectDeviceReferenceResponse(
    @SerializedName("ep-grd-device-nickname") var epGrdDeviceNickname: String? = null,
    @SerializedName("ep-grd-device-uuid") var epGrdDeviceUuid: String? = null,
    @SerializedName("ep-grd-device-created-at") var epGrdDeviceCreatedAt: Long? = null,
    @SerializedName("ep-grd-device-subscriber-pet") var epGrdDeviceSubscriberPet: String? = null
)
