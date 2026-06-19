package com.guardianconnect.api

import com.google.gson.JsonObject
import com.guardianconnect.model.api.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface IApiCalls {
	//
	// Secure Gateway endpoints
    @GET("/api/v1.3/server-status")
    fun getServerStatus(): Call<ResponseBody>

	@GET("/api/v1.3/server-status/{device-id}")
	fun getServerStatusForDeviceId(@Path("device-id") deviceId: String): Call<ResponseBody>

	@POST("/api/v1.4/device-credentials")
	fun createNewVPNDevice(
		@Body requestData: @JvmSuppressWildcards MutableMap<String, Any>
	): Call<ResponseBody>

	@GET("/api/v1.4/device/{device-id}/verify-credentials")
	fun verifyVPNCredentials(
		@Path("device-id") deviceId: String,
		@Header("grd-api-auth-token") apiAuthToken: String
	): Call<ResponseBody>

	@POST("/api/v1.4/device/{device-id}/invalidate-credentials")
	fun invalidateVPNCredentials(
		@Path("device-id") deviceId: String,
		@Body requestData: @JvmSuppressWildcards MutableMap<String, Any>
	): Call<ResponseBody>

	@POST("/api/v1.4/device/{device-id}/alerts")
	fun downloadAlerts(
		@Path("device-id") deviceId: String,
		@Body requestData: @JvmSuppressWildcards MutableMap<String, Any>
	): Call<ResponseBody>

	@GET("/api/v1.4/device/{device-id}/config/filters")
	fun getDeviceFilterConfig(
		@Path("device-id") deviceId: String,
		@Header("grd-api-auth-token") apiAuthToken: String
	): Call<ResponseBody>

	@POST("/api/v1.4/device/{device-id}/config/filters")
	fun setDeviceFilterConfig(
		@Path("device-id") deviceId: String,
		@Body requestData: JsonObject
	): Call<ResponseBody>

	@GET("/api/v1.4/device/{device-id}/config/rules")
	fun getClientRules(
		@Path("device-id") deviceId: String,
		@Header("grd-api-auth-token") apiAuthToken: String
	): Call<ResponseBody>

	@POST("/api/v1.4/device/{device-id}/config/rules")
	fun setClientRules(
		@Path("device-id") deviceId: String,
		@Body requestData: @JvmSuppressWildcards MutableMap<String, Any>
	): Call<ResponseBody>

	@GET("/api/v1.4/device/{device-id}/config/multihop")
	fun getMultihopExitRegion(
		@Path("device-id") deviceId: String,
		@Header("grd-api-auth-token") apiAuthToken: String
	): Call<ResponseBody>

	@POST("/api/v1.4/device/{device-id}/config/multihop")
	fun settMultihopExitRegion(
		@Path("device-id") deviceId: String,
		@Body requestData: @JvmSuppressWildcards MutableMap<String, Any>
	): Call<ResponseBody>


	//
	// Connect API endpoints
	@POST("/api/v1.2/subscriber-credential/create")
	fun getSubscriberCredential(
		@Body request: @JvmSuppressWildcards MutableMap<String, Any>
	): Call<ResponseBody>

    @POST("/api/v1/users/sign-out")
    fun signOutUser(
        @Body signOutUserRequest: SignOutUserRequest
    ): Call<ResponseBody>

    @GET("/api/v1/servers/all-server-regions")
    fun requestAllGuardianRegions(): Call<ResponseBody>

    @GET("/api/v1.3/servers/all-server-regions/{precision}")
    fun requestAllRegionsWithPrecision(
        @Path("precision") precision: String
    ): Call<ResponseBody>

    @POST("/api/v1.3/servers/hostnames-for-region")
    fun requestListOfServersForRegionWithRegionPrecision(
        @Body requestData: @JvmSuppressWildcards MutableMap<String, Any>
    ): Call<ResponseBody>

    @GET("/api/v1.1/servers/timezones-for-regions")
    fun getListOfSupportedTimeZones(): Call<ResponseBody>

    //
	// Connect API endpoints
	// 	Connect Subscriber
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

	@POST("/api/v1.2/partners/subscriber/account-creation-state")
	fun getAccountSignUpState(
		@Body request: @JvmSuppressWildcards MutableMap<String, Any>
	): Call<ResponseBody>

	@POST("/api/v1.2/partners/subscriber/logout")
	fun logoutConnectSubscriber(
		@Body logoutConnectSubscriberRequest: MutableMap<String, Any>
	): Call<ResponseBody>

	//
	// Connect API endpoints
	// 	Connect Subscriber
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
}