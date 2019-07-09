package ru.hunkel.mgrouptracker.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import ru.hunkel.mgrouptracker.database.entities.Punches

/**
 * Interface for communicate with database
 */
@Dao
interface TrackingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPunch(punch: Punches)

}