package ru.hunkel.mgrouptracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "gps_point",
    foreignKeys = [
        ForeignKey(entity = Event::class, parentColumns = ["id"], childColumns = ["event_id"])
    ]
)
data class GPSPoint(
    @PrimaryKey(autoGenerate = true)
    var id: Int,

    @ColumnInfo(name = "event_id")
    var eventId: Int,

    var time: Long,

    var longitude: Double,

    var latitude: Double
)