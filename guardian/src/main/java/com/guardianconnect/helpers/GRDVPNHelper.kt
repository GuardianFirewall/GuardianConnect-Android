package com.guardianconnect.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.guardianconnect.GRDCredential
import com.guardianconnect.GRDPEToken
import com.guardianconnect.GRDSubscriberCredential
import com.guardianconnect.GRDTransportProtocol
import com.guardianconnect.GRDWireGuardConfiguration
import com.guardianconnect.R
import com.guardianconnect.api.IOnApiResponse
import com.guardianconnect.api.Repository
import com.guardianconnect.billing.BillingManager
import com.guardianconnect.enumeration.GRDServerFeatureEnvironment
import com.guardianconnect.managers.GRDConnectManager
import com.guardianconnect.managers.GRDCredentialManager
import com.guardianconnect.managers.GRDServerManager
import com.guardianconnect.model.TunnelModel
import com.guardianconnect.model.api.*
import com.guardianconnect.util.Constants
import com.guardianconnect.util.Constants.Companion.GRD_CONNECT_USER_PREFERRED_DNS_SERVERS
import com.guardianconnect.util.Constants.Companion.GRD_SUBSCRIBER_CREDENTIAL
import com.guardianconnect.util.Constants.Companion.GRD_WIREGUARD
import com.guardianconnect.util.ErrorMessages
import com.guardianconnect.util.GRDLogger
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.Reader
import java.io.StringReader

/* The GRDVPNHelper class is the main class the integrating app is interacting with. It should
    provide high level APIs to start a VPN connection, stop a VPN connection and is the source of
    truth for currently used VPN credentials and other securely stored tokens */

@SuppressLint("StaticFieldLeak")
object GRDVPNHelper {

    private val TAG = GRDVPNHelper::class.java.simpleName
    var grdSubscriberCredential: GRDSubscriberCredential? = null
    var grdCredentialManager: GRDCredentialManager? = null
    var grdServerManager: GRDServerManager? = null
    private var context: Context? = null
    var connectAPIHostname: String = ""
    var connectPublishableKey: String = ""
    var tunnelName: String = ""
    var validForDays: Long = 60
    private var preferBetaCapableServers: Boolean? = null
    private var vpnServerFeatureEnvironment: GRDServerFeatureEnvironment? = null
    private var regionPrecision: String? = null
    var appExceptions: ArrayList<String> = arrayListOf()
    var excludeLANTraffic: Boolean? = true
    var iapApplicationId: String? = null

    init {
        grdSubscriberCredential = GRDSubscriberCredential()
        grdCredentialManager = GRDCredentialManager()
        grdServerManager = GRDServerManager()
    }

    fun initHelper(context: Context) {
        GRDVPNHelper.context = context
        preferBetaCapableServers = false
        vpnServerFeatureEnvironment = GRDServerFeatureEnvironment.ServerFeatureEnvironmentProduction
        appExceptions = getArrayListOfAppExceptions()

        excludeLANTraffic =
            GRDConnectManager.getSharedPrefs().getBoolean(
                Constants.kGRDExcludeLANTraffic, true
            )

        val savedPrecision = GRDConnectManager.getSharedPrefs()
            .getString(Constants.kGRDPreferredRegionPrecision, null)

        if (!savedPrecision.isNullOrEmpty()) {
            regionPrecision = savedPrecision
            when (regionPrecision) {
                Constants.kGRDRegionPrecisionDefault -> {}
                Constants.kGRDRegionPrecisionCity -> {}
                Constants.kGRDRegionPrecisionCountry -> {}
                Constants.kGRDRegionPrecisionCityByCountry -> {}
                else -> {
                    Log.d(
                        TAG,
                        "Preferred region precision '$regionPrecision' does not match any of the known constants!"
                    )
                }
            }
        }
        observeStatus()
    }

