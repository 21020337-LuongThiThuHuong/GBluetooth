package com.example.gbluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gbluetooth.databinding.ActivityBluetoothBinding
import com.google.android.material.snackbar.Snackbar

class BluetoothActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBluetoothBinding
    private lateinit var deviceAdapter: DeviceAdapter
    private val viewModel: BluetoothViewModel by viewModels()

    private val bluetoothReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                when (intent.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        viewModel.checkBluetoothStatus()
                    }

                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            viewModel.addDeviceToList(it, binding.showUnknownDevice.isChecked)
                        }
                    }

                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        viewModel.updateDeviceList(binding.showUnknownDevice.isChecked)
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.myDeviceName.text = Build.MODEL

        setupRecyclerView()

        viewModel.devicesList.observe(this) { devices ->
            deviceAdapter.updateDevices(devices)
        }

        viewModel.isBluetoothEnabled.observe(this) { isEnabled ->
            binding.switchBluetooth.isChecked = isEnabled
            if (isEnabled) {
                viewModel.startDiscovery()
            } else {
                Snackbar.make(binding.root, "Bluetooth is off", Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.permissionGranted.observe(this) { granted ->
            if (granted) {
                viewModel.startDiscovery()
            } else {
                Snackbar
                    .make(
                        binding.root,
                        "Bluetooth permissions not granted",
                        Snackbar.LENGTH_SHORT,
                    ).show()
                viewModel.requestBluetoothPermissions()
            }
        }

        binding.reload.setOnClickListener {
            viewModel.startDiscovery()
            viewModel.updateDeviceList(binding.showUnknownDevice.isChecked)
        }

        binding.showUnknownDevice.setOnCheckedChangeListener { _, _ ->
            viewModel.updateDeviceList(binding.showUnknownDevice.isChecked)
        }

        binding.switchBluetooth.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleBluetooth(isChecked)
        }

        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))

        viewModel.requestBluetoothPermissions()
    }

    private fun setupRecyclerView() {
        deviceAdapter =
            DeviceAdapter(mutableListOf()) { device ->
                // Handle device click
            }
        binding.deviceList.layoutManager = LinearLayoutManager(this)
        binding.deviceList.adapter = deviceAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
    }
}
