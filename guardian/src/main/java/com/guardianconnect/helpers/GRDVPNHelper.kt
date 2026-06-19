package com.guardianconnect.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.guardianconnect.GRDClientRule
import com.guardianconnect.GRDCredential
import com.guardianconnect.GRDPEToken
import com.guardianconnect.GRDRegion
import com.guardianconnect.GRDSubscriberCredential
import com.guardianconnect.GRDTransportProtocol
import com.guardianconnect.GRDWireGuardConfiguration
import com.guardianconnect.R
import com.guardianconnect.api.IOnApiResponse
import com.guardianconnect.api.Repository
import com.guardianconnect.billing.GRDBillingManager
import com.guardianconnect.enumeration.GRDServerFeatureEnvironment
import com.guardianconnect.managers.GRDConnectManager
import com.guardianconnect.managers.GRDCredentialManager
import com.guardianconnect.managers.GRDServerManager
import com.guardianconnect.model.GRDSubscriberCredentialValidationMethod
import com.guardianconnect.model.TimeZoneNotification
import com.guardianconnect.model.TunnelModel
import com.guardianconnect.model.api.*
import com.guardianconnect.util.Constants
import com.guardianconnect.util.Constants.Companion.GRD_CONNECT_CLIENT_RULES_DATA
import com.guardianconnect.util.Constants.Companion.GRD_CONNECT_USER_PREFERRED_DNS_SERVERS
import com.guardianconnect.util.Constants.Companion.GRD_CONNECT_USER_PREFERRED_EXIT_REGION
import com.guardianconnect.util.Constants.Companion.GRD_SUBSCRIBER_CREDENTIAL
import com.guardianconnect.util.Constants.Companion.GRD_WIREGUARD
import com.guardianconnect.util.Constants.Companion.kGRDLastKnownAutomaticRegion
import com.guardianconnect.util.ErrorMessages
import com.guardianconnect.util.GRDLogger
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.Reader
import java.io.StringReader
import java.util.TimeZone


@SuppressLint("StaticFieldLeak")
object GRDVPNHelper {
    private val TAG = GRDVPNHelper::class.java.simpleName
    private var context: Context? 			= null
    var connectAPIHostname: String 			= ""
    var connectPublishableKey: String 		= ""
    var tunnelName: String 					= ""
    var validForDays: Long 					= 60
    var preferBetaCapableServers: Boolean? 	= null
    var vpnServerFeatureEnvironment: GRDServerFeatureEnvironment? = null
    private var regionPrecision: String?	= null
    var excludeLANTraffic: Boolean? 		= true
    var iapApplicationId: String? 			= null
    var allowedProductIds: List<String>? 	= mutableListOf()
    internal val _timezoneChannel 			= Channel<TimeZoneNotification>()
    val timezoneChannel 					= _timezoneChannel.receiveAsFlow()
    var preferredValidationMethod: GRDSubscriberCredentialValidationMethod = GRDSubscriberCredentialValidationMethod.Invalid
    var customSubscriberCredentialAuthKeys: MutableMap<String, Any>? = null


    fun initHelper(context: Context) {
        GRDVPNHelper.context = context
        preferBetaCapableServers = false
        vpnServerFeatureEnvironment = GRDServerFeatureEnvironment.ServerFeatureEnvironmentProduction
        appExceptions = getArrayListOfAppExceptions()

        excludeLANTraffic = GRDConnectManager.getSharedPrefs().getBoolean(Constants.kGRDExcludeLANTraffic, true)

        val savedPrecision = GRDConnectManager.getSharedPrefs().getString(Constants.kGRDPreferredRegionPrecision, null)
        if (!savedPrecision.isNullOrEmpty()) {
            regionPrecision = savedPrecision
            when (regionPrecision) {
                Constants.kGRDRegionPrecisionDefault -> {}
                Constants.kGRDRegionPrecisionCity -> {}
                Constants.kGRDRegionPrecisionCountry -> {}
                Constants.kGRDRegionPrecisionCityByCountry -> {}
                else -> {
                    Log.d(
                        TAG,
                        "Preferred region precision '$regionPrecision' does not match any of the known constants!"
                    )
                }
            }
        }
        observeStatus()
    }

