package com.guardianconnect

import com.google.gson.Gson
import com.guardianconnect.api.IOnApiResponse
import com.guardianconnect.api.Repository
import com.guardianconnect.model.api.*
import com.guardianconnect.util.Constants.Companion.GRD_CONNECT_SUBSCRIBER
import com.guardianconnect.util.Constants.Companion.GRD_CONNECT_SUBSCRIBER_EMAIL
import com.guardianconnect.util.Constants.Companion.GRD_CONNECT_SUBSCRIBER_SECRET
import com.guardianconnect.util.GRDKeystore
import java.util.Date
import kotlin.jvm.Throws

class GRDConnectSubscriber {

    var identifier: String? = null

    var secret: String? = null

    var email: String? = null

    var subscriptionSKU: String? = null

    var subscriptionNameFormatted: String? = null

    var subscriptionExpirationDate: Date? = null

    var createdAt: Date? = null

    var device: GRDConnectDevice? = null

    companion object {
        const val kGRDConnectSubscriberIdentifierKey                   = "ep-grd-subscriber-identifier"
        const val kGRDConnectSubscriberSecretKey                       = "ep-grd-subscriber-secret"
        const val kGRDConnectSubscriberEmailKey                        = "ep-grd-subscriber-email"
        const val kGuardianConnectSubscriberPETNickname                = "ep-grd-subscriber-pet-nickname"
        const val kGRDConnectSubscriberSubscriptionSKUKey              = "ep-grd-subscription-sku"
        const val kGRDConnectSubscriberSubscriptionNameFormattedKey    = "ep-grd-subscription-name-formatted"
        const val kGRDConnectSubscriberSubscriptionExpirationDateKey   = "ep-grd-subscription-expiration-date"
        const val kGRDConnectSubscriberCreatedAtKey                    = "ep-grd-subscriber-created-at"
        const val kGRDConnectSubscriberAcceptedTOSKey                  = "ep-grd-subscriber-accepted-tos"

        fun initFromMap(map: Map<String, Any>): GRDConnectSubscriber {
            val newSubscriber = GRDConnectSubscriber()
            newSubscriber.identifier = map[kGRDConnectSubscriberIdentifierKey] as? String
            newSubscriber.secret = map[kGRDConnectSubscriberSecretKey] as? String
            newSubscriber.email = map[kGRDConnectSubscriberEmailKey] as? String
            newSubscriber.subscriptionSKU = map[kGRDConnectSubscriberSubscriptionSKUKey] as? String
            newSubscriber.subscriptionNameFormatted = map[kGRDConnectSubscriberSubscriptionNameFormattedKey] as? String

            val subDateUnix = map[kGRDConnectSubscriberSubscriptionExpirationDateKey] as? Double
            if (subDateUnix != null) {
                newSubscriber.subscriptionExpirationDate = Date(subDateUnix.toLong() * 1000)
            }
            val createdAtUnix = map[kGRDConnectSubscriberCreatedAtKey] as? Double
            if (createdAtUnix != null) {
                newSubscriber.createdAt = Date(createdAtUnix.toLong() * 1000)
            }

            // Skip for the time being until the same function is
            // implemented in GRDConnectDevice
            newSubscriber.device = GRDConnectDevice.initFromMap(map)
            newSubscriber.device?.currentDevice = true

            return newSubscriber
        }

        fun currentSubscriber(): GRDConnectSubscriber? {
            val secret = GRDKeystore.instance.retrieveFromKeyStore(GRD_CONNECT_SUBSCRIBER_SECRET)
            val grdConnectSubscriberString = GRDKeystore.instance.retrieveFromKeyStore(GRD_CONNECT_SUBSCRIBER)

            return if (!secret.isNullOrEmpty() && !grdConnectSubscriberString.isNullOrEmpty()) {
                val grdConnectSubscriber = Gson().fromJson(grdConnectSubscriberString, GRDConnectSubscriber::class.java)
                grdConnectSubscriber.secret = secret
                grdConnectSubscriber
            } else {
                null
            }
        }
    }

    @Throws(Exception::class)
    fun initGRDConnectSubscriber(): Exception? {
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
        } catch (exception: Exception) {
            return exception
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
        acceptedTOS: Boolean,
        deviceNickname: String,
        iOnApiResponse: IOnApiResponse
    ) {
        val requestBody = mutableMapOf<String, Any>(kGRDConnectSubscriberIdentifierKey to this.identifier.toString(), kGRDConnectSubscriberSecretKey to this.secret.toString(), kGRDConnectSubscriberAcceptedTOSKey to acceptedTOS, kGuardianConnectSubscriberPETNickname to deviceNickname)
        Repository.instance.createNewGRDConnectSubscriber(
            requestBody,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val response = any as Map<String, Any>
                    val pet = GRDPEToken.newPETFromMap(response, GRDVPNHelper.connectAPIHostname)
                    pet?.store()
                    val grdConnectSubscriber = initFromMap(response)

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
                            grdConnectDevice.createdAt = Date(it * 1000L)
                        }
                        grdConnectDevice.nickname =
                            connectDeviceReferenceResponse.epGrdDeviceNickname
                        grdConnectDevice.peToken =
                            connectDeviceReferenceResponse.epGrdDeviceSubscriberPet
                        grdConnectDevice.uuid =
                            connectDeviceReferenceResponse.epGrdDeviceUuid
                        grdConnectDevice.currentDevice = true

                        //
                        // Note from CJ 2023-12-06
                        // Setting the GRDConnectSubscriber instance's device to the GRDConnectDevice
                        // that was returned by the server to allow for easy access to the data
                        this@GRDConnectSubscriber.device = grdConnectDevice
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
                                if (device.epGrdDeviceUuid == currentDevice.uuid) {
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
        accountSignUpStateRequest.epGrdSubscriberIdentifier = currentSubscriber()?.identifier
        accountSignUpStateRequest.epGrdSubscriberSecret = currentSubscriber()?.secret
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
}