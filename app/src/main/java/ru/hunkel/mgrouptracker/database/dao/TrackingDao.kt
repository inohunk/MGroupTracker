package ru.hunkel.mgrouptracker.database.dao

import androidx.room.*
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

    @Delete
    fun deleteEvent(event: Event)

    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventById(id: Int): Event

    @Query("SELECT * FROM events ORDER BY ID DESC LIMIT 1")
    fun getLastEvent(): Event

    @Query("SELECT * FROM events ORDER BY ID ASC")
    fun getAllEvents(): List<Event>

    //Punches
    @Insert
    fun addPunch(punch: Punches)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updatePunch(punch: Punches)

    @Query("DELETE FROM punches WHERE control_point = :id")
    fun deletePunchByControlPointId(id: Int)

    @Query("SELECT * FROM punches ORDER BY ID DESC LIMIT 1")
    fun getLastPunch(): Punches

    @Query("SELECT * FROM punches WHERE event_id = :id")
    fun getPunchesByEventId(id: Int): List<Punches>

    @Query("SELECT * FROM punches WHERE event_id = :id ORDER BY punches.time ASC")
    fun getPunchesByEventIdWithAscSorting(id: Int): List<Punches>


    @Query("SELECT * FROM punches WHERE control_point = :cp")
    fun getPunchByControlPoint(cp: Int): Punches

    /**
     * Use this function for getting punches before certain time stamp
     */
    @Query("SELECT * FROM punches WHERE time < :time")
    fun getPunchesBeforeCertainTime(time: Long): List<Punches>
}