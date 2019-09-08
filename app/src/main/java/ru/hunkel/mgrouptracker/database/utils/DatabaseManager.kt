package ru.hunkel.mgrouptracker.database.utils

import android.content.Context
import android.util.Log
import ru.hunkel.mgrouptracker.database.TrackingDatabase
import ru.hunkel.mgrouptracker.database.entities.Event
import ru.hunkel.mgrouptracker.database.entities.Punches
import ru.hunkel.mgrouptracker.utils.convertLongToTime


class DatabaseManager(context: Context) {
    private var mDb = TrackingDatabase.getInstance(context)
    private val TAG = "DB-Manager"

    private var mCurrentEvent: Event = Event()

    //Events
    fun actionStartEvent(time: Long) {
        val event = Event(
            startTime = System.currentTimeMillis()
        )
        Log.i(TAG, "event started")
        mCurrentEvent = event
        Log.i(
            TAG,
            "EVENT INFO:\n\t" +
                    "id: ${event.id}\n\t" +
                    "start time: ${convertLongToTime(event.startTime)}\n\t" +
                    "end time: ${convertLongToTime(event.endTime)}\n"
        )
        mDb.trackingModel().addEvent(mCurrentEvent)
    }

    fun actionUpdateEvent(event: Event) {
        mDb.trackingModel().updateEvent(event)
    }

    fun actionStopEvent(time: Long) {
        val event = mDb.trackingModel().getLastEvent()
        event.endTime = time
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

    fun actionGetAllEvents(): List<Event> {
        val events = mDb.trackingModel().getAllEvents()
        Log.i(
            TAG,
            "EVENTS INFO:\n\t" +
                    "size: ${events.size}\n\t"
        )
        return events
    }

    fun actionDeleteEvent(event: Event) {
        Log.i(
            TAG,
            "REMOVED EVENT INFO:\n\t" +
                    "id: ${event.id}\n\t" +
                    "start time: ${convertLongToTime(event.startTime)}\n\t" +
                    "end time: ${convertLongToTime(event.endTime)}\n"
        )
        mDb.trackingModel().deleteEvent(event)
    }

    fun actionGetEventById(id: Int): Event {
        return mDb.trackingModel().getEventById(id)
    }

    //Punches
    fun actionAddPunch(punch: Punches) {
        mDb.trackingModel().addPunch(punch)
        val lastPunch = mDb.trackingModel().getLastPunch()

        Log.i(
            TAG,
            "\nAdding punch:\n" +
                    "id: ${lastPunch.id}\n" +
                    "event id: ${lastPunch.eventId}\n" +
                    "control point: ${lastPunch.controlPoint}\n" +
                    "time: ${convertLongToTime(lastPunch.time)}\n"
        )
    }

    fun actionReplacePunch(punch: Punches) {
        mDb.trackingModel().deletePunchByControlPointId(punch.controlPoint)
        mDb.trackingModel().addPunch(punch)
        val lastPunch = mDb.trackingModel().getLastPunch()
        Log.i(
            TAG,
            "\nReplaced punch:\n" +
                    "id: ${lastPunch.id}\n" +
                    "event id: ${lastPunch.eventId}\n" +
                    "control point: ${lastPunch.controlPoint}\n" +
                    "time: ${convertLongToTime(lastPunch.time)}\n"
        )
    }

    fun actionReplacePunchSimple(punch: Punches) {
        mDb.trackingModel().updatePunch(punch)
    }

    fun actionGetLastPunch(): Punches {
        return mDb.trackingModel().getLastPunch()
    }

    fun actionGetPunchesByEventId(id: Int): List<Punches> {
        val punches = mDb.trackingModel().getPunchesByEventId(id)
        Log.i(TAG, "PUNCHES LIST:\n")
        for (punch in punches) {
            Log.i(TAG, "\t${punch.controlPoint}\n")
        }
        return punches
    }

    fun actionGetPunchByControlPoint(cp: Int): Punches {
        return mDb.trackingModel().getPunchByControlPoint(cp)
    }

    fun actionGetPunchesBeforeCertainTime(time: Long): List<Punches> {
        return mDb.trackingModel().getPunchesBeforeCertainTime(time)
    }
}