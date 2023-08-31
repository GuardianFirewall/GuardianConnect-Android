package com.guardianconnect

import com.guardianconnect.api.IOnApiResponse
import com.guardianconnect.api.Repository
import com.guardianconnect.util.Constants.Companion.GRD_BLOCKLIST_BITMASK_STATE
import com.guardianconnect.util.Constants.Companion.GRD_BLOCKLIST_BLOCK_ADS
import com.guardianconnect.util.Constants.Companion.GRD_BLOCKLIST_BLOCK_NONE
import com.guardianconnect.util.Constants.Companion.GRD_BLOCKLIST_BLOCK_PHISHING
import kotlinx.coroutines.launch

/*  Manage and sync the device's custom blocklist state */

class GRDDeviceFilterConfigBlocklist {

    var bitwiseConfig: Int? = null

    enum class DeviceFilterConfigBlocklist(val bitmask: Int) {
        DeviceFilterConfigBlocklistDisableFirewall(1 shl 0),
        DeviceFilterConfigBlocklistBlockAds(1 shl 1),
        DeviceFilterConfigBlocklistBlockPhishing(1 shl 2),
        DeviceFilterConfigBlocklistMax(1 shl 3)
    }

    fun setConfig(config: DeviceFilterConfigBlocklist, enabled: Boolean) {
        if (enabled)
            addConfig(config)
        else
            removeConfig(config)
    }

    fun currentBlocklistConfig(): GRDDeviceFilterConfigBlocklist? {
        val bitmask = GRDConnectManager.getSharedPrefs()?.getInt(GRD_BLOCKLIST_BITMASK_STATE, -1)
        return if (bitmask != -1) {
            val grdDeviceFilterConfigBlocklist = GRDDeviceFilterConfigBlocklist()
            grdDeviceFilterConfigBlocklist.bitwiseConfig = bitmask
            grdDeviceFilterConfigBlocklist
        } else {
            null
        }
    }

    fun apiPortableBlocklist(): HashMap<String, Boolean> {
        val map = HashMap<String, Boolean>()
        apiKeyForDeviceFilterConfigBlocklist(DeviceFilterConfigBlocklist.DeviceFilterConfigBlocklistDisableFirewall)?.let {
            map.put(
                it,
                hasConfig(DeviceFilterConfigBlocklist.DeviceFilterConfigBlocklistDisableFirewall)
            )
        }
        apiKeyForDeviceFilterConfigBlocklist(DeviceFilterConfigBlocklist.DeviceFilterConfigBlocklistBlockAds)?.let {
            map.put(
                it,
                hasConfig(DeviceFilterConfigBlocklist.DeviceFilterConfigBlocklistBlockAds)
            )
        }
        apiKeyForDeviceFilterConfigBlocklist(DeviceFilterConfigBlocklist.DeviceFilterConfigBlocklistBlockPhishing)?.let {
            map.put(
                it,
                hasConfig(DeviceFilterConfigBlocklist.DeviceFilterConfigBlocklistBlockPhishing)
            )
        }
        return map
    }

    fun apiKeyForDeviceFilterConfigBlocklist(config: DeviceFilterConfigBlocklist): String? {
        if (config == DeviceFilterConfigBlocklist.DeviceFilterConfigBlocklistDisableFirewall) {
            return GRD_BLOCKLIST_BLOCK_NONE
        }
        if (config == DeviceFilterConfigBlocklist.DeviceFilterConfigBlocklistBlockAds) {
            return GRD_BLOCKLIST_BLOCK_ADS
        }
        if (config == DeviceFilterConfigBlocklist.DeviceFilterConfigBlocklistBlockPhishing) {
            return GRD_BLOCKLIST_BLOCK_PHISHING
        }
        return null
    }

    fun hasConfig(config: DeviceFilterConfigBlocklist): Boolean {
        return bitwiseConfig?.and(config.bitmask) != 0
    }

    fun addConfig(config: DeviceFilterConfigBlocklist) {
        bitwiseConfig = bitwiseConfig?.or(config.bitmask)
    }

    fun removeConfig(config: DeviceFilterConfigBlocklist) {
        bitwiseConfig = bitwiseConfig?.and(config.bitmask.inv())
    }

    fun blocklistEnabled(): Boolean {
        val apiPortableMap = apiPortableBlocklist()
        return apiPortableMap.containsValue(true)
    }

    fun titleForDeviceFilterConfigBlocklist(config: DeviceFilterConfigBlocklist): String {
        when (config) {
            DeviceFilterConfigBlocklist.DeviceFilterConfigBlocklistDisableFirewall -> {
                return "Bypass Firewall Rules"

            }
            DeviceFilterConfigBlocklist.DeviceFilterConfigBlocklistBlockAds -> {
                return "Block Ads"

            }
            DeviceFilterConfigBlocklist.DeviceFilterConfigBlocklistBlockPhishing -> {
                return "Block Phishing"
            }
            else -> {
                var names = ""
                for (blockList in DeviceFilterConfigBlocklist.values()) {
                    names += " | ${titleForDeviceFilterConfigBlocklist(blockList)}"
                }
                return names
            }
        }
    }

    fun syncBlocklist() {
        val grdCredentialManager = GRDCredentialManager()
        val mainCredentials = grdCredentialManager.getMainCredentials()
        mainCredentials?.clientId?.let { clientId ->
            mainCredentials.apiAuthToken?.let { authToken ->
                Repository.instance.setDeviceFilterConfig(
                    clientId,
                    authToken,
                    object : IOnApiResponse {
                        override fun onSuccess(any: Any?) {
                            GRDConnectManager.getCoroutineScope().launch {
                                GRDVPNHelper.grdMsgFlow.emit(any.toString())
                            }
                        }

                        override fun onError(error: String?) {
                            error?.let {
                                GRDConnectManager.getCoroutineScope().launch {
                                    GRDVPNHelper.grdErrorFlow.emit(error)
                                }
                            }
                        }
                    })
            }
        }
    }
}