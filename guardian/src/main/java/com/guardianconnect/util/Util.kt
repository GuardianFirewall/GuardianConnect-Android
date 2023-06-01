/*
 * Copyright © 2017-2021 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.guardianconnect.util

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.guardianconnect.GRDConnectManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Any.applicationScope: CoroutineScope
    get() = GRDConnectManager.getCoroutineScope()

object UserKnobs {

    private val RESTORE_ON_BOOT = booleanPreferencesKey("restore_on_boot")
    val restoreOnBoot: Flow<Boolean>
        get() = GRDConnectManager.getPreferencesDataStore().data.map {
            it[RESTORE_ON_BOOT] ?: false
        }

    private val LAST_USED_TUNNEL = stringPreferencesKey("last_used_tunnel")
    val lastUsedTunnel: Flow<String?>
        get() = GRDConnectManager.getPreferencesDataStore().data.map {
            it[LAST_USED_TUNNEL]
        }

    suspend fun setLastUsedTunnel(lastUsedTunnel: String?) {
        GRDConnectManager.getPreferencesDataStore().edit {
            if (lastUsedTunnel == null)
                it.remove(LAST_USED_TUNNEL)
            else
                it[LAST_USED_TUNNEL] = lastUsedTunnel
        }
    }

    private val RUNNING_TUNNELS = stringSetPreferencesKey("enabled_configs")
    val runningTunnels: Flow<Set<String>>
        get() = GRDConnectManager.getPreferencesDataStore().data.map {
            it[RUNNING_TUNNELS] ?: emptySet()
        }

    suspend fun setRunningTunnels(runningTunnels: Set<String>) {
        GRDConnectManager.getPreferencesDataStore().edit {
            if (runningTunnels.isEmpty())
                it.remove(RUNNING_TUNNELS)
            else
                it[RUNNING_TUNNELS] = runningTunnels
        }
    }
}
