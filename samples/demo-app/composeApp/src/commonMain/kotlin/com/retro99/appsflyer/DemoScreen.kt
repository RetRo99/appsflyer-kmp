package com.retro99.appsflyer.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Suppress("DEPRECATION")
@Composable
fun DemoScreen(viewModel: DemoViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current
    var showParams by remember { mutableStateOf(false) }
    if (showParams) {
        ParamsScreen(
            params = uiState.params,
            onParamChange = viewModel::updateParam,
            onDone = { showParams = false },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        val splitState = remember { SplitState() }
        SplitLayout(
            state = splitState,
            top = {
                LogPanel(
                    logs = uiState.logs,
                    activeFilter = uiState.logFilter,
                    onFilterChange = viewModel::setLogFilter,
                    onClear = viewModel::clearLogs,
                    onCopy = {
                        val text = viewModel.exportLogs()
                        clipboard.setText(AnnotatedString(text))
                    },
                    onShowParams = { showParams = true },
                    modifier = Modifier.weight(1f),
                )
            },
            bottom = {
                SectionsList(
                    sections = remember(uiState.params) { viewModel.sections },
                    collapsedSections = uiState.collapsedSections,
                    onToggleSection = viewModel::toggleSection,
                    isButtonEnabled = viewModel::isButtonEnabled,
                    onRunSection = viewModel::runSection,
                    modifier = Modifier.fillMaxSize(),
                )
            },
        )
    }
}

@Composable
private fun SplitLayout(
    state: SplitState,
    top: @Composable ColumnScope.() -> Unit,
    bottom: @Composable ColumnScope.() -> Unit,
) {
    var containerHeightPx by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerHeightPx = it.height.toFloat() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(state.topFraction),
            content = top,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .draggable(
                    state = rememberDraggableState { delta ->
                        if (containerHeightPx > 0f) {
                            state.topFraction =
                                (state.topFraction + delta / containerHeightPx).coerceIn(
                                    minimumValue = SplitState.MIN,
                                    maximumValue = SplitState.MAX,
                                )
                        }
                    },
                    orientation = Orientation.Vertical,
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f - state.topFraction),
            content = bottom,
        )
    }
}

