package com.guardianconnect.util

class Constants {

    companion object {
        const val GRD_CONFIG_STRING = "GRD_CONFIG_STRING"
        const val GRD_TRANSPORT_PROTOCOL = "TRANSPORT_PROTOCOL"
        const val GRD_WIREGUARD = "wireguard"
        const val GRD_UNKNOWN = "unknown"
        const val GRD_WIREGUARD_PRETTY = "Wireguard"
        const val GRD_UNKNOWN_PRETTY = "Unknown"
        const val GRD_SUBSCRIBER_CREDENTIAL = "GRD_SUBSCRIBER_CREDENTIAL"
        const val GRD_CONNECT_SUBSCRIBER = "GRD_CONNECT_SUBSCRIBER"
        const val GRD_CONNECT_SUBSCRIBER_SECRET = "GRD_CONNECT_SUBSCRIBER_SECRET"
        const val GRD_CONNECT_SUBSCRIBER_EMAIL = "GRD_CONNECT_SUBSCRIBER_EMAIL"
        const val GRD_CONNECT_DEVICE = "GRD_CONNECT_DEVICE"
        const val GRD_CREDENTIAL_LIST = "GRD_CREDENTIAL_LIST"
        const val GRD_MAIN = "main"
        const val GRD_Main_Credential_WG_Public_Key = "GRD-Main-Credential-WG-Public-Key"
        const val GRD_Main_Credential_WG_Private_Key = "GRD-Main-Credential-WG-Private-Key"
        const val GRD_Preferred_Region = "GRD_Preferred_Region"
        const val GRD_PREFERRED_REGION_NAME_PRETTY = "GRD_PREFERRED_REGION_NAME_PRETTY"
        const val GRD_REGIONS_LIST_FROM_SHARED_PREFS = "GRD_REGIONS_LIST_FROM_SHARED_PREFS"
        const val GRD_AUTOMATIC_REGION = "Automatic"
        const val GRD_PE_TOKEN = "GRD_PE_TOKEN"
        // TODO
        // this needs to be deleted as there is only one type of PET
        // the PET expiration date also only exists once and the namespace needs to be adjusted to
        // not specify being specifically for the Connect subscriber PET
        //const val GRD_CONNECT_SUBSCRIBER_PE_TOKEN = "GRD_CONNECT_SUBSCRIBER_PE_TOKEN"
        const val GRD_CONNECT_SUBSCRIBER_PE_TOKEN_EXP_DATE = "GRD_CONNECT_SUBSCRIBER_PE_TOKEN_EXP_DATE"
        const val GRD_BLOCKLIST_BITMASK_STATE = "BITMASK_STATE"
        const val GRD_BLOCKLIST_BLOCK_NONE = "block-none"
        const val GRD_BLOCKLIST_BLOCK_ADS = "block-ads"
        const val GRD_BLOCKLIST_BLOCK_PHISHING = "block-phishing"
        const val API_ERROR = "Cannot make API requests!"
    }
}