package com.retro99.platformlogs

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual class PlatformLogReader {

    actual fun read(tag: String): Flow<LogEntry> = callbackFlow {
        while (!channel.isClosedForSend) {
            delay(Long.MAX_VALUE)
        }
        awaitClose { }
    }

    actual fun clear() {
    }
}
