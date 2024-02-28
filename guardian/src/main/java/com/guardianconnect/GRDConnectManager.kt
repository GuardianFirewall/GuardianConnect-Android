package com.guardianconnect

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.guardianconnect.configStore.FileConfigStore
import com.guardianconnect.util.applicationScope
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch

class GRDConnectManager {

    private val futureBackend = CompletableDeferred<Backend>()
    private val coroutineScope = CoroutineScope(Job() + Dispatchers.Main.immediate)
    private var backend: Backend? = null
    private lateinit var preferencesDataStore: DataStore<Preferences>
    private lateinit var tunnelManager: TunnelManager
    private lateinit var sharedPreference: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var guardianContext: Context
    private lateinit var connectivityManager: ConnectivityManager
    private var tunnelState: Tunnel.State? = null
    private var tunnelStateLabel: String? = null
    private val sharedPrefsInitializationLatch = CountDownLatch(1)

    fun init(context: Context) {
        instance = this
        guardianContext = context
        if (!::sharedPreference.isInitialized) {
            coroutineScope.launch(Dispatchers.IO) {
                sharedPreference = withContext(Dispatchers.Default) {
                    androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
                }
                editor = withContext(Dispatchers.Default) {
                    sharedPreference.edit()
                }
                sharedPrefsInitializationLatch.countDown()
            }
        }
        preferencesDataStore =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }
        tunnelManager = TunnelManager(FileConfigStore(context))
        tunnelManager.onCreate()
        coroutineScope.launch(Dispatchers.IO) {
            try {
                backend = GoBackend(guardianContext)
                GoBackend.setAlwaysOnCallback {
                    get().applicationScope.launch {
                        get().tunnelManager.restoreState(
                            true
                        )
                    }
                }
                backend?.let { futureBackend.complete(it) }
            } catch (e: Throwable) {
                GRDVPNHelper.grdErrorFlow.emit(Log.getStackTraceString(e))
            }

            collectFlow(tunnelManager)
        }

        connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        connectivityManager.registerDefaultNetworkCallback(
            createNetworkListener
        )
    }

    private val createNetworkListener = object : NetworkCallback() {

        override fun onAvailable(network: Network) {
            Log.d(TAG, "onAvailable")

            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            val hasVPNTransport =
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            val hasNetCapability =
                networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)

            if (hasVPNTransport == true && hasNetCapability == false) {
                val intent = Intent("com.guardianconnect.ACTION_SEND")
                intent.action = tunnelStateLabel
                intent.putExtra("tunnel", GRDVPNHelper.tunnelName)
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                guardianContext.sendBroadcast(intent)
            } else {
                val intent = Intent("com.guardianconnect.ACTION_SEND")
                intent.action = tunnelStateLabel
                intent.putExtra("tunnel", GRDVPNHelper.tunnelName)
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                guardianContext.sendBroadcast(intent)
            }
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "onLost")
        }
    }

    private suspend fun collectFlow(tunnelManager: TunnelManager) {
        tunnelManager.grdTunnelStatusFlow.collect {
            Log.d(TAG, "TUNNEL STATE: $it")
            tunnelState = it
            tunnelStateLabel = when (it) {
                Tunnel.State.UP -> {
                    "com.guardianconnect.action.GRD_SET_TUNNEL_UP"
                }

                Tunnel.State.DOWN -> {
                    "com.guardianconnect.action.GRD_SET_TUNNEL_DOWN"
                }

                Tunnel.State.TOGGLE -> {
                    "com.guardianconnect.action.GRD_REFRESH_TUNNEL_STATES"
                }
            }
        }
    }

    fun getContext(): Context {
        return guardianContext
    }

    companion object {
        private const val TAG = "GRDConnectManager"
        private lateinit var instance: GRDConnectManager

        @JvmStatic
        fun get(): GRDConnectManager {
            return instance
        }

        @JvmStatic
        suspend fun getBackend() = get().futureBackend.await()

        @JvmStatic
        fun getPreferencesDataStore() = get().preferencesDataStore

        @JvmStatic
        fun getTunnelManager() = get().tunnelManager

        @JvmStatic
        fun getCoroutineScope() = get().coroutineScope

        @JvmStatic
        fun getSharedPrefs(): SharedPreferences {
            get().sharedPrefsInitializationLatch.await()
            return get().sharedPreference
        }

        @JvmStatic
        fun getSharedPrefsEditor(): SharedPreferences.Editor {
            get().sharedPrefsInitializationLatch.await()
            return get().editor
        }
    }
}