	private fun observeStatus() {
		GRDConnectManager.getCoroutineScope().launch {
			grdStatusFlow.collect {
				when (it) {
					GRDVPNHelperStatus.CONNECTED.status -> {
						handleOkHttpClient(GRDVPNHelperStatus.CONNECTED.status)
					}

					GRDVPNHelperStatus.DISCONNECTED.status -> {
						handleOkHttpClient(GRDVPNHelperStatus.DISCONNECTED.status)
					}

					GRDVPNHelperStatus.ERROR_CONNECTING.status -> {
						handleOkHttpClient(GRDVPNHelperStatus.ERROR_CONNECTING.status)
					}
				}
			}
		}
	}

    fun checkTimeZoneChanged() {
        val currentTimeZone = TimeZone.getDefault().id
        val currentGRDRegion = if (GRDServerManager.getPreferredRegion() != null) {
            GRDServerManager.getPreferredRegion()
        } else {
            GRDRegion.automaticRegion()
        }
        currentGRDRegion?.timeZoneName = currentTimeZone
        GRDLogger.d(TAG, "checkTimeZoneChanged currentTimeZone: ${currentTimeZone}")
        val lastKnownTimeZoneString =
            GRDConnectManager.getSharedPrefs().getString(kGRDLastKnownAutomaticRegion, null)
        val lastKnownTimeZone = Gson().fromJson(lastKnownTimeZoneString, GRDRegion::class.java)

        if (lastKnownTimeZone != null && lastKnownTimeZone.timeZoneName != currentGRDRegion?.timeZoneName) {
            val notification        = TimeZoneNotification()
            notification.changed    = true
            notification.oldRegion  = lastKnownTimeZone
            notification.newRegion  = currentGRDRegion
            GRDLogger.d(TAG, "checkTimeZoneChanged timeZoneNotification: old: ${notification.oldRegion?.timeZoneName} new: ${notification.newRegion?.timeZoneName}")
            //
            // Note from CJ 2024-11-06
            // Remove the last known automatic region from shared preferences prior to
            // posting the notification in order to ensure that the app is only notified once
            GRDConnectManager.getSharedPrefsEditor().remove(kGRDLastKnownAutomaticRegion)?.apply()
            _timezoneChannel.trySend(notification)
        }
    }

    fun setRegionPrecision(precision: String) {
		regionPrecision = precision
		if (precision == Constants.kGRDRegionPrecisionDefault) {
			GRDConnectManager.getSharedPrefsEditor().remove(Constants.kGRDPreferredRegionPrecision)?.apply()
			return
		}

        GRDConnectManager.getSharedPrefsEditor() .putString(Constants.kGRDPreferredRegionPrecision, precision)?.apply()
    }

    fun setAppExceptionPackages(apps: ArrayList<String>?) {
        appExceptions = if (apps == null) {
            GRDConnectManager.getSharedPrefsEditor().remove(Constants.kGRDAppExceptionsPackageNames)?.apply()
            ArrayList()

        } else {
			val gson = Gson()
			val jsonString = gson.toJson(apps)
			GRDConnectManager.getSharedPrefsEditor().putString(Constants.kGRDAppExceptionsPackageNames, jsonString)?.apply()
            apps
        }
    }

    private fun getArrayListOfAppExceptions(): ArrayList<String> {
        val jsonString = GRDConnectManager.getSharedPrefs()
            .getString(Constants.kGRDAppExceptionsPackageNames, null)
        val type = object : TypeToken<ArrayList<String>>() {}.type
        val gson = Gson()
        return gson.fromJson(jsonString, type) ?: ArrayList()
    }

    fun excludeLANTraffic(shouldExclude: Boolean) {
        excludeLANTraffic = shouldExclude
        val localExcludeLANTraffic = excludeLANTraffic as Boolean
        GRDConnectManager.getSharedPrefsEditor().putBoolean(Constants.kGRDExcludeLANTraffic, localExcludeLANTraffic)?.apply()
    }

	fun setPreferredDNSServer(dnsServerNumber: String) {
		GRDConnectManager.getSharedPrefsEditor().putString(
			GRD_CONNECT_USER_PREFERRED_DNS_SERVERS,
			dnsServerNumber
		)?.apply()
	}

	fun getPreferredDNSServers(): String? {
		return GRDConnectManager.getSharedPrefs().getString(GRD_CONNECT_USER_PREFERRED_DNS_SERVERS, null)
	}

