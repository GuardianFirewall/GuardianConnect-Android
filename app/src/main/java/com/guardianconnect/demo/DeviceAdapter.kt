package com.guardianconnect.demo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.guardianconnect.GRDConnectDevice

class DeviceAdapter(
    private var devices: List<GRDConnectDevice>,
    private val onDeleteClick: (GRDConnectDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewDeviceInfo: TextView = view.findViewById(R.id.tvDeviceInfo)
        val buttonDeleteDevice: ImageButton = view.findViewById(R.id.btnDeleteDevice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.textViewDeviceInfo.text =
            device.toString()

        holder.buttonDeleteDevice.setOnClickListener {
            onDeleteClick(device)
        }
    }

    override fun getItemCount(): Int = devices.size

    fun updateDevices(newDevices: List<GRDConnectDevice>) {
        devices = newDevices
        notifyDataSetChanged()
    }
}