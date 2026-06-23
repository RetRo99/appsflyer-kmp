package com.retro99.appsflyer.sample

import android.app.Activity
import android.os.Bundle
import android.view.WindowInsets
import android.widget.ScrollView
import android.widget.TextView
import com.retro99.appsflyer.CampaignData
import com.retro99.appsflyer.DeepLinkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var collectJob: Job? = null
    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        output = TextView(this).apply {
            setPadding(48, 48, 48, 48)
            textSize = 14f
        }
        val scrollView = ScrollView(this).also { it.addView(output) }
        scrollView.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(scrollView)

        val client = (application as SampleApp).client
        collectJob = scope.launch {
            launch {
                val data = client.getConversionData()
                when (data) {
                    is CampaignData.Success -> appendLine(
                        "Conversion: status=${data.status} " +
                            "mediaSource=${data.mediaSource} campaign=${data.campaign}",
                    )
                    is CampaignData.Error -> appendLine(
                        "Conversion error: ${data.message}",
                    )
                }
            }
            launch {
                client.deepLink.collect { result ->
                    when (result) {
                        is DeepLinkResult.Found -> appendLine(
                            "DeepLink: value=${result.deepLinkValue} " +
                                "deferred=${result.isDeferred} " +
                                "mediaSource=${result.mediaSource} " +
                                "campaign=${result.campaign}\n" +
                                "  raw=${result.raw}",
                        )
                        is DeepLinkResult.NotFound -> appendLine("DeepLink: not found")
                        is DeepLinkResult.Error -> appendLine(
                            "DeepLink error: ${result.message}",
                        )
                    }
                }
            }
            launch {
                val result = client.getStartResult()
                appendLine("StartResult: $result")
            }
        }
    }

    private fun appendLine(line: String) {
        output.append(line + "\n\n")
    }

    override fun onDestroy() {
        collectJob?.cancel()
        super.onDestroy()
    }
}
