package com.guardianconnect

import android.util.Log
import com.google.gson.Gson
import com.guardianconnect.api.IOnApiResponse
import com.guardianconnect.api.Repository
import com.guardianconnect.helpers.GRDVPNHelper
import com.guardianconnect.util.Constants.Companion.GRD_CONNECT_SUBSCRIBER
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

    companion object {
        const val kGRDConnectSubscriberIdentifierKey = "ep-grd-subscriber-identifier"
        const val kGRDConnectSubscriberSecretKey = "ep-grd-subscriber-secret"
        const val kGRDConnectSubscriberEmailKey = "ep-grd-subscriber-email"
        const val kGuardianConnectSubscriberPETNickname = "ep-grd-subscriber-pet-nickname"
        const val kGRDConnectSubscriberSubscriptionSKUKey = "ep-grd-subscription-sku"
        const val kGRDConnectSubscriberSubscriptionNameFormattedKey =
            "ep-grd-subscription-name-formatted"
        const val kGRDConnectSubscriberSubscriptionExpirationDateKey =
            "ep-grd-subscription-expiration-date"
        const val kGRDConnectSubscriberCreatedAtKey = "ep-grd-subscriber-created-at"
        const val kGRDConnectSubscriberAcceptedTOSKey = "ep-grd-subscriber-accepted-tos"
        const val peTokenKey = "pe-token"
        const val connectPublishableKey = "connect-publishable-key"

        fun initFromMap(map: Map<String, Any>): GRDConnectSubscriber {
            val newSubscriber = GRDConnectSubscriber()
            newSubscriber.identifier = map[kGRDConnectSubscriberIdentifierKey] as? String
            newSubscriber.secret = map[kGRDConnectSubscriberSecretKey] as? String
            newSubscriber.email = map[kGRDConnectSubscriberEmailKey] as? String
            newSubscriber.subscriptionSKU = map[kGRDConnectSubscriberSubscriptionSKUKey] as? String
            newSubscriber.subscriptionNameFormatted =
                map[kGRDConnectSubscriberSubscriptionNameFormattedKey] as? String

            val subDateUnix = map[kGRDConnectSubscriberSubscriptionExpirationDateKey] as? Long
            if (subDateUnix != null) {
                newSubscriber.subscriptionExpirationDate = Date(subDateUnix.toLong() * 1000)
            }
            val createdAtUnix = map[kGRDConnectSubscriberCreatedAtKey] as? Long
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

    fun initGRDConnectSubscriber(): Exception? {
        val secret = GRDKeystore.instance.retrieveFromKeyStore(GRD_CONNECT_SUBSCRIBER_SECRET)
        val grdConnectSubscriberString = GRDKeystore.instance.retrieveFromKeyStore(GRD_CONNECT_SUBSCRIBER)
        val grdConnectSubscriber = Gson().fromJson(grdConnectSubscriberString, GRDConnectSubscriber::class.java)
        if (secret.isNullOrEmpty() || grdConnectSubscriberString.isNullOrEmpty() || grdConnectSubscriber == null) {
            return Exception("Connect Subscriber missing")
        }

        this.identifier = grdConnectSubscriber.identifier
        this.secret = secret
        this.email = grdConnectSubscriber.email
        this.subscriptionSKU = grdConnectSubscriber.subscriptionSKU
        this.subscriptionNameFormatted = grdConnectSubscriber.subscriptionNameFormatted
        this.subscriptionExpirationDate = grdConnectSubscriber.subscriptionExpirationDate
        this.createdAt = grdConnectSubscriber.createdAt
        this.device = grdConnectSubscriber.device

        return null
    }

    /* Save the GRDConnectSubscriber that encodes the current subscriber object to then store it in
       the Shared Preferences. Secret is stored separately encrypted in the Android Keystore and the
       class property secret is stored as NULL in order to guarantee that the secret is never saved
       unencrypted. The function should return an error or NULL to indicate a successful or failed
       operation */
    fun store(
        grdConnectSubscriber: GRDConnectSubscriber
    ): Exception? {
        return try {
            grdConnectSubscriber.secret?.let {
                GRDKeystore.instance.saveToKeyStore(
                    GRD_CONNECT_SUBSCRIBER_SECRET,
                    it
                )
            }

            // Temporarily copy the secret
            val tmpSecret = grdConnectSubscriber.secret

            // Set the secret to null on the object so that
            // it is not written into the shared preferences
            // as a plaintext string
            grdConnectSubscriber.secret = null

            // store the actual object
            GRDKeystore.instance.saveToKeyStore(
                GRD_CONNECT_SUBSCRIBER,
                Gson().toJson(grdConnectSubscriber)
            )

            // restore the secret
            grdConnectSubscriber.secret = tmpSecret
            null
        } catch (error: Exception) {
            error
        }
    }

    /*  Retrieves the GRDConnectSubscriber secret string from the Android Keystore and set the
        instance’s secret property */
    fun loadFromKeystore(): Exception? {
        return try {
            val secret =
                GRDKeystore.instance.retrieveFromKeyStore(GRD_CONNECT_SUBSCRIBER_SECRET)
            if (secret?.isNotEmpty() == true)
                this.secret = secret
            null
        } catch (error: Exception) {
            error
        }
    }

    /* Returns an error or the new initialized GRDConnectSubscriber object */
    fun registerNewConnectSubscriber(
        acceptedTOS: Boolean,
        deviceNickname: String,
        iOnApiResponse: IOnApiResponse
    ) {
        val requestBody = mutableMapOf<String, Any>(
            kGRDConnectSubscriberIdentifierKey to this.identifier.toString(),
            kGRDConnectSubscriberSecretKey to this.secret.toString(),
            kGRDConnectSubscriberAcceptedTOSKey to acceptedTOS,
            kGuardianConnectSubscriberPETNickname to deviceNickname
        )
        Repository.instance.createNewGRDConnectSubscriber(
            requestBody,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val response = any as MutableMap<String, Any>
                    val pet = GRDPEToken.newPETFromMap(
                        response, GRDVPNHelper.connectAPIHostname
                    )
                    pet?.store()
                    response[kGRDConnectSubscriberSecretKey] = this@GRDConnectSubscriber.secret.toString()
                    val grdConnectSubscriber = initFromMap(response)
                    grdConnectSubscriber.secret = requestBody[kGRDConnectSubscriberSecretKey] as? String
                    grdConnectSubscriber.identifier = requestBody[kGRDConnectSubscriberIdentifierKey] as? String
                    store(grdConnectSubscriber)
                    iOnApiResponse.onSuccess(grdConnectSubscriber)
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                }
            })
    }

    fun updateConnectSubscriber(
        iOnApiResponse: IOnApiResponse
    ) {
        val pet = GRDPEToken.instance.retrievePEToken()

        val requestBody: MutableMap<String, Any> = mutableMapOf()
        requestBody[kGRDConnectSubscriberIdentifierKey] =
            identifier as String
        requestBody[kGRDConnectSubscriberSecretKey] =
            secret as String
        requestBody[peTokenKey] = pet as String
        requestBody[kGRDConnectSubscriberEmailKey] = email as String

        Repository.instance.updateGRDConnectSubscriber(
            requestBody,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val response = any as MutableMap<String, Any>
                    val grdConnectSubscriber = initFromMap(response)
                    iOnApiResponse.onSuccess(grdConnectSubscriber)
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                }
            })
    }

    fun validateConnectSubscriber(
        iOnApiResponse: IOnApiResponse
    ) {
        val pet = GRDPEToken.instance.retrievePEToken()

        val requestBody: MutableMap<String, Any> = mutableMapOf()
        requestBody[kGRDConnectSubscriberIdentifierKey] =
            identifier as String
        requestBody[kGRDConnectSubscriberSecretKey] =
            secret as String
        requestBody[peTokenKey] = pet as String

        Repository.instance.validateGRDConnectSubscriber(
            requestBody,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val response = any as MutableMap<String, Any>
                    val grdConnectSubscriber = initFromMap(response)
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
            val logoutConnectSubscriberRequest: MutableMap<String, Any> = mutableMapOf()
            logoutConnectSubscriberRequest[peTokenKey] = pet
            logoutConnectSubscriberRequest[connectPublishableKey] = publishableKey
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
        } else {
            iOnApiResponse.onError("PET or the publishable key is null")
        }
    }

    fun connectDeviceReference(
        iOnApiResponse: IOnApiResponse
    ) {
        val pet = GRDPEToken.instance.retrievePEToken()

        val requestBody: MutableMap<String, Any> = mutableMapOf()
        requestBody[kGRDConnectSubscriberIdentifierKey] =
            identifier as String
        requestBody[kGRDConnectSubscriberSecretKey] =
            secret as String
        requestBody[peTokenKey] = pet as String

        Repository.instance.getConnectDeviceReference(
            requestBody,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    if (any != null) {
                        val response = any as MutableMap<String, Any>
                        val grdConnectDevice = initFromMap(response).device

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
        iOnApiResponse: IOnApiResponse
    ) {
        // Check if identifier or secret are null or empty
        if (identifier.isNullOrEmpty() || secret.isNullOrEmpty()) {
            iOnApiResponse.onSuccess(ArrayList<GRDConnectDevice>())
            return
        }

        val requestBody: MutableMap<String, Any> = mutableMapOf()
        requestBody[kGRDConnectSubscriberIdentifierKey] =
            identifier as String
        requestBody[kGRDConnectSubscriberSecretKey] =
            secret as String
        //
        // Note from CJ 2024-08-02
        // Commenting this out for the time being
        // as we're currently only allowing Connect Subscribers
        // to list all devices which are always supposed to authenticate
        // with the Connect Subscriber identifier & secret
        //requestBody[peTokenKey] = pet as String

        val list = ArrayList<GRDConnectDevice>()
        Repository.instance.allConnectDevices(
            requestBody,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    if (any != null) {
                        val anyList = any as List<*>
                        val allDevices =
                            anyList.filterIsInstance<GRDConnectDevice>()
                        for (device in allDevices) {
                            device.createdAt = Date((device.createdAtUnix?: 0) * 1000)
                            device.petExpires = Date((device.petExpiresUnix?: 0) * 1000)
                        }
                        list.addAll(allDevices)

                        val currentDevice = this@GRDConnectSubscriber.device
                        if (currentDevice != null) {
                            list.forEach { device ->
                                if (device.uuid == currentDevice.uuid) {
                                    device.currentDevice = true
                                }
                            }
                        }
                        iOnApiResponse.onSuccess(list)
                    } else {
                        iOnApiResponse.onSuccess(ArrayList<GRDConnectDevice>())
                    }
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                }
            })
    }

    fun checkGuardianAccountSetupState(
        iOnApiResponse: IOnApiResponse
    ) {
        val accountSignUpStateRequest: MutableMap<String, Any> = mutableMapOf()
        accountSignUpStateRequest[kGRDConnectSubscriberIdentifierKey] =
            identifier as String
        accountSignUpStateRequest[kGRDConnectSubscriberSecretKey] =
            secret as String

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

    override fun toString(): String {
        return "GRDConnectSubscriber(identifier=$identifier, secret=$secret, email=$email, subscriptionSKU=$subscriptionSKU, subscriptionNameFormatted=$subscriptionNameFormatted, subscriptionExpirationDate=$subscriptionExpirationDate, createdAt=$createdAt, device=$device)"
    }
}