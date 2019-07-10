package ru.hunkel.mgrouptracker.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,

    var startTime: Long = 0L,

    var endTime: Long = 0L
)