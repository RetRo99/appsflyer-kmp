package org.retar.platformlogs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader

actual class PlatformLogReader {

    private var logcatProcess: java.lang.Process? = null

    actual fun read(tag: String): Flow<LogEntry> = callbackFlow {
        val pid = android.os.Process.myPid()
        val cmd = arrayOf(
            "logcat",
            "-v", "time",
            "--pid=$pid",
            "-s", "$tag:V",
        )
        logcatProcess = ProcessBuilder(*cmd).redirectErrorStream(true).start()

        val reader = BufferedReader(InputStreamReader(logcatProcess!!.inputStream))

        try {
            while (!channel.isClosedForSend) {
                val line = reader.readLine() ?: break
                val entry = parseLine(line)
                if (entry != null) {
                    trySend(entry)
                }
            }
        } catch (_: Exception) {
        } finally {
            reader.close()
            logcatProcess?.destroy()
        }

        awaitClose {
            logcatProcess?.destroy()
        }
    }.flowOn(Dispatchers.IO)

    actual fun clear() {
        try {
            ProcessBuilder("logcat", "-c").start().waitFor()
        } catch (_: Exception) {
        }
    }

    private fun parseLine(line: String): LogEntry? {
        val parts = line.split(" ", limit = 4)
        if (parts.size < 4) return null

        val timestamp = "${parts[0]} ${parts[1]}"
        val levelChar = parts[2].firstOrNull() ?: return null
        val message = parts[3]
        val level = when (levelChar) {
            'V' -> LogLevel.VERBOSE
            'D' -> LogLevel.DEBUG
            'I' -> LogLevel.INFO
            'W' -> LogLevel.WARN
            'E' -> LogLevel.ERROR
            else -> LogLevel.INFO
        }

        return LogEntry(
            timestamp = timestamp,
            message = message,
            level = level,
        )
    }
}
