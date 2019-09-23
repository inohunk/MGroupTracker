package ru.hunkel.mgrouptracker.database.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking
import ru.hunkel.mgrouptracker.database.TrackingDatabase
import ru.hunkel.mgrouptracker.database.entities.Event
import ru.hunkel.mgrouptracker.database.entities.Punches
import ru.hunkel.mgrouptracker.utils.convertMillisToTime
import ru.hunkel.mgrouptracker.utils.roundMilliseconds

const val TAG = "DatabaseManager"

class DatabaseManager(context: Context) {

    private var mDb = TrackingDatabase.getInstance(context)

    private var mCurrentEvent: Event = Event()

    //Events
    fun actionStartEvent(time: Long) {
        val event = Event(
            startTime = time
        )
        runBlocking {
            mDb.trackingModel().addEvent(event)
            mCurrentEvent = actionGetLastEvent()
        }
        Log.i(TAG, "event ${mCurrentEvent.id} started")
    }

    fun actionUpdateEvent(event: Event) {
        mDb.trackingModel().updateEvent(event)
        Log.i(TAG, "Event ${event.id} updated")
    }

    fun actionStopEvent(time: Long) {
        val event = actionGetLastEvent()
        event.endTime = time
        mDb.trackingModel().updateEvent(event)
        Log.i(TAG, "event ${event.id} stopped")
    }

    fun actionGetLastEvent(): Event {
        val event = mDb.trackingModel().getLastEvent()
        Log.i(
            TAG,
            "Get last event:\n" +
                    "EVENT INFO:\n\t" +
                    "id: ${event.id}\n\t" +
                    "start time: ${convertMillisToTime(event.startTime)}\n\t" +
                    "end time: ${convertMillisToTime(event.endTime)}\n"
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
        mDb.trackingModel().deleteEvent(event)
        Log.i(
            TAG,
            "REMOVED EVENT INFO:\n\t" +
                    "id: ${event.id}\n\t" +
                    "start time: ${convertMillisToTime(event.startTime)}\n\t" +
                    "end time: ${convertMillisToTime(event.endTime)}\n"
        )
    }

    fun actionGetEventById(id: Int): Event {
        return mDb.trackingModel().getEventById(id)
    }

    //Punches
    fun actionAddPunch(punch: Punches) {
        mDb.trackingModel().addPunch(punch)
        punch.time = roundMilliseconds(punch.time)
        val lastPunch = mDb.trackingModel().getLastPunch()

        Log.i(
            TAG,
            "\nAdded punch:\n" +
                    "id: ${lastPunch.id}\n" +
                    "event id: ${lastPunch.eventId}\n" +
                    "control point: ${lastPunch.controlPoint}\n" +
                    "time: ${convertMillisToTime(lastPunch.time)}\n"
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
                    "time: ${convertMillisToTime(lastPunch.time)}\n"
        )
    }

    fun actionReplacePunchSimple(punch: Punches) {
        punch.time = roundMilliseconds(punch.time)
        mDb.trackingModel().updatePunch(punch)
    }

    fun actionGetLastPunch(): Punches {
        return mDb.trackingModel().getLastPunch()
    }

    fun actionGetPunchesByEventId(id: Int): List<Punches> {
        val punches = mDb.trackingModel().getPunchesByEventId(id)
        Log.i(TAG, "PUNCHES LIST:\n")
        for (punch in punches) {
            punch.time = roundMilliseconds(punch.time)
            Log.i(TAG, "\t${punch.controlPoint}\n")
        }
        return punches
    }

    fun actionGetPunchesByEventIdWithAscSorting(id: Int):List<Punches>{
        val punches = mDb.trackingModel().getPunchesByEventIdWithAscSorting(id)
        Log.i(TAG, "PUNCHES LIST:\n")
        for (punch in punches) {
            punch.time = roundMilliseconds(punch.time)
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