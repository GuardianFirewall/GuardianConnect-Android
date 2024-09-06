package com.guardianconnect.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.guardianconnect.GRDPEToken
import com.guardianconnect.GRDRegion
import com.guardianconnect.GRDWireGuardConfiguration
import com.guardianconnect.helpers.GRDVPNHelper
import com.guardianconnect.managers.GRDConnectManager
import com.guardianconnect.managers.GRDServerManager
import com.guardianconnect.util.Constants
import com.guardianconnect.util.applicationScope
import com.wireguard.android.backend.Tunnel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private var myReceiver: MyBroadcastReceiver? = null
    private lateinit var etConfig: EditText
    private lateinit var btnStartTunnel: Button
    private lateinit var btnStopTunnel: Button
    private lateinit var btnResetConfiguration: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var etPeToken: EditText
    private lateinit var btnPeToken: Button
    private lateinit var btnDNSProxy: Button
    private lateinit var btnConnectSubscriber: Button
    private var adapter: AllRegionsAdapter? = null
    private var rvList: RecyclerView? = null
    private val regionsAdapterList: ArrayList<GRDRegion> = ArrayList()
    private lateinit var permissionActivityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        etConfig = findViewById(R.id.etConfig)

        permissionActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                // This completion handler is called after the user taps on allow in the OS modal alert
                // from createAndStartTunnel() & the grdVPNPermissionFlow
                // To ensure that the user is actually going to be connected createAndStartTunnel
                // needs to be called again
                lifecycleScope.launch {
                    GRDVPNHelper.createAndStartTunnel()
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                    }
                }
            }

        val intentFilter = IntentFilter()
        intentFilter.addAction("com.guardianconnect.action.GRD_REFRESH_TUNNEL_STATES")
        intentFilter.addAction("com.guardianconnect.action.GRD_SET_TUNNEL_UP")
        intentFilter.addAction("com.guardianconnect.action.GRD_SET_TUNNEL_DOWN")
        myReceiver = MyBroadcastReceiver()
        ContextCompat.registerReceiver(
            applicationContext,
            myReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        initGRDVPNHelper()
        initUI()
        initRecyclerView()
        loadRegionsList()
        collectFlowStates()
        setOnClick()
    }

    private fun setOnClick() {
        btnStartTunnel.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                GRDVPNHelper.createAndStartTunnel()
            }
            btnResetConfiguration.isClickable = true
        }

        btnStopTunnel.setOnClickListener {
            lifecycleScope.launch {
                GRDVPNHelper.stopTunnel()
            }
            btnStartTunnel.visibility = View.VISIBLE
            btnStopTunnel.visibility = View.GONE
            if (!etConfig.text.isNullOrEmpty()) {
                btnResetConfiguration.isClickable = true
            }
        }

        btnResetConfiguration.setOnClickListener {
            if (!etConfig.text.isNullOrEmpty()) {
                btnResetConfiguration.isClickable = true
            }
            lifecycleScope.launch {
                GRDVPNHelper.stopClearTunnel()
            }
            btnResetConfiguration.isClickable = false
            btnStartTunnel.visibility = View.VISIBLE
            btnStopTunnel.visibility = View.GONE
            etConfig.setText("")
            adapter?.setSelectedPosition(0)
        }

        btnPeToken.setOnClickListener {
            if (!etPeToken.text.isNullOrEmpty()) {
                savePeToken(etPeToken.text.toString())
            } else {
                progressBar.visibility = View.GONE
                Log.d("MainActivity", GRDVPNHelper.GRDVPNHelperStatus.MISSING_PET.status)
            }
        }

        val storedPET = GRDPEToken.instance.retrievePEToken()
        etPeToken.setText(storedPET)

        btnDNSProxy.setOnClickListener {
            startActivity(Intent(this, GRDDNSActivity::class.java))
        }

        btnConnectSubscriber.setOnClickListener {
            val connectSubscriberFragment = ConnectSubscriberFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, connectSubscriberFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun collectFlowStates() {
        GRDConnectManager.getCoroutineScope().launch {
            GRDVPNHelper.configStringFlow.collect { configString ->
                withContext(Dispatchers.Main) {
                    etConfig.setText(configString)
                }
            }
        }

        GRDConnectManager.getCoroutineScope().launch {
            GRDVPNHelper.grdStatusFlow.collect {
                Log.d("MainActivity", it)
                when (it) {
                    GRDVPNHelper.GRDVPNHelperStatus.CONNECTED.status -> {
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            btnStartTunnel.visibility = View.GONE
                            btnStopTunnel.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
        GRDConnectManager.getCoroutineScope().launch {
            GRDVPNHelper.grdErrorFlow.collect {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                }
                Log.e("MainActivity", it)
            }
        }
        GRDConnectManager.getCoroutineScope().launch {
            GRDVPNHelper.grdVPNPermissionFlow.collect {
                permissionActivityResultLauncher.launch(it)
            }
        }
        GRDConnectManager.getCoroutineScope().launch {
            GRDVPNHelper.grdMsgFlow.collect {
                Log.d("MainActivity", it)
            }
        }
    }

    private fun initGRDVPNHelper() {
        GRDVPNHelper.tunnelName = "TUNNEL_NAME"
        GRDVPNHelper.connectAPIHostname = "connect-api.guardianapp.com"
        GRDVPNHelper.setVariables()
        GRDVPNHelper.initHelper(this)
        GRDVPNHelper.initRegion()
    }

    override fun onPostResume() {
        super.onPostResume()
        val configString =
            GRDVPNHelper.grdCredentialManager?.getMainCredentials().let {
                it?.let { it1 ->
                    GRDWireGuardConfiguration().getWireGuardConfigString(
                        it1,
                        GRDConnectManager.getSharedPrefs()
                            .getString(Constants.GRD_CONNECT_USER_PREFERRED_DNS_SERVERS, null),
                        GRDVPNHelper.appExceptions,
                        GRDVPNHelper.excludeLANTraffic ?: true
                    )
                }
            }
        if (!configString.isNullOrEmpty()) etConfig.setText(configString)
    }

    private fun initRecyclerView() {
        adapter = AllRegionsAdapter(regionsAdapterList, onClickListener(), applicationContext)
        rvList?.setHasFixedSize(true)
        rvList?.visibility = View.VISIBLE
        rvList?.layoutManager = LinearLayoutManager(this)
        rvList?.adapter = adapter
        rvList?.addItemDecoration(
            DividerItemDecoration(
                rvList?.context, DividerItemDecoration.VERTICAL
            )
        )
    }

    private fun onClickListener(): IOnClickListener {
        return object : IOnClickListener {
            override fun onClick(grdRegion: GRDRegion?) {
                GRDServerManager.setPreferredRegion(grdRegion)
                lifecycleScope.launch {
                    GRDVPNHelper.updateTunnelRegion()
                }
            }
        }
    }

    private fun setGRDRegionPrecisionDefault(grdServerManager: GRDServerManager) {
        grdServerManager.regionPrecision = Constants.kGRDRegionPrecisionDefault
    }

    private fun loadRegionsList() {
        GRDVPNHelper.grdServerManager?.let { setGRDRegionPrecisionDefault(it) }
        GRDVPNHelper.grdServerManager?.returnAllAvailableRegions(object :
            GRDServerManager.OnRegionListener {
            override fun onRegionsAvailable(listOfGRDRegions: List<GRDRegion>) {
                regionsAdapterList.addAll(listOfGRDRegions)
                adapter?.notifyDataSetChanged()
                progressBar.visibility = View.GONE
            }
        })
    }

    private fun initUI() {
        btnStartTunnel = findViewById(R.id.btnStartTunnel)
        btnStopTunnel = findViewById(R.id.btnStopTunnel)
        btnResetConfiguration = findViewById(R.id.btnResetConfiguration)
        progressBar = findViewById(R.id.progressBar)
        rvList = findViewById(R.id.rvList)
        etPeToken = findViewById(R.id.etPeToken)
        btnPeToken = findViewById(R.id.btnPeToken)
        btnDNSProxy = findViewById(R.id.btnDNSProxy)
        btnConnectSubscriber = findViewById(R.id.btnConnectSubscriber)

        if (GRDVPNHelper.isTunnelRunning()) {
            btnStartTunnel.visibility = View.GONE
            btnStopTunnel.visibility = View.VISIBLE
        } else {
            btnStartTunnel.visibility = View.VISIBLE
            btnStopTunnel.visibility = View.GONE
        }
        if (!etConfig.text.isNullOrEmpty()) {
            btnResetConfiguration.isClickable = true
        }
    }

    private fun savePeToken(peToken: String) {
        GRDPEToken.instance.storePEToken(peToken)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (myReceiver != null) applicationContext.unregisterReceiver(myReceiver)
    }

    class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("MainActivity", "Intent action name: ${intent.action}")

            applicationScope.launch {
                val manager = GRDConnectManager.getTunnelManager()
                if (intent == null) return@launch
                val action = intent.action ?: return@launch
                if ("com.guardianconnect.action.GRD_REFRESH_TUNNEL_STATES" == action) {
                    manager.refreshTunnelStates()
                    return@launch
                }
                val state: Tunnel.State = when (action) {
                    "com.guardianconnect.action.GRD_SET_TUNNEL_UP" -> Tunnel.State.UP
                    "com.guardianconnect.action.GRD_SET_TUNNEL_DOWN" -> Tunnel.State.DOWN
                    else -> return@launch
                }
                val tunnelName = intent.getStringExtra("tunnel") ?: return@launch
                val tunnels = manager.getTunnels()
                val tunnel = tunnels[tunnelName] ?: return@launch
                try {
                    manager.setTunnelState(tunnel, state)
                } catch (e: Throwable) {
                    GRDVPNHelper.grdErrorFlow.emit(e.stackTraceToString())
                }
            }
        }
    }
}
