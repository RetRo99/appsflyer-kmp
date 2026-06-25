package com.retro99.appsflyer

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberCopyToClipboard(): (String) -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return remember(clipboard) {
        { text: String ->
            scope.launch {
                clipboard.setClipEntry(clipEntryOf(text))
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
expect fun clipEntryOf(text: String): ClipEntry
