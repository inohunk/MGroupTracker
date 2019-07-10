package ru.hunkel.mgrouptracker.database.utils

import android.content.Context
import ru.hunkel.mgrouptracker.database.TrackingDatabase


class DatabaseManager(context: Context) {
    private var mDb = TrackingDatabase.getInstance(context)
}