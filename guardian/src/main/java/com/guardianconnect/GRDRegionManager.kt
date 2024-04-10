package com.guardianconnect

import com.google.gson.Gson
import com.guardianconnect.util.Constants.Companion.GRD_AUTOMATIC_REGION
import com.guardianconnect.util.Constants.Companion.GRD_PREFERRED_REGION

class GRDRegionManager {

    companion object {
        fun setPreferredRegion(region: GRDRegion?) {
            val editor = GRDConnectManager.getSharedPrefsEditor()
            if (region == null || region.name == GRD_AUTOMATIC_REGION) {
                editor.remove(GRD_PREFERRED_REGION)
            } else {
                val gson = Gson()
                val json = gson.toJson(region)
                editor.putString(GRD_PREFERRED_REGION, json)
            }
            editor.apply()
        }

        fun getPreferredRegion(): GRDRegion? {
            val sharedPreferences = GRDConnectManager.getSharedPrefs()
            val json = sharedPreferences.getString(GRD_PREFERRED_REGION, null)
            if (json != null) {
                val gson = Gson()
                return gson.fromJson(json, GRDRegion::class.java)
            }
            return null
        }
    }
}