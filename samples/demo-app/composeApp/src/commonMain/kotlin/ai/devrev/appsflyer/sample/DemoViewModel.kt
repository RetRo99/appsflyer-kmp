package com.retro99.appsflyer.sample

import com.retro99.appsflyer.AppsFlyer
import com.retro99.appsflyer.CampaignData
import com.retro99.appsflyer.DeepLinkResult
import com.retro99.appsflyer.StartResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DemoUiState(
    val logs: List<String> = emptyList(),
)

class DemoViewModel : ViewModel() {

    private val client = AppsFlyer.client

    private val _uiState = MutableStateFlow(DemoUiState())
    val uiState: StateFlow<DemoUiState> = _uiState.asStateFlow()

    init {
        client.setCustomerUserId("demo-user-001")
        observeDeepLinks()
        observeStartResult()
        observeConversionData()
        client.start()
    }

    private fun observeStartResult() {
        viewModelScope.launch {
            val result = client.getStartResult()
            val message = when (result) {
                is StartResult.Success -> "Start: Success"
                is StartResult.Error -> "Start: Error(${result.code}, ${result.message})"
            }
            appendLog(message)
        }
    }

    private fun observeConversionData() {
        viewModelScope.launch {
            val data = client.getConversionData()
            val message = when (data) {
                is CampaignData.Success ->
                    "Conversion: ${data.status} | " +
                        "source=${data.mediaSource} campaign=${data.campaign}"
                is CampaignData.Error -> "Conversion Error: ${data.message}"
            }
            appendLog(message)
        }
    }

    private fun observeDeepLinks() {
        viewModelScope.launch {
            client.deepLink.collect { result ->
                val message = when (result) {
                    is DeepLinkResult.Found ->
                        "DeepLink: value=${result.deepLinkValue} " +
                            "deferred=${result.isDeferred} " +
                            "source=${result.mediaSource} " +
                            "campaign=${result.campaign}\n" +
                            "raw=${result.raw}"
                    is DeepLinkResult.NotFound -> "DeepLink: Not Found"
                    is DeepLinkResult.Error -> "DeepLink Error: ${result.message}"
                }
                appendLog(message)
            }
        }
    }

    fun logTestEvent() {
        client.logEvent("test_event", mapOf("key" to "value"))
        appendLog("Logged: test_event")
    }

    private fun appendLog(message: String) {
        _uiState.update { state ->
            state.copy(logs = state.logs + message)
        }
    }
}
