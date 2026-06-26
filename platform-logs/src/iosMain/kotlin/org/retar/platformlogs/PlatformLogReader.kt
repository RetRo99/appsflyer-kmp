package org.retar.platformlogs

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import PlatformLogsBridge.PlatformLogsBridge

@OptIn(ExperimentalForeignApi::class)
actual class PlatformLogReader {

    private val bridge = PlatformLogsBridge()

    actual fun read(tag: String): Flow<LogEntry> = callbackFlow {
        bridge.readLogsWithTag(tag) { timestamp, message, level ->
            trySend(
                LogEntry(
                    timestamp = timestamp ?: "",
                    message = message ?: "",
                    level = when (level.toInt()) {
                        1 -> LogLevel.DEBUG
                        4 -> LogLevel.ERROR
                        else -> LogLevel.INFO
                    },
                ),
            )
        }
        awaitClose {
            bridge.stop()
        }
    }.flowOn(Dispatchers.Default)

    actual fun clear() {
    }
}
