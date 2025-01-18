package com.nguyenmoclam.kbloom

/**
 * Logger interface for logging messages during BloomFilter operations.
 */
interface Logger {
    /**
     * Log a message with a specific log level.
     * @param: message: the message to log
     */
    fun log(message: String)
}

/**
 * DefaultLogger is a default logger that does nothing.
 * User can implement their own logger if needed.
 */
object DefaultLogger : Logger {
    override fun log(message: String) {

    }

}
