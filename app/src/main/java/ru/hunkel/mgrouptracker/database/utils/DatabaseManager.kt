package ru.hunkel.mgrouptracker.database.utils

import android.content.Context
import android.util.Log
import ru.hunkel.mgrouptracker.database.TrackingDatabase
import ru.hunkel.mgrouptracker.database.entities.Event
import ru.hunkel.mgrouptracker.database.entities.Punches
import java.text.SimpleDateFormat
import java.util.*


class DatabaseManager(context: Context) {
    private var mDb = TrackingDatabase.getInstance(context)
    private val TAG = "DB-Manager"

    private var mCurrentEvent: Event = Event()

    fun actionStartEvent() {
        val event = Event(
            startTime = System.currentTimeMillis()
        )
        Log.i(TAG, "event started")
        mCurrentEvent = event
        mDb.trackingModel().addEvent(mCurrentEvent)
    }

    fun actionStopEvent() {
        val event = mDb.trackingModel().getLastEvent()
        event.endTime = System.currentTimeMillis()
        mDb.trackingModel().updateEvent(event)
        Log.i(TAG, "event stopped")
        actionGetLastEvent()

    }

    fun actionGetLastEvent(): Event {
        val event = mDb.trackingModel().getLastEvent()

        Log.i(
            TAG,
            "EVENT INFO:\n\t" +
                    "id: ${event.id}\n\t" +
                    "start time: ${convertLongToTime(event.startTime)}\n\t" +
                    "end time: ${convertLongToTime(event.endTime)}\n"
        )
        return event
    }

    fun convertLongToTime(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
        return format.format(date)
    }

    fun actionAddPunch(punch: Punches){
        mDb.trackingModel().addPunch(punch)
        val lastPunch = mDb.trackingModel().getLastPunch()

        Log.i(TAG,"" +
                "Adding punch:\n" +
                "id: ${lastPunch.id}\n" +
                "event id: ${lastPunch.eventId}\n" +
                "control point: ${lastPunch.controlPoint}\n" +
                "time: ${convertLongToTime(lastPunch.time)}\n")
    }
}