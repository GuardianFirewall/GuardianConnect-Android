package com.guardianconnect

import com.google.gson.Gson
import com.guardianconnect.api.IOnApiResponse
import com.guardianconnect.api.Repository
import com.guardianconnect.model.api.*
import com.guardianconnect.util.Constants.Companion.GRD_CONNECT_SUBSCRIBER
import com.guardianconnect.util.Constants.Companion.GRD_CONNECT_SUBSCRIBER_EMAIL
import com.guardianconnect.util.Constants.Companion.GRD_CONNECT_SUBSCRIBER_PE_TOKEN_EXP_DATE
import com.guardianconnect.util.Constants.Companion.GRD_CONNECT_SUBSCRIBER_SECRET
import com.guardianconnect.util.GRDKeystore
import java.util.Date

class GRDConnectSubscriber {

    var identifier: String? = null

    var secret: String? = null

    var email: String? = null

    var subscriptionSKU: String? = null

    var subscriptionNameFormatted: String? = null

    var subscriptionExpirationDate: Date? = null

    var createdAt: Date? = null

    var device: GRDConnectDevice? = null


    fun initGRDConnectSubscriber(): Error? {
        return try {
            val secret =
                GRDKeystore.instance.retrieveFromKeyStore(GRD_CONNECT_SUBSCRIBER_SECRET)
            val grdConnectSubscriberString =
                GRDKeystore.instance.retrieveFromKeyStore(GRD_CONNECT_SUBSCRIBER)
            grdConnectSubscriberString.let { string ->
                val grdConnectSubscriber =
                    Gson().fromJson(string, GRDConnectSubscriber::class.java)
                secret.let { secret -> grdConnectSubscriber.secret = secret }

                this.identifier = grdConnectSubscriber.identifier
                this.secret = secret
                this.email = grdConnectSubscriber.email
                this.subscriptionSKU = grdConnectSubscriber.subscriptionSKU
                this.subscriptionNameFormatted = grdConnectSubscriber.subscriptionNameFormatted
                this.subscriptionExpirationDate = grdConnectSubscriber.subscriptionExpirationDate
                this.createdAt = grdConnectSubscriber.createdAt
                this.device = grdConnectSubscriber.device
            }
            null
        } catch (error: Error) {
            return error
        }
    }

    /* Save the GRDConnectSubscriber that encodes the current subscriber object to then store it in
       the Shared Preferences. Secret is stored separately encrypted in the Android Keystore and the
       class property secret is stored as NULL in order to guarantee that the secret is never saved
       unencrypted. The function should return an error or NULL to indicate a successful or failed
       operation */
    fun store(
        grdConnectSubscriber: GRDConnectSubscriber
    ): Error? {
        return try {
            grdConnectSubscriber.secret?.let {
                GRDKeystore.instance.saveToKeyStore(
                    GRD_CONNECT_SUBSCRIBER_SECRET,
                    it
                )
            }
            grdConnectSubscriber.secret = null
            GRDKeystore.instance.saveToKeyStore(
                GRD_CONNECT_SUBSCRIBER,
                Gson().toJson(grdConnectSubscriber)
            )
            null
        } catch (error: Error) {
            error
        }
    }

    /*  Returns the current GRDConnectSubscriber object out of the Shared Preferences with secret */
    fun currentSubscriber(): GRDConnectSubscriber? {
        return try {
            val secret =
                GRDKeystore.instance.retrieveFromKeyStore(GRD_CONNECT_SUBSCRIBER_SECRET)

            val grdConnectSubscriberString =
                GRDKeystore.instance.retrieveFromKeyStore(GRD_CONNECT_SUBSCRIBER)

            grdConnectSubscriberString.let { string ->
                val grdConnectSubscriber =
                    Gson().fromJson(string, GRDConnectSubscriber::class.java)
                secret.let { secret -> grdConnectSubscriber.secret = secret }
                grdConnectSubscriber
            }
        } catch (exception: Exception) {
            null
        }
    }

    /*  Returns the current GRDConnectSubscriber object out of the Shared Preferences */
    fun currentSubscriberWithoutSecret(): GRDConnectSubscriber? {
        return try {
            val grdConnectSubscriberString =
                GRDKeystore.instance.retrieveFromKeyStore(GRD_CONNECT_SUBSCRIBER)

            grdConnectSubscriberString.let {
                Gson().fromJson(it, GRDConnectSubscriber::class.java)
            }
        } catch (exception: Exception) {
            null
        }
    }

    /*  Retrieves the GRDConnectSubscriber secret string from the Android Keystore and set the
        instance’s secret property */
    fun loadFromKeystore(): Error? {
        return try {
            val secret =
                GRDKeystore.instance.retrieveFromKeyStore(GRD_CONNECT_SUBSCRIBER_SECRET)
            if (secret?.isNotEmpty() == true)
                this.secret = secret
            null
        } catch (error: Error) {
            error
        }
    }

