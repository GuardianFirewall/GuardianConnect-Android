package com.guardianconnect.demo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.guardianconnect.GRDConnectDevice
import com.guardianconnect.GRDConnectSubscriber
import com.guardianconnect.api.IOnApiResponse

class ConnectDevicesFragment : Fragment() {

    private lateinit var recyclerViewDevices: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter
    private var devices = mutableListOf<GRDConnectDevice>()
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_connect_devices, container, false)
        recyclerViewDevices = view.findViewById(R.id.rvDeviceList)
        progressBar = view.findViewById(R.id.progressBar)

        setupRecyclerView()
        fetchDevices()

        return view
    }

    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter(devices) { device ->
            deleteDevice(device)
        }
        recyclerViewDevices.layoutManager = LinearLayoutManager(context)
        recyclerViewDevices.adapter = deviceAdapter
    }

    private fun fetchDevices() {
        progressBar.visibility = View.VISIBLE
        val currentSubscriber = GRDConnectSubscriber.currentSubscriber()
        currentSubscriber?.allDevices(object : IOnApiResponse {
            override fun onSuccess(any: Any?) {
                progressBar.visibility = View.GONE
                val devices = any as ArrayList<GRDConnectDevice>
                devices.toMutableList()
                deviceAdapter.updateDevices(devices)
            }

            override fun onError(error: String?) {
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Error fetching devices", Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun deleteDevice(device: GRDConnectDevice) {
        val currentSubscriber = GRDConnectSubscriber.currentSubscriber()
        currentSubscriber?.let {
            GRDConnectDevice().deleteConnectDevice(it, device, object : IOnApiResponse {
                override fun onSuccess(any: Any?) {
                    fetchDevices()
                    Toast.makeText(context, "Device deleted", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: String?) {
                    Toast.makeText(context, "Error deleting device", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}