package ru.hunkel.mgrouptracker.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import ru.hunkel.mgrouptracker.ITrackingService
import ru.hunkel.mgrouptracker.database.utils.DatabaseManager

class TrackingService : Service() {

    /*
        VARIABLES
    */
    private lateinit var mDatabaseManager: DatabaseManager

    /*
        INNER CLASSES
    */
    inner class TrackingServiceImpl : ITrackingService.Stub() {
        override fun startEvent() {
            mDatabaseManager.actionStartEvent()
        }

        override fun punch() {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun stopEvent() {
            mDatabaseManager.actionStopEvent()
        }
    }

    /*
        FUNCTIONS
    */
    override fun onCreate() {
        mDatabaseManager = DatabaseManager(this)
    }

    override fun onBind(intent: Intent): IBinder {
        return TrackingServiceImpl()
    }

}
