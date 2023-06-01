package com.guardianconnect

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.VpnService
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object GRDDNSHelper {

    private var context: Context? = null

    fun initGRDDNSHelper(context: Context) {
        this.context = context
    }

    fun prepareGRDDNSProxyPermissions() {
        GRDConnectManager.getCoroutineScope().launch {
            val intent = VpnService.prepare(context)
            if (intent != null) {
                GRDVPNHelper.grdVPNPermissionFlow.emit(intent)
            } else {
                context?.let { startGRDDNSProxyService(it) }
            }
        }
    }

    private fun startGRDDNSProxyService(context: Context) {
        val grdDNSProxyService = Intent(context.applicationContext, GRDDNSProxy::class.java)
        context.applicationContext?.startService(grdDNSProxyService)
    }
    fun stopGRDDNSProxyService() {
        GRDDNSProxy.stopVpnConnection()
    }
}