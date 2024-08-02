package com.example.gbluetooth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gbluetooth.databinding.DeviceItemBinding

data class Device(
    val name: String,
    val address: String,
    val typeDrawable: Int,
    var isConnected: Boolean
)

class DeviceAdapter(
    private val devicesList: List<Device>,
    private val onDeviceClick: (Device) -> Unit // Thay đổi tham số listener
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = DeviceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devicesList[position]
        holder.bind(device)
        holder.itemView.setOnClickListener {
            onDeviceClick(device) // Gọi listener khi item được nhấp chuột
        }
    }

    override fun getItemCount() = devicesList.size

    class DeviceViewHolder(private val binding: DeviceItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(device: Device) {
            binding.deviceName.text = device.name
            binding.deviceStatus.text = if (device.isConnected) "Đã kết nối" else "Chưa kết nối"
            binding.deviceType.setImageResource(device.typeDrawable)
        }
    }
}
