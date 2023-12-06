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

    var epGrdDeviceCreatedAt: Date? = null

    var epGrdDeviceNickname: String? = null

    var epGrdDevicePeToken: String? = null

    var epGrdDevicePetExpires: Date? = null

    var epGrdDeviceUuid: String? = null

    var currentDevice: Boolean? = false

    fun initGRDConnectDevice(): Error? {
        return try {
            val grdConnectDeviceString =
                GRDKeystore.instance.retrieveFromKeyStore(GRD_CONNECT_DEVICE)
            grdConnectDeviceString.let { string ->
                val grdConnectDevice =
                    Gson().fromJson(string, GRDConnectDevice::class.java)

                this.epGrdDeviceCreatedAt = grdConnectDevice.epGrdDeviceCreatedAt
                this.epGrdDeviceNickname = grdConnectDevice.epGrdDeviceNickname
                this.epGrdDevicePeToken = grdConnectDevice.epGrdDevicePeToken
                this.epGrdDevicePetExpires = grdConnectDevice.epGrdDevicePetExpires
                this.epGrdDeviceUuid = grdConnectDevice.epGrdDeviceUuid
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
                    grdConnectDevice.epGrdDeviceCreatedAt = Date(it * 1000L)
                }
                grdConnectDevice.epGrdDeviceNickname = connectDeviceResponse.epGrdDeviceNickname
                grdConnectDevice.epGrdDevicePeToken = connectDeviceResponse.epGrdDevicePeToken
                connectDeviceResponse.epGrdDevicePetExpires?.let {
                    grdConnectDevice.epGrdDevicePetExpires = Date(it * 1000L)
                }
                grdConnectDevice.epGrdDeviceUuid = connectDeviceResponse.epGrdDeviceUuid

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
        connectDevicesAllDevicesRequest: ConnectDevicesAllDevicesRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        val list = ArrayList<ConnectDeviceResponse>()
        Repository.instance.allConnectDevices(
            connectDevicesAllDevicesRequest,
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