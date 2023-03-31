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
}