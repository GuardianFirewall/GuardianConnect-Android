package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

data class ConnectSubscriberUpdateResponse(

    @SerializedName("ep-grd-subscriber-created-at")
    var epGrdSubscriberCreatedAt: Long? = null,

    @SerializedName("ep-grd-subscriber-identifier")
    var epGrdSubscriberIdentifier: String? = null,

    @SerializedName("ep-grd-subscription-expiration-date")
    var epGrdSubscriptionExpirationDate: Long? = null,

    @SerializedName("ep-grd-subscription-name-formatted")
    var epGrdSubscriptionNameFormatted: String? = null,

    @SerializedName("ep-grd-subscription-sku")
    var epGrdSubscriptionSku: String? = null
)