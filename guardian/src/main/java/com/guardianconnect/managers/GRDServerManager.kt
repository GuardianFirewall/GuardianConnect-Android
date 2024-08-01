package com.guardianconnect.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.guardianconnect.GRDRegion
import com.guardianconnect.api.IOnApiResponse
import com.guardianconnect.api.Repository
import com.guardianconnect.enumeration.GRDServerFeatureEnvironment
import com.guardianconnect.model.api.GRDSGWServer
import com.guardianconnect.model.api.TimeZonesResponse
import com.guardianconnect.util.Constants
import com.guardianconnect.util.Constants.Companion.GRD_AUTOMATIC_REGION
import com.guardianconnect.util.Constants.Companion.GRD_REGIONS_LIST_FROM_SHARED_PREFS
import com.guardianconnect.util.Constants.Companion.kGRDLastKnownAutomaticRegion
import java.util.*
import kotlin.collections.ArrayList

/* This class provides higher level helper functions to quickly get the list of VPN servers and
    pick one
*/

class GRDServerManager {
    var preferBetaCapableServers: Boolean? = null
    var vpnServerFeatureEnvironment: GRDServerFeatureEnvironment? = null
    var regionPrecision: String? = null

    companion object {
        private val TAG = GRDServerManager::class.java.simpleName

        const val kGRDServerManagerRegionKey = "region"
        const val kGRDServerManagerPaidKey = "paid"
        const val kGRDServerManagerFeatureEnvironmentKey = "feature-environment"
        const val kGRDServerManagerBetaCapableKey = "beta-capable"

        /*  Function to reset the user preferred region. */
        fun clearPreferredRegion() {
            GRDConnectManager.getSharedPrefsEditor().remove(Constants.GRD_PREFERRED_REGION)?.apply()
            Log.d(TAG, "Preferred Region cleared!")
        }

        /*  Permanently override the region that the device should connect to.
            The function should take the name property of the region object and store it into the
            SharedPreferences for the key GRD_Preferred_Region */
        fun setPreferredRegion(region: GRDRegion?) {
            val editor = GRDConnectManager.getSharedPrefsEditor()
            if (region == null || region.name == GRD_AUTOMATIC_REGION) {
                clearPreferredRegion()
            } else {
                val gson = Gson()
                val json = gson.toJson(region)
                editor.putString(Constants.GRD_PREFERRED_REGION, json)
            }
            editor.apply()
        }

        /*  Function that retrieves the currently stored object region in the SharedPreferences */
        fun getPreferredRegion(): GRDRegion? {
            val sharedPreferences = GRDConnectManager.getSharedPrefs()
            val json = sharedPreferences.getString(Constants.GRD_PREFERRED_REGION, null)
            if (json != null) {
                val gson = Gson()
                return gson.fromJson(json, GRDRegion::class.java)
            }
            return null
        }
    }

