import com.google.gson.Gson
import com.guardianconnect.GRDDeviceFilterConfigBlocklist
import com.guardianconnect.GRDRegion
import com.guardianconnect.api.IApiCalls
import com.guardianconnect.model.EValidationMethod
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

    @Mock
    private lateinit var callGRDConnectSubscriber: Call<GRDConnectSubscriberResponse>

    @Mock
    private lateinit var callGRDConnectSubscriberValidate: Call<ConnectSubscriberValidateResponse>

    @Mock
    private lateinit var callGRDConnectSubscriberUpdate: Call<ConnectSubscriberUpdateResponse>

    @Mock
    private lateinit var callConnectDevice: Call<ConnectDeviceResponse>

    @Mock
    private lateinit var callUpdateConnectDevice: Call<ConnectDeviceResponse>

    @Mock
    private lateinit var callDeleteConnectDevice: Call<ResponseBody>

    @Mock
    private lateinit var callListAllConnectDevices: Call<List<ConnectDeviceResponse>>

    @Mock
    private lateinit var callGRDRegions: Call<List<GRDRegion>>

    @Mock
    private lateinit var callTimeZonesResponse: Call<List<TimeZonesResponse>>

    @Mock
    private lateinit var callListOfServersForRegion: Call<List<Server>>

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
        Mockito.`when`(callGRDConnectSubscriber.execute()).thenReturn(
            Response.success(
                GRDConnectSubscriberResponse()
            )
        )
        val grdConnectSubscriberRequest = GRDConnectSubscriberRequest()
        grdConnectSubscriberRequest.epGrdSubscriberEmail = "example@gmail.com"
        grdConnectSubscriberRequest.epGrdSubscriberSecret = "test-secret"
        grdConnectSubscriberRequest.connectPublishableKey = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        grdConnectSubscriberRequest.epGrdSubscriberIdentifier = "200"
        grdConnectSubscriberRequest.acceptedTos = false
        val response = apiService.createNewGRDConnectSubscriber(
            grdConnectSubscriberRequest
        ).execute()
        assertTrue(response.body() != null)
    }

    @Test
    fun testValidateGRDConnectSubscriber() {
        val apiService = retrofitGRD.create(IApiCalls::class.java)
        Mockito.`when`(callGRDConnectSubscriberValidate.execute())
            .thenReturn(Response.success(ConnectSubscriberValidateResponse()))
        val connectSubscriberValidateRequest = ConnectSubscriberValidateRequest()
        connectSubscriberValidateRequest.epGrdSubscriberIdentifier = "200"
        connectSubscriberValidateRequest.epGrdSubscriberSecret = "test-secret"
        connectSubscriberValidateRequest.connectPublishableKey = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        connectSubscriberValidateRequest.peToken = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
        val response = apiService.validateGRDConnectSubscriber(
            connectSubscriberValidateRequest
        ).execute()
        assertTrue(response.body() != null)
    }

    @Test
    fun testUpdateGRDConnectSubscriber() {
        val apiService = retrofitGRD.create(IApiCalls::class.java)
        Mockito.`when`(callGRDConnectSubscriberUpdate.execute())
            .thenReturn(Response.success(ConnectSubscriberUpdateResponse()))
        val connectDeviceUpdateRequest = ConnectSubscriberUpdateRequest()
        connectDeviceUpdateRequest.connectPublishableKey = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        connectDeviceUpdateRequest.peToken = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
        connectDeviceUpdateRequest.epGrdSubscriberSecret = "test-secret"
        connectDeviceUpdateRequest.epGrdSubscriberIdentifier = "200"
        // TODO: change email before every run (won't update with same data)
        connectDeviceUpdateRequest.epGrdSubscriberEmail = "test@example.com"
        val response = apiService.updateGRDConnectSubscriber(
            connectDeviceUpdateRequest
        ).execute()
        assertTrue(response.body() != null)
    }

    @Test
    fun testAddConnectDevice() {
        val apiService = retrofitGRD.create(IApiCalls::class.java)
        Mockito.`when`(callConnectDevice.execute()).thenReturn(
            Response.success(
                ConnectDeviceResponse()
            )
        )
        val connectDeviceRequest = ConnectDeviceRequest()
        connectDeviceRequest.connectPublishableKey = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        connectDeviceRequest.peToken = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
        connectDeviceRequest.epGrdDeviceNickname = "test_nickname"
        connectDeviceRequest.epGrdDeviceAcceptedTos = true
        val response = apiService.addConnectDevice(
            connectDeviceRequest
        ).execute()
        assertTrue(response.body() != null)
    }

    @Test
    fun testUpdateConnectDevice() {
        val apiService = retrofitGRD.create(IApiCalls::class.java)
        Mockito.`when`(callConnectDevice.execute()).thenReturn(
            Response.success(
                ConnectDeviceResponse()
            )
        )
        val connectDeviceRequest = ConnectDeviceRequest()
        connectDeviceRequest.connectPublishableKey = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        connectDeviceRequest.peToken = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
        connectDeviceRequest.epGrdDeviceNickname = "test_nickname"
        connectDeviceRequest.epGrdDeviceAcceptedTos = true
        val response = apiService.addConnectDevice(
            connectDeviceRequest
        ).execute()
        response.body()?.let {

            val apiService = retrofitGRD.create(IApiCalls::class.java)
            Mockito.`when`(callUpdateConnectDevice.execute()).thenReturn(
                Response.success(
                    ConnectDeviceResponse()
                )
            )
            val connectDeviceUpdateRequest = ConnectDeviceUpdateRequest()
            connectDeviceUpdateRequest.connectPublishableKey = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
            connectDeviceUpdateRequest.peToken = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
            connectDeviceUpdateRequest.deviceNickname = "test_nickname"
            connectDeviceUpdateRequest.deviceUuid = it.epGrdDeviceUuid
            val response = apiService.updateConnectDevice(
                connectDeviceUpdateRequest
            ).execute()
            assertTrue(response.body() != null)
        }
    }

    @Test
    fun testDeleteConnectDevice() {
        val apiService = retrofitGRD.create(IApiCalls::class.java)
        Mockito.`when`(callConnectDevice.execute()).thenReturn(
            Response.success(
                ConnectDeviceResponse()
            )
        )
        val connectDeviceRequest = ConnectDeviceRequest()
        connectDeviceRequest.connectPublishableKey = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        connectDeviceRequest.peToken = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
        connectDeviceRequest.epGrdDeviceNickname = "test_nickname"
        connectDeviceRequest.epGrdDeviceAcceptedTos = true
        val response = apiService.addConnectDevice(
            connectDeviceRequest
        ).execute()
        response.body()?.let {
            val apiService = retrofitGRD.create(IApiCalls::class.java)
            Mockito.`when`(callDeleteConnectDevice.execute()).thenReturn(
                Response.success(
                    "Test".toResponseBody()
                )
            )
            val connectDeviceDeleteRequest = ConnectDeleteDeviceRequest()
            connectDeviceDeleteRequest.connectPublishableKey = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
            connectDeviceDeleteRequest.peToken = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
            connectDeviceDeleteRequest.deviceUuid = it.epGrdDeviceUuid
            val response = apiService.deleteConnectDevice(
                connectDeviceDeleteRequest
            ).execute()
            assertTrue(response.code() == 200)
        }
    }

    @Test
    fun testListAllConnectDevices() {
        val apiService = retrofitGRD.create(IApiCalls::class.java)
        Mockito.`when`(callListAllConnectDevices.execute()).thenReturn(
            Response.success(
                listOf(ConnectDeviceResponse())
            )
        )
        val connectDevicesAllDevicesRequest = ConnectDevicesAllDevicesRequest()
        connectDevicesAllDevicesRequest.connectPublishableKey = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        connectDevicesAllDevicesRequest.peToken = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
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
    fun testListOfServersForRegion() {
        val apiService = retrofit.create(IApiCalls::class.java)
        Mockito.`when`(callListOfServersForRegion.execute()).thenReturn(
            Response.success(
                listOf(Server())
            )
        )
        val requestServersForRegion = RequestServersForRegion()
        requestServersForRegion.region = "eu-de"
        val response = apiService.requestListOfServersForRegion(
            requestServersForRegion
        ).execute()
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
        val validationMethodPEToken = ValidationMethodPEToken()
        validationMethodPEToken.validationMethod = EValidationMethod.PE_TOKEN.method
        validationMethodPEToken.peToken = "ZkReFZ58yttZP0rpg8DT8XObcXpGsRbl"
        val response = apiService.getSubscriberCredentialsPEToken(
            validationMethodPEToken
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
        val validationMethodPEToken = ValidationMethodPEToken()
        validationMethodPEToken.validationMethod = EValidationMethod.PE_TOKEN.method
        validationMethodPEToken.peToken = "ZkReFZ58yttZP0rpg8DT8XObcXpGsRbl"
        val response = apiService.getSubscriberCredentialsPEToken(
            validationMethodPEToken
        ).execute()

        response.body()?.string().let {
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
                assertTrue(response.body() != null)
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
        val validationMethodPEToken = ValidationMethodPEToken()
        validationMethodPEToken.validationMethod = EValidationMethod.PE_TOKEN.method
        validationMethodPEToken.peToken = "ZkReFZ58yttZP0rpg8DT8XObcXpGsRbl"
        val response = apiService.getSubscriberCredentialsPEToken(
            validationMethodPEToken
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
                    val vpnCredentials = VPNCredentials()
                    vpnCredentials.apiAuthToken = vpn?.apiAuthToken
                    vpnCredentials.subscriberCredential =
                        subscriberCredentialResponse.subscriberCredential

                    Mockito.`when`(callVerifyVPNDevice.execute()).thenReturn(
                        Response.success(
                            "Test".toResponseBody()
                        )
                    )
                    val response = vpn?.clientId?.let { it1 ->
                        apiService.verifyVPNCredentials(
                            it1,
                            vpnCredentials
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
        val validationMethodPEToken = ValidationMethodPEToken()
        validationMethodPEToken.validationMethod = EValidationMethod.PE_TOKEN.method
        validationMethodPEToken.peToken = "ZkReFZ58yttZP0rpg8DT8XObcXpGsRbl"
        val response = apiService.getSubscriberCredentialsPEToken(
            validationMethodPEToken
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
                    val vpnCredentials = VPNCredentials()
                    vpnCredentials.apiAuthToken = vpn?.apiAuthToken
                    vpnCredentials.subscriberCredential =
                        subscriberCredentialResponse.subscriberCredential

                    Mockito.`when`(callInvalidateVPNDevice.execute()).thenReturn(
                        Response.success(
                            "Test".toResponseBody()
                        )
                    )
                    val response = vpn?.clientId?.let { it1 ->
                        apiService.invalidateVPNCredentials(
                            it1,
                            vpnCredentials
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
        val validationMethodPEToken = ValidationMethodPEToken()
        validationMethodPEToken.validationMethod = EValidationMethod.PE_TOKEN.method
        validationMethodPEToken.peToken = "ZkReFZ58yttZP0rpg8DT8XObcXpGsRbl"
        val response = apiService.getSubscriberCredentialsPEToken(
            validationMethodPEToken
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
                    val vpnCredentials = VPNCredentials()
                    vpnCredentials.apiAuthToken = vpn?.apiAuthToken
                    vpnCredentials.subscriberCredential =
                        subscriberCredentialResponse.subscriberCredential

                    Mockito.`when`(callDownloadAlerts.execute()).thenReturn(
                        Response.success(
                            listOf(AlertsResponse())
                        )
                    )
                    val alerts = Alerts()
                    alerts.apiAuthToken = vpnCredentials.apiAuthToken
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
        val validationMethodPEToken = ValidationMethodPEToken()
        validationMethodPEToken.validationMethod = EValidationMethod.PE_TOKEN.method
        validationMethodPEToken.peToken = "ZkReFZ58yttZP0rpg8DT8XObcXpGsRbl"
        val response = apiService.getSubscriberCredentialsPEToken(
            validationMethodPEToken
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
                    val vpnCredentials = VPNCredentials()
                    vpnCredentials.apiAuthToken = vpn?.apiAuthToken
                    vpnCredentials.subscriberCredential =
                        subscriberCredentialResponse.subscriberCredential

                    Mockito.`when`(callSetAlertsDownloadTimeStamp.execute()).thenReturn(
                        Response.success(
                            "Test".toResponseBody()
                        )
                    )
                    val baseRequest = BaseRequest()
                    baseRequest.apiAuthToken = vpnCredentials.apiAuthToken
                    val response = vpn?.clientId?.let { it1 ->
                        apiService.setAlertsDownloadTimestamp(
                            it1,
                            baseRequest
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
        val validationMethodPEToken = ValidationMethodPEToken()
        validationMethodPEToken.validationMethod = EValidationMethod.PE_TOKEN.method
        validationMethodPEToken.peToken = "ZkReFZ58yttZP0rpg8DT8XObcXpGsRbl"
        val response = apiService.getSubscriberCredentialsPEToken(
            validationMethodPEToken
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
                    val vpnCredentials = VPNCredentials()
                    vpnCredentials.apiAuthToken = vpn?.apiAuthToken
                    vpnCredentials.subscriberCredential =
                        subscriberCredentialResponse.subscriberCredential

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
        val logoutConnectSubscriberRequest = LogoutConnectSubscriberRequest()
        logoutConnectSubscriberRequest.connectPublishableKey = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        logoutConnectSubscriberRequest.peToken = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
        val response = apiService.logoutConnectSubscriber(
            logoutConnectSubscriberRequest
        ).execute()
        assertTrue(response.code() == 200)
    }
}
