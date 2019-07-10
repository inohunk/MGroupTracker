package ru.hunkel.mgrouptracker.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import ru.hunkel.mgrouptracker.database.entities.Event

/**
 * Interface for communicate with database
 */
@Dao
interface TrackingDao {

    @Insert
    fun addEvent(event: Event)

    @Update
    fun updateEvent(event: Event)

    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventById(id: Int): Event

    @Query("SELECT * FROM events ORDER BY ID DESC LIMIT 1")
    fun getLastEvent():Event
}