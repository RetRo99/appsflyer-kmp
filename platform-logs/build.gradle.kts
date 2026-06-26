import io.github.frankois944.spmForKmp.swiftPackageConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.spmForKmp)
    `maven-publish`
}

group = "org.retar.platformlogs"
version = providers.gradleProperty("LIBRARY_VERSION").orElse("0.1.0").get()

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "org.retar.platformlogs"
        compileSdk = 36
        minSdk = 29

        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "PlatformLogs"
            isStatic = true
        }
        target.swiftPackageConfig(cinteropName = "PlatformLogsBridge") {
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.coroutines.core)
            }
        }
    }
}
