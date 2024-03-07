package com.guardianconnect

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.guardianconnect.util.Constants.Companion.GRD_AUTOMATIC_REGION
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

    @SerializedName("cities")
    var cities: List<GRDRegion>? = null

    @SerializedName("region-precision")
    var regionPrecision: String? = null

    @SerializedName("longitude")
    var longitude: Double? = null

    @SerializedName("latitude")
    var latitude: Double? = null

    @SerializedName("country")
    var country: String? = null

    companion object {
        fun automaticRegion(): GRDRegion {
            val auto = GRDRegion()
            auto.continent = GRD_AUTOMATIC_REGION
            auto.name = GRD_AUTOMATIC_REGION
            auto.namePretty = GRD_AUTOMATIC_REGION
            return auto
        }
    }

    /*  Permanently override the region that the device should connect to.
        The function should take the name property of the region object and store it into the
        SharedPreferences for the key GRD_Preferred_Region */
    fun setPreferredRegion(grdRegion: GRDRegion) {
        //
        // Note from CJ 2023-11-06
        // If we're passing the automatic region nothing should be stored
        // and the preferred region object in the shared preferences
        // should be removed entirely instead
        if (grdRegion.name == GRD_AUTOMATIC_REGION) {
            grdRegion.clearPreferredRegion()
            return
        }

        val grdRegionName = grdRegion.name
        val grdRegionNamePretty = grdRegion.namePretty
        val editor = GRDConnectManager.getSharedPrefsEditor()
        editor?.putString(GRD_Preferred_Region, grdRegionName)
        editor?.putString(GRD_PREFERRED_REGION_NAME_PRETTY, grdRegionNamePretty)
        editor?.apply()
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