private class SplitState(
    initialFraction: Float = DEFAULT,
) {
    var topFraction by mutableStateOf(initialFraction)

    companion object {
        const val MIN: Float = 0.15f
        const val MAX: Float = 0.85f
        const val DEFAULT: Float = 0.6f
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParamsScreen(
    params: Map<ParamKey, String>,
    onParamChange: (ParamKey, String) -> Unit,
    onDone: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parameters") },
                actions = {
                    TextButton(onClick = onDone) { Text("Done") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ParamKey.entries.forEach { key ->
                OutlinedTextField(
                    value = params[key] ?: key.default,
                    onValueChange = { onParamChange(key, it) },
                    label = { Text(key.label) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun LogPanel(
    logs: List<LogEntry>,
    activeFilter: LogFilter,
    onFilterChange: (LogFilter) -> Unit,
    onClear: () -> Unit,
    onCopy: () -> Unit,
    onShowParams: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredLogs = remember(logs, activeFilter) {
        when (activeFilter) {
            LogFilter.ALL -> logs
            LogFilter.SDK -> logs.filter { entry -> entry.source == LogSource.SDK }
            LogFilter.ERRORS -> logs.filter { entry -> entry.level == LogLevel.ERROR }
            LogFilter.DEEPLINK -> logs.filter { entry -> entry.level == LogLevel.DEEPLINK }
        }.reversed()
    }

    var autoScroll by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var unseenCount by remember { mutableStateOf(0) }
    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }
    LaunchedEffect(filteredLogs.firstOrNull()?.id) {
        if (filteredLogs.isEmpty()) return@LaunchedEffect
        if (autoScroll) {
            listState.animateScrollToItem(0)
            unseenCount = 0
        } else if (!isAtTop) {
            unseenCount++
        }
    }
    LaunchedEffect(isAtTop) {
        if (isAtTop) { unseenCount = 0 }
    }

    val expandedLogIds = remember { mutableStateMapOf<Long, Boolean>() }
    val panelClipboard = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Logs",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.size(4.dp))
                if (logs.isNotEmpty()) {
                    Text(
                        text = "(${filteredLogs.size}/${logs.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.size(4.dp))
                TextButton(onClick = onShowParams) { Text("Params") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                CompactTextButton(
                    onClick = {
                        autoScroll = !autoScroll
                        if (autoScroll) {
                            scope.launch { listState.animateScrollToItem(0) }
                        }
                    },
                    text = if (autoScroll) "Auto \u2713" else "Auto",
                )
                CompactTextButton(onClick = onCopy, text = "Copy")
                CompactTextButton(onClick = onClear, text = "Clear")
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        FilterChipRow(
            activeFilter = activeFilter,
            onFilterChange = onFilterChange,
            showScrollToTop = logs.isNotEmpty() && (!autoScroll || unseenCount > 0) && !isAtTop,
            onScrollToTop = { scope.launch { listState.animateScrollToItem(0) } },
            unseenCount = unseenCount,
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (logs.isEmpty()) {
                        "No logs yet.\nTap a button below to get started."
                    } else {
                        "No logs match \"${activeFilter.label}\"."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(
                        items = filteredLogs,
                        key = { entry -> entry.id },
                    ) { entry ->
                        val expanded = expandedLogIds[entry.id] == true
                        LogRow(
                            entry = entry,
                            expanded = expanded,
                            onToggle = { expandedLogIds[entry.id] = !expanded },
                            onCopy = {
                                val text = "${entry.timestamp} [${entry.level}] [${entry.source}] ${entry.message}"
                                panelClipboard.setText(AnnotatedString(text))
                            },
                        )
                    }
                }
                if (unseenCount > 0) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                unseenCount = 0
                                scope.launch { listState.animateScrollToItem(0) }
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (unseenCount == 1) "1 new log" else "$unseenCount new logs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                        )
                        Text(
                            text = "↑",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    activeFilter: LogFilter,
    onFilterChange: (LogFilter) -> Unit,
    showScrollToTop: Boolean = false,
    onScrollToTop: () -> Unit = {},
    unseenCount: Int = 0,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LogFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == activeFilter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.label, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = levelColorForFilter(filter)
                        .copy(alpha = 0.18f)
                        .compositeOver(MaterialTheme.colorScheme.surface),
                    selectedLabelColor = levelColorForFilter(filter),
                ),
            )
        }
        if (showScrollToTop) {
            FilterChip(
                selected = true,
                onClick = onScrollToTop,
                label = {
                    Text(
                        text = if (unseenCount > 0) "↑ $unseenCount new" else "↑ Top",
                        fontSize = 11.sp,
                    )
                },
            )
        }
    }
}

private fun levelColorForFilter(filter: LogFilter): Color = when (filter) {
    LogFilter.ALL -> Color(0xFF455A64)
    LogFilter.SDK -> Color(0xFF6A1B9A)
    LogFilter.ERRORS -> Color(0xFFC62828)
    LogFilter.DEEPLINK -> Color(0xFF1565C0)
}

@Composable
private fun LogRow(
    entry: LogEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
    onCopy: () -> Unit,
) {
    val messageColor = when (entry.level) {
        LogLevel.SUCCESS -> Color(0xFF2E7D32)
        LogLevel.ERROR -> Color(0xFFC62828)
        LogLevel.DEEPLINK -> Color(0xFF1565C0)
        LogLevel.INFO -> MaterialTheme.colorScheme.onSurface
    }
    val dotColor = when (entry.level) {
        LogLevel.SUCCESS -> Color(0xFF2E7D32)
        LogLevel.ERROR -> Color(0xFFC62828)
        LogLevel.DEEPLINK -> Color(0xFF1565C0)
        LogLevel.INFO -> Color(0xFF9E9E9E)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Column {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = entry.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (entry.source == LogSource.SDK) {
                    Text(
                        text = "SDK",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = Color(0xFF6A1B9A),
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF6A1B9A).copy(alpha = 0.12f))
                            .padding(horizontal = 3.dp, vertical = 1.dp),
                    )
                }
                Text(
                    text = entry.message,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = messageColor,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val longEnough = entry.message.length > 80 || '\n' in entry.message
                if (longEnough) {
                    Text(
                        text = if (expanded) "▾" else "▸",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "⧉",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 4.dp, top = 2.dp)
                        .clickable { onCopy() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SectionsList(
    sections: List<DemoSection>,
    collapsedSections: Set<Section>,
    onToggleSection: (Section) -> Unit,
    isButtonEnabled: (ButtonPlatform) -> Boolean,
    onRunSection: (DemoSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        sections.forEach { section ->
            val collapsed = section.section in collapsedSections
            item(key = "header_${section.section}") {
                SectionHeader(
                    section = section.section,
                    collapsed = collapsed,
                    enabledCount = section.buttons.count { isButtonEnabled(it.platform) },
                    totalCount = section.buttons.size,
                    onToggle = { onToggleSection(section.section) },
                    onRunAll = { onRunSection(section) },
                )
            }
            if (!collapsed) {
                items(
                    items = section.buttons,
                    key = { "${section.section}_${it.label}" },
                ) { button ->
                    val enabled = isButtonEnabled(button.platform)
                    OutlinedButton(
                        onClick = button.onClick,
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (enabled) 1f else 0.35f),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = button.label,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f),
                            )
                            PlatformBadge(button.platform)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    section: Section,
    collapsed: Boolean,
    enabledCount: Int,
    totalCount: Int,
    onToggle: () -> Unit,
    onRunAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clickable { onToggle() }
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (collapsed) "▶" else "▼",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "($enabledCount/$totalCount)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (enabledCount > 0) {
            TextButton(onClick = onRunAll) {
                Text("Run all", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CompactTextButton(
    onClick: () -> Unit,
    text: String,
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun PlatformBadge(platform: ButtonPlatform) {
    val (text, color) = when (platform) {
        ButtonPlatform.ANDROID -> "A" to Color(0xFF4CAF50)
        ButtonPlatform.IOS -> "i" to Color(0xFF2196F3)
        ButtonPlatform.BOTH -> "★" to Color(0xFFFF9800)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        color = color,
        modifier = Modifier.padding(start = 4.dp),
    )
}
