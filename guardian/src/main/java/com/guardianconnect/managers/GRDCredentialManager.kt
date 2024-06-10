package com.guardianconnect.managers

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.guardianconnect.GRDCredential
import com.guardianconnect.helpers.GRDVPNHelper
import com.guardianconnect.util.Constants.Companion.GRD_CREDENTIAL_LIST
import com.guardianconnect.util.Constants.Companion.GRD_Main_Credential_WG_Private_Key
import com.guardianconnect.util.Constants.Companion.GRD_Main_Credential_WG_Public_Key
import com.guardianconnect.util.GRDKeystore
import com.guardianconnect.util.GRDLogger
import kotlinx.coroutines.launch
import org.json.JSONException
import java.lang.reflect.Type

/* This class manages multiple stored instances of GRDCredential. Once a device connected to the VPN
    connection it will have one credential that is the mainCredential and could potentially have
    generated more credentials for export to another device.
    The credential need to be stored and retrieved from the Android Keystore. */

class GRDCredentialManager {
    val credentialsArrayList: ArrayList<GRDCredential> = ArrayList()
    val tag = GRDCredentialManager::class.java.simpleName

    init {
        initListOfCredentials()
    }

    // Delete only the main credential
    fun deleteMainCredential() {
        synchronized(credentialsArrayList) {
            if (credentialsArrayList.isNotEmpty()) {
                getMainCredentials()?.let {
                    credentialsArrayList.remove(it)
                    saveListOfCredentials(credentialsArrayList)
                }
            } else {
                GRDConnectManager.getSharedPrefsEditor().remove(GRD_CREDENTIAL_LIST)?.apply()
            }
        }
    }

    // Remove a credential
    fun removeCredential(grdCredential: GRDCredential) {
        GRDLogger.d(tag, "List before removal: ${Gson().toJson(credentialsArrayList)}")
        synchronized(credentialsArrayList) {
            credentialsArrayList.isNotEmpty().let {
                val grdCredentialToRemove =
                    credentialsArrayList.find { it.hostname == grdCredential.hostname }
                val removed =
                    grdCredentialToRemove?.let { credentialsArrayList.remove(it) } ?: false
                GRDLogger.d(tag, "Credential removed $removed")
                GRDLogger.d(
                    tag,
                    "List after removal before save: ${Gson().toJson(credentialsArrayList)}"
                )
                if (removed) {
                    saveListOfCredentials(credentialsArrayList)
                }
            }
        }
    }

    // Find credential for a given identifier
    fun findCredentialByIdentifier(identifier: String): GRDCredential? {
        return if (getAllCredentials().isNotEmpty())
            getAllCredentials().first { it.identifier == identifier }
        else
            null
    }

    // Add a new credential or update an existing credential
    fun addOrUpdateCredential(grdCredential: GRDCredential) {
        synchronized(credentialsArrayList) {
            GRDLogger.d(
                tag,
                "List addOrUpdateCredential before add: ${Gson().toJson(credentialsArrayList)}"
            )
            val existingCredentialIndex =
                credentialsArrayList.indexOfFirst { it.hostname == grdCredential.hostname }
            if (existingCredentialIndex == -1) {
                credentialsArrayList.add(grdCredential)
            } else {
                credentialsArrayList[existingCredentialIndex] = grdCredential
            }
            GRDLogger.d(
                tag,
                "List addOrUpdateCredential after add: ${Gson().toJson(credentialsArrayList)}"
            )
            saveListOfCredentials(credentialsArrayList)
        }
    }

    // Get main credentials
    fun getMainCredentials(): GRDCredential? {
        return getAllCredentials().firstOrNull { it.mainCredential == true }
    }

    // return the currently valid Credential
    fun retrieveCredential(): GRDCredential? {
        initListOfCredentials()
        return if (credentialsArrayList.isNotEmpty()) {
            return credentialsArrayList.first()
        } else {
            null
        }
    }

    // Retrieve all credentials
    fun getAllCredentials(): java.util.ArrayList<GRDCredential> {
        initListOfCredentials()
        return ArrayList(credentialsArrayList)
    }

    fun initListOfCredentials() {
        synchronized(credentialsArrayList) {
            credentialsArrayList.clear()
            try {
                val jsonString = GRDKeystore.instance.retrieveFromKeyStore(GRD_CREDENTIAL_LIST)
                if (!jsonString.isNullOrEmpty()) {
                    val type: Type = object : TypeToken<ArrayList<GRDCredential>>() {}.type
                    val newArrayList: ArrayList<GRDCredential> = Gson().fromJson(jsonString, type)
                    credentialsArrayList.clear()
                    credentialsArrayList.addAll(newArrayList)
                    GRDLogger.d(
                        tag,
                        "List initListOfCredentials: ${Gson().toJson(credentialsArrayList)}"
                    )
                } else {
                    GRDLogger.d(
                        tag,
                        "List Of Credentials is empty"
                    )
                }
            } catch (e: JSONException) {
                GRDConnectManager.getCoroutineScope().launch {
                    GRDVPNHelper.grdErrorFlow.emit(e.stackTraceToString())
                }
            }
        }
    }

    fun saveListOfCredentials(inputArrayList: ArrayList<GRDCredential>) {
        GRDLogger.d(tag, "List before saving: ${Gson().toJson(credentialsArrayList)}")
        if (inputArrayList.isNotEmpty()) {
            val stringToSave = Gson().toJson(inputArrayList)
            GRDLogger.d(tag, "List after saving: ${Gson().toJson(credentialsArrayList)}")
            GRDKeystore.instance.saveToKeyStore(GRD_CREDENTIAL_LIST, stringToSave)
            GRDLogger.d(tag, "List from keystore after saving: ${GRDKeystore.instance.retrieveFromKeyStore(
                GRD_CREDENTIAL_LIST)}")
            initListOfCredentials()
        } else {
            GRDConnectManager.getSharedPrefsEditor().remove(GRD_CREDENTIAL_LIST)?.apply()
        }
    }

    fun retrieveGRDMainCredentialWGPublicKey(): String? {
        return GRDKeystore.instance.retrieveFromKeyStore(GRD_Main_Credential_WG_Public_Key)
    }

    fun retrieveGRDMainCredentialWGPrivateKey(): String? {
        return GRDKeystore.instance.retrieveFromKeyStore(GRD_Main_Credential_WG_Private_Key)
    }
}