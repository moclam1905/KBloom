package com.nguyenmoclam.kbloom

class MockLogger : Logger {
    private val logs = mutableListOf<String>()
    override fun log(message: String) {
        logs.add(message)
    }

    fun clear() {
        logs.clear()
    }
}