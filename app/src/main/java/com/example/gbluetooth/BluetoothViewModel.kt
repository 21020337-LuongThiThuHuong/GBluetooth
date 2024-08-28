package com.example.gbluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class BluetoothViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val _devicesList = MutableLiveData<MutableList<Device>>()
    val devicesList: LiveData<MutableList<Device>> get() = _devicesList

    private val _isBluetoothEnabled = MutableLiveData<Boolean>()
    val isBluetoothEnabled: LiveData<Boolean> get() = _isBluetoothEnabled

    private val _permissionGranted = MutableLiveData<Boolean>()
    val permissionGranted: LiveData<Boolean> get() = _permissionGranted

    private val context = getApplication<Application>().applicationContext

    init {
        _devicesList.value = mutableListOf()
        checkBluetoothStatus()
        requestBluetoothPermissions()
    }

    fun checkBluetoothStatus() {
        _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
    }

    fun requestBluetoothPermissions() {
        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }

        val granted =
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }

        _permissionGranted.value = granted
    }

    @SuppressLint("MissingPermission")
    fun addDeviceToList(
        device: BluetoothDevice,
        showUnknownDevice: Boolean,
    ) {
        val deviceName = device.name
        if (showUnknownDevice || deviceName != null) {
            val displayName = deviceName ?: device.address
            val deviceAddress = device.address
            val deviceTypeDrawable =
                when (device.bluetoothClass?.deviceClass) {
                    BluetoothClass.Device.PHONE_SMART -> R.drawable.phone
                    BluetoothClass.Device.COMPUTER_LAPTOP -> R.drawable.computer
                    BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> R.drawable.headphone
                    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY -> R.drawable.game
                    else -> R.drawable.bluetooth
                }
            val deviceEntry =
                Device(displayName, deviceAddress, deviceTypeDrawable, isDeviceConnected(device))
            _devicesList.value?.add(deviceEntry)
            _devicesList.value = _devicesList.value // Trigger LiveData update
        }
    }

    @SuppressLint("MissingPermission")
    fun updateDeviceList(showUnknownDevice: Boolean) {
        if (_permissionGranted.value == true) {
            _devicesList.value?.clear()
            val bondedDevices = bluetoothAdapter?.bondedDevices
            bondedDevices?.forEach { device ->
                val deviceName = device.name
                if (showUnknownDevice || deviceName != null) {
                    val displayName = deviceName ?: device.address
                    val deviceAddress = device.address
                    val deviceTypeDrawable =
                        when (device.bluetoothClass?.deviceClass) {
                            BluetoothClass.Device.PHONE_SMART -> R.drawable.phone
                            BluetoothClass.Device.COMPUTER_LAPTOP -> R.drawable.computer
                            BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> R.drawable.headphone
                            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY -> R.drawable.game
                            else -> R.drawable.bluetooth
                        }
                    _devicesList.value?.add(
                        Device(
                            displayName,
                            deviceAddress,
                            deviceTypeDrawable,
                            isDeviceConnected(device),
                        ),
                    )
                }
            }
            _devicesList.value = _devicesList.value // Trigger LiveData update
        } else {
            _devicesList.value = mutableListOf() // Clear the list if permissions are not granted
        }
    }

    @SuppressLint("MissingPermission")
    private fun isDeviceConnected(device: BluetoothDevice): Boolean =
        device.bondState == BluetoothDevice.BOND_BONDED &&
            bluetoothAdapter?.bondedDevices?.contains(
                device,
            ) == true

    @SuppressLint("MissingPermission")
    fun toggleBluetooth(enable: Boolean) {
        bluetoothAdapter?.let {
            if (enable && !it.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBtIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(enableBtIntent)
            } else if (!enable && it.isEnabled) {
                it.disable()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (_permissionGranted.value == true) {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter?.startDiscovery()
        }
    }
}
