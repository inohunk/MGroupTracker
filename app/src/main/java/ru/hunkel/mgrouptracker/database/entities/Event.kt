package ru.hunkel.mgrouptracker.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(

    @PrimaryKey(autoGenerate = true)
    var id: Int,

    var startTime: Long,

    var endTime: Long
)