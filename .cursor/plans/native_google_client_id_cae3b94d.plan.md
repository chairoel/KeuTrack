---
name: Native Google Client ID
overview: Pindahkan Google Server Client ID dari BuildConfig ke native C++ (NDK) mengikuti pattern yang sudah ada untuk base URL di core/network, agar lebih aman dan konsisten.
todos:
  - id: add-cpp-function
    content: Tambahkan getGoogleServerClientId JNI function di network_native_config.cpp
    status: completed
  - id: add-kotlin-external
    content: Tambahkan external fun getGoogleServerClientId() di NetworkNativeWrapper.kt
    status: completed
  - id: inject-native-wrapper
    content: Inject NetworkNativeWrapper ke GoogleAuthDataSourceImpl, ganti BuildConfig dengan native call
    status: completed
  - id: revert-buildconfig
    content: Hapus buildFeatures dan defaultConfig dari core/data/build.gradle.kts
    status: completed
isProject: false
---

# Pindahkan Google Server Client ID ke Native C++

Mengganti pendekatan `BuildConfig` yang sudah diimplementasi sebelumnya dengan pendekatan native C++ (NDK), mengikuti pattern yang sudah ada untuk base URL di `core/network`.

## Perubahan

### 1. Tambahkan JNI function di C++ native code

**File:** [core/network/src/main/cpp/network_native_config.cpp](core/network/src/main/cpp/network_native_config.cpp)

Tambahkan function baru `getGoogleServerClientId` yang return client ID berdasarkan flavor (`TYPE`):

```cpp
jstring
Java_com_mascill_keutrack_core_network_utils_NetworkNativeWrapper_getGoogleServerClientId(
        JNIEnv *env, jobject thi) {
    std::string type = TYPE;
    std::string clientId;

    if (type == "dev") {
        clientId = "100547827166-uqtn9is2df1k931808lm6ff8i79ns988.apps.googleusercontent.com";
    } else {
        clientId = "100547827166-uqtn9is2df1k931808lm6ff8i79ns988.apps.googleusercontent.com";
    }

    return env->NewStringUTF(clientId.c_str());
}
```

Kedua flavor saat ini memakai client ID yang sama. Nanti tinggal ganti value `prod` ketika sudah punya client ID production yang berbeda.

### 2. Tambahkan external function di Kotlin wrapper

**File:** [core/network/src/main/kotlin/com/mascill/keutrack/core/network/utils/NetworkNativeWrapper.kt](core/network/src/main/kotlin/com/mascill/keutrack/core/network/utils/NetworkNativeWrapper.kt)

Tambahkan deklarasi external function:

```kotlin
external fun getGoogleServerClientId(): String
```

### 3. Inject `NetworkNativeWrapper` ke `GoogleAuthDataSourceImpl`

**File:** [core/data/src/main/kotlin/com/mascill/keutrack/core/data/datasource/GoogleAuthDataSourceImpl.kt](core/data/src/main/kotlin/com/mascill/keutrack/core/data/datasource/GoogleAuthDataSourceImpl.kt)

- Tambahkan `NetworkNativeWrapper` sebagai constructor parameter (sudah tersedia di Hilt graph via `NetworkModule.provideNetworkNativeWrapper()`)
- Ganti `BuildConfig.GOOGLE_SERVER_CLIENT_ID` dengan `nativeWrapper.getGoogleServerClientId()`
- Hapus import `com.mascill.keutrack.core.data.BuildConfig`

```kotlin
class GoogleAuthDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nativeWrapper: NetworkNativeWrapper
) : GoogleAuthDataSource {
    // ...
    .setServerClientId(nativeWrapper.getGoogleServerClientId())
}
```

### 4. Revert BuildConfig dari core/data build.gradle.kts

**File:** [core/data/build.gradle.kts](core/data/build.gradle.kts)

Hapus block `buildFeatures` dan `defaultConfig` yang ditambahkan sebelumnya, kembalikan ke:

```kotlin
android {
    namespace = "com.mascill.keutrack.core.data"
}
```

BuildConfig tidak lagi diperlukan di module ini.
