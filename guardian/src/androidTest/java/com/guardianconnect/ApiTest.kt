import com.guardianconnect.GRDRegion
import com.guardianconnect.api.IApiCalls
import com.guardianconnect.model.api.*
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
        grdConnectSubscriberRequest.epGrdSubscriberSecret = "secret"
        grdConnectSubscriberRequest.connectPublicKey = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
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
        connectSubscriberValidateRequest.connectPublicKey = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
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
        connectDeviceUpdateRequest.connectPublicKey = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        connectDeviceUpdateRequest.peToken = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
        connectDeviceUpdateRequest.epGrdSubscriberSecret = "test-secret"
        connectDeviceUpdateRequest.epGrdSubscriberIdentifier = "200"
        connectDeviceUpdateRequest.epGrdSubscriberEmail = "testt@example.com"
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
        connectDeviceRequest.connectPublicKey = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
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
        Mockito.`when`(callUpdateConnectDevice.execute()).thenReturn(
            Response.success(
                ConnectDeviceResponse()
            )
        )
        val connectDeviceUpdateRequest = ConnectDeviceUpdateRequest()
        connectDeviceUpdateRequest.connectPublicKey = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        connectDeviceUpdateRequest.peToken = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
        connectDeviceUpdateRequest.deviceNickname = "test_nickname"
        connectDeviceUpdateRequest.deviceUuid = "8C5701B0-8E3F-55F0-10C9-0A20A15DB926"
        val response = apiService.updateConnectDevice(
            connectDeviceUpdateRequest
        ).execute()
        assertTrue(response.body() != null)
    }

    @Test
    fun testDeleteConnectDevice() {
        val apiService = retrofitGRD.create(IApiCalls::class.java)
        Mockito.`when`(callDeleteConnectDevice.execute()).thenReturn(
            Response.success(
                "Test".toResponseBody()
            )
        )
        val connectDeviceDeleteRequest = ConnectDeviceDeleteRequest()
        connectDeviceDeleteRequest.connectPublicKey = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        connectDeviceDeleteRequest.peToken = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
        connectDeviceDeleteRequest.deviceUuid = "CAAE01BC-7F3A-7743-05E6-DBF55E4619C7"
        val response = apiService.deleteConnectDevice(
            connectDeviceDeleteRequest
        ).execute()
        assertTrue(response.code() == 200)
    }

    @Test
    fun testListAllConnectDevices() {
        val apiService = retrofitGRD.create(IApiCalls::class.java)
        Mockito.`when`(callListAllConnectDevices.execute()).thenReturn(
            Response.success(
                listOf(ConnectDeviceResponse())
            )
        )
        val connectDevicesAllRequest = ConnectDevicesAllRequest()
        connectDevicesAllRequest.connectPublicKey = "pk_bvntksq4xX5MGY4KedBa6Ck6R"
        connectDevicesAllRequest.peToken = "HpmO5f6Ty3U4WdCb5kfJ5Jgj6RB9wuc3"
        val response = apiService.allConnectDevices(
            connectDevicesAllRequest
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
}
