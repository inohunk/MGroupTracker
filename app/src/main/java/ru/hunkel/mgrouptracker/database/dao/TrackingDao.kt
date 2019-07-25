package ru.hunkel.mgrouptracker.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import ru.hunkel.mgrouptracker.database.entities.Event
import ru.hunkel.mgrouptracker.database.entities.Punches

/**
 * Interface for communicate with database
 */
@Dao
interface TrackingDao {

    //Events
    @Insert
    fun addEvent(event: Event)

    @Update
    fun updateEvent(event: Event)

    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventById(id: Int): Event

    @Query("SELECT * FROM events ORDER BY ID DESC LIMIT 1")
    fun getLastEvent(): Event

    @Query("SELECT * FROM events ORDER BY ID ASC")
    fun getAllEvents():List<Event>

    //Punches
    @Insert
    fun addPunch(punch: Punches)

    @Query("SELECT * FROM punches ORDER BY ID DESC LIMIT 1")
    fun getLastPunch(): Punches

    @Query("SELECT * FROM punches INNER JOIN events ON events.id = punches.event_id WHERE event_id = :id ORDER BY punches.time ASC")
    fun getPunchesByEventId(id: Int):List<Punches>
}