package ru.hunkel.mgrouptracker.utils

import android.bluetooth.BluetoothAdapter

const val STATE_OFF = 0
const val STATE_ON = 1

const val DEFAULT_LOCK_UPDATE_INTERVAL = 120 * 1000L //120 secs
const val DEFAULT_CONTROL_POINT_UPDATE = 10 * 1000L //10 secs

const val PUNCH_UPDATE_STATE_NOTHING = 0
const val PUNCH_UPDATE_STATE_REPLACE = 1
const val PUNCH_UPDATE_STATE_ADD = 2

fun enableBluetooth(){
    val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    mBluetoothAdapter.cancelDiscovery()
    if (mBluetoothAdapter.isEnabled.not()) {
        mBluetoothAdapter.enable()
    }
}