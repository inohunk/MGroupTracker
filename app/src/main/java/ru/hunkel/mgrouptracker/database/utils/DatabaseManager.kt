package ru.hunkel.mgrouptracker.database.utils

import android.content.Context
import ru.hunkel.mgrouptracker.database.TrackingDatabase
import ru.hunkel.mgrouptracker.database.entities.Punches


object DatabaseManager {
    private var mDatabase: TrackingDatabase? = null

    fun insertPunches(context: Context, punch: Punches) {
        connectToDatabase(context)
        mDatabase!!.trackingModel().insertPunch(punch)
    }

    private fun connectToDatabase(context: Context) {
        if (mDatabase == null) mDatabase = TrackingDatabase.getInstance(context)
    }
}