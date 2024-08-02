package com.example.gbluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gbluetooth.databinding.ActivityBluetoothBinding
import com.google.android.material.snackbar.Snackbar

class BluetoothActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBluetoothBinding
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val devicesList = mutableListOf<String>()
    private lateinit var deviceAdapter: DeviceAdapter

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_ON) {
                        checkBluetoothStatus()
                    }
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        val deviceName = it.name ?: "Unknown Device"
                        val deviceAddress = it.address
                        devicesList.add("$deviceName\n$deviceAddress")
                        deviceAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBluetoothBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Snackbar.make(binding.root, "Bluetooth không được hỗ trợ trên thiết bị này", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        } else {
            setupRecyclerView()
            checkBluetoothStatus()
            registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
            registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
            requestBluetoothPermissions()
        }

        binding.connectBtn.setOnClickListener {
            toggleBluetooth()
        }
    }

    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter(devicesList)
        binding.deviceList.layoutManager = LinearLayoutManager(this)
        binding.deviceList.adapter = deviceAdapter
    }

    @SuppressLint("MissingPermission")
    private fun toggleBluetooth() {
        bluetoothAdapter?.let {
            if (it.isEnabled) {
                it.disable()
                Snackbar.make(binding.root, "Đang tắt Bluetooth", Snackbar.LENGTH_SHORT).show()
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                Snackbar.make(binding.root, "Đang bật Bluetooth", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkBluetoothStatus() {
        if (bluetoothAdapter?.isEnabled == true) {
            binding.connectBtn.text = "Disconnect"
            binding.connectBtn.setBackgroundColor(getColor(R.color.purple_700))
            startDiscovery()
        } else {
            binding.connectBtn.text = "Connect"
            binding.connectBtn.setBackgroundColor(getColor(R.color.teal_200))
            devicesList.clear()
            deviceAdapter.notifyDataSetChanged()
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION // Thêm quyền này để đảm bảo việc quét thiết bị hoạt động
        )
        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startDiscovery()
            } else {
                Snackbar.make(binding.root, "Permission denied", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startDiscovery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter?.cancelDiscovery()
            }
            bluetoothAdapter?.startDiscovery()
        } else {
            requestBluetoothPermissions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_PERMISSIONS = 2
    }
}
