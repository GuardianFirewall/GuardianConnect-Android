package com.guardianconnect.demo

import GRDDNSProxy
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import com.guardianconnect.GRDConnectManager
import com.guardianconnect.GRDDNSHelper
import com.guardianconnect.GRDVPNHelper
import kotlinx.coroutines.launch

class GRDDNSActivity : AppCompatActivity() {
    private var vpnServiceIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grddnsactivity)
        vpnServiceIntent = Intent(this, GRDDNSProxy::class.java)

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
            GRDDNSHelper.prepareGRDDNSProxyPermissions()
            startService(vpnServiceIntent)
            btnStartVpn.visibility = View.GONE
            btnStopVpn.visibility = View.VISIBLE
        }
        btnStopVpn.setOnClickListener {
            stopService(vpnServiceIntent)
            btnStartVpn.visibility = View.VISIBLE
            btnStopVpn.visibility = View.GONE
        }

        GRDConnectManager.getCoroutineScope().launch {
            GRDVPNHelper.grdStatusFlow.collect {
                when (it) {
                    GRDVPNHelper.GRDVPNHelperStatus.CONNECTED.name -> {
                        btnStartVpn.visibility = View.GONE
                        btnStopVpn.visibility = View.VISIBLE
                    }
                    GRDVPNHelper.GRDVPNHelperStatus.DISCONNECTED.name -> {
                        btnStartVpn.visibility = View.VISIBLE
                        btnStopVpn.visibility = View.GONE
                    }
                }
            }
        }
    }

    private val permissionActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            startService(vpnServiceIntent)
        }
}