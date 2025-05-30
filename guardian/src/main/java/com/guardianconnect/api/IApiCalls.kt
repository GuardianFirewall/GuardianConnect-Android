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

    @POST("/api/v1/users/sign-out")
    fun signOutUser(
        @Body signOutUserRequest: SignOutUserRequest
    ): Call<ResponseBody>

    @POST("/api/v1.3/device")
    fun createNewVPNDevice(
        @Body newVPNDevice: NewVPNDevice
    ): Call<NewVPNDeviceResponse>

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

    @GET("/api/v1.3/servers/all-server-regions/{precision}")
    fun requestAllRegionsWithPrecision(
        @Path("precision") precision: String
    ): Call<ResponseBody>

    @POST("/api/v1.3/servers/hostnames-for-region")
    fun requestListOfServersForRegionWithRegionPrecision(
        @Body request: @JvmSuppressWildcards MutableMap<String, Any>
    ): Call<ResponseBody>

    @GET("/api/v1.1/servers/timezones-for-regions")
    fun getListOfSupportedTimeZones(): Call<ResponseBody>

    @POST("/api/v1.2/subscriber-credential/create")
    fun getSubscriberCredential(
        @Body request: @JvmSuppressWildcards MutableMap<String, Any>
    ): Call<ResponseBody>

    @POST("/api/v1.3/partners/subscribers/new")
    fun createNewGRDConnectSubscriber(
        @Body request: @JvmSuppressWildcards Map<String, Any>
    ): Call<ResponseBody>

    @PUT("/api/v1.2/partners/subscriber/update")
    fun updateGRDConnectSubscriber(
        @Body request: @JvmSuppressWildcards MutableMap<String, Any>
    ): Call<ResponseBody>

    @POST("/api/v1.2/partners/subscriber/validate")
    fun validateGRDConnectSubscriber(
        @Body request: @JvmSuppressWildcards MutableMap<String, Any>
    ): Call<ResponseBody>

    @POST("/api/v1.2/partners/subscriber/devices/add")
    fun addConnectDevice(
        @Body request: @JvmSuppressWildcards MutableMap<String, Any>
    ): Call<ResponseBody>

    @PUT("/api/v1.2/partners/subscriber/device/update")
    fun updateConnectDevice(
        @Body request: @JvmSuppressWildcards MutableMap<String, Any>
    ): Call<ResponseBody>

    @POST("/api/v1.2/partners/subscriber/device/delete")
    fun deleteConnectDevice(
        @Body request: @JvmSuppressWildcards MutableMap<String, Any>
    ): Call<ResponseBody>

    @POST("/api/v1.2/partners/subscriber/devices/list")
    fun allConnectDevices(
        @Body request: @JvmSuppressWildcards MutableMap<String, Any>
    ): Call<ResponseBody>

    @POST("/api/v1.2/partners/subscriber/device-reference")
    fun getConnectDeviceReference(
        @Body request: @JvmSuppressWildcards MutableMap<String, Any>
    ): Call<ResponseBody>

    @POST("/api/v1.2/partners/subscriber/account-creation-state")
    fun getAccountSignUpState(
        @Body request: @JvmSuppressWildcards MutableMap<String, Any>
    ): Call<ResponseBody>

    @POST("/api/v1.3/device/{deviceid}/config/filters")
    fun setDeviceFilterConfig(
        @Path("deviceid") deviceid: String,
        @Body jsonObject: JsonObject
    ): Call<ResponseBody>

    @POST("/api/v1.2/partners/subscriber/logout")
    fun logoutConnectSubscriber(
        @Body logoutConnectSubscriberRequest: MutableMap<String, Any>
    ): Call<ResponseBody>
}