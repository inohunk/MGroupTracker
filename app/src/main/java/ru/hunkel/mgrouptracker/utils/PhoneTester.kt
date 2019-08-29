package ru.hunkel.mgrouptracker.utils

import android.content.Context
import android.util.Log
import org.altbeacon.beacon.BeaconTransmitter

class PhoneTester(val context: Context) {
    private val TAG = "PhoneTester"

    fun testDevice(): Boolean {
        return (checkBeaconSupport() or checkBattery())
    }

    private fun checkBattery(): Boolean {
        return true
    }

    private fun checkBeaconSupport(): Boolean {
        return when (BeaconTransmitter.checkTransmissionSupported(context)) {
            BeaconTransmitter.NOT_SUPPORTED_MIN_SDK -> {
                Log.i(TAG, "CHECKING SUPPORT: not supported but min sdk")
                false
            }
            BeaconTransmitter.NOT_SUPPORTED_CANNOT_GET_ADVERTISER,
            BeaconTransmitter.NOT_SUPPORTED_CANNOT_GET_ADVERTISER_MULTIPLE_ADVERTISEMENTS
            -> {
                Log.i(TAG, "CHECKING SUPPORT: not supported but device have not api")
                false
            }
            BeaconTransmitter.NOT_SUPPORTED_BLE -> {
                Log.i(TAG, "CHECKING SUPPORT: not supported but bluetooth problem")
                false
            }
            BeaconTransmitter.SUPPORTED -> {
                Log.i(TAG, "CHECKING SUPPORT: supported")
                true
            }
            else -> false
        }
    }
}