	fun getPreferredMultihopExitRegion(): String {
		val exitRegion = GRDConnectManager.getSharedPrefs().getString(GRD_CONNECT_USER_PREFERRED_EXIT_REGION, null)
		if (exitRegion == null) {
			return "disabled"
		}
		return exitRegion
	}

	fun setPreferredMultihopExitRegion(exitRegion: String) {
		GRDConnectManager.getSharedPrefsEditor().putString(GRD_CONNECT_USER_PREFERRED_EXIT_REGION, exitRegion)?.apply()
	}

	fun getAllClientRules(): List<GRDClientRule> {
		var allClientRules = mutableListOf<GRDClientRule>()
		val rulesJSON = GRDConnectManager.getSharedPrefs().getString(GRD_CONNECT_CLIENT_RULES_DATA, null)
		var rulesRaw = listOf<Map<String,Any>>()
		rulesRaw = Gson().fromJson(rulesJSON, object : TypeToken<Map<String, Any>>() {}.type)

		for (encodedRule: Map<String,Any> in rulesRaw) {
			val rule = GRDClientRule.initFromMap(encodedRule)
			allClientRules.add(rule)
		}

		return allClientRules.toList()
	}

	fun indexOfClientRuleInAllClientRules(clientRule: GRDClientRule, allClientRules: List<GRDClientRule>): Int {
		var index = 0
		for (rule: GRDClientRule in allClientRules) {
			if (rule.equals(clientRule)) {
				return index
			}

			index++
		}

		return -1
	}

	fun addClientRule(clientRule: GRDClientRule) {
		var mutableClientRules = getAllClientRules().toMutableList()
		if (mutableClientRules.count() < 1) {
			mutableClientRules = mutableListOf<GRDClientRule>()
		}

		val index = indexOfClientRuleInAllClientRules(clientRule, mutableClientRules)
		if (index != -1) {
			mutableClientRules[index] = clientRule

		} else {
			mutableClientRules.add(clientRule)
		}

		storeClientRules(mutableClientRules)
	}

	fun removeClientRule(clientRule: GRDClientRule) {
		val mutableClientRules = getAllClientRules().toMutableList()
		if (mutableClientRules.count() < 1) {
			return
		}

		val index = indexOfClientRuleInAllClientRules(clientRule, mutableClientRules)
		if (index != -1) {
			return
		}

		mutableClientRules.removeAt(index)
		storeClientRules(mutableClientRules)
	}

	// TODO: this probably needs to emit an exception (?)
	fun storeClientRules(clientRules: List<GRDClientRule>) {
		val encodedClientRules = mutableListOf<Map<String,Any>>()
		for (rule: GRDClientRule in clientRules) {
			val encodedRule = rule.encodeToMap()
			encodedClientRules.add(encodedRule)
		}
		val encoded = Gson().toJson(encodedClientRules)
		GRDConnectManager.getSharedPrefsEditor().putString(GRD_CONNECT_CLIENT_RULES_DATA, encoded)?.apply()
	}

	fun apiPortableClientRules(clientRules: List<GRDClientRule>): List<Map<String, Any>> {
		val portable = mutableListOf<Map<String, Any>>()
		for (rule: GRDClientRule in clientRules) {
			val encodedRule = rule.encodeToMap().toMutableMap()
			encodedRule.remove("match-port")
			encodedRule.remove("rule-id")
			encodedRule.remove("multihop-exit-region")
			portable.add(encodedRule)
		}

		return portable
	}

	fun allRegions(onRegionListener: GRDServerManager.OnRegionListener) {
		val serverManager = GRDServerManager()
		serverManager.preferBetaCapableServers = preferBetaCapableServers
		serverManager.vpnServerFeatureEnvironment = vpnServerFeatureEnvironment
		serverManager.regionPrecision = regionPrecision
		serverManager.returnAllAvailableRegions(onRegionListener)
	}

    suspend fun stopTunnel() {
        try {
            grdStatusFlow.emit(GRDVPNHelperStatus.DISCONNECTING.status)
            getActiveTunnel()?.setStateAsync(Tunnel.State.DOWN)
            grdStatusFlow.emit(GRDVPNHelperStatus.DISCONNECTED.status)

        } catch (t: Throwable) {
            grdErrorFlow.emit("Error stopping tunnel! " + t.stackTraceToString())
        }
    }

