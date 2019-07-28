package ru.hunkel.mgrouptracker.activities

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import ru.hunkel.mgrouptracker.ITrackingService
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.drawables.EventDrawable
import ru.hunkel.mgrouptracker.services.TrackingService
import ru.hunkel.mgrouptracker.utils.enableBluetooth

class MainActivity : AppCompatActivity() {

    /*
        VARIABLES
    */
    var mTrackingService: ITrackingService? = null
    var mServiceBounded = false

    private val mTrackingServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {

        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mTrackingService = ITrackingService.Stub.asInterface(service)
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        start_event_button.background = EventDrawable(ContextCompat.getColor(this,R.color.color_start_button))
        stop_event_button.background = EventDrawable(ContextCompat.getColor(this,R.color.color_stop_button))

        start_event_button.setOnClickListener {
            startServiceOnClick()
        }

        stop_event_button.setOnClickListener {
            stopServiceOnClick()
        }
        updateUIWithCurrentState(false)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.overflow_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.settings_button -> {
                if (mServiceBounded.not()) {
                    startActivity(Intent(this, TrackingSettings::class.java))
                } else {
                    Toast.makeText(this, "Нельзя открыть настройки во время соревнования.", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.info_button -> startActivity(Intent(this, InfoActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startServiceOnClick() {
        if (acceptPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            )
        ) {
            enableBluetooth()

            val serviceIntent =
                Intent(this, TrackingService::class.java)
            startService(serviceIntent)
            bindService(serviceIntent, mTrackingServiceConnection, Context.BIND_WAIVE_PRIORITY)

            updateUIWithCurrentState(true)
            mServiceBounded = true
        }
    }

    private fun stopServiceOnClick() {

        unbindService(mTrackingServiceConnection)
        stopService(Intent(this, TrackingService::class.java))

        updateUIWithCurrentState(false)
        mServiceBounded = false
    }

    private fun updateUIWithCurrentState(state: Boolean) {
        start_event_button.isEnabled = !state
        stop_event_button.isEnabled = state
    }

    override fun onBackPressed() {}
    override fun onDestroy() {
        super.onDestroy()
        if (mServiceBounded) {
            unbindService(mTrackingServiceConnection)
        }
    }

    private fun acceptPermissions(permissions: Array<String>): Boolean {
        var grandted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (
                    (checkSelfPermission((permission)) != PackageManager.PERMISSION_GRANTED)
                    and
                    (shouldShowRequestPermissionRationale(permission))
                ) {
                    Toast.makeText(
                        this,
                        "Разрешение на местоположение необходимо для записи ваших треков",
                        Toast.LENGTH_LONG
                    ).show()
                    grandted = false
                }
            }
            requestPermissions(permissions, 1)
        } else {
            grandted = true
        }
        return grandted
    }
}
