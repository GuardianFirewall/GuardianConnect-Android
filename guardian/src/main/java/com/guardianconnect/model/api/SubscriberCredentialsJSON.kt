package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class SubscriberCredentialsJSON {

    @SerializedName("subscription-type")
    val subscriptionType: String? = null

    @SerializedName("subscription-type-pretty")
    val subscriptionTypePretty: String? = null

    @SerializedName("subscription-expiration-date")
    val subscriptionExpirationDate: Long? = null

    @SerializedName("exp")
    val exp: Long? = null

    @SerializedName("iat")
    val iat: Long? = null

    override fun toString(): String {
        return "SubscriberCredentialsJSON(subscriptionType=$subscriptionType, subscriptionTypePretty=$subscriptionTypePretty, subscriptionExpirationDate=$subscriptionExpirationDate, exp=$exp, iat=$iat)"
    }

}