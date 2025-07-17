import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.guardianconnect.GRDDeviceFilterConfigBlocklist
import com.guardianconnect.GRDRegion
import com.guardianconnect.api.IApiCalls
import com.guardianconnect.enumeration.GRDServerFeatureEnvironment
import com.guardianconnect.managers.GRDServerManager.Companion.kGRDServerManagerBetaCapableKey
import com.guardianconnect.managers.GRDServerManager.Companion.kGRDServerManagerFeatureEnvironmentKey
import com.guardianconnect.managers.GRDServerManager.Companion.kGRDServerManagerRegionKey
import com.guardianconnect.managers.GRDServerManager.Companion.kGRDServerManagerRegionPrecision
import com.guardianconnect.model.api.*
import com.guardianconnect.util.Constants
import com.wireguard.crypto.KeyPair
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class ApiTest {

    private val BASE_URL_GRD = "https://connect-api.dev.guardianapp.com/"
    private val retrofitGRD = Retrofit.Builder()
        .baseUrl(BASE_URL_GRD)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val BASE_URL = "https://connect-api.guardianapp.com"
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val BASE_URL_REGION = "https://frankfurt-10.sgw.guardianapp.com"
    private val retrofitRegion = Retrofit.Builder()
        .baseUrl(BASE_URL_REGION)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

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

        const val kGRDConnectDeviceKey = "ep-grd-device"
        const val kGRDConnectDeviceNicknameKey = "ep-grd-device-nickname"
        const val kGRDConnectDeviceUUIDKey = "ep-grd-device-uuid"
        const val kGRDConnectDeviceCreatedAtKey = "ep-grd-device-created-at"
        const val kGrdDeviceAcceptedTos = "ep-grd-device-accepted-tos"
    }

    @Mock
    private lateinit var callGRDConnectSubscriber: Call<ResponseBody>

    @Mock
    private lateinit var callGRDConnectSubscriberValidate: Call<ResponseBody>

    @Mock
    private lateinit var callGRDConnectSubscriberUpdate: Call<ResponseBody>

    @Mock
    private lateinit var callConnectDevice: Call<ResponseBody>

    @Mock
    private lateinit var callUpdateConnectDevice: Call<ResponseBody>

    @Mock
    private lateinit var callDeleteConnectDevice: Call<ResponseBody>

    @Mock
    private lateinit var callListAllConnectDevices: Call<List<ResponseBody>>

    @Mock
    private lateinit var callGRDRegions: Call<List<GRDRegion>>

    @Mock
    private lateinit var callTimeZonesResponse: Call<List<TimeZonesResponse>>

    @Mock
    private lateinit var callListOfServersForRegion: Call<List<GRDSGWServer>>

    @Mock
    private lateinit var callSubscriberCredential: Call<SubscriberCredentialResponse>

    @Mock
    private lateinit var callServerStatus: Call<ResponseBody>

    @Mock
    private lateinit var callNewVPNDevice: Call<NewVPNDeviceResponse>

    @Mock
    private lateinit var callVerifyVPNDevice: Call<ResponseBody>

    @Mock
    private lateinit var callInvalidateVPNDevice: Call<ResponseBody>

    @Mock
    private lateinit var callDownloadAlerts: Call<List<AlertsResponse>>

    @Mock
    private lateinit var callSetAlertsDownloadTimeStamp: Call<ResponseBody>

    @Mock
    private lateinit var callSetDeviceFilterConfig: Call<ResponseBody>

    @Mock
    private lateinit var callLogoutConnectSubscriber: Call<ResponseBody>

    @Test
    fun testCreateNewGRDConnectSubscriber() {
        val apiService = retrofitGRD.create(IApiCalls::class.java)
        Mockito.`when`(callGRDConnectSubscriber.execute())
            .thenReturn(
                Response.success("Test".toResponseBody())
            )
        val grdConnectSubscriberRequest = mutableMapOf<String, Any>()
        grdConnectSubscriberRequest[kGRDConnectSubscriberEmailKey] = "example@gmail.com"
        grdConnectSubscriberRequest[kGRDConnectSubscriberSecretKey] = "test-secret"
        grdConnectSubscriberRequest[connectPublishableKey] = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        grdConnectSubscriberRequest[kGRDConnectSubscriberIdentifierKey] = "200"
        grdConnectSubscriberRequest[kGRDConnectSubscriberAcceptedTOSKey] = false
        val response = apiService.createNewGRDConnectSubscriber(
            grdConnectSubscriberRequest
        ).execute()
        assertTrue(response.body() != null)
    }

    @Test
    fun testValidateGRDConnectSubscriber() {
        val apiService = retrofitGRD.create(IApiCalls::class.java)
        Mockito.`when`(callGRDConnectSubscriberValidate.execute())
            .thenReturn(Response.success("Test".toResponseBody()))
        val connectSubscriberValidateRequest: MutableMap<String, Any> = mutableMapOf()
        connectSubscriberValidateRequest[kGRDConnectSubscriberIdentifierKey] = "200"
        connectSubscriberValidateRequest[kGRDConnectSubscriberSecretKey] = "test-secret"
        connectSubscriberValidateRequest[connectPublishableKey] = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        connectSubscriberValidateRequest[peTokenKey] = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
        val response = apiService.validateGRDConnectSubscriber(
            connectSubscriberValidateRequest
        ).execute()
        assertTrue(response.body() != null)
    }

    @Test
    fun testUpdateGRDConnectSubscriber() {
        val apiService = retrofitGRD.create(IApiCalls::class.java)
        Mockito.`when`(callGRDConnectSubscriberUpdate.execute())
            .thenReturn(
                Response.success("Test".toResponseBody())
            )
        val connectDeviceUpdateRequest: MutableMap<String, Any> = mutableMapOf()
        connectDeviceUpdateRequest[connectPublishableKey] = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        connectDeviceUpdateRequest[peTokenKey] = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
        connectDeviceUpdateRequest[kGRDConnectSubscriberSecretKey] = "test-secret"
        connectDeviceUpdateRequest[kGRDConnectSubscriberIdentifierKey] = "test-200"

        // TODO: change email before every run (won't update with same data)
        connectDeviceUpdateRequest["epGrdSubscriberEmail"] = "test@example.com"
        val response = apiService.updateGRDConnectSubscriber(
            connectDeviceUpdateRequest
        ).execute()
        assertTrue(response.body() != null)
    }

    @Test
    fun testAddConnectDevice() {
        val apiService = retrofitGRD.create(IApiCalls::class.java)
        Mockito.`when`(callConnectDevice.execute()).thenReturn(
            Response.success("Test".toResponseBody())
        )
        val connectDeviceRequest = mutableMapOf<String, Any>()
        connectDeviceRequest[connectPublishableKey] = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        connectDeviceRequest[peTokenKey] = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
        connectDeviceRequest[kGRDConnectDeviceNicknameKey] = "test_nickname"
        connectDeviceRequest[kGrdDeviceAcceptedTos] = true
        val response = apiService.addConnectDevice(
            connectDeviceRequest
        ).execute()
        assertTrue(response.body() != null)
    }

    @Test
    fun testUpdateConnectDevice() {
        val apiService = retrofitGRD.create(IApiCalls::class.java)
        Mockito.`when`(callConnectDevice.execute()).thenReturn(
            Response.success("Test".toResponseBody())
        )
        val connectDeviceRequest = mutableMapOf<String, Any>()
        connectDeviceRequest[connectPublishableKey] = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        connectDeviceRequest[peTokenKey] = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
        connectDeviceRequest[kGRDConnectDeviceNicknameKey] = "test_nickname"
        connectDeviceRequest[kGrdDeviceAcceptedTos] = true
        val response = apiService.addConnectDevice(
            connectDeviceRequest
        ).execute()
        response.body()?.let {
            it as MutableMap<*, *>

            val apiService2 = retrofitGRD.create(IApiCalls::class.java)
            Mockito.`when`(callUpdateConnectDevice.execute()).thenReturn(
                Response.success(
                    "Test".toResponseBody()
                )
            )
            val connectDeviceUpdateRequest = mutableMapOf<String, Any>()
            connectDeviceRequest[connectPublishableKey] = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
            connectDeviceRequest[peTokenKey] = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
            connectDeviceRequest[kGRDConnectDeviceNicknameKey] = "test_nickname"
            connectDeviceUpdateRequest[kGRDConnectDeviceUUIDKey] =
                it[kGRDConnectDeviceUUIDKey] as MutableMap<*, *>
            val response2 = apiService2.updateConnectDevice(
                connectDeviceUpdateRequest
            ).execute()
            assertTrue(response2.body() != null)
        }
    }

    @Test
    fun testDeleteConnectDevice() {
        val apiService = retrofitGRD.create(IApiCalls::class.java)
        Mockito.`when`(callConnectDevice.execute()).thenReturn(
            Response.success(
                "Test".toResponseBody()
            )
        )
        val connectDeviceRequest = mutableMapOf<String, Any>()
        connectDeviceRequest[connectPublishableKey] = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        connectDeviceRequest[peTokenKey] = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
        connectDeviceRequest[kGRDConnectDeviceNicknameKey] = "test_nickname"
        connectDeviceRequest[kGrdDeviceAcceptedTos] = true
        val response = apiService.addConnectDevice(
            connectDeviceRequest
        ).execute()
        response.body()?.let {
            it as MutableMap<*, *>

            val apiService2 = retrofitGRD.create(IApiCalls::class.java)
            Mockito.`when`(callDeleteConnectDevice.execute()).thenReturn(
                Response.success(
                    "Test".toResponseBody()
                )
            )
            val connectDeviceDeleteRequest = mutableMapOf<String, Any>()
            connectDeviceRequest[connectPublishableKey] = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
            connectDeviceRequest[peTokenKey] = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
            connectDeviceRequest[kGRDConnectDeviceUUIDKey] =
                it[kGRDConnectDeviceUUIDKey] as MutableMap<*, *>
            val response2 = apiService2.deleteConnectDevice(
                connectDeviceDeleteRequest
            ).execute()
            assertTrue(response2.code() == 200)
        }
    }

    @Test
    fun testListAllConnectDevices() {
        val apiService = retrofitGRD.create(IApiCalls::class.java)
        Mockito.`when`(callListAllConnectDevices.execute()).thenReturn(
            Response.success(
                listOf("Test".toResponseBody())
            )
        )
        val connectDevicesAllDevicesRequest = mutableMapOf<String, Any>()
        connectDevicesAllDevicesRequest[connectPublishableKey] = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        connectDevicesAllDevicesRequest[peTokenKey] = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
        val response = apiService.allConnectDevices(
            connectDevicesAllDevicesRequest
        ).execute()
        assertTrue(response.body() != null)
    }

    @Test
    fun testAllRegions() {
        val apiService = retrofit.create(IApiCalls::class.java)
        Mockito.`when`(callGRDRegions.execute()).thenReturn(Response.success(listOf(GRDRegion())))
        val response = apiService.requestAllGuardianRegions().execute()
        assertTrue(response.body() != null)
    }

    @Test
    fun testTimeZonesForRegion() {
        val apiService = retrofit.create(IApiCalls::class.java)
        Mockito.`when`(callTimeZonesResponse.execute())
            .thenReturn(Response.success(listOf(TimeZonesResponse())))
        val response = apiService.getListOfSupportedTimeZones().execute()
        assertTrue(response.body() != null)
    }

    @Test
    fun testGetSubscriberCredentialsPEToken() {
        val apiService = retrofit.create(IApiCalls::class.java)
        Mockito.`when`(callSubscriberCredential.execute()).thenReturn(
            Response.success(
                SubscriberCredentialResponse()
            )
        )
        val request = mutableMapOf<String, Any>()
        request["validation-method"] = "pe-token"
        request["pe-token"] = "ZkReFZ58yttZP0rpg8DT8XObcXpGsRbl"
        val gson = Gson()
        val requestMap: MutableMap<String, Any> = gson.fromJson(
            gson.toJson(request),
            object : TypeToken<MutableMap<String, Any>>() {}.type
        )
        val response = apiService.getSubscriberCredential(
            requestMap
        ).execute()

        assertTrue(response.body() != null)
    }

    @Test
    fun testServerStatus() {
        val apiService = retrofitRegion.create(IApiCalls::class.java)
        Mockito.`when`(callServerStatus.execute()).thenReturn(
            Response.success(
                "Test".toResponseBody()
            )
        )
        val response = apiService.getServerStatus().execute()
        assertTrue(response.code() == 200)
    }

    @Test
    fun testNewVPNDevice() {
        val apiService = retrofit.create(IApiCalls::class.java)
        Mockito.`when`(callSubscriberCredential.execute()).thenReturn(
            Response.success(
                SubscriberCredentialResponse()
            )
        )
        val request = mutableMapOf<String, Any>()
        request["validation-method"] = "pe-token"
        request["pe-token"] = "ZkReFZ58yttZP0rpg8DT8XObcXpGsRbl"
        val gson = Gson()
        val requestMap: MutableMap<String, Any> = gson.fromJson(
            gson.toJson(request),
            object : TypeToken<MutableMap<String, Any>>() {}.type
        )
        val response = apiService.getSubscriberCredential(
            requestMap
        ).execute()

        response.body()?.string().let {
            val subscriberCredentialResponse =
                Gson().fromJson(
                    it,
                    SubscriberCredentialResponse::class.java
                )
            subscriberCredentialResponse.subscriberCredential?.let { scs ->
                val apiService2 = retrofitRegion.create(IApiCalls::class.java)
                Mockito.`when`(callNewVPNDevice.execute()).thenReturn(
                    Response.success(
                        NewVPNDeviceResponse()
                    )
                )
                val newVPNDevice = NewVPNDevice()
                newVPNDevice.transportProtocol = Constants.GRD_WIREGUARD
                newVPNDevice.subscriberCredential = scs
                val keyPair = KeyPair()
                val keyPairGenerated = KeyPair(keyPair.privateKey)
                val publicKey = keyPairGenerated.publicKey.toBase64()
                newVPNDevice.publicKey = publicKey
                val response2 = apiService2.createNewVPNDevice(
                    newVPNDevice
                ).execute()
                assertTrue(response2.body() != null)
            }
        }
    }

    @Test
    fun testVerifyVPNDevice() {
        val apiService = retrofit.create(IApiCalls::class.java)
        Mockito.`when`(callSubscriberCredential.execute()).thenReturn(
            Response.success(
                SubscriberCredentialResponse()
            )
        )
        val request = mutableMapOf<String, Any>()
        request["validation-method"] = "pe-token"
        request["pe-token"] = "ZkReFZ58yttZP0rpg8DT8XObcXpGsRbl"
        val gson = Gson()
        val requestMap: MutableMap<String, Any> = gson.fromJson(
            gson.toJson(request),
            object : TypeToken<MutableMap<String, Any>>() {}.type
        )
        val response = apiService.getSubscriberCredential(
            requestMap
        ).execute()

        val responseString = response.body()?.string()
        responseString.let {
            val subscriberCredentialResponse =
                Gson().fromJson(
                    it,
                    SubscriberCredentialResponse::class.java
                )
            subscriberCredentialResponse.subscriberCredential?.let { scs ->
                val apiService = retrofitRegion.create(IApiCalls::class.java)
                Mockito.`when`(callNewVPNDevice.execute()).thenReturn(
                    Response.success(
                        NewVPNDeviceResponse()
                    )
                )
                val newVPNDevice = NewVPNDevice()
                newVPNDevice.transportProtocol = Constants.GRD_WIREGUARD
                newVPNDevice.subscriberCredential = scs
                val keyPair = KeyPair()
                val keyPairGenerated = KeyPair(keyPair.privateKey)
                val publicKey = keyPairGenerated.publicKey.toBase64()
                newVPNDevice.publicKey = publicKey
                val response = apiService.createNewVPNDevice(
                    newVPNDevice
                ).execute()

                response.body().let { vpn ->
                    val requestBody = mutableMapOf<String, Any>()
                    requestBody["api-auth-token"]           = vpn?.apiAuthToken!!

                    Mockito.`when`(callVerifyVPNDevice.execute()).thenReturn(
                        Response.success(
                            "Test".toResponseBody()
                        )
                    )
                    val response = vpn?.clientId?.let { it1 ->
                        apiService.verifyVPNCredentials(
                            it1,
                            requestBody
                        ).execute()
                    }
                    assertTrue(response?.code() == 200)
                }
            }
        }
    }

    @Test
    fun testInvalidateVPNDevice() {
        val apiService = retrofit.create(IApiCalls::class.java)
        Mockito.`when`(callSubscriberCredential.execute()).thenReturn(
            Response.success(
                SubscriberCredentialResponse()
            )
        )
        val request = mutableMapOf<String, Any>()
        request["validation-method"] = "pe-token"
        request["pe-token"] = "ZkReFZ58yttZP0rpg8DT8XObcXpGsRbl"
        val gson = Gson()
        val requestMap: MutableMap<String, Any> = gson.fromJson(
            gson.toJson(request),
            object : TypeToken<MutableMap<String, Any>>() {}.type
        )
        val response = apiService.getSubscriberCredential(
            requestMap
        ).execute()

        val responseString = response.body()?.string()
        responseString.let {
            val subscriberCredentialResponse =
                Gson().fromJson(
                    it,
                    SubscriberCredentialResponse::class.java
                )
            subscriberCredentialResponse.subscriberCredential?.let { scs ->
                val apiService = retrofitRegion.create(IApiCalls::class.java)
                Mockito.`when`(callNewVPNDevice.execute()).thenReturn(
                    Response.success(
                        NewVPNDeviceResponse()
                    )
                )
                val newVPNDevice = NewVPNDevice()
                newVPNDevice.transportProtocol = Constants.GRD_WIREGUARD
                newVPNDevice.subscriberCredential = scs
                val keyPair = KeyPair()
                val keyPairGenerated = KeyPair(keyPair.privateKey)
                val publicKey = keyPairGenerated.publicKey.toBase64()
                newVPNDevice.publicKey = publicKey
                val response = apiService.createNewVPNDevice(
                    newVPNDevice
                ).execute()

                response.body().let { vpn ->
                    val requestBody = mutableMapOf<String, Any>()
                    requestBody["subscriber-credential"]    = subscriberCredentialResponse.subscriberCredential!!
                    requestBody["api-auth-token"]           = vpn?.apiAuthToken!!

                    Mockito.`when`(callInvalidateVPNDevice.execute()).thenReturn(
                        Response.success(
                            "Test".toResponseBody()
                        )
                    )
                    val response = vpn?.clientId?.let { it1 ->
                        apiService.invalidateVPNCredentials(
                            it1,
                            requestBody
                        ).execute()
                    }
                    assertTrue(response?.code() == 200)
                }
            }
        }
    }

    @Test
    fun testDownloadAlerts() {
        val apiService = retrofit.create(IApiCalls::class.java)
        Mockito.`when`(callSubscriberCredential.execute()).thenReturn(
            Response.success(
                SubscriberCredentialResponse()
            )
        )
        val request = mutableMapOf<String, Any>()
        request["validation-method"] = "pe-token"
        request["pe-token"] = "ZkReFZ58yttZP0rpg8DT8XObcXpGsRbl"
        val gson = Gson()
        val requestMap: MutableMap<String, Any> = gson.fromJson(
            gson.toJson(request),
            object : TypeToken<MutableMap<String, Any>>() {}.type
        )
        val response = apiService.getSubscriberCredential(
            requestMap
        ).execute()

        val responseString = response.body()?.string()
        responseString.let {
            val subscriberCredentialResponse =
                Gson().fromJson(
                    it,
                    SubscriberCredentialResponse::class.java
                )
            subscriberCredentialResponse.subscriberCredential?.let { scs ->
                val apiService = retrofitRegion.create(IApiCalls::class.java)
                Mockito.`when`(callNewVPNDevice.execute()).thenReturn(
                    Response.success(
                        NewVPNDeviceResponse()
                    )
                )
                val newVPNDevice = NewVPNDevice()
                newVPNDevice.transportProtocol = Constants.GRD_WIREGUARD
                newVPNDevice.subscriberCredential = scs
                val keyPair = KeyPair()
                val keyPairGenerated = KeyPair(keyPair.privateKey)
                val publicKey = keyPairGenerated.publicKey.toBase64()
                newVPNDevice.publicKey = publicKey
                val response = apiService.createNewVPNDevice(
                    newVPNDevice
                ).execute()

                response.body().let { vpn ->
                    Mockito.`when`(callDownloadAlerts.execute()).thenReturn(
                        Response.success(
                            listOf(AlertsResponse())
                        )
                    )
                    val alerts = Alerts()
                    alerts.apiAuthToken = vpn?.apiAuthToken
                    alerts.timestamp = 128396546
                    val response = vpn?.clientId?.let { it1 ->
                        apiService.downloadAlerts(
                            it1,
                            alerts
                        ).execute()
                    }
                    assertTrue(response?.body() != null)
                }
            }
        }
    }

    @Test
    fun testSetAlertsDownloadStamp() {
        val apiService = retrofit.create(IApiCalls::class.java)
        Mockito.`when`(callSubscriberCredential.execute()).thenReturn(
            Response.success(
                SubscriberCredentialResponse()
            )
        )
        val request = mutableMapOf<String, Any>()
        request["validation-method"] = "pe-token"
        request["pe-token"] = "ZkReFZ58yttZP0rpg8DT8XObcXpGsRbl"
        val gson = Gson()
        val requestMap: MutableMap<String, Any> = gson.fromJson(
            gson.toJson(request),
            object : TypeToken<MutableMap<String, Any>>() {}.type
        )
        val response = apiService.getSubscriberCredential(
            requestMap
        ).execute()

        val responseString = response.body()?.string()
        responseString.let {
            val subscriberCredentialResponse =
                Gson().fromJson(
                    it,
                    SubscriberCredentialResponse::class.java
                )
            subscriberCredentialResponse.subscriberCredential?.let { scs ->
                val apiService = retrofitRegion.create(IApiCalls::class.java)
                Mockito.`when`(callNewVPNDevice.execute()).thenReturn(
                    Response.success(
                        NewVPNDeviceResponse()
                    )
                )
                val newVPNDevice = NewVPNDevice()
                newVPNDevice.transportProtocol = Constants.GRD_WIREGUARD
                newVPNDevice.subscriberCredential = scs
                val keyPair = KeyPair()
                val keyPairGenerated = KeyPair(keyPair.privateKey)
                val publicKey = keyPairGenerated.publicKey.toBase64()
                newVPNDevice.publicKey = publicKey
                val response = apiService.createNewVPNDevice(
                    newVPNDevice
                ).execute()

                response.body().let { vpn ->
                    val requestBody = mutableMapOf<String, Any>()
                    requestBody["api-auth-token"]           = vpn?.apiAuthToken!!

                    Mockito.`when`(callSetAlertsDownloadTimeStamp.execute()).thenReturn(
                        Response.success(
                            "Test".toResponseBody()
                        )
                    )
                    
                    val response = vpn?.clientId?.let { it1 ->
                        apiService.setAlertsDownloadTimestamp(
                            it1,
                            requestBody
                        ).execute()
                    }
                    assertTrue(response?.code() == 200)
                }
            }
        }
    }

    @Test
    fun testSetDeviceFilterConfig() {
        val apiService = retrofit.create(IApiCalls::class.java)
        Mockito.`when`(callSubscriberCredential.execute()).thenReturn(
            Response.success(
                SubscriberCredentialResponse()
            )
        )
        val request = mutableMapOf<String, Any>()
        request["validation-method"] = "pe-token"
        request["pe-token"] = "ZkReFZ58yttZP0rpg8DT8XObcXpGsRbl"
        val gson = Gson()
        val requestMap: MutableMap<String, Any> = gson.fromJson(
            gson.toJson(request),
            object : TypeToken<MutableMap<String, Any>>() {}.type
        )
        val response = apiService.getSubscriberCredential(
            requestMap
        ).execute()

        val responseString = response.body()?.string()
        responseString.let {
            val subscriberCredentialResponse =
                Gson().fromJson(
                    it,
                    SubscriberCredentialResponse::class.java
                )
            subscriberCredentialResponse.subscriberCredential?.let { scs ->
                val apiService = retrofitRegion.create(IApiCalls::class.java)
                Mockito.`when`(callNewVPNDevice.execute()).thenReturn(
                    Response.success(
                        NewVPNDeviceResponse()
                    )
                )
                val newVPNDevice = NewVPNDevice()
                newVPNDevice.transportProtocol = Constants.GRD_WIREGUARD
                newVPNDevice.subscriberCredential = scs
                val keyPair = KeyPair()
                val keyPairGenerated = KeyPair(keyPair.privateKey)
                val publicKey = keyPairGenerated.publicKey.toBase64()
                newVPNDevice.publicKey = publicKey
                val response = apiService.createNewVPNDevice(
                    newVPNDevice
                ).execute()

                response.body().let { vpn ->
                    Mockito.`when`(callSetDeviceFilterConfig.execute()).thenReturn(
                        Response.success(
                            "Test".toResponseBody()
                        )
                    )
                    val grdDeviceFilterConfigBlocklist = GRDDeviceFilterConfigBlocklist()
                    val map = HashMap<Any, Any>()
                    map.putAll(grdDeviceFilterConfigBlocklist.apiPortableBlocklist())
                    map["api-auth-token"] = vpn?.apiAuthToken.toString()
                    val json = Gson().toJsonTree(map).asJsonObject
                    val response = vpn?.clientId?.let { it1 ->
                        apiService.setDeviceFilterConfig(
                            it1,
                            json
                        ).execute()
                    }
                    assertTrue(response?.body() != null)
                }
            }
        }
    }

    @Test
    fun testLogoutConnectSubscriber() {
        val apiService = retrofitGRD.create(IApiCalls::class.java)
        Mockito.`when`(callLogoutConnectSubscriber.execute()).thenReturn(
            Response.success(
                "Test".toResponseBody()
            )
        )
        val logoutConnectSubscriberRequest: MutableMap<String, Any> = mutableMapOf()
        logoutConnectSubscriberRequest["connectPublishableKey"] = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        logoutConnectSubscriberRequest["peToken"] = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
        val response = apiService.logoutConnectSubscriber(
            logoutConnectSubscriberRequest
        ).execute()
        assertTrue(response.code() == 200)
    }
}