    /* Returns an error or the new initialized GRDConnectSubscriber object */
    fun registerNewConnectSubscriber(
        grdConnectSubscriberRequest: GRDConnectSubscriberRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        Repository.instance.createNewGRDConnectSubscriber(
            grdConnectSubscriberRequest,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val grdConnectSubscriber = GRDConnectSubscriber()
                    val grdConnectSubscriberResponse = any as GRDConnectSubscriberResponse
                    grdConnectSubscriber.identifier =
                        grdConnectSubscriberResponse.epGrdSubscriberIdentifier
                    grdConnectSubscriber.secret = grdConnectSubscriberRequest.epGrdSubscriberSecret
                    grdConnectSubscriber.email = grdConnectSubscriberRequest.epGrdSubscriberEmail
                    if (grdConnectSubscriberRequest.epGrdSubscriberEmail != null) {
                        GRDConnectManager.getSharedPrefs()?.edit()
                            ?.putString(
                                GRD_CONNECT_SUBSCRIBER_EMAIL,
                                grdConnectSubscriberRequest.epGrdSubscriberEmail
                            )?.apply()
                    }
                    grdConnectSubscriber.subscriptionSKU =
                        grdConnectSubscriberResponse.epGrdSubscriptionSku
                    grdConnectSubscriber.subscriptionNameFormatted =
                        grdConnectSubscriberResponse.epGrdSubscriptionNameFormatted
                    grdConnectSubscriberResponse.epGrdSubscriptionExpirationDate?.let {
                        grdConnectSubscriber.subscriptionExpirationDate = Date(it * 1000L)
                    }
                    grdConnectSubscriberResponse.epGrdSubscriberCreatedAt?.let {
                        grdConnectSubscriber.createdAt = Date(it * 1000L)
                    }
                    grdConnectSubscriberResponse.peToken?.let {
                        GRDPEToken.instance.storePEToken(it)
                    }
                    grdConnectSubscriberResponse.petExpires?.let {
                        GRDKeystore.instance.saveToKeyStore(
                            GRD_CONNECT_SUBSCRIBER_PE_TOKEN_EXP_DATE,
                            it.toString()
                        )
                    }
                    grdConnectSubscriber.device = grdConnectSubscriberResponse.grdConnectDevice
                    store(grdConnectSubscriber)
                    iOnApiResponse.onSuccess(grdConnectSubscriber)
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                }
            })
    }

    fun updateConnectSubscriber(
        connectSubscriberUpdateRequest: ConnectSubscriberUpdateRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        Repository.instance.updateGRDConnectSubscriber(
            connectSubscriberUpdateRequest,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val grdConnectSubscriber = GRDConnectSubscriber()
                    val connectSubscriberUpdateResponse =
                        any as ConnectSubscriberUpdateResponse
                    grdConnectSubscriber.identifier =
                        connectSubscriberUpdateResponse.epGrdSubscriberIdentifier
                    grdConnectSubscriber.secret =
                        connectSubscriberUpdateRequest.epGrdSubscriberSecret
                    grdConnectSubscriber.email = connectSubscriberUpdateRequest.epGrdSubscriberEmail
                    grdConnectSubscriber.subscriptionSKU =
                        connectSubscriberUpdateResponse.epGrdSubscriptionSku
                    grdConnectSubscriber.subscriptionNameFormatted =
                        connectSubscriberUpdateResponse.epGrdSubscriptionNameFormatted
                    connectSubscriberUpdateResponse.epGrdSubscriptionExpirationDate?.let {
                        grdConnectSubscriber.subscriptionExpirationDate = Date(it * 1000L)
                    }
                    connectSubscriberUpdateResponse.epGrdSubscriberCreatedAt?.let {
                        grdConnectSubscriber.createdAt = Date(it * 1000L)
                    }
                    iOnApiResponse.onSuccess(grdConnectSubscriber)
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                }
            })
    }

    fun validateConnectSubscriber(
        connectSubscriberValidateRequest: ConnectSubscriberValidateRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        Repository.instance.validateGRDConnectSubscriber(
            connectSubscriberValidateRequest,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val grdConnectSubscriber = GRDConnectSubscriber()
                    val connectSubscriberValidateResponse =
                        any as ConnectSubscriberValidateResponse
                    grdConnectSubscriber.identifier =
                        connectSubscriberValidateRequest.epGrdSubscriberIdentifier
                    grdConnectSubscriber.secret =
                        connectSubscriberValidateRequest.epGrdSubscriberSecret
                    val email =
                        GRDConnectManager.getSharedPrefs()
                            ?.getString(GRD_CONNECT_SUBSCRIBER_EMAIL, "")
                    if (!email.isNullOrEmpty()) {
                        grdConnectSubscriber.email = email
                    }
                    grdConnectSubscriber.subscriptionSKU =
                        connectSubscriberValidateResponse.epGrdSubscriptionSku
                    grdConnectSubscriber.subscriptionNameFormatted =
                        connectSubscriberValidateResponse.epGrdSubscriptionNameFormatted
                    connectSubscriberValidateResponse.epGrdSubscriptionExpirationDate?.let {
                        grdConnectSubscriber.subscriptionExpirationDate = Date(it * 1000L)
                    }
                    connectSubscriberValidateResponse.epGrdSubscriberCreatedAt?.let {
                        grdConnectSubscriber.createdAt = Date(it * 1000L)
                    }
                    connectSubscriberValidateResponse.peToken?.let {
                        GRDPEToken.instance.storePEToken(it)
                    }
                    iOnApiResponse.onSuccess(grdConnectSubscriber)
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                }
            })
    }

    fun logoutConnectSubscriber(
        iOnApiResponse: IOnApiResponse
    ) {
        val pet = GRDPEToken.instance.retrievePEToken()
        val publishableKey = GRDVPNHelper.connectPublishableKey
        if (pet != null && publishableKey.isNotEmpty()) {
            val logoutConnectSubscriberRequest = LogoutConnectSubscriberRequest()
            logoutConnectSubscriberRequest.peToken = pet
            logoutConnectSubscriberRequest.connectPublishableKey = publishableKey
            Repository.instance.logoutConnectSubscriber(
                logoutConnectSubscriberRequest,
                object : IOnApiResponse {
                    override fun onSuccess(any: Any?) {
                        iOnApiResponse.onSuccess(null)
                    }

                    override fun onError(error: String?) {
                        iOnApiResponse.onError(error)
                    }
                })
        }
    }

    fun connectDeviceReference(
        connectDeviceReferenceRequest: ConnectDeviceReferenceRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        Repository.instance.getConnectDeviceReference(
            connectDeviceReferenceRequest,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    if (any != null) {
                        val connectDeviceReferenceResponse =
                            any as ConnectDeviceReferenceResponse
                        val grdConnectDevice = GRDConnectDevice()

                        connectDeviceReferenceResponse.epGrdDeviceCreatedAt?.let {
                            grdConnectDevice.epGrdDeviceCreatedAt = Date(it * 1000L)
                        }
                        grdConnectDevice.epGrdDeviceNickname =
                            connectDeviceReferenceResponse.epGrdDeviceNickname
                        grdConnectDevice.epGrdDevicePeToken =
                            connectDeviceReferenceResponse.epGrdDeviceSubscriberPet
                        grdConnectDevice.epGrdDeviceUuid =
                            connectDeviceReferenceResponse.epGrdDeviceUuid
                        grdConnectDevice.currentDevice = true

                        iOnApiResponse.onSuccess(grdConnectDevice)
                    } else {
                        iOnApiResponse.onError("No GRDConnectDevice refs available")
                    }
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                }
            }
        )
    }

    fun allDevices(
        connectSubscriberAllDevicesRequest: ConnectDevicesAllDevicesRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        val list = ArrayList<ConnectDeviceResponse>()
        Repository.instance.allConnectDevices(
            connectSubscriberAllDevicesRequest,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    if (any != null) {
                        val anyList = any as List<*>
                        val allDevices =
                            anyList.filterIsInstance<ConnectDeviceResponse>()
                        list.addAll(allDevices)

                        val currentDevice = this@GRDConnectSubscriber.device
                        if (currentDevice != null) {
                            list.forEach { device ->
                                if (device.epGrdDeviceUuid == currentDevice.epGrdDeviceUuid) {
                                    device.currentDevice = true
                                }
                            }
                        }
                        iOnApiResponse.onSuccess(list)
                    } else {
                        iOnApiResponse.onSuccess(null)
                    }
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                }
            })
    }

    fun checkGuardianAccountSetupState(iOnApiResponse: IOnApiResponse) {
        val accountSignUpStateRequest = AccountSignUpStateRequest()
        accountSignUpStateRequest.epGrdSubscriberIdentifier = getLocalInstance()?.identifier
        accountSignUpStateRequest.epGrdSubscriberSecret = getLocalInstance()?.secret
        Repository.instance.getAccountCreationState(
            accountSignUpStateRequest,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    iOnApiResponse.onSuccess(null)
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                }

            })
    }

    companion object {
        fun getLocalInstance(): GRDConnectSubscriber? {
            val secret = GRDKeystore.instance.retrieveFromKeyStore(GRD_CONNECT_SUBSCRIBER_SECRET)
            val grdConnectSubscriberString =
                GRDKeystore.instance.retrieveFromKeyStore(GRD_CONNECT_SUBSCRIBER)

            return if (!secret.isNullOrEmpty() && !grdConnectSubscriberString.isNullOrEmpty()) {
                val grdConnectSubscriber =
                    Gson().fromJson(grdConnectSubscriberString, GRDConnectSubscriber::class.java)
                grdConnectSubscriber.secret = secret
                grdConnectSubscriber
            } else {
                null
            }
        }
    }
}