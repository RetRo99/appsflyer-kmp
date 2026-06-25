package com.retro99.platformlogs

import kotlinx.coroutines.flow.Flow

expect class PlatformLogReader {

    fun read(tag: String): Flow<LogEntry>

    fun clear()
}

data class LogEntry(
    val timestamp: String,
    val message: String,
    val level: LogLevel,
)

enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }
