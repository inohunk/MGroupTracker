package ru.hunkel.mgrouptracker.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import ru.hunkel.mgrouptracker.ITrackingService
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.services.TrackingService

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            R.id.settings_button -> startActivity(Intent(this, TrackingSettings::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startServiceOnClick() {

        val serviceIntent =
            Intent(this, TrackingService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, mTrackingServiceConnection, Context.BIND_AUTO_CREATE)

        updateUIWithCurrentState(true)
        mServiceBounded = true
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

    override fun onDestroy() {
        super.onDestroy()
        if (mServiceBounded) {
            unbindService(mTrackingServiceConnection)
        }
    }
}
