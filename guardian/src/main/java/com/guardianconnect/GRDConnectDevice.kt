package com.guardianconnect

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.guardianconnect.api.IOnApiResponse
import com.guardianconnect.api.Repository
import com.guardianconnect.helpers.GRDVPNHelper
import com.guardianconnect.managers.GRDConnectManager
import com.guardianconnect.util.Constants.Companion.GRD_CONNECT_DEVICE
import com.guardianconnect.util.GRDKeystore
import kotlinx.coroutines.launch
import java.util.Date

class GRDConnectDevice {

//    @SerializedName("ep-grd-device-created-at")
//    var epGrdDeviceCreatedAt: Long? = null
//
//    @SerializedName("ep-grd-device-nickname")
//    var epGrdDeviceNickname: String? = null
//
//    @SerializedName("ep-grd-device-pe-token")
//    var epGrdDevicePeToken: String? = null
//
//    @SerializedName("ep-grd-device-pet-expires")
//    var epGrdDevicePetExpires: Long? = null
//
//    @SerializedName("ep-grd-device-uuid")
//    var epGrdDeviceUuid: String? = null

    @SerializedName("ep-grd-device-created-at")
    var createdAtUnix: Long? = null

    var createdAt: Date? = null

    @SerializedName("ep-grd-device-nickname")
    var nickname: String? = null

    @SerializedName("ep-grd-device-pe-token")
    var peToken: String? = null

    @SerializedName("ep-grd-device-pet-expires")
    var petExpiresUnix: Long? = null

    var petExpires: Date? = null

    @SerializedName("ep-grd-device-uuid")
    var uuid: String? = null

    var currentDevice: Boolean? = false

    companion object {
        const val kGRDConnectDeviceKey = "ep-grd-device"
        const val kGRDConnectDeviceNicknameKey = "ep-grd-device-nickname"
        const val kGRDConnectDeviceUUIDKey = "ep-grd-device-uuid"
        const val kGRDConnectDeviceCreatedAtKey = "ep-grd-device-created-at"
        const val kGrdDeviceAcceptedTos = "ep-grd-device-accepted-tos"
        const val peTokenKey = "pe-token"
        const val kGRDConnectSubscriberIdentifierKey = "ep-grd-subscriber-identifier"
        const val kGRDConnectSubscriberSecretKey = "ep-grd-subscriber-secret"

        fun initFromMap(map: Map<String, Any>): GRDConnectDevice? {
            val device = map[kGRDConnectDeviceKey] as? Map<String, Any>
            if (device == null) {
                return null
            }

            val newDevice = GRDConnectDevice()
            newDevice.nickname = device[kGRDConnectDeviceNicknameKey] as? String
            newDevice.uuid = device[kGRDConnectDeviceUUIDKey] as? String
            newDevice.peToken = map["pe-token"] as? String

            val petExpiresUnix = map["pet-expires"] as? Long
            if (petExpiresUnix != null) {
                newDevice.petExpires = Date(petExpiresUnix.toLong() * 1000)
            }

            val createdAtUnix = device[kGRDConnectDeviceCreatedAtKey] as? Long
            if (createdAtUnix != null) {
                newDevice.createdAt = Date(createdAtUnix.toLong() * 1000)
            }

            return newDevice
        }

        fun currentDevice(): GRDConnectDevice? {
            val grdConnectDeviceString =
                GRDKeystore.instance.retrieveFromKeyStore(GRD_CONNECT_DEVICE)
            if (grdConnectDeviceString.isNullOrEmpty()) {
                return null
            }

            return Gson().fromJson(grdConnectDeviceString, GRDConnectDevice::class.java)
        }
    }

    fun initGRDConnectDevice(): Exception? {
        return try {
            val grdConnectDeviceString =
                GRDKeystore.instance.retrieveFromKeyStore(GRD_CONNECT_DEVICE)
            grdConnectDeviceString.let { string ->
                val grdConnectDevice =
                    Gson().fromJson(string, GRDConnectDevice::class.java)

                this.createdAt = grdConnectDevice.createdAt
                this.nickname = grdConnectDevice.nickname
                this.peToken = grdConnectDevice.peToken
                this.petExpires = grdConnectDevice.petExpires
                this.uuid = grdConnectDevice.uuid
            }
            null
        } catch (error: Exception) {
            error
        }
    }

