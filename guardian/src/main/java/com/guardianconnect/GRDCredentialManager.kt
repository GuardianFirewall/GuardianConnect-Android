package com.guardianconnect

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.guardianconnect.util.Constants.Companion.GRD_CREDENTIAL_LIST
import com.guardianconnect.util.Constants.Companion.GRD_Main_Credential_WG_Private_Key
import com.guardianconnect.util.Constants.Companion.GRD_Main_Credential_WG_Public_Key
import com.guardianconnect.util.GRDKeystore
import kotlinx.coroutines.launch
import org.json.JSONException
import java.lang.reflect.Type

/* This class manages multiple stored instances of GRDCredential. Once a device connected to the VPN
    connection it will have one credential that is the mainCredential and could potentially have
    generated more credentials for export to another device.
    The credential need to be stored and retrieved from the Android Keystore. */

class GRDCredentialManager {
    val credentialsArrayList: ArrayList<GRDCredential> = ArrayList()

    init {
        initListOfCredentials()
    }

    // Delete only the main credential
    fun deleteMainCredential() {
        if (credentialsArrayList.isNotEmpty()) {
            getMainCredentials()?.let {
                credentialsArrayList.remove(it)
                saveListOfCredentials(credentialsArrayList)
            }
        } else {
            GRDConnectManager.getSharedPrefsEditor().remove(GRD_CREDENTIAL_LIST)?.apply()
        }
    }

    // Remove a credential
    fun removeCredential(grdCredential: GRDCredential) {
        credentialsArrayList.isNotEmpty().let {
            credentialsArrayList.remove(grdCredential)
            saveListOfCredentials(credentialsArrayList)
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
        if (!credentialsArrayList.contains(grdCredential)) {
            credentialsArrayList.add(grdCredential)
        } else {
            credentialsArrayList.remove(grdCredential)
            credentialsArrayList.add(grdCredential)
        }
        saveListOfCredentials(credentialsArrayList)
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
        return ArrayList(credentialsArrayList)
    }

    fun initListOfCredentials() {
        credentialsArrayList.clear()
        try {
            val jsonString = GRDKeystore.instance.retrieveFromKeyStore(GRD_CREDENTIAL_LIST)
            if (!jsonString.isNullOrEmpty()) {
                val type: Type = object : TypeToken<ArrayList<GRDCredential>>() {}.type
                val newArrayList: ArrayList<GRDCredential> = Gson().fromJson(jsonString, type)
                credentialsArrayList.clear()
                credentialsArrayList.addAll(newArrayList)
            }
        } catch (e: JSONException) {
            GRDConnectManager.getCoroutineScope().launch {
                GRDVPNHelper.grdErrorFlow.emit(e.stackTraceToString())
            }
        }
    }

    fun saveListOfCredentials(inputArrayList: ArrayList<GRDCredential>) {
        if (inputArrayList.isNotEmpty()) {
            val stringToSave = Gson().toJson(inputArrayList)
            GRDKeystore.instance.saveToKeyStore(GRD_CREDENTIAL_LIST, stringToSave)
        } else {
            GRDConnectManager.getSharedPrefsEditor()?.remove(GRD_CREDENTIAL_LIST)?.apply()
        }
    }

    fun retrieveGRDMainCredentialWGPublicKey(): String? {
        return GRDKeystore.instance.retrieveFromKeyStore(GRD_Main_Credential_WG_Public_Key)
    }

    fun retrieveGRDMainCredentialWGPrivateKey(): String? {
        return GRDKeystore.instance.retrieveFromKeyStore(GRD_Main_Credential_WG_Private_Key)
    }
}