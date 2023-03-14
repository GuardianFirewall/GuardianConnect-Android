package com.guardianconnect

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.guardianconnect.api.IOnApiResponse
import com.guardianconnect.api.Repository
import com.guardianconnect.model.api.RequestServersForRegion
import com.guardianconnect.model.api.Server
import com.guardianconnect.model.api.TimeZonesResponse
import com.guardianconnect.util.Constants.Companion.GRD_AUTOMATIC_REGION
import com.guardianconnect.util.Constants.Companion.GRD_REGIONS_LIST_FROM_SHARED_PREFS
import java.util.*
import kotlin.collections.ArrayList

/* This class provides higher level helper functions to quickly get the list of VPN servers and
    pick one
*/

class GRDServerManager {

    private val TAG = GRDServerManager::class.java.simpleName

    /*  Function that calls the GRDHousekeeping APIs to get the list of available time zones
        compares the device's current time zone to the available ones and selects a region
        gets the list of VPN servers for the selected region from GRDHousekeeping
        return the array of VPN servers
    */
    fun getGuardianHosts(
        iOnApiResponse: IOnApiResponse
    ) {
        val grdRegion = GRDRegion()
        val serversForRegion = RequestServersForRegion()
        val selectedRegion = grdRegion.getPreferredRegion()
        if (!selectedRegion.isNullOrEmpty()) {
            serversForRegion.region = selectedRegion
            requestListOfServersForRegion(serversForRegion, iOnApiResponse)
        } else {
            Repository.instance.getListOfSupportedTimeZones(object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val list = any as ArrayList<TimeZonesResponse>
                    list.let {
                        val currentTimeZoneId = TimeZone.getDefault().id
                        for (item in it) {
                            item.getTimezones()?.let { tz ->
                                for (timeZone in tz) {
                                    if (timeZone == currentTimeZoneId) {
                                        item.getName()?.let { name ->
                                            serversForRegion.region = name
                                            Log.d(
                                                TAG, "Selected region: " + serversForRegion.region
                                            )
                                            requestListOfServersForRegion(
                                                serversForRegion,
                                                iOnApiResponse
                                            )
                                        }
                                    } else {
                                        iOnApiResponse.onError("No available servers for your timezone!")
                                    }
                                }
                            }
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
        serversForRegion: RequestServersForRegion,
        iOnApiResponse: IOnApiResponse
    ) {
        Repository.instance.requestListOfServersForRegion(
            serversForRegion,
            object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val listOfServersToReturn =
                        any as ArrayList<Server>
                    iOnApiResponse.onSuccess(
                        listOfServersToReturn
                    )
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
        iOnApiResponse: IOnApiResponse
    ) {
        getGuardianHosts(object : IOnApiResponse {
            override fun onSuccess(any: Any?) {
                val listOfServers = any as ArrayList<Server>?
                listOfServers?.let {
                    val filteredServers: List<Server> =
                        listOfServers.filter { it.getCapacityScore() in setOf(0, 1) }
                    var selectedServer: Server? = null
                    if (filteredServers.isNotEmpty()) {
                        selectedServer = filteredServers.random()
                    } else if (listOfServers.size == 1) {
                        selectedServer = listOfServers[0]
                    }
                    Log.d(TAG, "Selected Server is: " + selectedServer?.getDisplayName())
                    Log.d(TAG, "Selected server hostname is: " + selectedServer?.hostname())
                    iOnApiResponse.onSuccess(selectedServer)
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
        val activeNetwork: Network = connectivityManager.activeNetwork!!
        val caps: NetworkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)!!
        val vpnInUse = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        val listFromSharedPreferences =
            GRDConnectManager.getSharedPrefs()?.getString(GRD_REGIONS_LIST_FROM_SHARED_PREFS, "")
        val list = ArrayList<GRDRegion>()
        val automaticGRDRegion = GRDRegion()
        automaticGRDRegion.namePretty = GRD_AUTOMATIC_REGION
        list.add(0, automaticGRDRegion)

        // Make API call only when VPN is not in use
        if (vpnInUse && !listFromSharedPreferences.isNullOrEmpty()) {
            val type = object : TypeToken<List<GRDRegion>>() {}.type
            val arrayList: List<GRDRegion> = Gson().fromJson(listFromSharedPreferences, type)
            onRegionListener.onRegionsAvailable(arrayList)
        } else {
            Repository.instance.requestAllGuardianRegions(object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    val regionsList = any as List<GRDRegion>
                    list.addAll(regionsList)
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

    interface OnRegionListener {

        fun onRegionsAvailable(listOfGRDRegions: List<GRDRegion>)
    }

}