package com.example.gbluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
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
    private val devicesList = mutableListOf<Device>()
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
                        val deviceName = it.name
                        if (binding.showUnknownDevice.isChecked || deviceName != null) { // Thêm kiểm tra trạng thái của switch
                            val displayName = deviceName ?: it.address
                            val deviceAddress = it.address
                            val deviceTypeDrawable = when {
                                it.bluetoothClass.deviceClass == BluetoothClass.Device.PHONE_SMART  -> R.drawable.phone
                                it.bluetoothClass.deviceClass == BluetoothClass.Device.COMPUTER_LAPTOP -> R.drawable.computer
                                it.bluetoothClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> R.drawable.headphone
                                it.bluetoothClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY -> R.drawable.game
                                else -> R.drawable.bluetooth
                            }
                            val isConnected = isDeviceConnected(it) // Kiểm tra trạng thái kết nối
                            devicesList.add(Device(displayName, deviceAddress, deviceTypeDrawable, isConnected))
                            deviceAdapter.notifyDataSetChanged()
                        }
                    }
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        when (it.bondState) {
                            BluetoothDevice.BOND_BONDED -> {
                                Snackbar.make(binding.root, "Device paired", Snackbar.LENGTH_SHORT).show()
                            }
                            BluetoothDevice.BOND_BONDING -> {
                                Snackbar.make(binding.root, "Pairing...", Snackbar.LENGTH_SHORT).show()
                            }
                            BluetoothDevice.BOND_NONE -> {
                                Snackbar.make(binding.root, "Pairing failed", Snackbar.LENGTH_SHORT).show()
                            }
                        }
                        updateDeviceList()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBluetoothBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.myDeviceName.text = Build.MODEL

        binding.reload.setOnClickListener {
            reloadDevices()
        }

        binding.showUnknownDevice.setOnCheckedChangeListener { _, isChecked ->
            updateDeviceList()
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Snackbar.make(binding.root, "Bluetooth không được hỗ trợ trên thiết bị này", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        } else {
            setupRecyclerView()
            checkBluetoothStatus()
            registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
            registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
            registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            requestBluetoothPermissions()
        }

        // Setup switch listener
        binding.switchBluetooth.setOnCheckedChangeListener { _, isChecked ->
            toggleBluetooth(isChecked)
        }
    }

    @SuppressLint("MissingPermission")
    private fun reloadDevices() {
        devicesList.clear()
        addConnectedDevices()
        deviceAdapter.notifyDataSetChanged()
        startDiscovery()
    }

    @SuppressLint("MissingPermission")
    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter(devicesList) { device ->
            val deviceAddress = device.address
            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            bluetoothDevice?.let {
                // Request pairing if necessary
                if (it.bondState != BluetoothDevice.BOND_BONDED) {
                    it.createBond()
                } else {
                    // Already paired, initiate connection if needed
                    Snackbar.make(binding.root, "Device already paired", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        binding.deviceList.layoutManager = LinearLayoutManager(this)
        binding.deviceList.adapter = deviceAdapter
    }

    @SuppressLint("MissingPermission")
    private fun toggleBluetooth(enable: Boolean) {
        bluetoothAdapter?.let {
            if (enable) {
                if (!it.isEnabled) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                    Snackbar.make(binding.root, "Đang bật Bluetooth", Snackbar.LENGTH_SHORT).show()
                }
            } else {
                if (it.isEnabled) {
                    it.disable()
                    Snackbar.make(binding.root, "Đang tắt Bluetooth", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkBluetoothStatus() {
        if (bluetoothAdapter?.isEnabled == true) {
            binding.switchBluetooth.isChecked = true
            startDiscovery()
        } else {
            binding.switchBluetooth.isChecked = false
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

    @SuppressLint("MissingPermission")
    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        return device.bondState == BluetoothDevice.BOND_BONDED && bluetoothAdapter?.bondedDevices?.contains(device) == true
    }

    @SuppressLint("MissingPermission")
    private fun updateDeviceList() {
        devicesList.clear()
        val bondedDevices = bluetoothAdapter?.bondedDevices
        bondedDevices?.forEach { device ->
            val deviceName = device.name
            if (binding.showUnknownDevice.isChecked == false || deviceName != null) {
                val displayName = deviceName ?: device.address
                val deviceAddress = device.address
                val deviceTypeDrawable = when {
                    device.bluetoothClass.deviceClass == BluetoothClass.Device.PHONE_SMART  -> R.drawable.phone
                    device.bluetoothClass.deviceClass == BluetoothClass.Device.COMPUTER_LAPTOP -> R.drawable.computer
                    device.bluetoothClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> R.drawable.headphone
                    device.bluetoothClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY -> R.drawable.game
                    else -> R.drawable.bluetooth
                }
                devicesList.add(Device(displayName, deviceAddress, deviceTypeDrawable, isDeviceConnected(device)))
            }
        }
        deviceAdapter.notifyDataSetChanged()
    }

    @SuppressLint("MissingPermission")
    private fun addConnectedDevices() {
        val bondedDevices = bluetoothAdapter?.bondedDevices
        bondedDevices?.forEach { device ->
            val deviceName = device.name ?: device.address
            val deviceAddress = device.address
            val deviceTypeDrawable = when {
                device.bluetoothClass.deviceClass == BluetoothClass.Device.PHONE_SMART  -> R.drawable.phone
                device.bluetoothClass.deviceClass == BluetoothClass.Device.COMPUTER_LAPTOP -> R.drawable.computer
                device.bluetoothClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> R.drawable.headphone
                device.bluetoothClass.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY -> R.drawable.game
                else -> R.drawable.bluetooth
            }
            devicesList.add(Device(deviceName, deviceAddress, deviceTypeDrawable, true))
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
