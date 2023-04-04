package com.guardianconnect

import com.guardianconnect.util.Constants.Companion.BITMASK_STATE

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
        val bitmask = GRDConnectManager.getSharedPrefs()?.getInt(BITMASK_STATE, -1)
        return if (bitmask != -1) {
            val grdDeviceFilterConfigBlocklist = GRDDeviceFilterConfigBlocklist()
            grdDeviceFilterConfigBlocklist.bitwiseConfig = bitmask
            grdDeviceFilterConfigBlocklist
        } else {
            null
        }
    }

    fun apiPortableBlocklist(): HashMap<GRDDeviceFilterBlocklistOptions, String> {
        val map = HashMap<GRDDeviceFilterBlocklistOptions, String>()
        map.put(
            GRDDeviceFilterBlocklistOptions.BLOCK_NONE,
            GRDDeviceFilterBlocklistOptions.BLOCK_NONE.name
        )
        map.put(
            GRDDeviceFilterBlocklistOptions.BLOCK_ADS,
            GRDDeviceFilterBlocklistOptions.BLOCK_ADS.name
        )
        map.put(
            GRDDeviceFilterBlocklistOptions.BLOCK_PHISHING,
            GRDDeviceFilterBlocklistOptions.BLOCK_PHISHING.name
        )
        return map
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
}