package com.guardianconnect.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.guardianconnect.managers.GRDConnectManager
import com.guardianconnect.helpers.GRDDNSHelper
import com.guardianconnect.helpers.GRDVPNHelper
import kotlinx.coroutines.launch

class GRDDNSActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grddnsactivity)

        GRDDNSHelper.initGRDDNSHelper(applicationContext)

        val btnStartVpn = findViewById<Button>(R.id.btnStartVPN)
        val btnStopVpn = findViewById<Button>(R.id.btnStopVPN)


        GRDConnectManager.getCoroutineScope().launch {
            GRDVPNHelper.grdVPNPermissionFlow.collect {
                permissionActivityResultLauncher.launch(it)
            }
        }

        btnStartVpn.visibility = View.VISIBLE
        btnStopVpn.visibility = View.GONE
        btnStartVpn.setOnClickListener {
            lifecycleScope.launch {
                GRDDNSHelper.prepareGRDDNSProxyPermissions()
            }
            btnStartVpn.visibility = View.GONE
            btnStopVpn.visibility = View.VISIBLE
        }
        btnStopVpn.setOnClickListener {
            GRDDNSHelper.stopGRDDNSProxyService()
            btnStartVpn.visibility = View.VISIBLE
            btnStopVpn.visibility = View.GONE
        }

        GRDConnectManager.getCoroutineScope().launch {
            GRDVPNHelper.grdStatusFlow.collect {
                when (it) {
                    GRDVPNHelper.GRDVPNHelperStatus.CONNECTED.status -> {
                        btnStartVpn.visibility = View.GONE
                        btnStopVpn.visibility = View.VISIBLE
                    }
                    GRDVPNHelper.GRDVPNHelperStatus.DISCONNECTED.status -> {
                        btnStartVpn.visibility = View.VISIBLE
                        btnStopVpn.visibility = View.GONE
                    }
                }
            }
        }
    }

    private val permissionActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            lifecycleScope.launch {
                GRDDNSHelper.prepareGRDDNSProxyPermissions()
            }
        }
}