package ru.hunkel.mgrouptracker.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ru.hunkel.mgrouptracker.database.entities.Event

/**
 * Interface for communicate with database
 */
@Dao
interface TrackingDao {

    @Insert
    fun addEvent(event: Event)

    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventById(id: Int): Event
}