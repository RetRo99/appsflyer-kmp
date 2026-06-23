plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.retro99.appsflyer.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.retro99.appsflyer.sample"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField(
            "String",
            "DEV_KEY",
            "\"${project.findProperty("appsflyer.devKey") ?: ""}\"",
        )
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":appsflyer"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.ads.identifier)
}
