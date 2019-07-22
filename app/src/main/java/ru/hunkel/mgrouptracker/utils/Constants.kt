package ru.hunkel.mgrouptracker.utils

import android.bluetooth.BluetoothAdapter

const val STATE_OFF = 0
const val STATE_ON = 1

fun enableBluetooth(){
    val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    mBluetoothAdapter.cancelDiscovery()
    if (mBluetoothAdapter.isEnabled.not()) {
        mBluetoothAdapter.enable()
    }
}