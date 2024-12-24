package com.guardianconnect.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.guardianconnect.GRDDNSProxy

@SuppressLint("StaticFieldLeak")
object GRDDNSHelper {

    private var context: Context? = null

    fun initGRDDNSHelper(context: Context) {
        this.context = context
    }

    suspend fun prepareGRDDNSProxyPermissions() {
        val intent = VpnService.prepare(context)
        if (intent != null) {
                GRDVPNHelper.grdVPNPermissionFlow.emit(intent)
        } else {
            context?.let { startGRDDNSProxyService(it) }
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