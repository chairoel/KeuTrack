package com.mascill.keutrack.buildplugin.convention.utils

/**
 * Configuration of android build
 */
object BuildAndroidConfig {
    const val APPLICATION_ID = "com.mascill.keutrack"

    const val COMPILE_SDK_VERSION = 36
    const val MIN_SDK_VERSION = 28
    const val TARGET_SDK_VERSION = 36

    const val VERSION_CODE = 1
    const val VERSION_NAME = "1.0.0"

    const val SUPPORT_LIBRARY_VECTOR_DRAWABLES = true

    const val TEST_INSTRUMENTATION_RUNNER = "androidx.test.runner.AndroidJUnitRunner"
    val TEST_INSTRUMENTATION_RUNNER_ARGUMENTS = mapOf(
        "leakcanary.FailTestOnLeakRunListener" to "listener"
    )
}