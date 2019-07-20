package ru.hunkel.mgrouptracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "punches",
    foreignKeys = [
        ForeignKey(entity = Event::class, parentColumns = ["id"], childColumns = ["event_id"])
    ]
)

data class Punches(

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,

    @ColumnInfo(name = "event_id")
    var eventId: Int,

    var time: Long = 0L,

    @ColumnInfo(name = "control_point")
    var controlPoint: Int = 0
)