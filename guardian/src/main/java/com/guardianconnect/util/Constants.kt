package com.guardianconnect.util

import com.google.gson.reflect.TypeToken

class Constants {
    companion object {
        const val GRD_TRANSPORT_PROTOCOL = "TRANSPORT_PROTOCOL"
        const val GRD_WIREGUARD = "wireguard"
        const val GRD_UNKNOWN = "unknown"
        const val GRD_WIREGUARD_PRETTY = "Wireguard"
        const val GRD_UNKNOWN_PRETTY = "Unknown"
        const val GRD_SUBSCRIBER_CREDENTIAL = "GRD_SUBSCRIBER_CREDENTIAL"
        const val GRD_CONNECT_SUBSCRIBER = "GRD_CONNECT_SUBSCRIBER"
        const val GRD_CONNECT_SUBSCRIBER_SECRET = "GRD_CONNECT_SUBSCRIBER_SECRET"
        const val GRD_CONNECT_DEVICE = "GRD_CONNECT_DEVICE"
        const val GRD_CREDENTIAL_LIST = "GRD_CREDENTIAL_LIST"
        const val GRD_MAIN = "main"
        const val GRD_PERSISTENT_LOG = "kGRDPersistentLog"
        const val GRD_PERSISTENT_LOG_ENABLED = "kGRDPersistentLogEnabled"
        const val GRD_PREFERRED_REGION = "GRD_PREFERRED_REGION"
        const val GRD_REGIONS_LIST_FROM_SHARED_PREFS = "GRD_REGIONS_LIST_FROM_SHARED_PREFS"
        const val GRD_AUTOMATIC_REGION = "Automatic"
        const val GRD_PE_TOKEN = "GRD_PE_TOKEN"
        const val GRD_BLOCKLIST_BITMASK_STATE = "BITMASK_STATE"
        const val GRD_BLOCKLIST_BLOCK_NONE = "block-none"
        const val GRD_BLOCKLIST_BLOCK_ADS = "block-ads"
        const val GRD_BLOCKLIST_BLOCK_PHISHING = "block-phishing"
        const val API_ERROR = "Cannot make API requests!"
        const val GRD_PE_TOKEN_CONNECT_API_ENV = "GRD_PE_TOKEN_CONNECT_API_ENV"
        const val GRD_PE_TOKEN_EXPIRATION_DATE = "GRD_PE_TOKEN_EXPIRATION_DATE"
        const val GRD_CONNECT_USER_PREFERRED_DNS_SERVERS = "GRD_CONNECT_USER_PREFERRED_DNS_SERVERS"
		const val GRD_CONNECT_USER_PREFERRED_EXIT_REGION = "GRD_CONNECT_USER_PREFERRED_EXIT_REGION"
        const val kGRDErrGuardianAccountNotSetup = "Guardian account setup not yet completed!"
        const val kGRDConnectAPIHostname = "connect-api.guardianapp.com"
        const val kGRDPreferredRegionPrecision = "kGRDPreferredRegionPrecision"
        const val kGRDRegionPrecisionDefault = "default"
        const val kGRDRegionPrecisionCity = "city"
        const val kGRDRegionPrecisionCountry = "country"
        const val kGRDRegionPrecisionCityByCountry = "city-by-country"
        const val kGRDAppExceptionsPackageNames = "kGRDAppExceptionsPackageNames"
		const val GRD_CONNECT_CLIENT_RULES_DATA = "GRD_CONNECT_CLIENT_RULES_DATA"
        const val kGRDExcludeLANTraffic = "kGRDExcludeLANTraffic"
        const val VPN_CREDENTIALS_INVALIDATION_ERROR = "VPN_CREDENTIALS_INVALIDATION_ERROR"
        const val SUBSCRIBER_CREDENTIAL_FAIL_PET = "SUBSCRIBER_CREDENTIAL_FAIL_PET"
        const val kGRDLastKnownAutomaticRegion = "kGRDLastKnownAutomaticRegion"
        const val kGRDSubscriberCredentialValidationMethod = "kGRDSubscriberCredentialValidationMethod"
        const val kGRDDemoAppPublishableKey = "kGRDDemoAppPublishableKey"

		val APITYPETOKENMAP = object : TypeToken<MutableMap<String, Any>>() {}.type
		val APITYPETOKENARRAY = object : TypeToken<MutableList<Any>>() {}.type
    }
}