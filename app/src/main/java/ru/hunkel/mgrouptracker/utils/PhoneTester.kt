package ru.hunkel.mgrouptracker.utils

import android.content.Context
import android.content.pm.PackageManager

class PhoneTester(val context: Context) {
    private val TAG = "PhoneTester"

    fun test(packageManager: PackageManager): Int {
        val result = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        return if (result) {
            SUCCESS
        } else {
            ERROR_BLUETOOTH
        }
    }
}