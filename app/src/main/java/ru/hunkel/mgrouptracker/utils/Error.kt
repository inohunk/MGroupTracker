package ru.hunkel.mgrouptracker.utils

data class Error(
    val code: Int,
    var name: String,
    var description: String,
    val level: ErrorLevel
)