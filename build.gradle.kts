import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.gitSemVer)
    alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.taskTree)
}

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            filter {
                isFailOnNoMatchingTests = false
            }
            testLogging {
                showExceptions = true
                events = setOf(
                    TestLogEvent.FAILED,
                    TestLogEvent.PASSED,
                )
                exceptionFormat = TestExceptionFormat.FULL
            }
        }
    }

    js(IR) {
        browser{
            useEsModules()
        }
        nodejs{
            useCommonJs()
        }
        binaries.library()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                api(libs.arrow.core)
                implementation(libs.kotlinx.coroutines.core)
                api(libs.kotlin.logging)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.bundles.kotlin.testing.common)
                implementation(libs.bundles.kotest.common)
            }
        }

        val multithreadMain by creating{
            dependencies{
                dependsOn(commonMain)
                implementation(libs.bundles.kmqtt)
            }
        }

        val jsMain by getting{
            dependencies{
                dependsOn(commonMain)
                implementation(npm("mqtt", "5.5.3")) //todo use toml
                implementation("org.jetbrains.kotlin:kotlin-stdlib-js:1.9.23")
            }
        }

        val jvmMain by getting {
            dependencies {
                dependsOn(multithreadMain)
                api(libs.slf4j.simple)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.runner.junit5)
            }
        }
        val nativeMain by creating {
            dependsOn(multithreadMain)
        }
        val nativeTest by creating {
            dependsOn(commonTest)
        }
        val linuxX64Main by creating {
            dependencies {
                dependsOn(nativeMain)
                implementation(files("openssl/openssl-linux-x64.klib"))
            }
        }
        val linuxX64Test by creating {
            dependencies {
                dependsOn(nativeTest)
                implementation(files("openssl/openssl-linux-x64.klib"))
            }
        }

        val tvosSimulatorArm64Main by creating {
            dependencies {
                dependsOn(nativeMain)
                implementation(files("openssl/openssl-tvos-simulator-arm64.klib"))
            }
        }
        val tvosSimulatorArm64Test by creating {
            dependencies {
                dependsOn(nativeTest)
                implementation(files("openssl/openssl-tvos-simulator-arm64.klib"))
            }
        }

        val tvosArm64Main by creating {
            dependencies {
                dependsOn(nativeMain)
                implementation(files("openssl/openssl-tvos-arm64.klib"))
            }
        }
        val tvosArm64Test by creating {
            dependencies {
                dependsOn(nativeTest)
                implementation(files("openssl/openssl-tvos-arm64.klib"))
            }
        }

        val watchosSimulatorArm64Main by creating {
            dependencies {
                dependsOn(nativeMain)
                implementation(files("openssl/openssl-watchos-simulator-arm64.klib"))
            }
        }
        val watchosSimulatorArm64Test by creating {
            dependencies {
                dependsOn(nativeTest)
                implementation(files("openssl/openssl-watchos-simulator-arm64.klib"))
            }
        }

        val watchosArm64Main by creating {
            dependencies {
                dependsOn(nativeMain)
                implementation(files("openssl/openssl-watchos-arm64.klib"))
            }
        }
        val watchosArm64Test by creating {
            dependencies {
                dependsOn(nativeTest)
                implementation(files("openssl/openssl-watchos-arm64.klib"))
            }
        }

        val iosX64Main by creating {
            dependencies {
                dependsOn(nativeMain)
                implementation(files("openssl/openssl-ios-x64.klib"))
            }
        }
        val iosX64Test by creating {
            dependencies {
                dependsOn(nativeTest)
                implementation(files("openssl/openssl-ios-x64.klib"))
            }
        }

        val iosSimulatorArm64Main by creating {
            dependencies {
                dependsOn(nativeMain)
                implementation(files("openssl/openssl-ios-simulator-arm64.klib"))
            }
        }
        val iosSimulatorArm64Test by creating {
            dependencies {
                dependsOn(nativeTest)
                implementation(files("openssl/openssl-ios-simulator-arm64.klib"))
            }
        }

        val iosArm64Main by creating {
            dependencies {
                dependsOn(nativeMain)
                implementation(files("openssl/openssl-ios-arm64.klib"))
            }
        }
        val iosArm64Test by creating {
            dependencies {
                dependsOn(nativeTest)
                implementation(files("openssl/openssl-ios-arm64.klib"))
            }
        }

        val macosArm64Main by creating {
            dependencies {
                dependsOn(nativeMain)
                implementation(files("openssl/openssl-macos-arm64.klib"))
            }
        }
        val macosArm64Test by creating {
            dependencies {
                dependsOn(nativeTest)
                implementation(files("openssl/openssl-macos-arm64.klib"))
            }
        }

        val macosX64Main by creating {
            dependencies {
                dependsOn(nativeMain)
                implementation(files("openssl/openssl-macos-x64.klib"))
            }
        }
        val macosX64Test by creating {
            dependencies {
                dependsOn(nativeTest)
                implementation(files("openssl/openssl-macos-x64.klib"))
            }
        }

        val mingwX64Main by creating {
            dependencies {
                dependsOn(nativeMain)
                implementation(files("openssl/openssl-mingw-x64.klib"))
            }
        }
        val mingwX64Test by creating {
            dependencies {
                dependsOn(nativeTest)
                implementation(files("openssl/openssl-mingw-x64.klib"))
            }
        }
    }

    val nativeSetup: KotlinNativeTarget.(targetName: String) -> Unit = { targetName ->
        compilations["main"].defaultSourceSet.dependsOn(sourceSets[targetName+"Main"])
        compilations["test"].defaultSourceSet.dependsOn(kotlin.sourceSets[targetName+"Test"])
        binaries {
            executable() //todo capire peroblema build gradle
            sharedLib()
            staticLib()
        }
    }

    applyDefaultHierarchyTemplate()


    linuxX64 {
        nativeSetup("linuxX64")
    }

    mingwX64 {
        nativeSetup("mingwX64")
    }

    macosX64 {
        nativeSetup("macosX64")
    }

    macosArm64 {
        nativeSetup("macosArm64")
    }

    iosArm64 {
        nativeSetup("iosArm64")
    }

    iosSimulatorArm64 {
        nativeSetup("iosSimulatorArm64")
    }

    iosX64 {
        nativeSetup("iosX64")
    }

    watchosArm64 {
        nativeSetup("watchosArm64")
    }

    watchosSimulatorArm64 {
        nativeSetup("watchosSimulatorArm64")
    }

    tvosArm64 {
        nativeSetup("tvosArm64")
    }

    tvosSimulatorArm64 {
        nativeSetup("tvosSimulatorArm64")
    }

    targets.all {
        compilations.all {
            kotlinOptions {
                allWarningsAsErrors = true
                freeCompilerArgs += listOf("-Xexpect-actual-classes")
            }
        }
    }

    val os = OperatingSystem.current()
    val excludeTargets = when {
        os.isLinux -> kotlin.targets.filterNot { "linux" in it.name }
        os.isWindows -> kotlin.targets.filterNot { "mingw" in it.name }
        os.isMacOsX -> kotlin.targets.filter { "linux" in it.name || "mingw" in it.name }
        else -> emptyList()
    }.mapNotNull { it as? KotlinNativeTarget }

    configure(excludeTargets) {
        compilations.configureEach {
            cinterops.configureEach { tasks[interopProcessingTaskName].enabled = false }
            compileTaskProvider.get().enabled = false
            tasks[processResourcesTaskName].enabled = false
        }
        binaries.configureEach { linkTask.enabled = false }
    }
}
