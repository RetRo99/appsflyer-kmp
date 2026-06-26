import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

val afDevKey = localProps.getProperty("appsflyer.devKey")
    ?: error("appsFlyer.devKey not found in local.properties. Add: appsflyer.devKey=YOUR_KEY")

android {
    namespace = "org.retar.appsflyer.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.retar.appsflyer.sample"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
            buildConfigField(
                "String",
                "AF_DEV_KEY",
                "\"$afDevKey\"",
            )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":samples:demo-app:composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose.android)
}
