package com.guardianconnect.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.ToNumberPolicy
import com.google.gson.reflect.TypeToken
import com.guardianconnect.*
import com.guardianconnect.helpers.GRDVPNHelper
import com.guardianconnect.managers.GRDConnectManager
import com.guardianconnect.model.api.*
import com.guardianconnect.util.Constants
import com.guardianconnect.util.Constants.Companion.APITYPETOKENARRAY
import com.guardianconnect.util.Constants.Companion.APITYPETOKENMAP
import com.guardianconnect.util.Constants.Companion.API_ERROR
import com.guardianconnect.util.Constants.Companion.kGRDErrGuardianAccountNotSetup
import com.guardianconnect.util.GRDLogger
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
    var httpClient: 			OkHttpClient? = null
    var apiCalls: 				IApiCalls? = null
    var apiCallsConnect: 		IApiCalls? = null
    var apiCallsGRDConnect: 	IApiCalls? = null
    var connectPublishableKey: 	String? = null
	var gson: 					Gson? = null
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

    fun initSGWServer(hostname: String) {
        GRDLogger.d(TAG, "initSGWServer() hostname: $hostname")
        if (hostname.isNotEmpty()) {
            val baseUrl = "https://$hostname"
			//
			// Note from CJ 2026-06-24
			// These are duplicated but over time we are going to
			// migrate everything to the one right below which
			// leverages the LAZILY_PARSED_NUMBER policy given
			// that parsing of JSON numbers is an absolute mess
			// on Android due to things in Java that are ancient
			// and will never be fixed
			gson = GsonBuilder()
				.setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
				.create()

            val gson = GsonBuilder()
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
		//
		// Note from CJ 2026-06-24
		// These are duplicated but over time we are going to
		// migrate everything to the one right below which
		// leverages the LAZILY_PARSED_NUMBER policy given
		// that parsing of JSON numbers is an absolute mess
		// on Android due to things in Java that are ancient
		// and will never be fixed
		gson = GsonBuilder()
			.setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
			.create()
        val gsonConnect = GsonBuilder()
            .create()
        val retrofitConnect: Retrofit = Retrofit.Builder()
            .baseUrl("https://${Constants.kGRDConnectAPIHostname}")
            .addConverterFactory(GsonConverterFactory.create(gsonConnect))
            .client(httpClient ?: defaultHTTPClient())
            .build()
        apiCallsConnect = retrofitConnect.create(IApiCalls::class.java)
    }

    fun initConnectSubscriberServer(baseURLConnect: String) {
		//
		// Note from CJ 2026-06-24
		// These are duplicated but over time we are going to
		// migrate everything to the one right below which
		// leverages the LAZILY_PARSED_NUMBER policy given
		// that parsing of JSON numbers is an absolute mess
		// on Android due to things in Java that are ancient
		// and will never be fixed
		gson = GsonBuilder()
			.setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
			.create()
        val gsonConnect = GsonBuilder()
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

                } else if (response.code() == 500) {
					Log.d(TAG, "Server error! Need to use different server")
					iOnApiResponse.onError("Selected server not operational")

                } else if (response.code() == 404) {
                    Log.d(TAG, "Endpoint not found on this server!")
					iOnApiResponse.onError("Selected server not operational")

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

	fun getServerStatusForDeviceId(deviceId: String, iOnApiResponse: IOnApiResponse) {
		val call: Call<ResponseBody>? = apiCalls?.getServerStatusForDeviceId(deviceId)
		call?.enqueue(object : Callback<ResponseBody> {
			override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
				if (response.isSuccessful) {
					iOnApiResponse.onSuccess(true)

				} else if (response.code() == 500) {
					Log.d(TAG, "Server error! Need to use different server")
					iOnApiResponse.onError("Selected server not operational")

				} else if (response.code() == 404) {
					Log.d(TAG, "Endpoint not found on this server!")
					iOnApiResponse.onError("Selected server not operational")

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

    fun createNewVPNDevice(transportProtocol: String, subscriberCredential: String, transportOptions: Map<String, Any>?, deviceFilterConfig: Map<String, Any>?, clientRules: List<GRDClientRule>?, multihopExitRegion: String?, iOnApiResponse: IOnApiResponse) {
		val requestData = mutableMapOf<String, Any>()
		requestData["transport-protocol"] = transportProtocol
		requestData["subscriber-credential"] = subscriberCredential

		if (transportOptions != null) {
			requestData.putAll(transportOptions)
		}

		if (deviceFilterConfig != null) {
			requestData["device-filter-config"] = deviceFilterConfig
		}

		if (clientRules != null) {
			requestData["client-rule"] = clientRules
		}

		if (!multihopExitRegion.isNullOrEmpty()) {
			requestData["multihop-exit-region"] = multihopExitRegion
		}

        val call: Call<ResponseBody>? = apiCalls?.createNewVPNDevice(requestData)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody?>) {
				if (response.isSuccessful) {
					val responseData: Map<String, Any> = gson!!.fromJson(response.body()?.string(), APITYPETOKENMAP)
					iOnApiResponse.onSuccess(responseData)
					return
				}

				val apiErr = GRDAPIError.apiErrorFromResponseBody(response)
				iOnApiResponse.onError(apiErr.toString())
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(TAG, API_ERROR + " createNewVPNDevice() " + t.message)
            }
        })
    }

    fun verifyVPNCredentials(deviceId: String, apiAuthToken: String, iOnApiResponse: IOnApiResponse) {
        val call: Call<ResponseBody>? = apiCalls?.verifyVPNCredentials(deviceId, apiAuthToken)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
					val responseData: Map<String, Any> = gson!!.fromJson(response.body()?.string(), APITYPETOKENMAP)
					iOnApiResponse.onSuccess(responseData)
					return
                }

				val apiErr = GRDAPIError.apiErrorFromResponseBody(response)
				iOnApiResponse.onError(apiErr.toString())
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(TAG, API_ERROR + " verifyVPNCredentials() " + t.message)
            }
        })
    }

    fun invalidateVPNCredentials(deviceId: String, requestData: MutableMap<String, Any>, iOnApiResponse: IOnApiResponse) {
        val call: Call<ResponseBody>? = apiCalls?.invalidateVPNCredentials(deviceId, requestData)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody?>) {
				if (response.isSuccessful) {
					val responseData: Map<String, Any> = gson!!.fromJson(response.body()?.string(), APITYPETOKENMAP)
					iOnApiResponse.onSuccess(responseData)
					return
                }

				val apiErr = GRDAPIError.apiErrorFromResponseBody(response)
				iOnApiResponse.onError(apiErr.toString())
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
                Log.d(TAG, API_ERROR + " invalidateVPNCredentials() " + t.message)
            }
        })
    }

    fun downloadAlerts(deviceId: String, requestData: MutableMap<String, Any>, iOnApiResponse: IOnApiResponse) {
        val call: Call<ResponseBody>? = apiCalls?.downloadAlerts(deviceId, requestData)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.string().let {
                        iOnApiResponse.onSuccess(it)
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
                            Log.e(TAG, "Error response is not in JSON format: $e")
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
                            Log.e(TAG, "Error response is not in JSON format: $e")
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
                            Log.e(TAG, "Error response is not in JSON format: $e")
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


    fun requestListOfServersForRegionWithRegionPrecision(
        requestServersForRegion: MutableMap<String, Any>,
        iOnApiResponse: IOnApiResponse
    ) {
        val call: Call<ResponseBody>? =
            apiCallsConnect?.requestListOfServersForRegionWithRegionPrecision(requestServersForRegion)
        var objectList: ArrayList<GRDSGWServer>?
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val string = response.body()?.string()
                    objectList = ArrayList(
                        Gson().fromJson(
                            string,
                            Array<GRDSGWServer>::class.java
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
                            Log.e(TAG, "Error response is not in JSON format: $e")
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
                    API_ERROR + " requestListOfServersForRegionWithRegionPrecision() " + t.message
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
                            Log.e(TAG, "Error response is not in JSON format: $e")
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

    fun getSubscriberCredential(
        request: Any,
        iOnApiResponse: IOnApiResponse
    ) {
        val gson = Gson()
        val requestMap: MutableMap<String, Any> = when (request) {
            is ValidationMethodPEToken -> {
                gson.fromJson(gson.toJson(request), object : TypeToken<MutableMap<String, Any>>() {}.type)
            }
            is ValidationMethodIAPAndroid -> {
                gson.fromJson(gson.toJson(request), object : TypeToken<MutableMap<String, Any>>() {}.type)
            }
            else -> {
                gson.fromJson(gson.toJson(request), object : TypeToken<MutableMap<String, Any>>() {}.type)

            }
        }

        val call: Call<ResponseBody>? =
            apiCallsGRDConnect?.getSubscriberCredential(requestMap)
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
                            Log.e(TAG, "Error response is not in JSON format: $e")
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
                            Log.e(TAG, "Error response is not in JSON format: $e")
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



    fun createNewGRDConnectSubscriber(requestBody: MutableMap<String, Any>, iOnApiResponse: IOnApiResponse) {
        requestBody["connect-publishable-key"] = instance.connectPublishableKey.toString()
        val call: Call<ResponseBody>? = apiCallsGRDConnect?.createNewGRDConnectSubscriber(requestBody)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody> ) {
                if (response.isSuccessful) {
                    var grdConnectSubscriberResponse = mapOf<String, Any>()
                    val body = response.body()?.string()
                    Log.d(TAG, "New GRDConnect Subscriber created BODY: $body")
                    if (body != null) {
                        val gson = GsonBuilder()
                            .setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
                            .create()

                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        grdConnectSubscriberResponse = gson.fromJson(body, type)
                    }

                    iOnApiResponse.onSuccess(grdConnectSubscriberResponse)
                    Log.d(TAG, "New GRDConnect Subscriber created GRD SUBSCRIBER MAP: $grdConnectSubscriberResponse")

                } else {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val jObjError = JSONObject(errorBody)
                            Log.d(TAG, jObjError.toString())
                            iOnApiResponse.onError(jObjError.toString())
                        } catch (e: JSONException) {
                            // Handle the case when the error response is not in JSON format
                            Log.e(TAG, "Error response is not in JSON format: $e")
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

    fun updateGRDConnectSubscriber(requestBody: MutableMap<String, Any>, iOnApiResponse: IOnApiResponse) {
        requestBody["connect-publishable-key"] = instance.connectPublishableKey.toString()
        val call: Call<ResponseBody>? = apiCallsGRDConnect?.updateGRDConnectSubscriber(requestBody)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    var connectSubscriberUpdateResponse = mapOf<String, Any>()
                    val body = response.body()?.string()
                    if (body != null) {
                        val gson = GsonBuilder()
                            .setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
                            .create()
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        connectSubscriberUpdateResponse = gson.fromJson(body, type)
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
                            Log.e(TAG, "Error response is not in JSON format: $e")
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

    fun validateGRDConnectSubscriber(requestBody: MutableMap<String, Any>,  iOnApiResponse: IOnApiResponse) {
        requestBody["connect-publishable-key"] = instance.connectPublishableKey.toString()
        val call: Call<ResponseBody>? = apiCallsGRDConnect?.validateGRDConnectSubscriber(requestBody)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    var connectSubscriberValidateResponse = mapOf<String, Any>()
                    val body = response.body()?.string()
                    if (body != null) {
                        val gson = GsonBuilder()
                            .setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
                            .create()
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        connectSubscriberValidateResponse = gson.fromJson(body, type)
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
                            Log.e(TAG, "Error response is not in JSON format: $e")
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
                        val gson = GsonBuilder()
                            .setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
                            .create()
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        connectDeviceResponse = gson.fromJson(body, type)
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
                            Log.e(TAG, "Error response is not in JSON format: $e")
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
                        val gson = GsonBuilder()
                            .setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
                            .create()
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        connectDeviceResponse = gson.fromJson(body, type)
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
                            Log.e(TAG, "Error response is not in JSON format: $e")
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

    fun allConnectDevices(requestBody: MutableMap<String, Any>, iOnApiResponse: IOnApiResponse) {
        requestBody["connect-publishable-key"] = instance.connectPublishableKey.toString()
        val call: Call<ResponseBody>? =  apiCallsGRDConnect?.allConnectDevices(requestBody)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    if (body != null) {
                        try {
                            val gson = GsonBuilder()
                                .setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
                                .create()
                            val objectList: List<GRDConnectDevice> = gson.fromJson(body, object : TypeToken<List<GRDConnectDevice>>() {}.type)
                            iOnApiResponse.onSuccess(objectList)
                            Log.d(TAG, "All Connect subscriber devices returned successfully!")

                        } catch (e: JsonSyntaxException) {
                            Log.e(TAG, "Failed to parse JSON", e)
                            iOnApiResponse.onError("Failed to parse response")
                        }
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
                            Log.e(TAG, "Error response is not in JSON format: $e")
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
                    iOnApiResponse.onSuccess(true)
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
                            Log.e(TAG, "Error response is not in JSON format: $e")
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

    fun getConnectDeviceReference(requestBody: MutableMap<String, Any>, iOnApiResponse: IOnApiResponse) {
        requestBody["connect-publishable-key"] = instance.connectPublishableKey.toString()
        val call: Call<ResponseBody>? = apiCallsGRDConnect?.getConnectDeviceReference(requestBody)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    var connectDeviceReferenceResponse = mapOf<String, Any>()
                    val body = response.body()?.string()
                    if (body != null) {
                        val gson = GsonBuilder()
                            .setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
                            .create()
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        connectDeviceReferenceResponse = gson.fromJson(body, type)
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
                            Log.e(TAG, "Error response is not in JSON format: $e")
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
                            Log.e(TAG, "Error response is not in JSON format: $e")
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

	fun getDeviceFilterConfig(deviceId: String, apiAuthToken: String, iOnApiResponse: IOnApiResponse) {
		apiCalls?.getDeviceFilterConfig(deviceId, apiAuthToken)?.enqueue(object: Callback<ResponseBody> {
			override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
				if (response.isSuccessful) {
					val responseData: Map<String, Any> = gson!!.fromJson(response.body()?.string(), APITYPETOKENMAP)
					iOnApiResponse.onSuccess(responseData)
					return
				}

				val apiErr = GRDAPIError.apiErrorFromResponseBody(response)
				iOnApiResponse.onError(apiErr.toString())
			}

			override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
				iOnApiResponse.onError(t.message)
			}
		})
	}

    fun setDeviceFilterConfig(deviceId: String, apiAuthToken: String, iOnApiResponse: IOnApiResponse) {
        val grdDeviceFilterConfigBlocklist = GRDDeviceFilterConfigBlocklist().currentBlocklistConfig()
        val requestData = mutableMapOf<String, Any>()
		requestData["api-auth-token"] = apiAuthToken
        grdDeviceFilterConfigBlocklist?.apiPortableBlocklist()?.let { requestData.putAll(it) }

        val call: Call<ResponseBody>? = apiCalls?.setDeviceFilterConfig(deviceId, requestData)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    iOnApiResponse.onSuccess("Device filter config blocklist settings successfully synced with the VPN gateways.")
					return
                }

				val apiErr = GRDAPIError.apiErrorFromResponseBody(response)
				iOnApiResponse.onError(apiErr.toString())
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                iOnApiResponse.onError(t.message)
            }
        })
    }

	fun getClientRules(deviceId: String, apiAuthToken: String, iOnApiResponse: IOnApiResponse) {
		apiCalls?.getClientRules(deviceId, apiAuthToken)?.enqueue(object: Callback<ResponseBody> {
			override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
				if (response.isSuccessful) {
					val responseData: Map<String, Any> = gson!!.fromJson(response.body()?.string(), APITYPETOKENARRAY)
					iOnApiResponse.onSuccess(responseData)
					return
				}

				val apiErr = GRDAPIError.apiErrorFromResponseBody(response)
				iOnApiResponse.onError(apiErr.toString())
			}

			override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
				iOnApiResponse.onError(t.message)
			}
		})
	}

	fun setClientRules(clientRules: ArrayList<GRDClientRule>, deviceId: String, apiAuthToken: String, iOnApiResponse: IOnApiResponse) {
		val encodedRules = mutableListOf<Map<String, Any>>()
		for (rule: GRDClientRule in clientRules) {
			val mapRule = rule.encodeToMap()
			encodedRules.add(mapRule)
		}
		val requestData = Gson().toJson(encodedRules)
		apiCalls?.setClientRules(deviceId, requestData)?.enqueue(object : Callback<ResponseBody> {
			override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
				if (response.isSuccessful) {
					val responseData: Map<String, Any> = gson!!.fromJson(response.body()?.string(), APITYPETOKENARRAY)
					iOnApiResponse.onSuccess(responseData)
					return
				}

				val apiErr = GRDAPIError.apiErrorFromResponseBody(response)
				iOnApiResponse.onError(apiErr.toString())
			}

			override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
				iOnApiResponse.onError(t.message)
			}
		})
	}

	fun getMultihopExitRegion(deviceId: String, apiAuthToken: String, iOnApiResponse: IOnApiResponse) {
		apiCalls?.getMultihopExitRegion(deviceId,apiAuthToken)?.enqueue(object : Callback<ResponseBody> {
			override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
				if (response.isSuccessful) {
					val responseData: Map<String, Any> = gson!!.fromJson(response.body()?.string(), APITYPETOKENMAP)
					iOnApiResponse.onSuccess(responseData)
					return
				}

				val apiErr = GRDAPIError.apiErrorFromResponseBody(response)
				iOnApiResponse.onError(apiErr.toString())
			}

			override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
				iOnApiResponse.onError(t.message)
			}
		})
	}

	fun setMultihopExitRegion(multihopExitRegion: String, deviceId: String, apiAuthToken: String, iOnApiResponse: IOnApiResponse) {
		val requestMap = mutableMapOf<String, Any>()
		requestMap["api-auth-token"] = apiAuthToken
		requestMap["multihop-exit-region"] = multihopExitRegion
		val requestData = Gson().toJson(requestMap)
		apiCalls?.settMultihopExitRegion(deviceId, requestData)?.enqueue(object : Callback<ResponseBody> {
			override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
				if (response.isSuccessful) {
					val responseData: Map<String, Any> = gson!!.fromJson(response.body()?.string(), APITYPETOKENMAP)
					iOnApiResponse.onSuccess(responseData)
					return
				}

				val apiErr = GRDAPIError.apiErrorFromResponseBody(response)
				iOnApiResponse.onError(apiErr.toString())
			}

			override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
				iOnApiResponse.onError(t.message)
			}
		})
	}

    fun logoutConnectSubscriber(requestBody: MutableMap<String, Any>, iOnApiResponse: IOnApiResponse) {
        requestBody["connect-publishable-key"] = instance.connectPublishableKey.toString()
        val call: Call<ResponseBody>? = apiCallsGRDConnect?.logoutConnectSubscriber(requestBody)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody?>) {
				if (response.isSuccessful) {
                    iOnApiResponse.onSuccess("GRDConnect Subscriber logout successfully.")
					return
                }

				val apiErr = GRDAPIError.apiErrorFromResponseBody(response)
				iOnApiResponse.onError(apiErr.toString())
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                GRDConnectManager.getCoroutineScope().launch {
                    t.message?.let { GRDVPNHelper.grdErrorFlow.emit(it) }
                }
                Log.d(TAG, API_ERROR + " logoutConnectSubscriber() " + t.message)
            }
        })
    }
}
