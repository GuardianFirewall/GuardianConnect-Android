package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

data class SubscriberCredentialResponse(

    @SerializedName("subscriber-credential")
    var subscriberCredential: String? = null
)