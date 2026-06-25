package com.retro99.appsflyer.sample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DemoScreen(viewModel: DemoViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current
    var showParams by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val badge = if (viewModel.isAndroid) "Android" else "iOS"
                    Text("AppsFlyer KMP Demo [$badge]")
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ParamsPanel(
                params = uiState.params,
                onParamChange = viewModel::updateParam,
                expanded = showParams,
                onToggle = { showParams = !showParams },
                modifier = Modifier.weight(0.3f),
            )
            HorizontalDivider()
            LogPanel(
                logs = uiState.logs,
                onClear = viewModel::clearLogs,
                onCopy = {
                    val text = viewModel.exportLogs()
                    clipboard.setText(AnnotatedString(text))
                },
                modifier = Modifier.weight(0.25f),
            )
            HorizontalDivider()
            SectionsList(
                sections = remember(uiState.params) { viewModel.sections },
                collapsedSections = uiState.collapsedSections,
                onToggleSection = viewModel::toggleSection,
                isButtonEnabled = viewModel::isButtonEnabled,
                onRunSection = viewModel::runSection,
                modifier = Modifier.weight(0.45f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParamsPanel(
    params: Map<ParamKey, String>,
    onParamChange: (ParamKey, String) -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            Text("Parameters", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = onToggle) {
                Text(if (expanded) "Hide" else "Show")
            }
        }
        AnimatedVisibility(visible = expanded) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ParamKey.entries.forEach { key ->
                    OutlinedTextField(
                        value = params[key] ?: key.default,
                        onValueChange = { onParamChange(key, it) },
                        label = { Text(key.label, fontSize = 11.sp) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.widthIn(min = 120.dp),
                        singleLine = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun LogPanel(
    logs: List<LogEntry>,
    onClear: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                Text("Logs", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.size(4.dp))
                if (logs.isNotEmpty()) {
                    Text(
                        text = "(${logs.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (logs.isNotEmpty()) {
                    TextButton(onClick = onCopy) { Text("Copy") }
                    TextButton(onClick = onClear) { Text("Clear") }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(logs.reversed()) { entry ->
                LogRow(entry)
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val color = when (entry.level) {
        LogLevel.SUCCESS -> Color(0xFF2E7D32)
        LogLevel.ERROR -> Color(0xFFC62828)
        LogLevel.DEEPLINK -> Color(0xFF1565C0)
        LogLevel.INFO -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = entry.timestamp,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = color,
        )
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