    /*  Function that calls the GRDHousekeeping APIs to get the list of available time zones
        compares the device's current time zone to the available ones and selects a region
        gets the list of VPN servers for the selected region from GRDHousekeeping
        return the array of VPN servers
    */
    fun getGuardianHosts(
        region: GRDRegion?,
        iOnApiResponse: IOnApiResponse
    ) {
        var preferredRegion = region
        if (region == null) {
            preferredRegion = getPreferredRegion()
        }
        var requestBody: MutableMap<String, Any> = mutableMapOf()
        if (!preferredRegion?.name.isNullOrEmpty()) {
            Log.d(TAG, "Using user preferred region: " + preferredRegion?.name.toString())

            requestBody = mutableMapOf<String, Any>(
                kGRDServerManagerRegionKey to preferredRegion?.name.toString(),
                kGRDServerManagerFeatureEnvironmentKey to vpnServerFeatureEnvironment as GRDServerFeatureEnvironment,
                kGRDServerManagerBetaCapableKey to preferBetaCapableServers as Boolean
            )
            Log.d(TAG, "serversForRegion requestBody: $requestBody")

            requestListOfServersForRegion(
                requestBody,
                iOnApiResponse
            )

        } else {
            var selectedRegion = String()
            Repository.instance.getListOfSupportedTimeZones(object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val anyList = any as List<*>
                    val list = anyList.filterIsInstance<TimeZonesResponse>()
                    list.let {
                        val currentTimeZoneId = TimeZone.getDefault().id
                        var grdRegion: GRDRegion? = null
                        for (item in it) {
                            item.timezones?.let { tz ->
                                for (timeZone in tz) {
                                    if (timeZone == currentTimeZoneId) {
                                        item.name?.let { name ->
                                            grdRegion = GRDRegion().apply {
                                                this.name = name
                                                this.namePretty = item.namePretty
                                                this.continent = item.continent
                                                this.country = item.countryISOCode
                                                this.isAutomatic = true
                                            }
                                            selectedRegion = name
                                            requestBody[kGRDServerManagerRegionKey] = selectedRegion

                                            Log.d(
                                                TAG,
                                                "Selected region: " + requestBody[kGRDServerManagerRegionKey]
                                            )
                                        }
                                        break
                                    }
                                }
                            }
                        }

                        if (selectedRegion.isEmpty()) {
                            iOnApiResponse.onError("No available servers for your timezone: $currentTimeZoneId")
                        } else {
                            grdRegion?.timeZoneName = currentTimeZoneId
                            GRDConnectManager.getSharedPrefsEditor().putString(kGRDLastKnownAutomaticRegion, Gson().toJson(grdRegion)).apply()
                            requestListOfServersForRegion(
                                requestBody,
                                iOnApiResponse
                            )
                        }
                    }
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                    error?.let { Log.d(TAG, it) }
                }
            })
        }
    }

    fun requestListOfServersForRegion(
        serversForRegion: MutableMap<String, Any>,
        iOnApiResponse: IOnApiResponse
    ) {
        Repository.instance.requestListOfServersForRegion(
            serversForRegion,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val anyList = any as List<*>
                    val listOfServersToReturn = anyList.filterIsInstance<GRDSGWServer>()
                    iOnApiResponse.onSuccess(listOfServersToReturn)
                    Log.d(
                        TAG,
                        "List of servers for selected region: " + Gson().toJson(
                            listOfServersToReturn
                        )
                    )
                }

                override fun onError(error: String?) {
                    iOnApiResponse.onError(error)
                    error?.let { it1 -> Log.d(TAG, it1) }
                }
            })
    }

    /*  Function to quickly select a VPN server based on our filters and preferences for load
        balancing. The function should call the function to get the list of VPN servers for the
        device's region, quickly sort them to prefer servers with a capacity-score of 0 or 1 and
        then randomly select one from that array of servers.
    */
    fun selectServerFromRegion(
        region: GRDRegion?,
        iOnApiResponse: IOnApiResponse
    ) {
        getGuardianHosts(region, object : IOnApiResponse {
            override fun onSuccess(any: Any?) {
                val anyList = any as List<*>
                val listOfGRDSGWServers = anyList.filterIsInstance<GRDSGWServer>()
                listOfGRDSGWServers.let {
                    val filteredGRDSGWServers: List<GRDSGWServer> =
                        listOfGRDSGWServers.filter { it.capacityScore in setOf(0, 1) }
                    var selectedGRDSGWServer: GRDSGWServer? = null
                    if (filteredGRDSGWServers.isNotEmpty()) {
                        selectedGRDSGWServer = filteredGRDSGWServers.random()
                    } else if (listOfGRDSGWServers.size == 1) {
                        selectedGRDSGWServer = listOfGRDSGWServers[0]
                    }
                    Log.d(TAG, "Selected Server is: " + selectedGRDSGWServer?.displayName)
                    Log.d(TAG, "Selected server hostname is: " + selectedGRDSGWServer?.hostname)
                    iOnApiResponse.onSuccess(selectedGRDSGWServer)
                }
            }

            override fun onError(error: String?) {
                iOnApiResponse.onError(error)
                error?.let { Log.d(TAG, it) }
            }
        })
    }

    /* Returns the array of GRDRegion items */
    fun returnAllAvailableRegions(
        onRegionListener: OnRegionListener
    ) {
        val connectivityManager =
            GRDConnectManager.get()
                .getContext().applicationContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: Network? = connectivityManager.activeNetwork
        if (activeNetwork == null) {
            onRegionListener.onRegionsAvailable(emptyList())

        } else {
            val caps: NetworkCapabilities? =
                connectivityManager.getNetworkCapabilities(activeNetwork)
            if (caps == null) {
                onRegionListener.onRegionsAvailable(emptyList())
                return
            }

            val vpnInUse = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            val listFromSharedPreferences =
                GRDConnectManager.getSharedPrefs()
                    ?.getString(GRD_REGIONS_LIST_FROM_SHARED_PREFS, "")
            val list = ArrayList<GRDRegion>()
            val auto = GRDRegion.automaticRegion()
            list.add(0, auto)

            // Make API call only when VPN is not in use
            if (vpnInUse && !listFromSharedPreferences.isNullOrEmpty()) {
                val type = object : TypeToken<List<GRDRegion>>() {}.type
                val arrayList: List<GRDRegion> = Gson().fromJson(listFromSharedPreferences, type)
                onRegionListener.onRegionsAvailable(arrayList)
            } else {
                regionPrecision?.let { regionPrecision ->
                    Repository.instance.requestAllRegionsWithPrecision(
                        regionPrecision,
                        object : IOnApiResponse {
                            override fun onSuccess(any: Any?) {
                                val anyList = any as List<*>
                                val regionsList = anyList.filterIsInstance<GRDRegion>()
                                list.addAll(regionsList)
                                list.sortWith(compareBy<GRDRegion> { item ->
                                    if (item.namePretty == GRD_AUTOMATIC_REGION) 0 else 1
                                }.thenBy { it.namePretty.toString() })
                                onRegionListener.onRegionsAvailable(list)
                                GRDConnectManager.getSharedPrefsEditor()?.putString(
                                    GRD_REGIONS_LIST_FROM_SHARED_PREFS, Gson().toJson(list)
                                )?.apply()
                                Log.d(TAG, "returnAllAvailableRegions success")
                            }

                            override fun onError(error: String?) {
                                error?.let { Log.d(TAG, it) }
                                onRegionListener.onRegionsAvailable(ArrayList())
                            }
                        })
                }
            }
        }
    }

    interface OnRegionListener {

        fun onRegionsAvailable(listOfGRDRegions: List<GRDRegion>)
    }

}