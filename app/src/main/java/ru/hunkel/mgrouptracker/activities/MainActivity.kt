package ru.hunkel.mgrouptracker.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import ru.hunkel.mgrouptracker.ITrackingService
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.services.TrackingService

class MainActivity : AppCompatActivity() {

    /*
        VARIABLES
    */
    var mTrackingService: ITrackingService? = null

    val mTrackingServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {

        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mTrackingService = ITrackingService.Stub.asInterface(service)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val serviceIntent =
            Intent(this,TrackingService::class.java)
        bindService(serviceIntent,mTrackingServiceConnection, Context.BIND_AUTO_CREATE)

    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mTrackingServiceConnection)
    }
}
