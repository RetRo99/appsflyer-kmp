import io.github.frankois944.spmForKmp.swiftPackageConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.spmForKmp)
    `maven-publish`
}

group = "com.retro99.appsflyer"
version = providers.gradleProperty("LIBRARY_VERSION").orElse("0.1.0").get()

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.retro99.appsflyer"
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

    // JVM target exists only for running unit tests that need org.json
    jvm {
        withSourcesJar(publish = false)
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
            baseName = "AppsFlyerKmp"
            isStatic = true
        }
        target.swiftPackageConfig(cinteropName = "AppsFlyerBridge") {
            dependency {
                remotePackageVersion(
                    url = uri("https://github.com/AppsFlyerSDK/AppsFlyerFramework.git"),
                    version = libs.versions.appsflyerIosSdk.get(),
                    products = {
                        add("AppsFlyerLib")
                    },
                )
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val jvmCommon by creating
        jvmCommon.dependsOn(commonMain.get())
        androidMain.get().dependsOn(jvmCommon)
        jvmMain.get().dependsOn(jvmCommon)

        commonMain {
            dependencies {
                api(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
            }
        }
        androidMain {
            dependencies {
                api(libs.appsflyer.android.sdk)
            }
        }
        jvmCommon.dependencies {
            compileOnly(libs.json)
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.json)
            }
        }
    }
}

publishing {
    publications.withType<org.gradle.api.publish.maven.MavenPublication>().configureEach {
        pom {
            name.set("appsflyer-kmp")
            description.set("Kotlin Multiplatform wrapper for the AppsFlyer SDK (Android + iOS).")
            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/license/mit")
                }
            }
            developers {
                developer {
                    id.set("retro99")
                    name.set("Retros99")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/retro99/appsflyer-kmp.git")
                url.set("https://github.com/retro99/appsflyer-kmp")
            }
        }
    }
}
