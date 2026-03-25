package com.mascill.keutrack.buildplugin.convention.utils

/**
 * Build type abstraction to define mandatory data
 */
interface BuildType {
    val type: String
    val isMinifyEnabled: Boolean
    val isDebuggable: Boolean
}

/**
 * Build Type Debug data
 */
object BuildTypeDebug : BuildType {
    override val type: String = "debug"
    override val isMinifyEnabled = false
    override val isDebuggable = true
    const val VERSION_NAME_SUFFIX = "-DEBUG"
}

/**
 * Build Type Release data
 */
object BuildTypeRelease : BuildType {
    override val type: String = "release"
    override val isMinifyEnabled = true
    override val isDebuggable = false
}