package com.mascill.keutrack.buildplugin.convention.utils

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ProductFlavor

@Suppress("EnumEntryName")
enum class FlavorDimension {
    contentType
}

// The content for the app can either come from local static data which is useful for demo
// purposes, or from a production backend server which supplies up-to-date, real content.
// These two product flavors reflect this behaviour.
@Suppress("EnumEntryName")
enum class KeuTrackFlavor(
    val dimension: FlavorDimension,
    val appName: String,
    val applicationIdSuffix: String? = null,
    val versionNameSuffix: String? = null
) {
    dev(
        dimension = FlavorDimension.contentType,
        appName = "Dev KeuTrack",
        applicationIdSuffix = ".dev",
        versionNameSuffix = "-dev"
    ),
    prod(
        dimension = FlavorDimension.contentType,
        appName = "KeuTrack"
    ),
}

/**
 * Configure app product flavor options
 *
 * @param flavorConfigurationBlock block code, in-case need other flavor configuration for
 * some gradle plugin
 */
fun configureFlavors(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
    flavorConfigurationBlock: ProductFlavor.(flavor: KeuTrackFlavor) -> Unit = {},
) {
    commonExtension.apply {
        FlavorDimension.values().forEach { flavorDimension ->
            flavorDimensions += flavorDimension.name
        }

        productFlavors {
            KeuTrackFlavor.values().forEach { flavor ->
                register(flavor.name) {
                    dimension = flavor.dimension.name
                    flavorConfigurationBlock(this, flavor)

                    val manifestPlaceholders = mapOf(
                        Pair("appName", flavor.appName)
                    )
                    addManifestPlaceholders(manifestPlaceholders)

                    if (this@apply is ApplicationExtension && this is ApplicationProductFlavor) {
                        if (flavor.applicationIdSuffix != null) {
                            applicationIdSuffix = flavor.applicationIdSuffix
                        }

                        if (flavor.versionNameSuffix != null) {
                            versionNameSuffix = flavor.versionNameSuffix
                        }
                    }

                    // Native C NDK configuration
                    externalNativeBuild {
                        cmake {
                            // argument used to get data from C
                            arguments("-DTYPE=${flavor.name}")
                        }
                    }
                }
            }
        }
    }
}