package com.nguyenmoclam.kbloom

/**
 * ConsoleLogger is a logger that prints messages to the console.
 */
class ConsoleLogger : Logger {
    override fun log(message: String) {
        println("[BloomFilter] $message")
    }
}