    fun setRegionPrecision(precision: String) {
        GRDConnectManager.getSharedPrefsEditor()
            .putString(Constants.kGRDPreferredRegionPrecision, precision)?.apply()
        regionPrecision = precision

        if (precision == Constants.kGRDRegionPrecisionDefault) {
            GRDConnectManager.getSharedPrefsEditor().remove(Constants.kGRDPreferredRegionPrecision)
                ?.apply()
        }
    }

    fun setAppExceptionPackages(apps: ArrayList<String>?) {
        appExceptions = if (apps == null) {
            GRDConnectManager.getSharedPrefsEditor()
                .remove(Constants.kGRDAppExceptionsPackageNames)?.apply()
            ArrayList()
        } else {
            setArrayListOfAppExceptions(apps)
            apps
        }
    }

    private fun setArrayListOfAppExceptions(stringList: ArrayList<String>) {
        val gson = Gson()
        val jsonString = gson.toJson(stringList)
        GRDConnectManager.getSharedPrefsEditor()
            .putString(Constants.kGRDAppExceptionsPackageNames, jsonString)?.apply()
    }

    private fun getArrayListOfAppExceptions(): ArrayList<String> {
        val jsonString = GRDConnectManager.getSharedPrefs()
            .getString(Constants.kGRDAppExceptionsPackageNames, null)
        val type = object : TypeToken<ArrayList<String>>() {}.type
        val gson = Gson()
        return gson.fromJson(jsonString, type) ?: ArrayList()
    }

    fun excludeLANTraffic(shouldExclude: Boolean) {
        excludeLANTraffic = shouldExclude
        val localExcludeLANTraffic = excludeLANTraffic as Boolean
        GRDConnectManager.getSharedPrefsEditor()
            .putBoolean(Constants.kGRDExcludeLANTraffic, localExcludeLANTraffic)
            ?.apply()
    }

    suspend fun createAndStartTunnel() {
        // Check if the user had already granted the permission to set the VPN profile
        val intent = GoBackend.VpnService.prepare(context)
        when {
            // in case the permission was not yet granted emit the intent so that the
            // OS can be asked to present the modal alert
            intent != null -> grdVPNPermissionFlow.emit(intent)

            // Ensure that a tunnel name has been set
            tunnelName.isEmpty() -> grdErrorFlow.emit("Tunnel name should not be empty!")

            // Check if VPN credentials are already present in the GRDCredentialManager
            else -> grdCredentialManager?.getMainCredentials().let {
                if (it?.let { it1 -> activeConnectionPossible(it1) } == true) {
                    // If VPN credentials already exist try to start the VPN tunnel again
                    createTunnelWithExistingCredentials()
                } else {
                    // No VPN credentials exist yet
                    createTunnelFirstTime(validForDays)
                }
            }
        }
    }

    private suspend fun createTunnelWithExistingCredentials() {
        val hostname = grdCredentialManager?.getMainCredentials()?.hostname
        if (!hostname.isNullOrEmpty()) {
            Repository.instance.initRegionServer(hostname)
            val mainCredentials = grdCredentialManager?.getMainCredentials()
            val configString = mainCredentials?.let {
                GRDWireGuardConfiguration().getWireGuardConfigString(
                    it,
                    GRDConnectManager.getSharedPrefs()
                        .getString(GRD_CONNECT_USER_PREFERRED_DNS_SERVERS, null),
                    appExceptions,
                    excludeLANTraffic ?: true
                )
            }
            if (configString?.isNotEmpty() == true) {
                configStringFlow.emit(configString)
                createTunnel(configString)
                grdMsgFlow.emit("Create tunnel with existing credentials successful!")
            }
        } else {
            grdErrorFlow.emit("Hostname is empty!")
        }
    }

