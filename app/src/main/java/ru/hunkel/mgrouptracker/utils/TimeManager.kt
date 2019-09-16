package ru.hunkel.mgrouptracker.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat

const val TAG_TIME_MANAGER = "TimeManager"

class TimeManager(private val context: Context) : LocationListener {
    private var mTime = 0L
    private var mDeltaTime = 0L

    interface TimeChangedListener {
        fun onTimeChaned(time: Long)
    }

    companion object {
        var sTimeChangedListener: TimeChangedListener? = null
    }

    //Location
    private var mLocationManager: LocationManager? = null

    init {
        Log.i(TAG_TIME_MANAGER, "init")
        getTimeFromGPS()
    }

    fun getTime(): Long {
        mTime = getTimeFromSystem() + mDeltaTime
        Log.i(TAG_TIME_MANAGER, "getTime called")
        return mTime
    }

    fun getTimeDifference(): Long {
        Log.i(TAG_TIME_MANAGER, "getTimeDifference called")
        return mDeltaTime
    }

    private fun getTimeFromGPS() {
        mLocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //TODO do something  if app don't have permissions
        } else {
            mLocationManager!!.requestSingleUpdate(
                LocationManager.GPS_PROVIDER,
                this,
                Looper.getMainLooper()
            )
        }
        Log.i(TAG_TIME_MANAGER, "getTimeFromGPS called")

    }

    private fun getTimeFromSystem(): Long {
        return System.currentTimeMillis()
    }

    override fun onLocationChanged(location: Location?) {
        mDeltaTime = location!!.time - System.currentTimeMillis()
        mTime = location.time
        if (sTimeChangedListener != null) {
            sTimeChangedListener!!.onTimeChaned(mTime)
        }
        Log.i(TAG_TIME_MANAGER, "onLocationChanged")
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String?) {
    }

    override fun onProviderDisabled(provider: String?) {

    }
}