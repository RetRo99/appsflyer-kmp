package com.retro99.appsflyer.sample

import android.app.Application
import com.retro99.appsflyer.AppsFlyerClient
import com.retro99.appsflyer.AppsFlyerClientFactory
import com.retro99.appsflyer.AppsFlyerConfig

class SampleApp : Application() {

    lateinit var client: AppsFlyerClient
        private set

    override fun onCreate() {
        super.onCreate()
        client = AppsFlyerClientFactory(this).create(
            AppsFlyerConfig(
                devKey = BuildConfig.DEV_KEY,
                isDebug = true,
            ),
        )
        client.setCustomerUserId("sample-user-001")
        client.start()
    }
}