	fun isTunnelRunning(): Boolean {
		val runningTunnel = getActiveTunnel()
		return (runningTunnel != null && runningTunnel.state == Tunnel.State.UP)
	}

	fun getActiveTunnel(): TunnelModel? {
		return GRDConnectManager.getTunnelManager().tunnelMap[tunnelName]
	}

	/**
	 * Prepare to establish a VPN connection. This method returns null if the VPN application is
	 * already prepared or if the user has previously consented to the VPN application.
	 * Otherwise, it returns an Intent to a system activity.
	 * The application should launch the activity using Activity.startActivityForResult to get itself prepared.
	 * The activity may pop up a dialog to require user action, and the result will come back via its Activity.onActivityResult.
	 * If the result is Activity.RESULT_OK, the application becomes prepared and is granted to use other methods in this class.
	 * Only one application can be granted at the same time.
	 * The right is revoked when another application is granted.
	 * The application losing the right will be notified via its onRevoke.
	 * Unless it becomes prepared again, subsequent calls to other methods in this class will fail
	 * The user may disable the VPN at any time while it is activated, in which case this method
	 * will return an intent the next time it is executed to obtain the user's consent again.
	 */
	fun getIntentVpnPermissions(context: Context): Intent? = GoBackend.VpnService.prepare(context)

	suspend fun createAndStartTunnel() {
		// Check if the user had already granted the permission to set the VPN profile
		val intent = GoBackend.VpnService.prepare(context)
		when {
			// in case the permission was not yet granted emit the intent so that the
			// OS can be asked to present the modal alert
			intent != null -> grdVPNPermissionFlow.emit(intent)

			// Ensure that a tunnel name has been set
			tunnelName.isEmpty() -> grdErrorFlow.emit("Tunnel name should not be empty!")

			// Check if VPN credentials are already present in the GRDCredentialManager
			else -> GRDCredentialManager().getMainCredentials().let {
				if (activeConnectionPossible(it)) {
					// If VPN credentials already exist try to start the VPN tunnel again
					if (!it?.hostname.isNullOrEmpty()) {
						Repository.instance.initSGWServer(it.hostname.toString())
						Repository.instance.getServerStatusForDeviceId(it.clientId.toString(), object : IOnApiResponse {
							override fun onSuccess(any: Any?) {
								val configString = it.let {
									val appExceptionsList = getAppExceptions()
									GRDWireGuardConfiguration.getWireGuardConfigString(it, GRDConnectManager.getSharedPrefs().getString(GRD_CONNECT_USER_PREFERRED_DNS_SERVERS, null), appExceptionsList, excludeLANTraffic ?: true)
								}
								if (configString.isNotEmpty()) {
									GRDConnectManager.getCoroutineScope().launch {
										configStringFlow.emit(configString)
										connectTunnel(configString)
										grdMsgFlow.emit("Create tunnel with existing credentials successful!")
									}
								}
							}

							override fun onError(error: String?) {
								GRDConnectManager.getCoroutineScope().launch {
									grdErrorFlow.emit("SGW credential no longer valid on the selected host: {$error}")
								}
							}
						})

					} else {
						grdErrorFlow.emit("SGW credential does not contain a hostname is empty!")
					}

				} else {
					// No VPN credentials exist yet
					configureFirstTimeUserAndConnect(
						object : IOnApiResponse {
							override fun onSuccess(any: Any?) {
								val configString = any as String
								Log.d(TAG, configString)
								GRDConnectManager.getCoroutineScope().launch {
									grdMsgFlow.emit("Create tunnel first time successful!")
								}
							}

							override fun onError(error: String?) {
								error?.let { it1 ->
									GRDConnectManager.getCoroutineScope().launch {
										grdErrorFlow.emit(it1)
									}
								}
							}
						}
					)
				}
			}
		}
	}

