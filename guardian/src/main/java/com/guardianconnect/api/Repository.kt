package com.guardianconnect.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.guardianconnect.*
import com.guardianconnect.model.api.*
import com.guardianconnect.util.Constants.Companion.API_ERROR
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class Repository {

    val grdSubscriberCredential = GRDSubscriberCredential()
    var apiCalls: IApiCalls? = null
    var apiCallsConnect: IApiCalls? = null
    var apiCallsGRDConnect: IApiCalls? = null
    val TAG: String = Repository::class.java.simpleName

    companion object {
        val instance = Repository()
    }

    fun initRegionServer(hostname: String) {
        if (hostname.isNotEmpty()) {
            val baseUrl = "https://$hostname"
            val gson = GsonBuilder()
                .setLenient()
                .create()
            val interceptor = HttpLoggingInterceptor()
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            val client: OkHttpClient =
                OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .connectTimeout(5, TimeUnit.MINUTES)
                    .readTimeout(5, TimeUnit.MINUTES)
                    .writeTimeout(5, TimeUnit.MINUTES)
                    .build()
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build()
            apiCalls = retrofit.create(IApiCalls::class.java)
        }
    }

    fun initConnectAPIServer() {
        val gsonConnect = GsonBuilder()
            .setLenient()
            .create()
        val interceptorConnect = HttpLoggingInterceptor()
        interceptorConnect.setLevel(HttpLoggingInterceptor.Level.BODY)
        val clientConnect: OkHttpClient =
            OkHttpClient
                .Builder()
                .addInterceptor(interceptorConnect)
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .build()
        val retrofitConnect: Retrofit = Retrofit.Builder()
            .baseUrl("https://connect-api.guardianapp.com")
            .addConverterFactory(GsonConverterFactory.create(gsonConnect))
            .client(clientConnect)
            .build()
        apiCallsConnect = retrofitConnect.create(IApiCalls::class.java)
    }

    fun initConnectSubscriberServer(baseURLConnect: String) {
        val gsonConnect = GsonBuilder()
            .setLenient()
            .create()
        val interceptorConnect = HttpLoggingInterceptor()
        interceptorConnect.setLevel(HttpLoggingInterceptor.Level.BODY)
        val clientConnect: OkHttpClient =
            OkHttpClient
                .Builder()
                .addInterceptor(interceptorConnect)
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .build()
        val retrofitConnect: Retrofit = Retrofit.Builder()
            .baseUrl("https://$baseURLConnect")
            .addConverterFactory(GsonConverterFactory.create(gsonConnect))
            .client(clientConnect)
            .build()
        apiCallsGRDConnect = retrofitConnect.create(IApiCalls::class.java)
    }

    fun getServerStatus(iOnApiResponse: IOnApiResponse) {
        val call: Call<ResponseBody>? = apiCalls?.getServerStatus()
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    iOnApiResponse.onSuccess(true)
                    Log.d(TAG, "Server is online. Ready to accept connections.")
                } else if (response.code() == 500) {
                    Log.d(TAG, "Server error! Need to use different server")
                } else if (response.code() == 404) {
                    Log.d(TAG, "Endpoint not found on this server!")
                } else {
                    Log.d(TAG, "Unknown error!")
                    iOnApiResponse.onError("Unknown error!")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(TAG, API_ERROR + " getServerStatus() " + t.message)
            }
        })
    }

    fun createNewVPNDevice(newVPNDevice: NewVPNDevice, iOnApiResponse: IOnApiResponse) {
        val call: Call<NewVPNDeviceResponse>? = apiCalls?.createNewVPNDevice(newVPNDevice)
        call?.enqueue(object : Callback<NewVPNDeviceResponse> {
            override fun onResponse(
                call: Call<NewVPNDeviceResponse>,
                response: Response<NewVPNDeviceResponse>
            ) {
                val newVPNDeviceResponse = response.body()
                if (response.isSuccessful) {
                    iOnApiResponse.onSuccess(newVPNDeviceResponse)
                    Log.d(TAG, "New VPN device created.")
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    if (jObjError != null) {
                        Log.d(TAG, jObjError.toString())
                        iOnApiResponse.onError(jObjError.toString())
                    }
                }
            }

            override fun onFailure(call: Call<NewVPNDeviceResponse>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(TAG, API_ERROR + " createNewVPNDevice() " + t.message)
            }
        })
    }

    fun verifyVPNCredentials(
        deviceId: String,
        vpnCredentials: VPNCredentials,
        iOnApiResponse: IOnApiResponse
    ) {
        val call: Call<ResponseBody>? = apiCalls?.verifyVPNCredentials(deviceId, vpnCredentials)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    iOnApiResponse.onSuccess(response)
                    Log.d(TAG, "VPN credentials verified")
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    if (jObjError != null) {
                        Log.d(TAG, jObjError.toString())
                        iOnApiResponse.onError(jObjError.toString())
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(TAG, API_ERROR + " verifyVPNCredentials() " + t.message)
            }
        })
    }

    fun invalidateVPNCredentials(
        deviceId: String,
        vpnCredentials: VPNCredentials,
        iOnApiResponse: IOnApiResponse
    ) {
        val call: Call<ResponseBody>? =
            apiCalls?.invalidateVPNCredentials(deviceId, vpnCredentials)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    iOnApiResponse.onSuccess(response)
                    Log.d(TAG, "VPN credentials invalidated")
                } else {
                    val jObjError = response.errorBody()?.string()
                    if (jObjError != null) {
                        Log.d(TAG, jObjError)
                        iOnApiResponse.onError(jObjError)
                    } else {
                        iOnApiResponse.onError("Error")
                        Log.d(TAG, "invalidateVPNCredentials error")
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(TAG, API_ERROR + " invalidateVPNCredentials() " + t.message)
            }
        })
    }

    fun downloadAlerts(deviceId: String, alerts: Alerts, iOnApiResponse: IOnApiResponse) {
        val call: Call<ResponseBody>? = apiCalls?.downloadAlerts(deviceId, alerts)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.string().let {
                        val objectList = ArrayList(
                            Gson().fromJson(
                                it,
                                Array<AlertsResponse>::class.java
                            ).asList()
                        )
                        iOnApiResponse.onSuccess(objectList)
                        Log.d(TAG, "Alerts downloaded successfully!")
                    }
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    if (jObjError != null) {
                        Log.d(TAG, jObjError.toString())
                        iOnApiResponse.onError(jObjError.toString())
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(TAG, API_ERROR + " downloadAlerts() " + t.message)
            }
        })
    }

    fun setAlertsDownloadTimestamp(
        deviceId: String,
        baseRequest: BaseRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        val call: Call<ResponseBody>? = apiCalls?.setAlertsDownloadTimestamp(deviceId, baseRequest)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    iOnApiResponse.onSuccess(response)
                    Log.d(TAG, "Alerts download timestamp set successfully!")
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    if (jObjError != null) {
                        Log.d(TAG, jObjError.toString())
                        iOnApiResponse.onError(jObjError.toString())
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(TAG, API_ERROR + " setAlertsDownloadTimestamp() " + t.message)
            }
        })
    }

    fun requestAllGuardianRegions(iOnApiResponse: IOnApiResponse) {
        val call: Call<ResponseBody>? = apiCallsConnect?.requestAllGuardianRegions()
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.string().let {
                        val objectList = ArrayList(
                            Gson().fromJson(
                                it,
                                Array<GRDRegion>::class.java
                            ).asList()
                        )
                        iOnApiResponse.onSuccess(objectList)
                        Log.d(TAG, "Regions returned successfully!")
                    }
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    Log.d(TAG, jObjError.toString())
                    iOnApiResponse.onError(jObjError.toString())
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(TAG, API_ERROR + " requestAllGuardianRegions() " + t.message)
            }
        })
    }

    fun requestListOfServersForRegion(
        requestServersForRegion: RequestServersForRegion,
        iOnApiResponse: IOnApiResponse
    ) {
        val call: Call<ResponseBody>? =
            apiCallsConnect?.requestListOfServersForRegion(requestServersForRegion)
        var objectList: ArrayList<Server>?
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val string = response.body()?.string()
                    objectList = ArrayList(
                        Gson().fromJson(
                            string,
                            Array<Server>::class.java
                        ).asList()
                    )
                    iOnApiResponse.onSuccess(objectList)
                    Log.d(
                        TAG,
                        "List of servers returned successfully!"
                    )
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    if (jObjError != null) {
                        Log.d(TAG, jObjError.toString())
                        iOnApiResponse.onError(jObjError.toString())
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(
                    TAG,
                    API_ERROR + " requestListOfServersForRegion() " + t.message
                )
            }
        })
    }

    fun getListOfSupportedTimeZones(iOnApiResponse: IOnApiResponse) {
        val call: Call<ResponseBody>? = apiCallsConnect?.getListOfSupportedTimeZones()
        var objectList: ArrayList<TimeZonesResponse>?
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val string = response.body()?.string()
                    objectList = ArrayList(
                        Gson().fromJson(
                            string,
                            Array<TimeZonesResponse>::class.java
                        ).asList()
                    )
                    iOnApiResponse.onSuccess(objectList)
                    Log.d(
                        TAG,
                        "List of supported time zones returned successfully!"
                    )
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    if (jObjError != null) {
                        Log.d(TAG, jObjError.toString())
                        iOnApiResponse.onError(jObjError.toString())
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(
                    TAG,
                    API_ERROR + " getListOfSupportedTimeZones() " + t.message
                )
            }
        })
    }

    fun getSubscriberCredentialsIAP(
        validationMethodIAPAndroid: ValidationMethodIAPAndroid,
        iOnApiResponse: IOnApiResponse
    ) {
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.getSubscriberCredentialsIAPAndroid(validationMethodIAPAndroid)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    response.body()?.string().let {
                        val subscriberCredentialResponse =
                            Gson().fromJson(
                                it,
                                SubscriberCredentialResponse::class.java
                            )
                        iOnApiResponse.onSuccess(subscriberCredentialResponse)
                        Log.d(
                            TAG,
                            "Subscriber credentials IAP Android returned successfully!"
                        )
                    }
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    if (jObjError != null) {
                        Log.d(TAG, jObjError.toString())
                        iOnApiResponse.onError(jObjError.toString())
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(
                    TAG,
                    API_ERROR + " getSubscriberCredentials() " + t.message
                )
            }
        })
    }

    fun getSubscriberCredentialsPET(
        validationMethodPEToken: ValidationMethodPEToken,
        iOnApiResponse: IOnApiResponse
    ) {
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.getSubscriberCredentialsPEToken(validationMethodPEToken)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    response.body()?.string().let {
                        val subscriberCredentialResponse =
                            Gson().fromJson(
                                it,
                                SubscriberCredentialResponse::class.java
                            )
                        iOnApiResponse.onSuccess(subscriberCredentialResponse)

                        Log.d(
                            TAG, "Subscriber credentials PE Token returned successfully!" +
                                    subscriberCredentialResponse.subscriberCredential?.let { it1 ->
                                        grdSubscriberCredential.parseAndDecodeJWTFormat(
                                            it1
                                        )
                                    }
                        )
                    }
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    if (jObjError != null) {
                        Log.d(TAG, jObjError.toString())
                        iOnApiResponse.onError(jObjError.toString())
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(
                    TAG,
                    API_ERROR + " getSubscriberCredentials() " + t.message
                )
            }
        })
    }

    fun getSubscriberCredentialsElse(
        validationMethodElse: ValidationMethodElse,
        iOnApiResponse: IOnApiResponse
    ) {
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.getSubscriberCredentialsElse(validationMethodElse)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    response.body()?.string().let {
                        val subscriberCredentialResponse =
                            Gson().fromJson(
                                it,
                                SubscriberCredentialResponse::class.java
                            )
                        iOnApiResponse.onSuccess(subscriberCredentialResponse)
                        Log.d(TAG, "Subscriber credentials returned successfully!")
                    }
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    if (jObjError != null) {
                        Log.d(TAG, jObjError.toString())
                        iOnApiResponse.onError(jObjError.toString())
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(
                    TAG,
                    API_ERROR + " getSubscriberCredentials() " + t.message
                )
            }
        })
    }

    fun createNewGRDConnectSubscriber(
        grdConnectSubscriberRequest: GRDConnectSubscriberRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        val call: Call<GRDConnectSubscriberResponse>? =
            apiCallsGRDConnect?.createNewGRDConnectSubscriber(grdConnectSubscriberRequest)
        call?.enqueue(object : Callback<GRDConnectSubscriberResponse> {
            override fun onResponse(call: Call<GRDConnectSubscriberResponse>, response: Response<GRDConnectSubscriberResponse>) {
                if (response.isSuccessful) {
                    val grdConnectSubscriberResponse = response.body()
                    iOnApiResponse.onSuccess(grdConnectSubscriberResponse)
                    Log.d(TAG, "New GRDConnect Subscriber created.")
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    if (jObjError != null) {
                        Log.d(TAG, jObjError.toString())
                        iOnApiResponse.onError(jObjError.toString())
                    }
                }
            }

            override fun onFailure(call: Call<GRDConnectSubscriberResponse>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(
                    TAG,
                    API_ERROR + " createNewGRDConnectSubscriber() " + t.message
                )
            }
        })
    }

    fun updateGRDConnectSubscriber(
        connectSubscriberUpdateRequest: ConnectSubscriberUpdateRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.updateGRDConnectSubscriber(connectSubscriberUpdateRequest)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val connectSubscriberUpdateResponse = response.body()
                    iOnApiResponse.onSuccess(connectSubscriberUpdateResponse)
                    Log.d(TAG, "GRDConnect Subscriber updated.")
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    if (jObjError != null) {
                        Log.d(TAG, jObjError.toString())
                        iOnApiResponse.onError(jObjError.toString())
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(
                    TAG,
                    API_ERROR + " updateGRDConnectSubscriber() " + t.message
                )
            }
        })
    }

    fun validateGRDConnectSubscriber(
        connectSubscriberValidateRequest: ConnectSubscriberValidateRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.validateGRDConnectSubscriber(connectSubscriberValidateRequest)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val connectSubscriberValidateResponse = response.body()
                    iOnApiResponse.onSuccess(connectSubscriberValidateResponse)
                    Log.d(TAG, "GRDConnect Subscriber validated.")
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    if (jObjError != null) {
                        Log.d(TAG, jObjError.toString())
                        iOnApiResponse.onError(jObjError.toString())
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(
                    TAG,
                    API_ERROR + " validateGRDConnectSubscriber() " + t.message
                )
            }
        })
    }

    fun addNewConnectDevice(
        connectDeviceRequest: ConnectDeviceRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.addConnectDevice(connectDeviceRequest)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val connectDeviceResponse = response.body()
                    iOnApiResponse.onSuccess(connectDeviceResponse)
                    Log.d(TAG, "GRDConnect Device added.")
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    if (jObjError != null) {
                        Log.d(TAG, jObjError.toString())
                        iOnApiResponse.onError(jObjError.toString())
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(
                    TAG,
                    API_ERROR + " addNewConnectDevice() " + t.message
                )
            }
        })
    }

    fun updateConnectDevice(
        connectDeviceUpdateRequest: ConnectDeviceUpdateRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.updateConnectDevice(connectDeviceUpdateRequest)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val connectDeviceResponse = response.body()
                    iOnApiResponse.onSuccess(connectDeviceResponse)
                    Log.d(TAG, "GRDConnect Device updated.")
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    if (jObjError != null) {
                        Log.d(TAG, jObjError.toString())
                        iOnApiResponse.onError(jObjError.toString())
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(
                    TAG,
                    API_ERROR + " updateConnectDevice() " + t.message
                )
            }
        })
    }

    fun allConnectDevices(
        connectDevicesAllRequest: ConnectDevicesAllRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.allConnectDevices(connectDevicesAllRequest)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.string().let {
                        val objectList = ArrayList(
                            Gson().fromJson(
                                it,
                                Array<ConnectDeviceResponse>::class.java
                            ).asList()
                        )
                        iOnApiResponse.onSuccess(objectList)
                        Log.d(TAG, "All Connect subscriber devices returned successfully!")
                    }
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    if (jObjError != null) {
                        Log.d(TAG, jObjError.toString())
                        iOnApiResponse.onError(jObjError.toString())
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(
                    TAG,
                    API_ERROR + " allConnectDevices() " + t.message
                )
            }
        })
    }

    fun deleteConnectDevice(
        connectDeviceDeleteRequest: ConnectDeviceDeleteRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.deleteConnectDevice(connectDeviceDeleteRequest)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    iOnApiResponse.onSuccess("GRDConnectDevice successfully deleted.")
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    if (jObjError != null) {
                        Log.d(TAG, jObjError.toString())
                        iOnApiResponse.onError(jObjError.toString())
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                GRDConnectManager.getCoroutineScope().launch {
                    t.message?.let { GRDVPNHelper.grdErrorFlow.emit(it) }
                }
                Log.d(
                    TAG,
                    API_ERROR + " deleteConnectDevice() " + t.message
                )
            }
        })
    }

    fun setDeviceFilterConfig(
        deviceId: String,
        apiAuthToken: String,
        iOnApiResponse: IOnApiResponse
    ) {
        val grdDeviceFilterConfigBlocklist = GRDDeviceFilterConfigBlocklist()
        var map = HashMap<Any, Any>()
        map.putAll(grdDeviceFilterConfigBlocklist.apiPortableBlocklist())
        map["api-auth-token"] = apiAuthToken
        val json = Gson().toJsonTree(map).asJsonObject
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.setDeviceFilterConfig(deviceId, json)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    iOnApiResponse.onSuccess("Device filter config blocklist settings successfully synced with the VPN gateways.")
                } else {
                    val jObjError = response.errorBody()?.string()?.let { JSONObject(it) }
                    if (jObjError != null) {
                        Log.d(TAG, jObjError.toString())
                        iOnApiResponse.onError(jObjError.toString())
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                GRDConnectManager.getCoroutineScope().launch {
                    t.message?.let { GRDVPNHelper.grdErrorFlow.emit(it) }
                }
                Log.d(
                    TAG,
                    API_ERROR + " deleteConnectDevice() " + t.message
                )
            }
        })
    }

}