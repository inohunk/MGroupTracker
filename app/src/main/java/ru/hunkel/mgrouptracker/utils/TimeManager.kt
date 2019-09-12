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
import kotlin.math.abs

abstract class TimeManager(private val context: Context) : LocationListener {
    private var mTime = 0L
    private var mDeltaTime = 0L

    //Location
    private var mLocationManager: LocationManager? = null

    init {
        Log.i("TTT", "init")
        getTimeFromGPS()
    }

    fun getTime(): Long {
        mTime = getTimeFromSystem() + mDeltaTime
        Log.i("TTT", "getTime called")
        return mTime
    }

    fun getTimeDifference(): Long {
        Log.i("TTT", "getTimeDifference called")
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
            mLocationManager!!.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, Looper.getMainLooper())
        }
        Log.i("TTT", "getTimeFromGPS called")

    }

    abstract fun onGpsTimeReceived(time: Long)

    private fun getTimeFromSystem(): Long {
        return System.currentTimeMillis()
    }

    override fun onLocationChanged(location: Location?) {
        mDeltaTime = abs(mTime - location!!.time)
        mTime = location.time
        onGpsTimeReceived(mTime)
        Log.i("TTT", "onLocationChanged")
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String?) {
    }

    override fun onProviderDisabled(provider: String?) {

    }
}