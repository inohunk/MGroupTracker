package ru.hunkel.mgrouptracker.utils

//Errors
const val SUCCESS = -1

const val ERROR_BLUETOOTH = 0

const val ERROR_BLUETOOTH_VERSION = 1

const val ERROR_BLUETOOTH_NOT_SUPPORT_ADV = 2

const val ERROR_UNKNOWN = 9999

class ErrorCodes {
    private val errorMap = listOf(
        Error(
            ERROR_BLUETOOTH,
            "Ошибка блютуз",
            "Эта ошибка возникает, когда на устройстве отсутствует блютуз, либо его версия ниже 4.",
            ErrorLevel.FATAL
        ),
        Error(
            ERROR_BLUETOOTH_VERSION,
            "Ошибка блютуз",
            "Ваша версия блютуз не соответствует ",
            ErrorLevel.FATAL
        ),
        Error(
            ERROR_BLUETOOTH_NOT_SUPPORT_ADV,
            "Ошибка блютуз",
            "Ваше устройство не поддерживает создание",
            ErrorLevel.FATAL
        ),
        Error(
            ERROR_UNKNOWN,
            "Неизвестная ошибка",
            "Мы не можем сказать, что произошло",
            ErrorLevel.NORMAL
        )
    )

    fun getErrorByCode(errorCode: Int): Error? {
        return errorMap.find {
            it.code == errorCode
        }
    }
}