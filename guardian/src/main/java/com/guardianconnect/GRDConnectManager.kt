package com.guardianconnect

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.guardianconnect.configStore.FileConfigStore
import com.guardianconnect.util.applicationScope
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import kotlinx.coroutines.*

class GRDConnectManager {

    private val futureBackend = CompletableDeferred<Backend>()
    private val coroutineScope = CoroutineScope(Job() + Dispatchers.Main.immediate)
    private var backend: Backend? = null
    private lateinit var preferencesDataStore: DataStore<Preferences>
    private lateinit var tunnelManager: TunnelManager
    private var sharedPreference: SharedPreferences? = null
    private var editor: SharedPreferences.Editor? = null
    private lateinit var guardianContext: Context

    fun init(context: Context) {
        instance = this
        guardianContext = context
        sharedPreference =
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        editor = sharedPreference?.edit()
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
        fun getSharedPrefs() = get().sharedPreference

        @JvmStatic
        fun getSharedPrefsEditor() = get().editor
    }

}