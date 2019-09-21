package ru.hunkel.mgrouptracker.models

data class Error(
    val code: Int,
    var name: String,
    var description: String,
    val level: ErrorLevel
)

enum class ErrorLevel {
    FATAL,
    NORMAL
}