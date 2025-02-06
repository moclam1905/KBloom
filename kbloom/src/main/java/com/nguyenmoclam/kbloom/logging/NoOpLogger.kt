package com.nguyenmoclam.kbloom.logging

object NoOpLogger : Logger {
    override fun log(message: String) {
        // Do nothing
    }
}
