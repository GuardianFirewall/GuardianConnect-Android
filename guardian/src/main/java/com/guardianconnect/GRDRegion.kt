package com.guardianconnect

import com.google.gson.annotations.SerializedName
import com.guardianconnect.util.Constants.Companion.GRD_AUTOMATIC_REGION

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
}