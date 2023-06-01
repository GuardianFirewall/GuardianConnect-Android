package com.guardianconnect

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.guardianconnect.util.Constants.Companion.GRD_PREFERRED_REGION_NAME_PRETTY
import com.guardianconnect.util.Constants.Companion.GRD_Preferred_Region

class GRDRegion {

    private val TAG = GRDRegion::class.java.simpleName

    @SerializedName("continent")
    var continent: String? = null

    @SerializedName("country-iso-code")
    var countryIsoCode: String? = null

    @SerializedName("name")
    var name: String? = null

    @SerializedName("name-pretty")
    var namePretty: String? = null


    /*  Permanently override the region that the device should connect to.
        The function should take the name property of the region object and store it into the
        SharedPreferences for the key GRD_Preferred_Region */
    fun setPreferredRegion(grdRegion: GRDRegion) {
        val grdRegionName = grdRegion.name
        val grdRegionNamePretty = grdRegion.namePretty
        GRDConnectManager.getSharedPrefs()?.edit()
            ?.putString(GRD_Preferred_Region, grdRegionName)?.apply()
        GRDConnectManager.getSharedPrefs()?.edit()
            ?.putString(GRD_PREFERRED_REGION_NAME_PRETTY, grdRegionNamePretty)?.apply()
    }

    /*  Function that retrieves the currently stored object region name in the SharedPreferences */
    fun getPreferredRegion(): String? {
        val preferredRegionName =
            GRDConnectManager.getSharedPrefs()?.getString(GRD_Preferred_Region, null)
        return if (!preferredRegionName.isNullOrEmpty()) {
            preferredRegionName
        } else {
            null
        }
    }

    /*  Function that retrieves the currently stored object region name in the SharedPreferences */
    fun getPreferredRegionNamePretty(): String? {
        val preferredRegionName =
            GRDConnectManager.getSharedPrefs()?.getString(GRD_PREFERRED_REGION_NAME_PRETTY, null)
        return if (!preferredRegionName.isNullOrEmpty()) {
            preferredRegionName
        } else {
            null
        }
    }

    /*  Function to reset the user preferred region. */
    fun clearPreferredRegion() {
        GRDConnectManager.getSharedPrefsEditor()?.remove(GRD_Preferred_Region)?.apply()
        GRDConnectManager.getSharedPrefsEditor()?.remove(GRD_PREFERRED_REGION_NAME_PRETTY)?.apply()
        Log.d(TAG, "Preferred Region cleared!")
    }
}