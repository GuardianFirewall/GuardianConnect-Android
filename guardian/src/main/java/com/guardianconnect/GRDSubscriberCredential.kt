package com.guardianconnect

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LazilyParsedNumber
import com.google.gson.reflect.TypeToken
import com.guardianconnect.managers.GRDConnectManager
import com.guardianconnect.model.GRDSubscriberCredentialValidationMethod
import com.guardianconnect.util.Constants.Companion.GRD_SUBSCRIBER_CREDENTIAL
import com.guardianconnect.util.Constants.Companion.kGRDSubscriberCredentialValidationMethod
import com.guardianconnect.util.GRDKeystore
import com.guardianconnect.util.GRDLogger
import java.util.Date

class GRDSubscriberCredential {
    var jwt: 							String? = null
    var subscriptionType: 				String? = null
    var subscriptionTypePretty: 		String? = null
    var subscriptionExpirationDateUnix: Long? = null
    var subscriptionExpirationDate: 	Date? = null
    var tokenExpirationDateUnix: 		Long? = null
    var tokenExpirationDate: 			Date? = null

    // Securely store a Subscriber Credential in it's encoded JWT format
    fun storeSubscriberCredentialJWTFormat(subscriberCredential: String) {
        GRDKeystore.instance.saveToKeyStore(GRD_SUBSCRIBER_CREDENTIAL, subscriberCredential)
    }

    fun parseAndDecodeJWTFormat(jwtString: String): GRDSubscriberCredential {
        val parts: List<String> = jwtString.split(".")
		if (parts.count() < 1) {
			GRDLogger.e("GRDSubscriberCredential", "Trying to process invalid Subscriber Credential (JWT): $jwtString")
			return GRDSubscriberCredential()
		}

        val payloadString = String(Base64.decode(parts[1], Base64.DEFAULT))
		val gson = GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER).create()
		val type = object : TypeToken<Map<String, Any>>() {}.type
		val jwt: Map<String, Any> = gson.fromJson(payloadString, type)

		val subscriberCredential = GRDSubscriberCredential()
		subscriberCredential.subscriptionType 		= jwt["subscription-type"] as? String
		subscriberCredential.subscriptionTypePretty = jwt["subscription-type-pretty"] as? String

        subscriberCredential.jwt = jwtString
		val subscriptionExpirationDateUnix = (jwt["subscription-expiration-date"] as? LazilyParsedNumber)?.toLong() ?: 0L
		if (subscriptionExpirationDateUnix != 0L) {
			subscriberCredential.subscriptionExpirationDateUnix = subscriptionExpirationDateUnix
			subscriberCredential.subscriptionExpirationDate 	= Date(subscriptionExpirationDateUnix * 1000)
		}

		val tokenExpirationDateUnix = (jwt["exp"] as? LazilyParsedNumber)?.toLong() ?: 0L
		if (tokenExpirationDateUnix != 0L) {
			subscriberCredential.tokenExpirationDateUnix = tokenExpirationDateUnix
			subscriberCredential.tokenExpirationDate = Date(tokenExpirationDateUnix * 1000)
		}

        return subscriberCredential
    }

    // Returns a boolean indicating whether the JWT is expired or not
    fun isExpired(): Boolean {
        val currentUnixTime = System.currentTimeMillis() / 1000

		if (subscriptionExpirationDateUnix == null || tokenExpirationDateUnix == null) {
			return true
		}

		//
		// The expiration dates are set by subtracting two
		// days as grace periods in an attempt to ensure
		// that no Subscriber Credential reaches it's actual
		// expiration date during normal use of the service
		val twoDaysInSeconds = 172800L
		val subscriptionGradePeriod = subscriptionExpirationDateUnix!! - twoDaysInSeconds
		val tokenGradePeriod		= tokenExpirationDateUnix!! - twoDaysInSeconds

        if (subscriptionGradePeriod < currentUnixTime || tokenGradePeriod < currentUnixTime) {
            return true
        }

        return false
    }
    
    companion object {
        // Return the current Subscriber Credential as a GRDSubscriberCredential object
        fun currentSubscriberCredential(): GRDSubscriberCredential? {
            val jwt = GRDKeystore.instance.retrieveFromKeyStore(GRD_SUBSCRIBER_CREDENTIAL)
			if (jwt == null) {
				return null
			}
            val subscriberCredential = GRDSubscriberCredential().parseAndDecodeJWTFormat(jwt)
            
            return subscriberCredential
        }
        
        // Remove the Subscriber Credential JWT encoded string out of the shared preferences
        fun remove() {
            GRDConnectManager.getSharedPrefsEditor().remove(GRD_SUBSCRIBER_CREDENTIAL).apply()
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
