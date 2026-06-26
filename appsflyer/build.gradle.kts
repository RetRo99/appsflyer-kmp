import io.github.frankois944.spmForKmp.swiftPackageConfig
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.spmForKmp)
    alias(libs.plugins.mavenPublish)
}

group = "org.retar.appsflyer"
version = providers.gradleProperty("LIBRARY_VERSION").orElse("0.1.0").get()

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "org.retar.appsflyer"
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

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)

    if (providers.gradleProperty("signingInMemoryKey").isPresent ||
        providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey").isPresent
    ) {
        signAllPublications()
    }

    coordinates(group.toString(), "appsflyer", version.toString())

    pom {
        name.set("appsflyer-kmp")
        description.set("Kotlin Multiplatform wrapper for the AppsFlyer SDK (Android + iOS).")
        inceptionYear.set("2026")
        url.set("https://github.com/RetRo99/appsflyer-kmp")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/license/mit")
                distribution.set("https://opensource.org/license/mit")
            }
        }

        developers {
            developer {
                id.set("RetRo99")
                name.set("Rok Retar")
                url.set("https://github.com/RetRo99")
            }
        }

        scm {
            url.set("https://github.com/RetRo99/appsflyer-kmp")
            connection.set("scm:git:git://github.com/RetRo99/appsflyer-kmp.git")
            developerConnection.set("scm:git:ssh://git@github.com/RetRo99/appsflyer-kmp.git")
        }
    }
}
