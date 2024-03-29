package ru.hunkel.mgrouptracker.utils

import java.text.SimpleDateFormat
import java.util.*

//Date patterns
const val PATTERN_FULL_DATE = "yyyy.MM.dd HH:mm:ss"
const val PATTERN_YEAR_MONTH_DAY = "yyyy-MM-dd"
const val PATTERN_HOUR_MINUTE_SECOND = "HH:mm:ss"
const val PATTERN_FULL_DATE_INVERSE = "$PATTERN_HOUR_MINUTE_SECOND dd.MM.yyyy"

//Convert functions (from milliseconds)
fun convertMillisToTime(time: Long): String {
    val date = Date(time)
    val format = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
    format.timeZone = TimeZone.getTimeZone("Europe/Moscow")
    return format.format(date)
}

fun convertMillisToTime(time: Long, pattern: String): String {
    val date = Date(time)
    val format = SimpleDateFormat(pattern)
    format.timeZone = TimeZone.getTimeZone("Europe/Moscow")
    return format.format(date)
}

fun convertMillisToTime(time: Long, pattern: String, timeZone: TimeZone): String {
    val data = Date(time)
    val format = SimpleDateFormat(pattern)
    format.timeZone = timeZone
    return format.format(data)
}

/**
 * Function rounding milliseconds
 * @param ms Milliseconds for rounding
 */
fun roundMilliseconds(ms: Long): Long {
    return (1000 * (ms / 1000))
}