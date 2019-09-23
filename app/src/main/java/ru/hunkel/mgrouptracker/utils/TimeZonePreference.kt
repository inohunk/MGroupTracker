package ru.hunkel.mgrouptracker.utils

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import java.util.*

class TimeZonePreference
constructor(context: Context, attrs: AttributeSet? = null) :
    ListPreference(context, attrs) {
    init {
        entries = entries()
        entryValues = entryValues()
        setValueIndex(defaultIndex())
    }

    private fun entries(): Array<String> {
        return TimeZone.getAvailableIDs()
    }

    private fun entryValues(): Array<String> {
        return TimeZone.getAvailableIDs()
    }

    private fun defaultIndex(): Int {
        return TimeZone.getAvailableIDs().indexOf("Europe/Moscow")
    }
}