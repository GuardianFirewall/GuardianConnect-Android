package com.guardianconnect

import com.guardianconnect.api.IOnApiResponse
import com.guardianconnect.api.Repository
import com.guardianconnect.model.api.NewVPNDeviceResponse
import com.guardianconnect.model.api.SignOutUserRequest
import com.guardianconnect.util.Constants
import com.guardianconnect.util.Constants.Companion.GRD_PE_TOKEN
import com.guardianconnect.util.Constants.Companion.GRD_PE_TOKEN_CONNECT_API_ENV
import com.guardianconnect.util.Constants.Companion.GRD_PE_TOKEN_EXPIRATION_DATE
import com.guardianconnect.util.GRDKeystore
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.util.Date

class GRDPEToken {

    var token: String? = null
    var connectAPIEnv: String? = null
    var expirationDate: Date? = null
    var expirationDateUnix: Int? = null

    fun currentPEToken(): GRDPEToken? {
        val petFromKeystore = retrievePEToken() ?: return null
        this.token = petFromKeystore
        this.connectAPIEnv =
            GRDConnectManager.getSharedPrefs()?.getString(GRD_PE_TOKEN_CONNECT_API_ENV, null)
        this.expirationDateUnix =
            GRDConnectManager.getSharedPrefs()?.getInt(GRD_PE_TOKEN_EXPIRATION_DATE, -1)
        if (expirationDateUnix != -1) {
            expirationDateUnix?.let {
                this.expirationDate = Date(it * 1000L)
            }
        }
        return this
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
        GRDKeystore.instance.removePEToken()
        GRDConnectManager.getSharedPrefsEditor()?.remove(GRD_PE_TOKEN_CONNECT_API_ENV)?.apply()
        GRDConnectManager.getSharedPrefsEditor()?.remove(GRD_PE_TOKEN_EXPIRATION_DATE)?.apply()
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

    companion object {
        val instance = GRDPEToken()
    }
}