	suspend fun connectTunnel(configString: String) {
		val inputString: Reader = StringReader(configString)
		val reader = BufferedReader(inputString)
		try {
			val config: Config = Config.parse(reader)
			if (tunnelName.isNotEmpty()) {
				GRDConnectManager.getTunnelManager().create(tunnelName, config)
				Log.d(TAG, "Creating tunnel...")
				if (GRDConnectManager.getBackend() is GoBackend) {
					Repository.instance.getServerStatus(object : IOnApiResponse {
						override fun onSuccess(any: Any?) {
							val serverStatusOK = any as Boolean
							GRDConnectManager.getCoroutineScope().launch {
								if (serverStatusOK) {
									grdStatusFlow.emit(GRDVPNHelperStatus.SERVER_READY.status)
									val tunnel = GRDConnectManager.getTunnelManager().tunnelMap[tunnelName]
									try {
										grdStatusFlow.emit(GRDVPNHelperStatus.CONNECTING.status)
										tunnel?.setStateAsync(Tunnel.State.UP)
										grdStatusFlow.emit(GRDVPNHelperStatus.CONNECTED.status)

									} catch (e: Throwable) {
										val wireGuardError = ErrorMessages[e]
										e.message?.let {
											grdErrorFlow.emit("Failed to connect VPN tunnel: $wireGuardError")
										}
										val message = context?.getString(R.string.starting_error, wireGuardError)
										Log.e(TAG, message, e)
									}

								} else {
									grdErrorFlow.emit(GRDVPNHelperStatus.SERVER_ERROR.status)
								}
							}
						}

						override fun onError(error: String?) {
							GRDConnectManager.getCoroutineScope().launch {
								error?.let { grdErrorFlow.emit("getServerStatus failed with error $it") }
							}
						}
					})
				}

			} else {
				grdErrorFlow.emit("Tunnel name should not be empty!")
			}

		} catch (e: Exception) {
			e.message?.let { grdErrorFlow.emit("Error parsing config! $it") }
		}
	}

