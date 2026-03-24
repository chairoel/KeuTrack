package com.mascill.keutrack.buildplugin.convention.utils

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

/**
 * Configure base Kotlin with Android options
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = BuildAndroidConfig.COMPILE_SDK_VERSION

        defaultConfig {
            minSdk = BuildAndroidConfig.MIN_SDK_VERSION
            testInstrumentationRunner = BuildAndroidConfig.TEST_INSTRUMENTATION_RUNNER


            externalNativeBuild {
                cmake {
                    cppFlags("")
                }
            }
        }

        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_17.toString()
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        buildFeatures {
            buildConfig = true
        }

        lint {
            lintConfig = rootProject.file(".lint/config.xml")
            checkAllWarnings = true
            warningsAsErrors = false
        }

    }
}

private fun CommonExtension<*, *, *, *, *, *>.kotlinOptions(block: KotlinJvmOptions.() -> Unit) {
    (this as ExtensionAware).extensions.configure("kotlinOptions", block)
}