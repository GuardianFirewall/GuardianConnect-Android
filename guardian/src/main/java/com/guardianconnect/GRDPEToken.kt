package com.guardianconnect

import com.guardianconnect.util.Constants.Companion.GRD_PE_TOKEN
import com.guardianconnect.util.GRDKeystore

class GRDPEToken {

    fun storePEToken(peToken: String) {
        GRDKeystore.instance.saveToKeyStore(GRD_PE_TOKEN, peToken)
    }

    fun retrievePEToken(): String? {
        return GRDKeystore.instance.retrieveFromKeyStore(GRD_PE_TOKEN)
    }

    companion object {
        val instance = GRDPEToken()
    }
}