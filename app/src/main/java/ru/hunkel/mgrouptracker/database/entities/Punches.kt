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

data class Punches (

    @PrimaryKey
    var id: Long,

    @ColumnInfo(name = "event_id")
    var eventId: Long,

    var time: Long,

    @ColumnInfo(name = "control_point")
    var controlPoint: Int = 0
)