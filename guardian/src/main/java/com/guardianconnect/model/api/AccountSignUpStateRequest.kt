package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

data class AccountSignUpStateRequest(
    @SerializedName("connect-publishable-key") var connectPublishableKey: String? = null,
    @SerializedName("ep-grd-subscriber-identifier") var epGrdSubscriberIdentifier: String? = null,
    @SerializedName("ep-grd-subscriber-secret") var epGrdSubscriberSecret: String? = null
)
