/*
 * Copyright © 2017-2021 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.guardianconnect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.databinding.BaseObservable
import com.guardianconnect.model.TunnelModel
import com.guardianconnect.util.UserKnobs
import com.guardianconnect.util.applicationScope
import com.wireguard.android.backend.Tunnel
import com.guardianconnect.configStore.ConfigStore
import com.wireguard.config.Config
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Maintains and mediates changes to the set of available tunnels
 */
class TunnelManager(
    private val configStore: ConfigStore,
) : BaseObservable() {
    private val tunnels = CompletableDeferred<HashMap<String, TunnelModel>>()
    val tunnelMap: HashMap<String, TunnelModel> = HashMap()
    private var haveLoaded = false

    fun addToList(name: String, config: Config?, state: Tunnel.State): TunnelModel {
        val tunnel = TunnelModel(this, name, config, state)
        tunnelMap[name] = tunnel
        return tunnel
    }

    suspend fun getTunnels(): HashMap<String, TunnelModel> = tunnels.await()

    suspend fun create(name: String, config: Config?): TunnelModel =
        withContext(Dispatchers.Main.immediate) {
            addToList(
                name,
                withContext(Dispatchers.IO) { config?.let { configStore.create(name, it) } },
                Tunnel.State.DOWN
            )
        }

    private var lastUsedTunnel: TunnelModel? = null
        private set(value) {
            if (value == field) return
            field = value
            applicationScope.launch { UserKnobs.setLastUsedTunnel(value?.name) }
        }

    suspend fun getTunnelConfig(tunnel: TunnelModel): Config? =
        withContext(Dispatchers.Main.immediate) {
            tunnel.onConfigChanged(withContext(Dispatchers.IO) { configStore.load(tunnel.name) })
        }

    fun onCreate() {
        applicationScope.launch {
            try {
                onTunnelsLoaded()
            } catch (e: Throwable) {
                GRDVPNHelper.grdErrorFlow.emit(e.stackTraceToString())
            }
        }
    }

    fun onTunnelsLoaded() {
        applicationScope.launch {
            val lastUsedName = UserKnobs.lastUsedTunnel.first()
            if (lastUsedName != null)
                lastUsedTunnel = tunnelMap[lastUsedName]
            haveLoaded = true
            restoreState(true)
            tunnels.complete(tunnelMap)
        }
    }

    fun refreshTunnelStates() {
        applicationScope.launch {
            try {
                val running =
                    withContext(Dispatchers.IO) { GRDConnectManager.getBackend().runningTunnelNames }
                for (tunnel in tunnelMap)
                    tunnel.value.onStateChanged(if (running.contains(tunnel.key)) Tunnel.State.UP else Tunnel.State.DOWN)
            } catch (e: Throwable) {
                GRDVPNHelper.grdErrorFlow.emit(e.stackTraceToString())
            }
        }
    }

    suspend fun restoreState(force: Boolean) {
        if (!haveLoaded || (!force && !UserKnobs.restoreOnBoot.first()))
            return
        val previouslyRunning = UserKnobs.runningTunnels.first()
        if (previouslyRunning.isEmpty()) return
        withContext(Dispatchers.IO) {
            try {
                tunnelMap.filter { previouslyRunning.contains(it.key) }.map {
                    async(Dispatchers.IO + SupervisorJob()) {
                        setTunnelState(
                            it.value,
                            Tunnel.State.UP
                        )
                    }
                }.awaitAll()
            } catch (e: Throwable) {
                GRDVPNHelper.grdErrorFlow.emit(e.stackTraceToString())
            }
        }
    }

    private suspend fun saveState() {
        UserKnobs.setRunningTunnels(tunnelMap.filter { it.value.state == Tunnel.State.UP }
            .map { it.key }.toSet())
    }

    suspend fun setTunnelState(tunnel: TunnelModel, state: Tunnel.State): Tunnel.State =
        withContext(Dispatchers.Main.immediate) {
            var newState = tunnel.state
            var throwable: Throwable? = null
            try {
                newState = withContext(Dispatchers.IO) {
                    GRDConnectManager.getBackend().setState(
                        tunnel,
                        state,
                        tunnel.getConfigAsync()
                    )
                }
                if (newState == Tunnel.State.UP)
                    lastUsedTunnel = tunnel
            } catch (e: Throwable) {
                throwable = e
            }
            tunnel.onStateChanged(newState)
            grdTunnelStatusFlow.emit(newState)
            saveState()
            if (throwable != null)
                throw throwable
            newState
        }

    class IntentReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            applicationScope.launch {
                val manager = GRDConnectManager.getTunnelManager()
                if (intent == null) return@launch
                val action = intent.action ?: return@launch
                if ("com.wireguard.android.action.REFRESH_TUNNEL_STATES" == action) {
                    manager.refreshTunnelStates()
                    return@launch
                }
                val state: Tunnel.State = when (action) {
                    "com.wireguard.android.action.SET_TUNNEL_UP" -> Tunnel.State.UP
                    "com.wireguard.android.action.SET_TUNNEL_DOWN" -> Tunnel.State.DOWN
                    else -> return@launch
                }
                val tunnelName = intent.getStringExtra("tunnel") ?: return@launch
                val tunnels = manager.getTunnels()
                val tunnel = tunnels[tunnelName] ?: return@launch
                try {
                    manager.setTunnelState(tunnel, state)
                } catch (e: Throwable) {
                    GRDVPNHelper.grdErrorFlow.emit(e.stackTraceToString())
                }
            }
        }
    }

    companion object {
        private const val TAG = "TunnelManager"
    }

    val grdTunnelStatusFlow = MutableSharedFlow<Tunnel.State>()
}