    fun addNewConnectDevice(
        pet: String,
        nickname: String,
        acceptedTOS: Boolean,
        grdConnectSubscriber: GRDConnectSubscriber,
        iOnApiResponse: IOnApiResponse
    ) {
        val requestBody: MutableMap<String, Any> = mutableMapOf()
        requestBody[peTokenKey] = pet
        requestBody[kGRDConnectDeviceNicknameKey] = nickname
        requestBody[kGrdDeviceAcceptedTos] = acceptedTOS
        requestBody[kGRDConnectSubscriberIdentifierKey] =
            grdConnectSubscriber.identifier as String
        requestBody[kGRDConnectSubscriberSecretKey] =
            grdConnectSubscriber.secret as String

        Repository.instance.addNewConnectDevice(
            requestBody,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val connectDeviceResponse = any as MutableMap<String, Any>
                    val grdConnectDevice = initFromMap(connectDeviceResponse)
                    if (grdConnectDevice != null) {
                        store(grdConnectDevice)
                        iOnApiResponse.onSuccess(grdConnectDevice)
                    } else {
                        iOnApiResponse.onError("grdConnectDevice is null")
                    }
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                }
            })
    }

    fun updateConnectDevice(
        pet: String,
        nickname: String,
        iOnApiResponse: IOnApiResponse
    ) {
        val requestBody: MutableMap<String, Any> = mutableMapOf()
        requestBody[peTokenKey] = pet
        requestBody[kGRDConnectDeviceNicknameKey] = nickname
        requestBody[kGRDConnectDeviceUUIDKey] = uuid as String

        Repository.instance.updateConnectDevice(
            requestBody,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val connectDeviceResponse = any as MutableMap<String, Any>
                    val grdConnectDevice = initFromMap(connectDeviceResponse)
                    if (grdConnectDevice != null) {
                        store(grdConnectDevice)
                        iOnApiResponse.onSuccess(grdConnectDevice)
                    } else {
                        iOnApiResponse.onError("grdConnectDevice is null")
                    }
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                }
            })
    }

    fun deleteConnectDevice(
        grdConnectSubscriber: GRDConnectSubscriber,
        grdConnectDevice: GRDConnectDevice,
        iOnApiResponse: IOnApiResponse
    ) {
        val requestBody: MutableMap<String, Any> = mutableMapOf()
        requestBody[kGRDConnectDeviceUUIDKey] = grdConnectDevice.uuid as String

        if (grdConnectDevice.peToken.isNullOrEmpty() == false) {
            requestBody[peTokenKey] = grdConnectDevice.peToken as String

        } else {
            requestBody[kGRDConnectSubscriberIdentifierKey] =
                grdConnectSubscriber.identifier as String
            requestBody[kGRDConnectSubscriberSecretKey] =
                grdConnectSubscriber.secret as String
        }

        Repository.instance.deleteConnectDevice(
            requestBody,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    iOnApiResponse.onSuccess(any)
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                }
            })
    }

    //This one is most likely going to be removed. We have GRDConnectSubscriber.allDevices().
    //For now we’ll leave it unused.
    fun allDevices(
        grdConnectSubscriber: GRDConnectSubscriber,
        iOnApiResponse: IOnApiResponse
    ) {
        val pet = GRDPEToken.instance.retrievePEToken()

        val requestBody: MutableMap<String, Any> = mutableMapOf()
        requestBody[kGRDConnectSubscriberIdentifierKey] =
            grdConnectSubscriber.identifier as String
        requestBody[kGRDConnectSubscriberSecretKey] =
            grdConnectSubscriber.secret as String
        requestBody[peTokenKey] = pet as String

        val list = ArrayList<GRDConnectDevice>()
        Repository.instance.allConnectDevices(
            requestBody,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    if (any != null) {
                        val allDevices = any as List<GRDConnectDevice>
                        list.addAll(allDevices)
                        initGRDConnectDevice()
                        iOnApiResponse.onSuccess(list)
                    } else {
                        iOnApiResponse.onError("Error response body is null")
                    }
                }

                override fun onError(error: String?) {
                    GRDConnectManager.getCoroutineScope().launch {
                        error?.let { GRDVPNHelper.grdErrorFlow.emit(it) }
                    }
                }
            })
    }

    fun store(grdConnectDevice: GRDConnectDevice): Exception? {
        return try {
            GRDKeystore.instance.saveToKeyStore(
                GRD_CONNECT_DEVICE,
                Gson().toJson(grdConnectDevice)
            )
            null
        } catch (error: Exception) {
            error
        }
    }

    override fun toString(): String {
        return "GRDConnectDevice(createdAt=$createdAt, nickname=$nickname, peToken=$peToken, petExpires=$petExpires, uuid=$uuid, currentDevice=$currentDevice)"
    }
}