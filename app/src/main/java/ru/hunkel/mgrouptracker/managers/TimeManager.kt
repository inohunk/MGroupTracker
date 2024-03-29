package ru.hunkel.mgrouptracker.managers

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
import ru.hunkel.mgrouptracker.utils.roundMilliseconds

const val TAG_TIME_MANAGER = "TimeManager"

class TimeManager(private val context: Context) : LocationListener {
    private var mTime = 0L
    private var mDeltaTime = 0L

    interface TimeChangedListener {
        fun onTimeChanged(time: Long)
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

    fun getTimeDelta(): Long {
        Log.i(TAG_TIME_MANAGER, "getTimeDelta called")
        return mDeltaTime
    }

    fun stopGps(){
        mLocationManager?.removeUpdates(this)
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
        return roundMilliseconds(System.currentTimeMillis())
    }

    override fun onLocationChanged(location: Location?) {
        mDeltaTime = location!!.time - getTimeFromSystem()
        mTime = location.time
        if (sTimeChangedListener != null) {
            sTimeChangedListener!!.onTimeChanged(mTime)
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