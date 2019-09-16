package ru.hunkel.mgrouptracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.runBlocking
import ru.hunkel.mgrouptracker.database.dao.TrackingDao
import ru.hunkel.mgrouptracker.database.entities.Event
import ru.hunkel.mgrouptracker.database.entities.GPSPoint
import ru.hunkel.mgrouptracker.database.entities.Punches

@Database(
    entities = [Event::class, GPSPoint::class, Punches::class],
    version = 3
)
abstract class TrackingDatabase : RoomDatabase() {
    abstract fun trackingModel(): TrackingDao

    companion object {
        private var INSTANCE: TrackingDatabase? = null

        fun getInstance(context: Context): TrackingDatabase {
            if (INSTANCE == null) {
                runBlocking {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        TrackingDatabase::class.java,
                        "tracking-database"
                    )
                        //TODO used only for primary testing. In future rewrite all database queries with coroutines
                        .allowMainThreadQueries()
                        //
                        .build()
                }
            }
            return INSTANCE!!
        }
    }
}