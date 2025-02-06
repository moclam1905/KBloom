package com.nguyenmoclam.kbloom.logging

/**
 * Logger interface for logging messages during BloomFilter operations.
 */
interface Logger {
    /**
     * Log a message.
     * @param: message: the message to log
     */
    fun log(message: String)

    /**
     * Log an error message.
     * @param: message: the error message to log
     */
    fun error(message: String) {
        log("ERROR: $message")
    }
}
