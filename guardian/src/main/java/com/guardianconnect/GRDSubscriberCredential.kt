package com.guardianconnect

import android.util.Base64
import com.google.gson.Gson
import com.guardianconnect.model.api.SubscriberCredentialsJSON
import com.guardianconnect.util.Constants.Companion.GRD_SUBSCRIBER_CREDENTIAL
import com.guardianconnect.util.GRDKeystore

class GRDSubscriberCredential {

    // Securely store a Subscriber Credential in it's encoded JWT format
    fun storeSubscriberCredentialJWTFormat(subscriberCredential: String) {
        GRDKeystore.instance.saveToKeyStore(GRD_SUBSCRIBER_CREDENTIAL, subscriberCredential)
    }

    // return the currently valid Subscriber Credential in it's encoded JWT format.
    fun retrieveSubscriberCredentialJWTFormat(): String? {
        val subscriberCredential =
            GRDKeystore.instance.retrieveFromKeyStore(GRD_SUBSCRIBER_CREDENTIAL)
        return if (!subscriberCredential.isNullOrEmpty()) {
            return subscriberCredential
        } else {
            null
        }
    }

    // Parse and decode JWT format
    fun parseAndDecodeJWTFormat(jwtString: String): SubscriberCredentialsJSON {
        //split into 3 parts with . delimiter
        val parts: List<String> = jwtString.split(".")
        val payloadString = String(Base64.decode(parts[1], Base64.DEFAULT))
        return Gson().fromJson(payloadString, SubscriberCredentialsJSON::class.java)
    }

    // Returns a boolean indicating whether the JWT is expired or not
    fun isExpired(): Boolean {
        val subscriberCredentialsJSON = retrieveSubscriberCredentialJWTFormat()?.let {
            parseAndDecodeJWTFormat(
                it
            )
        }
        val tokenExpirationDate = subscriberCredentialsJSON?.exp
        val unixTime = System.currentTimeMillis() / 1000
        if (tokenExpirationDate != null) {
            if (tokenExpirationDate < unixTime) {
                return true
            }
        }
        return false
    }
}