package com.guardianconnect

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import com.guardianconnect.api.IOnApiResponse
import com.guardianconnect.api.Repository
import com.guardianconnect.model.EValidationMethod
import com.guardianconnect.model.TunnelModel
import com.guardianconnect.model.api.*
import com.guardianconnect.util.Constants.Companion.GRD_CONFIG_STRING
import com.guardianconnect.util.Constants.Companion.GRD_CREDENTIAL_LIST
import com.guardianconnect.util.Constants.Companion.GRD_PE_TOKEN
import com.guardianconnect.util.Constants.Companion.GRD_SUBSCRIBER_CREDENTIAL
import com.guardianconnect.util.Constants.Companion.GRD_WIREGUARD
import com.guardianconnect.util.ErrorMessages
import com.guardianconnect.util.GRDKeystore
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
    private val grdSubscriberCredential = GRDSubscriberCredential()
    private var grdCredentialManager = GRDCredentialManager()
    var grdServerManager = GRDServerManager()
    private var context: Context? = null
    var connectAPIHostname: String = ""
    var connectPublicKey: String = ""
    var tunnelName: String = ""
    var validForDays: Long = 60

    fun initHelper(context: Context) {
        this.context = context
    }

    fun createAndStartTunnel() {
        GRDConnectManager.getCoroutineScope().launch {
            if (tunnelName.isNotEmpty()) {
                grdCredentialManager.retrieveCredential().let {
                    if (it?.let { it1 -> activeConnectionPossible(it1) } == true) {
                        createTunnelWithExistingCredentials()
                    } else {
                        val decryptedPEToken =
                            GRDKeystore.instance.retrieveFromKeyStore(GRD_PE_TOKEN)
                        if (!decryptedPEToken.isNullOrEmpty()) {
                            createTunnelFirstTime(
                                decryptedPEToken,
                                validForDays
                            )
                        } else {
                            grdErrorFlow.emit("PEToken should not be empty!")
                        }
                    }
                }
            } else {
                grdErrorFlow.emit("Tunnel name should not be empty!")
            }
        }
    }

    private suspend fun createTunnelWithExistingCredentials() {
        val grdCredentialManager = GRDCredentialManager()
        val hostname = grdCredentialManager.getMainCredentials()?.hostname
        if (!hostname.isNullOrEmpty()) {
            Repository.instance.initRegionServer(hostname)
            val configString = GRDKeystore.instance.retrieveFromKeyStore(GRD_CONFIG_STRING)
            if (configString?.isNotEmpty() == true) {
                configStringFlow.emit(configString)
                createTunnel(configString)
                grdMsgFlow.emit("Create tunnel with existing credentials successful!")
            }
        } else {
            grdErrorFlow.emit("Hostname is empty!")
        }
    }

    private suspend fun createTunnelFirstTime(peToken: String, validForDays: Long) {
        val mainCredentials = true
        configureAndConnect(
            peToken,
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
    fun getIntentVpnPermissions(context: Context) = GoBackend.VpnService.prepare(context)

    /**
     * This method will emit Intent to a system activity to grdVPNPermissionFlow if user consent is needed,
     * or start tunnel if the VPN application is
     * already prepared or if user has previously consented to the VPN application.
     */
    fun prepareVPNPermissions() {
        GRDConnectManager.getCoroutineScope().launch {
            val intent = GoBackend.VpnService.prepare(context)
            if (intent != null) {
                grdVPNPermissionFlow.emit(intent)
            } else {
                startTunnel()
            }
        }
    }

    suspend fun createTunnel(configString: String) {
        val inputString: Reader = StringReader(configString)
        val reader = BufferedReader(inputString)
        try {
            val config: Config = Config.parse(reader)
            GRDConnectManager.getCoroutineScope().launch {
                if (tunnelName.isNotEmpty()) {
                    GRDConnectManager.getTunnelManager().create(tunnelName, config)
                    Log.d(TAG, "Creating tunnel...")
                    if (GRDConnectManager.getBackend() is GoBackend) {
                        getServerStatus(object : IOnApiResponse {
                            override fun onSuccess(any: Any?) {
                                val serverStatusOK = any as Boolean
                                if (serverStatusOK) {
                                    GRDConnectManager.getCoroutineScope().launch {
                                        grdMsgFlow.emit(GRDState.SERVER_READY.name)
                                    }
                                } else {
                                    GRDConnectManager.getCoroutineScope().launch {
                                        grdErrorFlow.emit(GRDState.SERVER_ERROR.name)
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
            }
        } catch (e: Exception) {
            e.message?.let { grdErrorFlow.emit("Error parsing config! $it") }
        }
    }

    fun startTunnel() {
        val tunnel = GRDConnectManager.getTunnelManager().tunnelMap[tunnelName]
        GRDConnectManager.getCoroutineScope().launch {
            grdMsgFlow.emit(GRDVPNHelperStatus.CONNECTING.name)
            try {
                tunnel?.setStateAsync(Tunnel.State.UP)
            } catch (e: Throwable) {
                val error = ErrorMessages[e]
                e.message?.let {
                    grdErrorFlow.emit(GRDVPNHelperStatus.ERROR_CONNECTING.name)
                }
                val message = context?.getString(R.string.starting_error, error)
                Log.e(TAG, message, e)
                return@launch
            }
            grdMsgFlow.emit(GRDVPNHelperStatus.CONNECTED.name)
        }
    }

    fun stopTunnel() {
        try {
            GRDConnectManager.getCoroutineScope().launch {
                grdMsgFlow.emit(GRDVPNHelperStatus.DISCONNECTING.name)
                getActiveTunnel()?.setStateAsync(Tunnel.State.DOWN)
                grdMsgFlow.emit(GRDVPNHelperStatus.DISCONNECTED.name)
            }
        } catch (t: Throwable) {
            GRDConnectManager.getCoroutineScope().launch {
                grdErrorFlow.emit("Error stopping tunnel! " + t.stackTraceToString())
            }
        }
    }

    fun isTunnelRunning(): Boolean {
        val runningTunnel = GRDConnectManager.getTunnelManager().tunnelMap[tunnelName]
        return runningTunnel != null
    }

    fun getActiveTunnel(): TunnelModel? {
        return GRDConnectManager.getTunnelManager().tunnelMap[tunnelName]
    }

    fun restartTunnel() {
        try {
            stopTunnel()
            clearVPNConfiguration()
            GRDConnectManager.getCoroutineScope().launch {
                grdMsgFlow.emit("Tunnel successfully restarted!")
            }
        } catch (e: Exception) {
            GRDConnectManager.getCoroutineScope().launch {
                grdErrorFlow.emit("Error restarting tunnel! " + e.message)
            }
        }
    }


/*  Function that handles the various tasks required to establish a new VPN connection.
    Create new VPN credentials on the selected VPN node with the created Subscriber Credential
    Create a new WireGuard configuration with the VPN credentials from the VPN node
    Connect WireGuard to the VPN node */

    fun configureAndConnect(
        peToken: String,
        validForDays: Long,
        mainCredentials: Boolean,
        iOnApiResponse: IOnApiResponse
    ) {
        val validationMethodPEToken = ValidationMethodPEToken()
        validationMethodPEToken.validationMethod = EValidationMethod.PE_TOKEN.method
        validationMethodPEToken.peToken = peToken
        Repository.instance.getSubscriberCredentialsPET(
            validationMethodPEToken,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val subCredentialResponse = any as SubscriberCredentialResponse
                    subCredentialResponse.subscriberCredential?.let { scs ->
                        grdSubscriberCredential.storeSubscriberCredentialJWTFormat(scs)
                        grdServerManager.selectServerFromRegion(
                            object : IOnApiResponse {
                                override fun onSuccess(any: Any?) {
                                    val server = any as Server
                                    server.hostname()?.let {
                                        Repository.instance.initRegionServer(it)
                                        connectVpnDevice(
                                            scs,
                                            server,
                                            iOnApiResponse,
                                            validForDays,
                                            mainCredentials
                                        )
                                    } ?: run {
                                        iOnApiResponse.onError("No Server")
                                        GRDConnectManager.getCoroutineScope().launch {
                                            grdErrorFlow.emit("No Server")
                                        }
                                    }
                                }

                                override fun onError(error: String?) {
                                    iOnApiResponse.onError(error)
                                    error?.let { Log.d(TAG, it) }
                                }
                            })
                    } ?: run {
                        iOnApiResponse.onError("Missing subscriberCredential")
                        GRDConnectManager.getCoroutineScope().launch {
                            grdErrorFlow.emit("Missing subscriberCredential")
                        }
                    }
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

    fun connectVpnDevice(
        subscriberCredentialString: String,
        server: Server,
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
                        server,
                        keyPairGenerated
                    )
                    grdCredentialManager.addOrUpdateCredential(grdCredential)
                    val grdWireGuardConfiguration = GRDWireGuardConfiguration()
                    val configString =
                        grdWireGuardConfiguration.getWireGuardConfigString(
                            grdCredential,
                            null
                        )
                    GRDKeystore.instance.saveToKeyStore(GRD_CONFIG_STRING, configString)
                    GRDConnectManager.getCoroutineScope().launch {
                        grdMsgFlow.emit("VPN device connected!")
                    }
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

    /*  Clear local cache - removes all values from the Android Keystore and SharedPreferences */
    fun clearLocalCache() {
        GRDConnectManager.getSharedPrefsEditor()?.remove(GRD_CREDENTIAL_LIST)?.apply()
        GRDConnectManager.getSharedPrefsEditor()?.remove(GRD_SUBSCRIBER_CREDENTIAL)?.apply()
    }

    /* Handles VPN credential invalidation on the server and removal locally on the device. */
    fun clearVPNConfiguration() {
        val grdCredentialObject = grdCredentialManager.retrieveCredential()
        val subscriberCredentialsJSON =
            grdSubscriberCredential.retrieveSubscriberCredentialJWTFormat()
        val deviceId = grdCredentialObject?.clientId
        if (deviceId != null) {
            val vpnCredential = VPNCredentials()
            vpnCredential.apiAuthToken = grdCredentialObject.apiAuthToken
            vpnCredential.subscriberCredential = subscriberCredentialsJSON
            grdCredentialManager.credentialsArrayList.clear()
            GRDConnectManager.getSharedPrefs()?.edit()?.clear()?.apply()
            Repository.instance.invalidateVPNCredentials(
                deviceId,
                vpnCredential,
                object : IOnApiResponse {
                    override fun onSuccess(any: Any?) {
                        GRDConnectManager.getCoroutineScope().launch {
                            grdMsgFlow.emit("Credentials invalidated!")
                        }
                    }

                    override fun onError(error: String?) {
                        error?.let {
                            GRDConnectManager.getCoroutineScope().launch {
                                grdErrorFlow.emit("Credentials NOT invalidated, something went wrong! $it")
                            }
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
            connectAPIHostname = "connect-api.guardianapp.com"
        }
        if (tunnelName.isEmpty()) {
            GRDConnectManager.getCoroutineScope().launch {
                grdErrorFlow.emit("Tunnel name is empty!")
            }
        }
        if (connectPublicKey.isEmpty()) {
            GRDConnectManager.getCoroutineScope().launch {
                grdErrorFlow.emit("Connect public key is empty!")
            }
        }
        Repository.instance.initMainServer(connectAPIHostname)
        Repository.instance.initConnectSubscriberServer()
    }

    fun hasCredentials(): Boolean {
        val credentials = grdCredentialManager.retrieveCredential()
        val haveCredentials = credentials?.let { activeConnectionPossible(it) } ?: false
        val havePEToken = !GRDKeystore.instance.retrieveFromKeyStore(GRD_PE_TOKEN).isNullOrEmpty()

        return haveCredentials && havePEToken
    }

    // TODO: start using these statuses in GRDVPNHelper functions
    enum class GRDVPNHelperStatus(status: String) {
        UNKNOWN("VPN status: unknown."),
        MISSING_PET("PEToken is missing!"),
        ERROR_CONNECTING("Connecting error has occurred!"),
        DISCONNECTED("VPN status: disconnected!"),
        DISCONNECTING("VPN status: disconnecting..."),
        CONNECTING("VPN status: connecting..."),
        CONNECTED("VPN status: connected!"),
        MIGRATING("VPN status: migrating...")
    }

    val configStringFlow = MutableSharedFlow<String>()
    val grdMsgFlow = MutableSharedFlow<String>()
    val grdErrorFlow = MutableSharedFlow<String>()
    val grdVPNPermissionFlow = MutableSharedFlow<Intent>()
}