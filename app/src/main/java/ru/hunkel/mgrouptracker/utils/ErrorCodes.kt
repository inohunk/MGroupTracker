package ru.hunkel.mgrouptracker.utils

import ru.hunkel.mgrouptracker.models.Error
import ru.hunkel.mgrouptracker.models.ErrorLevel

//Error codes
/**
 * Code when no errors founded
 */
const val SUCCESS = -1

/**
 * Bluetooth common error
 */
const val ERROR_BLUETOOTH = 0

/**
 * Incompatible Bluetooth version
 */
const val ERROR_BLUETOOTH_VERSION = 1

/**
 * Bluetooth not support advertising
 */
const val ERROR_BLUETOOTH_NOT_SUPPORT_ADV = 2

/**
 * Unknown error
 */
const val ERROR_UNKNOWN = 9999

/**
 * The class provides a means for error verification
 */
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

    /**
     * Function returns Error object by error code
     */
    fun getErrorByCode(errorCode: Int): Error? {
        return errorMap.find {
            it.code == errorCode
        }
    }
}