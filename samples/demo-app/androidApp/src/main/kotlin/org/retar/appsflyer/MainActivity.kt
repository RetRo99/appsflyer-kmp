package org.retar.appsflyer.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import org.retar.appsflyer.AppsFlyer
import org.retar.appsflyer.AppsFlyerConfig
import org.retar.appsflyer.initialize

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AppsFlyer.initialize(
            context = this,
            config = AppsFlyerConfig(
                devKey = BuildConfig.AF_DEV_KEY,
                isDebug = true,
            ),
        )

        setContent {
            val viewModel: DemoViewModel = viewModel()
            DemoScreen(viewModel)
        }
    }
}
