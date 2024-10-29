package com.guardianconnect

import com.google.gson.internal.LazilyParsedNumber
import com.guardianconnect.api.IOnApiResponse
import com.guardianconnect.api.Repository
import com.guardianconnect.helpers.GRDVPNHelper
import com.guardianconnect.managers.GRDConnectManager
import com.guardianconnect.model.api.SignOutUserRequest
import com.guardianconnect.util.Constants
import com.guardianconnect.util.Constants.Companion.GRD_PE_TOKEN
import com.guardianconnect.util.Constants.Companion.GRD_PE_TOKEN_CONNECT_API_ENV
import com.guardianconnect.util.Constants.Companion.GRD_PE_TOKEN_EXPIRATION_DATE
import com.guardianconnect.util.GRDKeystore
import kotlinx.coroutines.launch
import java.util.Date

class GRDPEToken {
    var token: String? = null
    var connectAPIEnv: String? = null
    var expirationDate: Date? = null
    var expirationDateUnix: Long? = null

    companion object {
        val instance = GRDPEToken()

        fun currentPEToken(): GRDPEToken? {
            val petFromKeystore = GRDKeystore.instance.retrieveFromKeyStore(GRD_PE_TOKEN)
            if (petFromKeystore == null) {
                return null
            }
            var petExpirationDate = GRDConnectManager.getSharedPrefs().getLong(GRD_PE_TOKEN_EXPIRATION_DATE, -1)
            if (petExpirationDate == -1L) {
                val sixMonthsInMillis = 6L * 30L * 24L * 60L * 60L * 1000L
                petExpirationDate = System.currentTimeMillis() + sixMonthsInMillis
            }

            val pet = GRDPEToken()
            pet.token = petFromKeystore
            pet.connectAPIEnv = GRDConnectManager.getSharedPrefs().getString(GRD_PE_TOKEN_CONNECT_API_ENV, null) ?: Constants.kGRDConnectAPIHostname
            pet.expirationDateUnix = petExpirationDate
            pet.expirationDate = Date(petExpirationDate as Long * 1000)

            return pet
        }

        fun newPETFromMap(map: Map<String, Any>, connectAPIEnv: String?): GRDPEToken? {
            val newPET = GRDPEToken()
            newPET.token = map["pe-token"] as? String ?: return null
            val expirationDateUnix = (map["pet-expires"] as? LazilyParsedNumber)?.toLong() ?: 0L
            if (expirationDateUnix == 0L) {
                return null
            }
            newPET.expirationDateUnix = expirationDateUnix
            newPET.expirationDate = Date((newPET.expirationDateUnix ?: 0) * 1000)
            newPET.connectAPIEnv = connectAPIEnv?: Constants.kGRDConnectAPIHostname

            return newPET
        }
    }

    fun isExpired(): Boolean {
        val currentTimestamp = System.currentTimeMillis() / 1000
        return expirationDateUnix?.let { it < currentTimestamp } ?: false
    }

    fun requiresValidation(): Boolean {
        if (isExpired()) {
            return true
        }

        val currentDateTimestamp = System.currentTimeMillis() / 1000
        val newExpirationDateTimestamp =
            (expirationDateUnix ?: 0) - (7 * 24 * 60 * 60) // Subtract 7 days

        return newExpirationDateTimestamp < currentDateTimestamp
    }

    fun destroy() {
        removePEToken()
        GRDConnectManager.getSharedPrefsEditor().remove(GRD_PE_TOKEN_CONNECT_API_ENV)?.apply()
        GRDConnectManager.getSharedPrefsEditor().remove(GRD_PE_TOKEN_EXPIRATION_DATE)?.apply()
    }

    fun removePEToken() {
        GRDConnectManager.getSharedPrefsEditor().remove(GRD_PE_TOKEN)?.apply()
    }

    fun invalidate() {
        val signOutUserRequest = SignOutUserRequest(
            peToken = currentPEToken()?.token
        )
        Repository.instance.signOutUser(signOutUserRequest,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    GRDConnectManager.getCoroutineScope().launch {
                        GRDVPNHelper.grdMsgFlow.emit(any.toString())
                    }
                }

                override fun onError(error: String?) {
                    error?.let {
                        GRDConnectManager.getCoroutineScope().launch {
                            GRDVPNHelper.grdErrorFlow.emit(error)
                        }
                    }
                }
            }
        )
    }

    fun storePEToken(peToken: String) {
        GRDKeystore.instance.saveToKeyStore(GRD_PE_TOKEN, peToken)
    }

    fun retrievePEToken(): String? {
        return GRDKeystore.instance.retrieveFromKeyStore(GRD_PE_TOKEN)
    }

    fun store() {
        this.token?.let { GRDKeystore.instance.saveToKeyStore(GRD_PE_TOKEN, it) }
        this.expirationDateUnix?.let {
            GRDConnectManager.getSharedPrefs().edit()?.putLong(GRD_PE_TOKEN_EXPIRATION_DATE, it)?.apply()
        }
        GRDConnectManager.getSharedPrefs().edit()?.putString(GRD_PE_TOKEN_CONNECT_API_ENV, this.connectAPIEnv)?.apply()
    }

}