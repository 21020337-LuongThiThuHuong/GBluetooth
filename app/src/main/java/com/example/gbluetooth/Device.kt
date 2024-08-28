package com.example.gbluetooth

data class Device(
    val name: String,
    val address: String,
    val typeDrawable: Int,
    var isConnected: Boolean,
)
