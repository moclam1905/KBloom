package com.nguyenmoclam.kbloom

import com.nguyenmoclam.kbloom.logging.Logger

class MockLogger : Logger {
    private val logs = mutableListOf<String>()
    override fun log(message: String) {
        logs.add(message)
    }

    fun clear() {
        logs.clear()
    }
}