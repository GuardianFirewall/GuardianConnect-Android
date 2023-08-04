package com.guardianconnect.api

import com.google.gson.JsonObject
import com.guardianconnect.model.api.NewVPNDevice
import com.guardianconnect.model.*
import com.guardianconnect.model.api.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface IApiCalls {

    @GET("/vpnsrv/api/server-status")
    fun getServerStatus(): Call<ResponseBody>

    @POST("/api/v1.3/device")
    fun createNewVPNDevice(@Body newVPNDevice: NewVPNDevice): Call<NewVPNDeviceResponse>

    @POST("/api/v1.3/device/{deviceid}/verify-credentials")
    fun verifyVPNCredentials(
        @Path("deviceid") deviceid: String,
        @Body apiVpnCredentials: VPNCredentials
    ): Call<ResponseBody>

    @POST("/api/v1.3/device/{deviceid}/invalidate-credentials")
    fun invalidateVPNCredentials(
        @Path("deviceid") deviceid: String,
        @Body apiVpnCredentials: VPNCredentials
    ): Call<ResponseBody>

    @POST("/api/v1.2/device/{deviceid}/alerts")
    fun downloadAlerts(
        @Path("deviceid") deviceid: String,
        @Body alerts: Alerts
    ): Call<ResponseBody>

    @POST("/api/v1.2/device/{deviceid}/set-alerts-download-timestamp")
    fun setAlertsDownloadTimestamp(
        @Path("deviceid") deviceid: String,
        @Body baseRequest: BaseRequest
    ): Call<ResponseBody>

    @GET("/api/v1/servers/all-server-regions")
    fun requestAllGuardianRegions(): Call<ResponseBody>

    @POST("/api/v1.2/servers/hostnames-for-region")
    fun requestListOfServersForRegion(@Body RequestServersForRegion: RequestServersForRegion): Call<ResponseBody>

    @GET("/api/v1.1/servers/timezones-for-regions")
    fun getListOfSupportedTimeZones(): Call<ResponseBody>

    @POST("/api/v1.2/subscriber-credential/create")
    fun getSubscriberCredentialsPEToken(@Body validationMethodPEToken: ValidationMethodPEToken): Call<ResponseBody>

    @POST("/api/v1.2/subscriber-credential/create")
    fun getSubscriberCredentialsIAPAndroid(@Body validationMethodIAPAndroid: ValidationMethodIAPAndroid): Call<ResponseBody>

    @POST("/api/v1.2/subscriber-credential/create")
    fun getSubscriberCredentialsElse(@Body validationMethodElse: ValidationMethodElse): Call<ResponseBody>

    @POST("/api/v1.2/partners/subscribers/new")
    fun createNewGRDConnectSubscriber(@Body grdConnectSubscriberRequest: GRDConnectSubscriberRequest): Call<GRDConnectSubscriberResponse>

    @PUT("/api/v1.2/partners/subscriber/update")
    fun updateGRDConnectSubscriber(@Body connectSubscriberUpdateRequest: ConnectSubscriberUpdateRequest): Call<ConnectSubscriberUpdateResponse>

    @POST("/api/v1.2/partners/subscriber/validate")
    fun validateGRDConnectSubscriber(@Body connectSubscriberValidateRequest: ConnectSubscriberValidateRequest): Call<ConnectSubscriberValidateResponse>

    @POST("/api/v1.2/partners/subscriber/devices/add")
    fun addConnectDevice(@Body connectDeviceRequest: ConnectDeviceRequest): Call<ConnectDeviceResponse>

    @PUT("/api/v1.2/partners/subscriber/device/update")
    fun updateConnectDevice(@Body connectDeviceUpdateRequest: ConnectDeviceUpdateRequest): Call<ConnectDeviceResponse>

    @POST("/api/v1.2/partners/subscriber/device/delete")
    fun deleteConnectDevice(@Body connectDeviceDeleteRequest: ConnectDeviceDeleteRequest): Call<ResponseBody>

    @POST("/api/v1.2/partners/subscriber/devices/list")
    fun allConnectDevices(@Body connectDevicesAllDevicesRequest: ConnectDevicesAllDevicesRequest): Call<ResponseBody>

    @POST("/api/v1.2/device/{deviceid}/alerts")
    fun setDeviceFilterConfig(
        @Path("deviceid") deviceid: String,
        @Body jsonObject: JsonObject
    ): Call<ResponseBody>

    @POST("/api/v1.2/partners/subscriber/logout")
    fun logoutConnectSubscriber(@Body logoutConnectSubscriberRequest: LogoutConnectSubscriberRequest): Call<ResponseBody>
}