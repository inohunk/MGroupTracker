package ru.hunkel.mgrouptracker.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import ru.hunkel.mgrouptracker.ITrackingService

class TrackingService : Service() {
    inner class TrackingServiceImpl : ITrackingService.Stub(){
        override fun startBeacon() {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun punch() {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun stopBeacon() {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
    override fun onBind(intent: Intent): IBinder {
        return TrackingServiceImpl()
    }
}