    private suspend fun createTunnelFirstTime(validForDays: Long) {
        val mainCredentials = true
        configureAndConnect(
            validForDays,
            mainCredentials,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val configString = any as String
                    Log.d(TAG, configString)
                    GRDConnectManager.getCoroutineScope().launch {
                        configStringFlow.emit(configString)
                        createTunnel(configString)
                        grdMsgFlow.emit("Create tunnel first time successful!")
                    }
                }

                override fun onError(error: String?) {
                    error?.let { it1 ->
                        GRDConnectManager.getCoroutineScope().launch {
                            grdErrorFlow.emit(it1)
                        }
                    }
                }
            }
        )
    }

    /**
     * Prepare to establish a VPN connection. This method returns null if the VPN application is
     * already prepared or if the user has previously consented to the VPN application.
     * Otherwise, it returns an Intent to a system activity.
     * The application should launch the activity using Activity.startActivityForResult to get itself prepared.
     * The activity may pop up a dialog to require user action, and the result will come back via its Activity.onActivityResult.
     * If the result is Activity.RESULT_OK, the application becomes prepared and is granted to use other methods in this class.
     * Only one application can be granted at the same time.
     * The right is revoked when another application is granted.
     * The application losing the right will be notified via its onRevoke.
     * Unless it becomes prepared again, subsequent calls to other methods in this class will fail
     * The user may disable the VPN at any time while it is activated, in which case this method
     * will return an intent the next time it is executed to obtain the user's consent again.
     */
    fun getIntentVpnPermissions(context: Context): Intent? = GoBackend.VpnService.prepare(context)

    suspend fun createTunnel(configString: String) {
        val inputString: Reader = StringReader(configString)
        val reader = BufferedReader(inputString)
        try {
            val config: Config = Config.parse(reader)
            if (tunnelName.isNotEmpty()) {
                GRDConnectManager.getTunnelManager().create(tunnelName, config)
                Log.d(TAG, "Creating tunnel...")
                if (GRDConnectManager.getBackend() is GoBackend) {
                    getServerStatus(object : IOnApiResponse {
                        override fun onSuccess(any: Any?) {
                            val serverStatusOK = any as Boolean
                            GRDConnectManager.getCoroutineScope().launch {
                                if (serverStatusOK) {
                                    grdStatusFlow.emit(GRDVPNHelperStatus.SERVER_READY.status)
                                    startTunnel()
                                } else {
                                    grdErrorFlow.emit(GRDVPNHelperStatus.SERVER_ERROR.status)
                                }
                            }
                        }

                        override fun onError(error: String?) {
                            GRDConnectManager.getCoroutineScope().launch {
                                error?.let { grdErrorFlow.emit("Server error! $it") }
                            }
                        }
                    })
                }
            } else {
                grdErrorFlow.emit("Tunnel name should not be empty!")
            }
        } catch (e: Exception) {
            e.message?.let { grdErrorFlow.emit("Error parsing config! $it") }
        }
    }

    suspend fun startTunnel() {
        val tunnel = GRDConnectManager.getTunnelManager().tunnelMap[tunnelName]
        try {
            grdStatusFlow.emit(GRDVPNHelperStatus.CONNECTING.status)
            tunnel?.setStateAsync(Tunnel.State.UP)
            grdStatusFlow.emit(GRDVPNHelperStatus.CONNECTED.status)
        } catch (e: Throwable) {
            val error = ErrorMessages[e]
            e.message?.let {
                grdErrorFlow.emit(GRDVPNHelperStatus.ERROR_CONNECTING.status)
            }
            val message = context?.getString(R.string.starting_error, error)
            Log.e(TAG, message, e)
        }
    }

    suspend fun stopTunnel() {
        try {
            grdStatusFlow.emit(GRDVPNHelperStatus.DISCONNECTING.status)
            getActiveTunnel()?.setStateAsync(Tunnel.State.DOWN)
            grdStatusFlow.emit(GRDVPNHelperStatus.DISCONNECTED.status)
        } catch (t: Throwable) {
            grdErrorFlow.emit("Error stopping tunnel! " + t.stackTraceToString())
        }
    }

    fun isTunnelRunning(): Boolean {
        val runningTunnel = GRDConnectManager.getTunnelManager().tunnelMap[tunnelName]
        return (runningTunnel != null && runningTunnel.state == Tunnel.State.UP)
    }

    fun getActiveTunnel(): TunnelModel? {
        return GRDConnectManager.getTunnelManager().tunnelMap[tunnelName]
    }

    suspend fun stopClearTunnel() {
        try {
            getActiveTunnel()?.setStateAsync(Tunnel.State.DOWN)
            clearVPNConfiguration()
            grdMsgFlow.emit("Tunnel successfully cleared!")
        } catch (e: Exception) {
            grdErrorFlow.emit("Error restarting tunnel! " + e.message)
        }
    }

    suspend fun restartTunnel() {
        try {
            getActiveTunnel()?.setStateAsync(Tunnel.State.DOWN)
            clearVPNConfiguration()
            createAndStartTunnel()
        } catch (e: Exception) {
            grdErrorFlow.emit("Error restarting tunnel! " + e.message)
        }
    }

    suspend fun updateTunnelRegion() {
        if (isTunnelRunning()) {
            restartTunnel()
        } else {
            clearVPNConfiguration()
        }
    }

    /*  Function that handles the various tasks required to establish a new VPN connection.
    Create new VPN credentials on the selected VPN node with the created Subscriber Credential
    Create a new WireGuard configuration with the VPN credentials from the VPN node
    Connect WireGuard to the VPN node */
    suspend fun configureAndConnect(
        validForDays: Long,
        mainCredentials: Boolean,
        iOnApiResponse: IOnApiResponse
    ) {
        var subscriberCredential: String? = null
        if (grdSubscriberCredential?.isExpired() == false) {
            subscriberCredential =
                grdSubscriberCredential?.retrieveSubscriberCredentialJWTFormat()
        }
        subscriberCredential?.let {
            initRegionAndConnectDevice(it, validForDays, mainCredentials, iOnApiResponse)
        } ?: run {
            createSubscriberCredential(object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val subscriberCredentialString = any as String
                    initRegionAndConnectDevice(
                        subscriberCredentialString,
                        validForDays,
                        mainCredentials,
                        iOnApiResponse
                    )
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                }
            })
        }
    }

    suspend fun createSubscriberCredential(
        iOnApiResponse: IOnApiResponse
    ) {
        val requestBody = mutableMapOf<String, Any>()
        val currentPEToken = GRDPEToken.currentPEToken()

        if (currentPEToken != null) {
            requestBody["validation-method"] = "pe-token"
            currentPEToken.token?.let { requestBody["pe-token"] = it }
        } else {
            requestBody["validation-method"] = "iap-android"
            val currentPurchase = BillingManager.getCurrentPurchase()
            if (currentPurchase != null) {
                currentPurchase.products.firstOrNull()?.let { requestBody["product-id"] = it }
                requestBody["purchase-token"] = currentPurchase.purchaseToken
                requestBody["bundle-id"] =
                    iapApplicationId ?: context?.packageName
                            ?: ""
                requestBody["product-type"] =
                    if (BillingManager.isSubscription(currentPurchase)) "subscription" else "consumable"
            } else {
                iOnApiResponse.onError("No valid purchase found")
                return
            }
        }

        Repository.instance.getSubscriberCredential(
            requestBody,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val subCredentialResponse = any as SubscriberCredentialResponse
                    subCredentialResponse.subscriberCredential?.let { scs ->
                        grdSubscriberCredential?.storeSubscriberCredentialJWTFormat(scs)
                        iOnApiResponse.onSuccess(scs)
                    } ?: run {
                        iOnApiResponse.onError("Missing subscriberCredential")
                        GRDConnectManager.getCoroutineScope().launch {
                            grdErrorFlow.emit("Missing subscriberCredential")
                        }
                    }
                }

                override fun onError(error: String?) {
                    error?.let { e ->
                        val errorMessage =
                            if (e.contains("Failed to query password equivalent token data or password equivalent token does not exist")) {
                                Constants.SUBSCRIBER_CREDENTIAL_FAIL_PET
                            } else {
                                e
                            }
                        iOnApiResponse.onError(errorMessage)
                        GRDConnectManager.getCoroutineScope().launch {
                            grdErrorFlow.emit(errorMessage)
                        }
                    }
                }
            })
    }

    fun initRegionAndConnectDevice(
        subscriberCredentialString: String,
        validForDays: Long,
        mainCredentials: Boolean,
        iOnApiResponse: IOnApiResponse
    ) {
        grdServerManager?.preferBetaCapableServers = preferBetaCapableServers
        grdServerManager?.vpnServerFeatureEnvironment = vpnServerFeatureEnvironment
        grdServerManager?.regionPrecision = regionPrecision

        grdServerManager?.selectServerFromRegion(
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val grdSgwServer = any as GRDSGWServer
                    grdSgwServer.hostname?.let {
                        if (mainCredentials) {
                            Repository.instance.initRegionServer(it)
                        }
                        connectVpnDevice(
                            subscriberCredentialString,
                            grdSgwServer,
                            iOnApiResponse,
                            validForDays,
                            mainCredentials
                        )
                    } ?: run {
                        iOnApiResponse.onError(GRDVPNHelperStatus.SERVER_ERROR.status)
                        GRDConnectManager.getCoroutineScope().launch {
                            grdErrorFlow.emit(GRDVPNHelperStatus.SERVER_ERROR.status)
                        }
                    }
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                    error?.let { Log.d(TAG, it) }
                }
            })
    }

    fun initRegion() {
        grdServerManager?.preferBetaCapableServers = preferBetaCapableServers
        grdServerManager?.vpnServerFeatureEnvironment = vpnServerFeatureEnvironment
        grdServerManager?.regionPrecision = regionPrecision
        GRDLogger.d(TAG, "initRegion()")

        grdServerManager?.selectServerFromRegion(
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val grdSgwServer = any as GRDSGWServer
                    GRDLogger.d(TAG, "initRegion() $grdSgwServer")

                    grdSgwServer.hostname?.let {
                        GRDLogger.d(TAG, "initRegion() hostname: $it")
                        Repository.instance.initRegionServer(it)
                    } ?: run {
                        GRDConnectManager.getCoroutineScope().launch {
                            grdErrorFlow.emit(GRDVPNHelperStatus.SERVER_ERROR.status)
                        }
                    }
                }

                override fun onError(error: String?) {
                    GRDLogger.d(TAG, "initRegion()")
                    error?.let { Log.d(TAG, it) }
                }
            })
    }

    fun connectVpnDevice(
        subscriberCredentialString: String,
        grdSgwServer: GRDSGWServer,
        iOnApiResponse: IOnApiResponse,
        validForDays: Long,
        mainCredentials: Boolean
    ) {
        val newVPNDevice = NewVPNDevice()
        newVPNDevice.transportProtocol = GRD_WIREGUARD
        newVPNDevice.subscriberCredential = subscriberCredentialString
        val keyPair = KeyPair()
        val keyPairGenerated = KeyPair(keyPair.privateKey)
        val publicKey = keyPairGenerated.publicKey.toBase64()
        newVPNDevice.publicKey = publicKey
        Repository.instance.createNewVPNDevice(newVPNDevice,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val newVPNDeviceResponse = any as NewVPNDeviceResponse
                    val grdCredential = GRDCredential()
                    grdCredential.createGRDCredential(
                        GRDTransportProtocol.GRDTransportProtocolType.GRD_TP_WIREGUARD,
                        validForDays,
                        mainCredentials,
                        newVPNDeviceResponse,
                        grdSgwServer,
                        keyPairGenerated
                    )
                    grdCredentialManager?.addOrUpdateCredential(grdCredential)
                    val grdWireGuardConfiguration = GRDWireGuardConfiguration()
                    val configString =
                        grdWireGuardConfiguration.getWireGuardConfigString(
                            grdCredential,
                            GRDConnectManager.getSharedPrefs()
                                ?.getString(GRD_CONNECT_USER_PREFERRED_DNS_SERVERS, null),
                            appExceptions,
                            excludeLANTraffic ?: true
                        )
                    iOnApiResponse.onSuccess(configString)
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                    error?.let {
                        GRDConnectManager.getCoroutineScope().launch {
                            grdErrorFlow.emit(it)
                        }
                    }
                }
            })
    }

    fun setPreferredDNSServer(dnsServerNumber: String) {
        GRDConnectManager.getSharedPrefsEditor()?.putString(
            GRD_CONNECT_USER_PREFERRED_DNS_SERVERS,
            dnsServerNumber
        )?.apply()
    }

    fun getPreferredDNSServers(): String? {
        return GRDConnectManager.getSharedPrefs()
            .getString(GRD_CONNECT_USER_PREFERRED_DNS_SERVERS, null)
    }

    /*  Clear local cache - removes all values from the Android Keystore and SharedPreferences */
    fun clearLocalCache() {
        grdCredentialManager?.getMainCredentials()
            ?.let { grdCredentialManager?.removeCredential(it) }
        GRDConnectManager.getSharedPrefsEditor().remove(GRD_SUBSCRIBER_CREDENTIAL)?.apply()
    }

    /* Handles VPN credential invalidation on the server and removal locally on the device. */
    suspend fun clearVPNConfiguration() {
        val grdCredentialObject = grdCredentialManager?.getMainCredentials()
        val subscriberCredentialsJSON =
            grdSubscriberCredential?.retrieveSubscriberCredentialJWTFormat()
        val deviceId = grdCredentialObject?.clientId
        if (deviceId != null) {
            val vpnCredential = VPNCredentials()
            vpnCredential.apiAuthToken = grdCredentialObject.apiAuthToken
            vpnCredential.subscriberCredential = subscriberCredentialsJSON
            grdCredentialManager?.deleteMainCredential()
            // TODO
            // this change should be complete and prevent the PET from being killed as well
            // whenever the reset config button is tapped in the sample app
            // but I am not entirely sure and we have to double check if any regressions
            // from this change may occur
            //GRDConnectManager.getSharedPrefs()?.edit()?.clear()?.apply()
            clearLocalCache()
            Repository.instance.invalidateVPNCredentials(
                deviceId,
                vpnCredential,
                object : IOnApiResponse {
                    override fun onSuccess(any: Any?) {
                        GRDConnectManager.getCoroutineScope().launch {
                            grdStatusFlow.emit(GRDVPNHelperStatus.VPN_CREDENTIALS_INVALIDATED.status)
                        }
                    }

                    override fun onError(error: String?) {
                        GRDConnectManager.getCoroutineScope().launch {
                            grdErrorFlow.emit(Constants.VPN_CREDENTIALS_INVALIDATION_ERROR)
                        }
                    }
                })
        } else {
            GRDConnectManager.getCoroutineScope().launch {
                grdErrorFlow.emit("Device id is null!")
            }
        }
    }

    /*  Checks whether the a valid GRDCredential object is present on the device and verifies
        properties of the object are not nil or an empty string. This function needs to be called
        in the high level API code paths prior to trying to re-establish a VPN connection to check
        whether all the information is present on device. */
    fun activeConnectionPossible(grdCredential: GRDCredential): Boolean {
        return !grdCredential.hostname.isNullOrEmpty() &&
                !grdCredential.apiAuthToken.isNullOrEmpty() &&
                !grdCredential.devicePublicKey.isNullOrEmpty() &&
                !grdCredential.devicePrivateKey.isNullOrEmpty() &&
                !grdCredential.clientId.isNullOrEmpty()
    }

    fun getServerStatus(onApiResponse: IOnApiResponse): Boolean {
        var boolean = false
        Repository.instance.getServerStatus(object : IOnApiResponse {
            override fun onSuccess(any: Any?) {
                val bool = any as Boolean
                boolean = bool
                onApiResponse.onSuccess(boolean)
                Log.d(TAG, "getServerStatus success")
            }

            override fun onError(error: String?) {
                onApiResponse.onError(error)
                boolean = false
                GRDConnectManager.getCoroutineScope().launch {
                    error?.let { grdErrorFlow.emit("getServerStatus error $it") }
                }

            }
        })
        return boolean
    }

    fun setVariables() {
        if (connectAPIHostname.isEmpty()) {
            connectAPIHostname = Constants.kGRDConnectAPIHostname
        }
        if (tunnelName.isEmpty()) {
            GRDConnectManager.getCoroutineScope().launch {
                grdErrorFlow.emit("Tunnel name is empty!")
            }
        }
        if (connectPublishableKey.isEmpty()) {
            GRDConnectManager.getCoroutineScope().launch {
                grdErrorFlow.emit("Connect public key is empty!")
            }
        }

        Repository.instance.connectPublishableKey = connectPublishableKey
        Repository.instance.initConnectAPIServer()
        Repository.instance.initConnectSubscriberServer(connectAPIHostname)
    }

    fun hasCredentials(): Boolean {
        val credentials = grdCredentialManager?.retrieveCredential()
        val haveCredentials = credentials?.let { activeConnectionPossible(it) } ?: false
        val havePEToken = !GRDPEToken.instance.retrievePEToken().isNullOrEmpty()

        return haveCredentials && havePEToken
    }

    private fun observeStatus() {
        GRDConnectManager.getCoroutineScope().launch {
            grdStatusFlow.collect {
                when (it) {
                    GRDVPNHelperStatus.CONNECTED.status -> {
                        handleOkHttpClient(GRDVPNHelperStatus.CONNECTED.status)
                    }

                    GRDVPNHelperStatus.DISCONNECTED.status -> {
                        handleOkHttpClient(GRDVPNHelperStatus.DISCONNECTED.status)
                    }

                    GRDVPNHelperStatus.ERROR_CONNECTING.status -> {
                        handleOkHttpClient(GRDVPNHelperStatus.ERROR_CONNECTING.status)
                    }
                }
            }
        }
    }

    private fun handleOkHttpClient(status: String) {
        if (Repository.instance.httpClient == Repository.instance.defaultHTTPClient()) {
            Repository.instance.httpClient = null
            Repository.instance.initConnectAPIServer()
            Repository.instance.initConnectSubscriberServer(connectAPIHostname)

            val hostname = grdCredentialManager?.getMainCredentials()?.hostname
            if (!hostname.isNullOrEmpty()) {
                Repository.instance.initRegionServer(hostname)
            }
        }
        Log.d(TAG, status)
        Log.d(
            TAG, "httpClient: ${Repository.instance.httpClient}, " +
                    "Default httpClient: ${Repository.instance.defaultHTTPClient()}, " +
                    "Host name: ${grdCredentialManager?.getMainCredentials()?.hostname}"
        )
    }

    enum class GRDVPNHelperStatus(val status: String) {
        UNKNOWN("VPN status: unknown."),
        MISSING_PET("PEToken is missing!"),
        ERROR_CONNECTING("Connecting error has occurred!"),
        DISCONNECTED("VPN status: disconnected!"),
        DISCONNECTING("VPN status: disconnecting..."),
        CONNECTING("VPN status: connecting..."),
        CONNECTED("VPN status: connected!"),
        MIGRATING("VPN status: migrating..."),
        VPN_CREDENTIALS_INVALIDATED("VPN status: credentials invalidated!"),
        SERVER_READY("Server status OK."),
        SERVER_ERROR("Server error!"),
        TUNNEL_CONNECTED("Connection Successful!")
    }

    val configStringFlow = MutableSharedFlow<String>()
    val grdMsgFlow = MutableSharedFlow<String>()
    val grdErrorFlow = MutableSharedFlow<String>()
    val grdVPNPermissionFlow = MutableSharedFlow<Intent>()
    val grdStatusFlow = MutableSharedFlow<String>()
}