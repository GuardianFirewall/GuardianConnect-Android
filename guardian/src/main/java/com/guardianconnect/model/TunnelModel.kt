/*
 * Copyright © 2017-2021 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.guardianconnect.model

import android.util.Log
import com.guardianconnect.TunnelManager
import com.guardianconnect.util.applicationScope
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Encapsulates the volatile and nonvolatile state of a WireGuard tunnel.
 */
class TunnelModel internal constructor(
    private val manager: TunnelManager,
    private var name: String,
    config: Config?,
    state: Tunnel.State
) : Tunnel {

    override fun getName() = name

    var state = state
        private set

    override fun onStateChange(newState: Tunnel.State) {
        onStateChanged(newState)
        Log.d(TAG, "onStateChange, new state: $newState")
    }

    fun onStateChanged(state: Tunnel.State): Tunnel.State {
        this.state = state
        return state
    }

    suspend fun setStateAsync(state: Tunnel.State): Tunnel.State =
        withContext(Dispatchers.Main.immediate) {
            if (state != this@TunnelModel.state)
                manager.setTunnelState(this@TunnelModel, state)
            else
                this@TunnelModel.state
        }


    var config = config
        get() {
            if (field == null)
            // Opportunistically fetch this if we don't have a cached one, and rely on data bindings to update it eventually
                applicationScope.launch {
                    try {
                        manager.getTunnelConfig(this@TunnelModel)
                    } catch (e: Throwable) {
                        Log.e(TAG, Log.getStackTraceString(e))
                    }
                }
            return field
        }
        private set

    suspend fun getConfigAsync(): Config? = withContext(Dispatchers.Main.immediate) {
        config ?: manager.getTunnelConfig(this@TunnelModel)
    }

    fun onConfigChanged(config: Config?): Config? {
        this.config = config
        Log.d(TAG, "onConfigChanged, config: $config")
        return config
    }

    companion object {
        private const val TAG = "TunnelModel"
    }
}