    /**
	 * Function that handles the various tasks required to establish a new VPN connection.
	 * Create new VPN credentials on the selected VPN node with a valid Subscriber Credential,
	 * create a new WireGuard configuration with the VPN credentials from the VPN node
     */
    suspend fun configureFirstTimeUserAndConnect(iOnApiResponse: IOnApiResponse) {
        validSubscriberCredential(object : IOnApiResponse {
            override fun onSuccess(any: Any?) {
                val subscriberCredential = any as String

				val serverManager = GRDServerManager()
				serverManager.preferBetaCapableServers = preferBetaCapableServers
				serverManager.vpnServerFeatureEnvironment = vpnServerFeatureEnvironment
				serverManager.regionPrecision = regionPrecision
				serverManager.selectServerFromRegion(null,
					object : IOnApiResponse {
						override fun onSuccess(any: Any?) {
							val grdSgwServer = any as GRDSGWServer
							grdSgwServer.hostname?.let {
								Repository.instance.initSGWServer(it)

								val keyPair = KeyPair()
								val keyPairGenerated = KeyPair(keyPair.privateKey)

								val requestData = mutableMapOf<String, Any>()
								requestData["transport-protocol"] = GRD_WIREGUARD
								requestData["subscriber-credential"] = subscriberCredential
								requestData["public-key"] = keyPairGenerated.publicKey.toBase64()

								Repository.instance.createNewVPNDevice(requestData, object : IOnApiResponse {
									override fun onSuccess(any: Any?) {
										val credentialMap: Map<String, Any> = Gson().fromJson(any as String, object : TypeToken<MutableMap<String, Any>>() {}.type)
										val grdCredential = GRDCredential.initGRDCredential(GRDTransportProtocol.GRDTransportProtocolType.GRD_TP_WIREGUARD, validForDays, true, credentialMap, grdSgwServer, keyPairGenerated)
										GRDCredentialManager().addOrUpdateCredential(grdCredential)

										Repository.instance.getServerStatus(object : IOnApiResponse {
											override fun onSuccess(any: Any?) {
												val preferredDNSServer = GRDConnectManager.getSharedPrefs().getString(GRD_CONNECT_USER_PREFERRED_DNS_SERVERS, null)
												val appExceptionsList = getAppExceptions()
												val configString = GRDWireGuardConfiguration.getWireGuardConfigString(grdCredential, preferredDNSServer, appExceptionsList, excludeLANTraffic ?: true)

												GRDConnectManager.getCoroutineScope().launch {
													configStringFlow.emit(configString)
													connectTunnel(configString)
												}
												iOnApiResponse.onSuccess(configString)
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

									override fun onError(error: String?) {
										iOnApiResponse.onError(error)
										error?.let {
											GRDConnectManager.getCoroutineScope().launch {
												grdErrorFlow.emit(it)
											}
										}
									}
								})

							} ?: run {
								iOnApiResponse.onError(GRDVPNHelperStatus.SERVER_ERROR.status)
								GRDConnectManager.getCoroutineScope().launch {
									grdErrorFlow.emit(GRDVPNHelperStatus.SERVER_ERROR.status)
								}
							}
						}

						override fun onError(error: String?) {
							iOnApiResponse.onError(error)
							error?.let { Log.d(TAG, it) }
						}
					})
            }

            override fun onError(error: String?) {
                iOnApiResponse.onError(error)
            }
        })
    }

	suspend fun configureFirstTimeUserForServerAndConnect(server: GRDSGWServer, iOnApiResponse: IOnApiResponse) {
		validSubscriberCredential(object : IOnApiResponse {
			override fun onSuccess(any: Any?) {
				val subscriberCredential = any as String
				Repository.instance.initSGWServer(server.hostname!!)

				val keyPair = KeyPair()
				val keyPairGenerated = KeyPair(keyPair.privateKey)

				val requestData = mutableMapOf<String, Any>()
				requestData["transport-protocol"] = GRD_WIREGUARD
				requestData["subscriber-credential"] = subscriberCredential
				requestData["public-key"] = keyPairGenerated.publicKey.toBase64()

				Repository.instance.createNewVPNDevice(requestData,
					object : IOnApiResponse {
						override fun onSuccess(any: Any?) {
							val credentialMap: Map<String, Any> = Gson().fromJson(any as String, object : TypeToken<MutableMap<String, Any>>() {}.type)
							val grdCredential = GRDCredential.initGRDCredential(GRDTransportProtocol.GRDTransportProtocolType.GRD_TP_WIREGUARD, validForDays, true, credentialMap, server, keyPairGenerated)
							GRDCredentialManager().addOrUpdateCredential(grdCredential)

							Repository.instance.getServerStatus(object : IOnApiResponse {
								override fun onSuccess(any: Any?) {
									val preferredDNSServer = GRDConnectManager.getSharedPrefs().getString(GRD_CONNECT_USER_PREFERRED_DNS_SERVERS, null)
									val appExceptionsList = getAppExceptions()
									val configString = GRDWireGuardConfiguration.getWireGuardConfigString(grdCredential, preferredDNSServer, appExceptionsList, excludeLANTraffic ?: true)

									GRDConnectManager.getCoroutineScope().launch {
										configStringFlow.emit(configString)
										connectTunnel(configString)
									}
									iOnApiResponse.onSuccess(configString)
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

			override fun onError(error: String?) {
				iOnApiResponse.onError(error)
			}
		})
	}

	/**
	 * Convenience function to reliably obtain a valid Subscriber Credential
	 * by either fetching the cached Subscriber Credential out of the shared preferences
	 * and checking that it is still valid for use within the grace period or
	 * attempts to obtain a new Subscriber Credential from a Connect API environment
	 */
	suspend fun validSubscriberCredential(iOnApiResponse: IOnApiResponse) {
        val subscriberCredential = GRDSubscriberCredential.currentSubscriberCredential()
        if (subscriberCredential != null && subscriberCredential.isExpired() == false) {
            iOnApiResponse.onSuccess(subscriberCredential.jwt)
            
        } else {
			var requestBody                 = mutableMapOf<String, Any>()
			val currentPEToken              = GRDPEToken.currentPEToken()
			var validationMethod            = GRDSubscriberCredentialValidationMethod.Invalid
			val preferredValidationMethod   = GRDSubscriberCredential.preferredValidationMethod()

			//
			// Determine if the validation method should be locked
			// to a specific kind of validation, or whether it should
			// run in automatic mode
			if (preferredValidationMethod != GRDSubscriberCredentialValidationMethod.Invalid) {
				validationMethod = preferredValidationMethod

			} else if (preferredValidationMethod == GRDSubscriberCredentialValidationMethod.Custom) {
				validationMethod = GRDSubscriberCredentialValidationMethod.Custom
				if (customSubscriberCredentialAuthKeys != null) {
					requestBody = customSubscriberCredentialAuthKeys as MutableMap<String, Any>
				}

			} else {
				GRDLogger.d(TAG, "Subscriber Credential validation automatic mode")
				//
				// Automatic mode attempting to determine what kind of
				//  subscription the user most likely has
				if (currentPEToken != null) {
					validationMethod = GRDSubscriberCredentialValidationMethod.PEToken

				} else {
					validationMethod = GRDSubscriberCredentialValidationMethod.GooglePlayToken
				}
			}

			if (validationMethod == GRDSubscriberCredentialValidationMethod.PEToken) {
				requestBody["validation-method"] = "pe-token"
				if (currentPEToken != null) {
					currentPEToken.token?.let { requestBody["pe-token"] = it }

				} else {
					grdErrorFlow.emit(GRDVPNHelperStatus.MISSING_PET.status)
					iOnApiResponse.onError("Subscriber Credential validation method set to PE-Token but no PE-Token is available on device")
					return
				}

			} else {
				requestBody["validation-method"] = "iap-android"
				val currentPurchase = GRDBillingManager.getCurrentPurchase()
				if (currentPurchase != null) {
					currentPurchase.products.firstOrNull()?.let { requestBody["product-id"] = it }
					requestBody["purchase-token"] = currentPurchase.purchaseToken
					requestBody["bundle-id"] =
						iapApplicationId ?: context?.packageName
								?: ""
					requestBody["product-type"] =
						if (GRDBillingManager.isSubscription(currentPurchase)) "subscription" else "consumable"

				} else {
					iOnApiResponse.onError("No valid purchase found")
					return
				}
			}

			Repository.instance.getSubscriberCredential(
				requestBody,
				object : IOnApiResponse {
					override fun onSuccess(any: Any?) {
						val subCredentialResponse = any as SubscriberCredentialResponse
						subCredentialResponse.subscriberCredential?.let { scs ->
							iOnApiResponse.onSuccess(scs)
						} ?: run {
							iOnApiResponse.onError("Missing subscriberCredential")
							GRDConnectManager.getCoroutineScope().launch {
								grdErrorFlow.emit("Missing subscriberCredential")
							}
						}
					}

					override fun onError(error: String?) {
						error?.let { e ->
							val errorMessage =
								if (e.contains("Failed to query password equivalent token data or password equivalent token does not exist")) {
									Constants.SUBSCRIBER_CREDENTIAL_FAIL_PET
								} else {
									e
								}
							iOnApiResponse.onError(errorMessage)
							GRDConnectManager.getCoroutineScope().launch {
								grdErrorFlow.emit(errorMessage)
							}
						}
					}
				})
        }
    }

	/**
	 * Convenience function removing the Subscriber Credential
	 * from the keystore, the main credential out of GRDCredentialManager
	 * as well as GRDLogger specific logs if any were previously generated
	 */
	fun clearLocalCache() {
        GRDConnectManager.getSharedPrefsEditor().remove(GRD_SUBSCRIBER_CREDENTIAL)?.apply()
		GRDCredentialManager().deleteMainCredential()
		GRDLogger.deleteAllLogs()
    }

    /* Handles VPN credential invalidation on the server and removal locally on the device. */
    suspend fun clearVPNConfiguration() {
        val mainCredentials = GRDCredentialManager().getMainCredentials()
        validSubscriberCredential(object : IOnApiResponse {
            override fun onSuccess(any: Any?) {
                if (mainCredentials?.clientId.isNullOrEmpty() == false) {
                    val requestBody = mutableMapOf<String, Any>()

                    requestBody["subscriber-credential"]    = any as String
                    requestBody["api-auth-token"]           = mainCredentials.apiAuthToken.toString()
                    
                    Repository.instance.invalidateVPNCredentials(mainCredentials.clientId.toString(), requestBody, object : IOnApiResponse {
                        override fun onSuccess(any: Any?) {
                            GRDConnectManager.getCoroutineScope().launch {
                                grdStatusFlow.emit(GRDVPNHelperStatus.VPN_CREDENTIALS_INVALIDATED.status)
                            }
                        }

                        override fun onError(error: String?) {
                            GRDConnectManager.getCoroutineScope().launch {
                                grdErrorFlow.emit(Constants.VPN_CREDENTIALS_INVALIDATION_ERROR)
                            }
                        }
                    })
                    
                    //
                    // Regardless of the outcome of the API request
                    // ensure that we clear everything else locally
                    // on the device
					GRDCredentialManager().deleteMainCredential()
                    
                } else {
                    GRDConnectManager.getCoroutineScope().launch {
                        grdErrorFlow.emit("VPN credential 'device-id' missing")
                    }
                }
            }

            override fun onError(error: String?) {
                GRDConnectManager.getCoroutineScope().launch {
                    error?.let { grdErrorFlow.emit(it) }
                }
            }
        })
    }

    /*  Checks whether the a valid GRDCredential object is present on the device and verifies
        properties of the object are not nil or an empty string. This function needs to be called
        in the high level API code paths prior to trying to re-establish a VPN connection to check
        whether all the information is present on device. */
    fun activeConnectionPossible(credential: GRDCredential?): Boolean {
		if (credential != null) {
			return !credential.hostname.isNullOrEmpty() && !credential.apiAuthToken.isNullOrEmpty() && !credential.devicePublicKey.isNullOrEmpty() && !credential.devicePrivateKey.isNullOrEmpty() && !credential.clientId.isNullOrEmpty()
		}

		return false
    }

    fun setVariables() {
        if (connectAPIHostname.isEmpty()) {
            connectAPIHostname = Constants.kGRDConnectAPIHostname
        }
        if (tunnelName.isEmpty()) {
            GRDConnectManager.getCoroutineScope().launch {
                grdErrorFlow.emit("Tunnel name is empty!")
            }
        }
        if (connectPublishableKey.isEmpty()) {
            GRDConnectManager.getCoroutineScope().launch {
                grdErrorFlow.emit("Connect public key is empty!")
            }
        }
        preferredValidationMethod = GRDSubscriberCredential.preferredValidationMethod()

        Repository.instance.connectPublishableKey = connectPublishableKey
        Repository.instance.initConnectAPIServer()
        Repository.instance.initConnectSubscriberServer(connectAPIHostname)

		val mainCredentials = GRDCredentialManager().getMainCredentials()
		if (mainCredentials != null) {
			Repository.instance.initSGWServer(mainCredentials.hostname.toString())
		}
    }

    private fun handleOkHttpClient(status: String) {
        if (Repository.instance.httpClient == Repository.instance.defaultHTTPClient()) {
            Repository.instance.httpClient = null
            Repository.instance.initConnectAPIServer()
            Repository.instance.initConnectSubscriberServer(connectAPIHostname)

            val hostname = GRDCredentialManager().getMainCredentials()?.hostname
            if (!hostname.isNullOrEmpty()) {
                Repository.instance.initSGWServer(hostname)
            }
        }
        Log.d(TAG, status)
        Log.d(
            TAG, "httpClient: ${Repository.instance.httpClient}, " +
                    "Default httpClient: ${Repository.instance.defaultHTTPClient()}, " +
                    "Host name: ${GRDCredentialManager().getMainCredentials()?.hostname}"
        )
    }

	/**
	 * Resets all the values set in GuardianConnect either encrypted into the Keystore or unencrypted in the SharedPreferences
	 */
    fun resetAllGuardianConnectValues() {
        GRDConnectManager.getSharedPrefsEditor().clear().apply()
    }

    enum class GRDVPNHelperStatus(val status: String) {
        UNKNOWN("VPN status: unknown."),
        MISSING_PET("PEToken is missing!"),
        ERROR_CONNECTING("Connecting error has occurred!"),
        DISCONNECTED("VPN status: disconnected!"),
        DISCONNECTING("VPN status: disconnecting..."),
        CONNECTING("VPN status: connecting..."),
        CONNECTED("VPN status: connected!"),
        MIGRATING("VPN status: migrating..."),
        VPN_CREDENTIALS_INVALIDATED("VPN status: credentials invalidated!"),
        SERVER_READY("Server status OK."),
        SERVER_ERROR("Server error!"),
        TUNNEL_CONNECTED("Connection Successful!")
    }

    fun allRegions(onRegionListener: GRDServerManager.OnRegionListener) {
		val serverManager = GRDServerManager()
		serverManager.preferBetaCapableServers = preferBetaCapableServers
		serverManager.vpnServerFeatureEnvironment = vpnServerFeatureEnvironment
		serverManager.regionPrecision = regionPrecision
		serverManager.returnAllAvailableRegions(onRegionListener)
    }

    val configStringFlow        = MutableSharedFlow<String>()
    val grdMsgFlow              = MutableSharedFlow<String>()
    val grdErrorFlow            = MutableSharedFlow<String>()
    val grdVPNPermissionFlow    = MutableSharedFlow<Intent>()
    val grdStatusFlow           = MutableSharedFlow<String>()
}
