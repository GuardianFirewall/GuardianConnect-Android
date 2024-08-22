package com.guardianconnect

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.guardianconnect.managers.GRDConnectManager
import com.guardianconnect.model.GRDSubscriberCredentialValidationMethod
import com.guardianconnect.util.Constants.Companion.GRD_SUBSCRIBER_CREDENTIAL
import com.guardianconnect.util.Constants.Companion.kGRDSubscriberCredentialValidationMethod
import com.guardianconnect.util.GRDKeystore
import java.util.Date

class GRDSubscriberCredential {

    @SerializedName("jwt")
    var jwt: String? = null

    @SerializedName("subscription-type")
    var subscriptionType: String? = null

    @SerializedName("subscription-type-pretty")
    var subscriptionTypePretty: String? = null

    @SerializedName("subscription-expiration-date-unix")
    var subscriptionExpirationDateUnix: Long? = null

    var subscriptionExpirationDate: Date? = null

    @SerializedName("token-expiration-date-unix")
    var tokenExpirationDateUnix: Long? = null

    var tokenExpirationDate: Date? = null

    // Securely store a Subscriber Credential in it's encoded JWT format
    fun storeSubscriberCredentialJWTFormat(subscriberCredential: String) {
        GRDKeystore.instance.saveToKeyStore(GRD_SUBSCRIBER_CREDENTIAL, subscriberCredential)
    }

    // Parse and decode JWT format
    fun parseAndDecodeJWTFormat(jwtString: String): GRDSubscriberCredential {
        // Split into 3 parts with . delimiter
        val parts: List<String> = jwtString.split(".")
        val payloadString = String(Base64.decode(parts[1], Base64.DEFAULT))
        val subscriberCredential =
            Gson().fromJson(payloadString, GRDSubscriberCredential::class.java)

        subscriberCredential.jwt = jwtString
        subscriberCredential.subscriptionExpirationDateUnix?.let {
            subscriberCredential.subscriptionExpirationDate = Date(it * 1000)
        }
        subscriberCredential.tokenExpirationDateUnix?.let {
            subscriberCredential.tokenExpirationDate = Date(it * 1000)
        }

        return subscriberCredential
    }

    // Returns a boolean indicating whether the JWT is expired or not
    fun isExpired(): Boolean {
        val currentUnixTime = System.currentTimeMillis() / 1000

        val subscriptionExpirationUnix = subscriptionExpirationDateUnix
        val tokenExpirationUnix = tokenExpirationDateUnix

        if ((subscriptionExpirationUnix != null && subscriptionExpirationUnix < currentUnixTime) ||
            (tokenExpirationUnix != null && tokenExpirationUnix < currentUnixTime)
        ) {
            return true
        }
        return false
    }

    companion object {
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

        // Return the current Subscriber Credential as a GRDSubscriberCredential object
        fun currentSubscriberCredential(): GRDSubscriberCredential? {
            return retrieveSubscriberCredentialJWTFormat()?.let {
                GRDSubscriberCredential().parseAndDecodeJWTFormat(it)
            }
        }

        fun setPreferredValidationMethod(method: GRDSubscriberCredentialValidationMethod) {
            val prefs = GRDConnectManager.getSharedPrefs()
            with(prefs.edit()) {
                putString(kGRDSubscriberCredentialValidationMethod, method.name)
                apply()
            }
        }

        fun preferredValidationMethod(): GRDSubscriberCredentialValidationMethod {
            val methodName = GRDConnectManager.getSharedPrefs().getString(
                kGRDSubscriberCredentialValidationMethod,
                GRDSubscriberCredentialValidationMethod.Invalid.name
            ) ?: GRDSubscriberCredentialValidationMethod.Invalid.name

            return try {
                GRDSubscriberCredentialValidationMethod.valueOf(methodName)
            } catch (e: IllegalArgumentException) {
                GRDSubscriberCredentialValidationMethod.Invalid
            }
        }

    }
}