package com.guardianconnect.managers

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.guardianconnect.GRDCredential
import com.guardianconnect.GRDDeviceFilterConfigBlocklist
import com.guardianconnect.GRDTransportProtocol
import com.guardianconnect.GRDWireGuardConfiguration
import com.guardianconnect.api.IOnApiResponse
import com.guardianconnect.api.Repository
import com.guardianconnect.helpers.GRDVPNHelper
import com.guardianconnect.helpers.GRDVPNHelper.excludeLANTraffic
import com.guardianconnect.helpers.GRDVPNHelper.grdErrorFlow
import com.guardianconnect.model.api.*
import com.guardianconnect.util.Constants.Companion.GRD_CREDENTIAL_LIST
import com.guardianconnect.util.Constants.Companion.GRD_WIREGUARD
import com.guardianconnect.util.GRDKeystore
import com.guardianconnect.util.GRDLogger
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.launch
import org.json.JSONException
import java.lang.reflect.Type

class GRDCredentialManager {
    val TAG = GRDCredentialManager::class.java.simpleName

	fun allCredentials(): List<GRDCredential>? {
		try {
			val jsonString = GRDKeystore.instance.retrieveFromKeyStore(GRD_CREDENTIAL_LIST)
			if (!jsonString.isNullOrEmpty()) {
				val type: Type = object : TypeToken<List<GRDCredential>>() {}.type
				val credentials: List<GRDCredential> = Gson().fromJson(jsonString, type)
				GRDLogger.d(TAG, "All credentials count: ${credentials.count()}")
				for (cred in credentials) {
					GRDLogger.d(TAG, "Credential: ${Gson().toJson(cred)}")
				}
				return credentials

			} else {
				return null
			}

		} catch (e: JSONException) {
			GRDConnectManager.getCoroutineScope().launch {
				GRDVPNHelper.grdErrorFlow.emit("Failed to JSON decode list of all GRDCredentials stored on device: ${e.toString()}")
			}
			return null
		}
	}

	fun saveListOfCredentials(credentials: List<GRDCredential>) {
		if (credentials.isNotEmpty()) {
			val stringToSave = Gson().toJson(credentials)
			GRDKeystore.instance.saveToKeyStore(GRD_CREDENTIAL_LIST, stringToSave)

		} else {
			GRDConnectManager.getSharedPrefsEditor().remove(GRD_CREDENTIAL_LIST)?.apply()
		}
	}

	// Get main credentials
	fun getMainCredentials(): GRDCredential? {
		val allCredentials = allCredentials()
		if (allCredentials.isNullOrEmpty()) {
			return null
		}

		// Check if there are any null items in the list
		val nonNullCredentials = allCredentials.filterNotNull()
		if (nonNullCredentials.isEmpty()) {
			GRDLogger.e("GRDCredentialManager", "All credentials are null.")
			return null
		}

		return nonNullCredentials.firstOrNull { it.mainCredential == true }
	}

    // Delete only the main credential
    fun deleteMainCredential() {
		val mainCredential = getMainCredentials()
		if (mainCredential != null) {
			removeCredential(mainCredential)
		}
    }

	// Find credential for a given identifier
	fun findCredentialByIdentifier(identifier: String): GRDCredential? {
		val allCredentials = allCredentials()
		allCredentials?.count()?.let { count ->
			if (count > 0) {
				return allCredentials.find { it.identifier == identifier}
			}
		}

		return null
	}

    // Add a new credential or update an existing credential
    fun addOrUpdateCredential(grdCredential: GRDCredential) {
		var allCredentials = allCredentials()
		if (allCredentials != null) {
			val existing: Int = allCredentials.indexOfFirst { it.identifier == grdCredential.identifier }
			val mutableAllCredentials = allCredentials.toMutableList()
			if (existing != -1) {
				mutableAllCredentials[existing] = grdCredential

			} else {
				mutableAllCredentials.add(grdCredential)
			}
			saveListOfCredentials(mutableAllCredentials.toList())

		} else {
			allCredentials = mutableListOf<GRDCredential>()
			allCredentials.add(grdCredential)
			saveListOfCredentials(allCredentials.toList())
		}
    }

	// Remove a credential
	fun removeCredential(grdCredential: GRDCredential) {
		val allCredentials = allCredentials()?.toMutableList()
		if (allCredentials != null) {
			val grdCredentialToRemove = allCredentials.find { it.identifier == grdCredential.identifier }
			val removed = grdCredentialToRemove?.let { allCredentials.remove(it) } ?: false
			if (removed) {
				saveListOfCredentials(allCredentials)
			}
		}
	}

	fun createStandaloneSGWCredential(subscriberCredential: String, server: GRDSGWServer, validForDays: Long, iOnApiResponse: IOnApiResponse) {
		val api = Repository()
		server.hostname?.let {
			api.initSGWServer(it)

		} ?: run {
			GRDLogger.e("GRDCredentialManager", "Can't create standalone credential! SGW hostname missing")
			return
		}

		val keyPair = KeyPair()
		val keyPairGenerated = KeyPair(keyPair.privateKey)
		//
		// Note from CJ 2026-06-22
		// This is a little nonsensical though
		// it allows for rapid adoption of a new
		// transport protocol in the future
		val transportOptions = mapOf<String, Any>(
			"public-key" to keyPairGenerated.publicKey.toBase64()
		)

		val deviceFilterConfig = GRDDeviceFilterConfigBlocklist().currentBlocklistConfig()?.apiPortableBlocklist()
		val clientRules = GRDVPNHelper.getAllClientRules()
		val multihopExitRegion = GRDVPNHelper.getPreferredMultihopExitRegion()

		Repository.instance.createNewVPNDevice(GRD_WIREGUARD, subscriberCredential, transportOptions, deviceFilterConfig, clientRules, multihopExitRegion, object : IOnApiResponse {
			override fun onSuccess(any: Any?) {
				@Suppress("UNCHECKED_CAST")
				val responseData = any as Map<String, Any>
				val grdCredential = GRDCredential.initGRDCredential(GRDTransportProtocol.GRDTransportProtocolType.GRD_TP_WIREGUARD, validForDays, false, responseData, server, keyPairGenerated)
				GRDCredentialManager().addOrUpdateCredential(grdCredential)

				val preferredDNSServer = GRDVPNHelper.getPreferredDNSServers()
				val appExceptionsList = GRDVPNHelper.getAppExceptions()
				val wireGuardConfig = GRDWireGuardConfiguration.getWireGuardConfigString(grdCredential, preferredDNSServer, false, appExceptionsList, excludeLANTraffic ?: true, false)
				iOnApiResponse.onSuccess(wireGuardConfig)
			}

			override fun onError(error: String?) {
				iOnApiResponse.onError(error)
				error?.let {
					GRDConnectManager.getCoroutineScope().launch {
						grdErrorFlow.emit(it)
					}
				}
			}
		})
	}
}