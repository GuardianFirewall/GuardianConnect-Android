package com.guardianconnect

import com.google.gson.Gson
import com.guardianconnect.api.IOnApiResponse
import com.guardianconnect.api.Repository
import com.guardianconnect.model.api.*
import com.guardianconnect.util.Constants.Companion.GRD_CONNECT_DEVICE
import com.guardianconnect.util.GRDKeystore
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class GRDConnectDevice {

    var createdAt: Date? = null

    var nickname: String? = null

    var peToken: String? = null

    var petExpires: Date? = null

    var uuid: String? = null

    var currentDevice: Boolean? = false

    companion object {
        const val kGRDConnectDeviceKey = "ep-grd-device"
        const val kGRDConnectDeviceNicknameKey = "ep-grd-device-nickname"
        const val kGRDConnectDeviceUUIDKey = "ep-grd-device-uuid"
        const val kGRDConnectDeviceCreatedAtKey = "ep-grd-device-created-at"

        fun initFromMap(map: Map<String, Any>): GRDConnectDevice? {
            val device = map[kGRDConnectDeviceKey] as? Map<String, Any>
            if (device == null) {
                return null
            }

            val newDevice = GRDConnectDevice()
            newDevice.nickname = device[kGRDConnectDeviceNicknameKey] as? String
            newDevice.uuid = device[kGRDConnectDeviceUUIDKey] as? String
            newDevice.peToken = map["pe-token"] as? String

            val petExpiresUnix = map["pet-expires"] as? Double
            if (petExpiresUnix != null) {
                newDevice.petExpires = Date(petExpiresUnix.toLong() * 1000)
            }

            val createdAtUnix = device[kGRDConnectDeviceCreatedAtKey] as? Double
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

    fun initGRDConnectDevice(): Error? {
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
        } catch (error: Error) {
            error
        }
    }

    fun addNewConnectDevice(
        connectDeviceRequest: ConnectDeviceRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        Repository.instance.addNewConnectDevice(connectDeviceRequest, object : IOnApiResponse {
            override fun onSuccess(any: Any?) {
                val connectDeviceResponse = any as ConnectDeviceResponse
                val grdConnectDevice = GRDConnectDevice()
                connectDeviceResponse.epGrdDeviceCreatedAt?.let {
                    grdConnectDevice.createdAt = Date(it * 1000L)
                }
                grdConnectDevice.nickname = connectDeviceResponse.epGrdDeviceNickname
                grdConnectDevice.peToken = connectDeviceResponse.epGrdDevicePeToken
                connectDeviceResponse.epGrdDevicePetExpires?.let {
                    grdConnectDevice.petExpires = Date(it * 1000L)
                }
                grdConnectDevice.uuid = connectDeviceResponse.epGrdDeviceUuid

                store(grdConnectDevice)
                iOnApiResponse.onSuccess(grdConnectDevice)
            }

            override fun onError(error: String?) {
                iOnApiResponse.onError(error)
            }
        })
    }

    fun updateConnectDevice(
        connectDeviceUpdateRequest: ConnectDeviceUpdateRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        Repository.instance.updateConnectDevice(
            connectDeviceUpdateRequest,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val connectDeviceResponse = any as ConnectDeviceResponse
                    iOnApiResponse.onSuccess(connectDeviceResponse)
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                }
            })
    }

    fun deleteConnectDevice(
        connectDeviceDeleteRequest: ConnectDeleteDeviceRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        Repository.instance.deleteConnectDevice(
            connectDeviceDeleteRequest,
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
        iOnApiResponse: IOnApiResponse
    ) {
        val pet = GRDPEToken.instance.retrievePEToken()
        val publishableKey = GRDVPNHelper.connectPublishableKey

        val requestBody: MutableMap<String, Any> = mutableMapOf()
        requestBody["epGrdSubscriberIdentifier"] =
            GRDConnectSubscriber.currentSubscriber()?.identifier as String
        requestBody["epGrdSubscriberSecret"] =
            GRDConnectSubscriber.currentSubscriber()?.secret as String
        requestBody["connectPublishableKey"] = publishableKey
        requestBody["peToken"] = pet as String

        val list = ArrayList<ConnectDeviceResponse>()
        Repository.instance.allConnectDevices(
            requestBody,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    if (any != null) {
                        val anyList = any as List<*>
                        val allDevices =
                            anyList.filterIsInstance<ConnectDeviceResponse>()
                        list.addAll(allDevices)
                        initGRDConnectDevice()
                        iOnApiResponse.onSuccess(list)
                    } else {
                        iOnApiResponse.onSuccess(null)
                    }
                }

                override fun onError(error: String?) {
                    GRDConnectManager.getCoroutineScope().launch {
                        error?.let { GRDVPNHelper.grdErrorFlow.emit(it) }
                    }
                }
            })
    }

    fun store(grdConnectDevice: GRDConnectDevice): Error? {
        return try {
            GRDKeystore.instance.saveToKeyStore(
                GRD_CONNECT_DEVICE,
                Gson().toJson(grdConnectDevice)
            )
            null
        } catch (error: Error) {
            error
        }
    }
}