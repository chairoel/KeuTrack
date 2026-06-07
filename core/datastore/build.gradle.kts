plugins {
    alias(libs.plugins.keutrack.library)
    alias(libs.plugins.keutrack.hilt)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.mascill.keutrack.core.datastore"
    defaultConfig {
        consumerProguardFiles("consumer-proguard-rules.pro")
    }
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                register("java") {
                    option("lite")
                }
                register("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

androidComponents.beforeVariants {
    android.sourceSets.register(it.name) {
        val buildDir = layout.buildDirectory.get().asFile
        java.srcDir(buildDir.resolve("generated/source/proto/${it.name}/java"))
        kotlin.srcDir(buildDir.resolve("generated/source/proto/${it.name}/kotlin"))
    }
}

dependencies {
    implementation(projects.core.common)

    // api: consumers (e.g. :core:data) compile against proto types that extend GeneratedMessageLite
    // from protobuf-javalite; implementation() would hide the protobuf runtime from compile classpath.
    api(libs.protobuf.kotlin.lite)
    api(libs.androidx.datastore)
}
