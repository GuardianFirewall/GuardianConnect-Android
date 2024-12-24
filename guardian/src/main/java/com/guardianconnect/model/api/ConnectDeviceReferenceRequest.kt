package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

data class ConnectDeviceReferenceRequest(
    @SerializedName("ep-grd-subscriber-identifier") var epGrdSubscriberIdentifier: String? = null,
    @SerializedName("ep-grd-subscriber-secret") var epGrdSubscriberSecret: String? = null,
    @SerializedName("pe-token") var peToken: String? = null,
    @SerializedName("connect-publishable-key") var connectPublishableKey: String? = null
)
