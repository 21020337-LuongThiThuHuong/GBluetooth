package com.example.gbluetooth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gbluetooth.databinding.DeviceItemBinding

class DeviceAdapter(
    private var devicesList: List<Device>,
    private val onDeviceClick: (Device) -> Unit,
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): DeviceViewHolder {
        val binding = DeviceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: DeviceViewHolder,
        position: Int,
    ) {
        val device = devicesList[position]
        holder.bind(device)
        holder.itemView.setOnClickListener {
            onDeviceClick(device)
        }
    }

    override fun getItemCount() = devicesList.size

    fun updateDevices(newDevicesList: List<Device>) {
        devicesList = newDevicesList
        notifyDataSetChanged()
    }

    class DeviceViewHolder(
        private val binding: DeviceItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(device: Device) {
            binding.deviceName.text = device.name
            binding.deviceStatus.text = if (device.isConnected) "Connected" else "Disconnected"
            binding.deviceType.setImageResource(device.typeDrawable)
        }
    }
}
