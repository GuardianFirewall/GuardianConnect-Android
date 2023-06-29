package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class SubscriberCredentialsJSON {

    @SerializedName("subscription-type")
    var subscriptionType: String? = null

    @SerializedName("subscription-type-pretty")
    var subscriptionTypePretty: String? = null

    @SerializedName("subscription-expiration-date")
    var subscriptionExpirationDate: Long? = null

    @SerializedName("exp")
    var exp: Long? = null

    @SerializedName("iat")
    var iat: Long? = null

    override fun toString(): String {
        return "SubscriberCredentialsJSON(subscriptionType=$subscriptionType, subscriptionTypePretty=$subscriptionTypePretty, subscriptionExpirationDate=$subscriptionExpirationDate, exp=$exp, iat=$iat)"
    }

}