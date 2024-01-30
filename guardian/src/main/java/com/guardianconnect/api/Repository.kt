package com.guardianconnect.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.guardianconnect.*
import com.guardianconnect.model.api.*
import com.guardianconnect.util.Constants
import com.guardianconnect.util.Constants.Companion.API_ERROR
import com.guardianconnect.util.Constants.Companion.kGRDErrGuardianAccountNotSetup
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class Repository {

    val grdSubscriberCredential = GRDSubscriberCredential()
    var httpClient: OkHttpClient? = null
    var apiCalls: IApiCalls? = null
    var apiCallsConnect: IApiCalls? = null
    var apiCallsGRDConnect: IApiCalls? = null
    var connectPublishableKey: String? = null
    val TAG: String = Repository::class.java.simpleName

    companion object {
        val instance = Repository()
    }

    fun defaultHTTPClient(): OkHttpClient {
        val interceptorConnect = HttpLoggingInterceptor()
        interceptorConnect.setLevel(HttpLoggingInterceptor.Level.BODY)
        return OkHttpClient
            .Builder()
            .addInterceptor(interceptorConnect)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun initRegionServer(hostname: String) {
        if (hostname.isNotEmpty()) {
            val baseUrl = "https://$hostname"
            val gson = GsonBuilder()
                .setLenient()
                .create()
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient ?: defaultHTTPClient())
                .build()
            apiCalls = retrofit.create(IApiCalls::class.java)
        }
    }

    fun initConnectAPIServer() {
        val gsonConnect = GsonBuilder()
            .setLenient()
            .create()
        val retrofitConnect: Retrofit = Retrofit.Builder()
            .baseUrl("https://${Constants.kGRDConnectAPIHostname}")
            .addConverterFactory(GsonConverterFactory.create(gsonConnect))
            .client(httpClient ?: defaultHTTPClient())
            .build()
        apiCallsConnect = retrofitConnect.create(IApiCalls::class.java)
    }

    fun initConnectSubscriberServer(baseURLConnect: String) {
        val gsonConnect = GsonBuilder()
            .setLenient()
            .create()
        val retrofitConnect: Retrofit = Retrofit.Builder()
            .baseUrl("https://$baseURLConnect")
            .addConverterFactory(GsonConverterFactory.create(gsonConnect))
            .client(httpClient ?: defaultHTTPClient())
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
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
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
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
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
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
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
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
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
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
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
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(TAG, API_ERROR + " requestAllGuardianRegions() " + t.message)
            }
        })
    }

    fun requestAllRegionsWithPrecision(
        precision: String,
        iOnApiResponse: IOnApiResponse
    ) {
        val call: Call<ResponseBody>? = apiCallsConnect?.requestAllRegionsWithPrecision(precision)
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
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
                    }
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
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
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
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
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
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
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
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
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

    fun signOutUser(
        signOutUserRequest: SignOutUserRequest,
        iOnApiResponse: IOnApiResponse
    ) {
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.signOutUser(signOutUserRequest)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    iOnApiResponse.onSuccess("User sign out successfully.")
                } else {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                GRDConnectManager.getCoroutineScope().launch {
                    t.message?.let { GRDVPNHelper.grdErrorFlow.emit(it) }
                }
                Log.d(
                    TAG,
                    API_ERROR + " signOutUser() " + t.message
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
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
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
        requestBody: MutableMap<String, Any>,
        iOnApiResponse: IOnApiResponse
    ) {
        requestBody["connect-publishable-key"] = instance.connectPublishableKey.toString()
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.createNewGRDConnectSubscriber(requestBody)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    var grdConnectSubscriberResponse = mapOf<String, Any>()
                    val body = response.body()?.string()
                    if (body != null) {
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        grdConnectSubscriberResponse = Gson().fromJson(body, type)
                    }

                    iOnApiResponse.onSuccess(grdConnectSubscriberResponse)
                    Log.d(TAG, "New GRDConnect Subscriber created.")

                } else {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(
                    TAG,
                    API_ERROR + " createNewGRDConnectSubscriber() " + t.message
                )
            }
        })
    }

    fun updateGRDConnectSubscriber(
        requestBody: MutableMap<String, Any>,
        iOnApiResponse: IOnApiResponse
    ) {
        requestBody["connect-publishable-key"] = instance.connectPublishableKey.toString()
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.updateGRDConnectSubscriber(requestBody)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    var connectSubscriberUpdateResponse = mapOf<String, Any>()
                    val body = response.body()?.string()
                    if (body != null) {
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        connectSubscriberUpdateResponse = Gson().fromJson(body, type)
                    }

                    iOnApiResponse.onSuccess(connectSubscriberUpdateResponse)
                    Log.d(TAG, "GRDConnect Subscriber updated.")
                } else {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
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
        requestBody: MutableMap<String, Any>,
        iOnApiResponse: IOnApiResponse
    ) {
        requestBody["connect-publishable-key"] = instance.connectPublishableKey.toString()
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.validateGRDConnectSubscriber(requestBody)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    var connectSubscriberValidateResponse = mapOf<String, Any>()
                    val body = response.body()?.string()
                    if (body != null) {
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        connectSubscriberValidateResponse = Gson().fromJson(body, type)
                    }
                    iOnApiResponse.onSuccess(connectSubscriberValidateResponse)
                    Log.d(TAG, "GRDConnect Subscriber validated.")

                } else {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
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
        requestBody: MutableMap<String, Any>,
        iOnApiResponse: IOnApiResponse
    ) {
        requestBody["connect-publishable-key"] = instance.connectPublishableKey.toString()
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.addConnectDevice(requestBody)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    var connectDeviceResponse = mapOf<String, Any>()
                    val body = response.body()?.string()
                    if (body != null) {
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        connectDeviceResponse = Gson().fromJson(body, type)
                    }
                    iOnApiResponse.onSuccess(connectDeviceResponse)
                    Log.d(TAG, "GRDConnect Device added.")

                } else {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
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
        requestBody: MutableMap<String, Any>,
        iOnApiResponse: IOnApiResponse
    ) {
        requestBody["connect-publishable-key"] = instance.connectPublishableKey.toString()
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.updateConnectDevice(requestBody)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    var connectDeviceResponse = mapOf<String, Any>()
                    val body = response.body()?.string()
                    if (body != null) {
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        connectDeviceResponse = Gson().fromJson(body, type)
                    }
                    iOnApiResponse.onSuccess(connectDeviceResponse)
                    Log.d(TAG, "GRDConnect Device updated.")

                } else {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
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
        requestBody: MutableMap<String, Any>,
        iOnApiResponse: IOnApiResponse
    ) {
        requestBody["connect-publishable-key"] = instance.connectPublishableKey.toString()
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.allConnectDevices(requestBody)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    data class ObjectList(val items: List<Map<String, Any>>)

                    val body = response.body()?.string()
                    if (body != null) {
                        val objectList: ObjectList =
                            Gson().fromJson(body, object : TypeToken<ObjectList>() {}.type)

                        iOnApiResponse.onSuccess(objectList)
                        Log.d(TAG, "All Connect subscriber devices returned successfully!")
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
                    }

                } else {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
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
        requestBody: MutableMap<String, Any>,
        iOnApiResponse: IOnApiResponse
    ) {
        requestBody["connect-publishable-key"] = instance.connectPublishableKey.toString()
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.deleteConnectDevice(requestBody)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    var connectDeviceResponse = mapOf<String, Any>()
                    val body = response.body()?.string()
                    if (body != null) {
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        connectDeviceResponse = Gson().fromJson(body, type)
                    }
                    iOnApiResponse.onSuccess(connectDeviceResponse)
                    Log.d(TAG, "GRDConnect Device successfully deleted.")

                } else {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
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

    fun getConnectDeviceReference(
        requestBody: MutableMap<String, Any>,
        iOnApiResponse: IOnApiResponse
    ) {
        requestBody["connect-publishable-key"] = instance.connectPublishableKey.toString()
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.getConnectDeviceReference(requestBody)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    var connectDeviceReferenceResponse = mapOf<String, Any>()
                    val body = response.body()?.string()
                    if (body != null) {
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        connectDeviceReferenceResponse = Gson().fromJson(body, type)
                    }
                    iOnApiResponse.onSuccess(connectDeviceReferenceResponse)
                    Log.d(TAG, "Connect subscriber device reference returned successfully!")
                } else {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(
                    TAG,
                    API_ERROR + " getConnectDeviceReference() " + t.message
                )
            }
        })
    }

    fun getAccountCreationState(
        requestBody: MutableMap<String, Any>,
        iOnApiResponse: IOnApiResponse
    ) {
        requestBody["connect-publishable-key"] = instance.connectPublishableKey.toString()
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.getAccountSignUpState(requestBody)

        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    var accountCreationStateResponse = mapOf<String, Any>()
                    val body = response.body()?.string()
                    if (body != null) {
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        accountCreationStateResponse = Gson().fromJson(body, type)
                    }
                    iOnApiResponse.onSuccess(accountCreationStateResponse)
                    Log.d(TAG, "Account creation state retrieved successfully!")
                } else {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())

                            val httpStatusCode = response.code()
                            if (httpStatusCode == 400 && jObjError.toString()
                                    .contains("not yet setup")
                            ) {
                                iOnApiResponse.onError(kGRDErrGuardianAccountNotSetup)
                            } else {
                                iOnApiResponse.onError(jObjError.toString())
                            }
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // Handle failure scenario
                iOnApiResponse.onError(t.message)
                Log.d(TAG, API_ERROR + " getAccountCreationState() " + t.message)
            }
        })
    }


    fun setDeviceFilterConfig(
        deviceId: String,
        apiAuthToken: String,
        iOnApiResponse: IOnApiResponse
    ) {
        val grdDeviceFilterConfigBlocklist =
            GRDDeviceFilterConfigBlocklist().currentBlocklistConfig()
        val map = HashMap<Any, Any>()
        grdDeviceFilterConfigBlocklist?.apiPortableBlocklist()?.let { map.putAll(it) }
        map["api-auth-token"] = apiAuthToken
        val json = Gson().toJsonTree(map).asJsonObject
        val call: Call<ResponseBody>? =
            apiCalls?.setDeviceFilterConfig(deviceId, json)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    iOnApiResponse.onSuccess("Device filter config blocklist settings successfully synced with the VPN gateways.")
                } else {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
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

    fun logoutConnectSubscriber(
        requestBody: MutableMap<String, Any>,
        iOnApiResponse: IOnApiResponse
    ) {
        requestBody["connect-publishable-key"] = instance.connectPublishableKey.toString()
        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.logoutConnectSubscriber(requestBody)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    iOnApiResponse.onSuccess("GRDConnect Subscriber logout successfully.")
                } else {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format")
                            iOnApiResponse.onError("Error response is not in JSON format")
                        }
                    } else {
                        Log.e(TAG, "Error response body is null")
                        iOnApiResponse.onError("Error response body is null")
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                GRDConnectManager.getCoroutineScope().launch {
                    t.message?.let { GRDVPNHelper.grdErrorFlow.emit(it) }
                }
                Log.d(
                    TAG,
                    API_ERROR + " logoutConnectSubscriber() " + t.message
                )
            }
        })
    }
}