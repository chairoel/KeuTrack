---
name: Merge Firebase Plugins
overview: Menggabungkan `KeuTrackFirebasePlugin` dan `KeuTrackFirebaseAppPlugin` menjadi satu plugin yang otomatis mendeteksi tipe module (application vs library) untuk menentukan apakah perlu apply `google-services` plugin.
todos:
  - id: rewrite-firebase-plugin
    content: Rewrite KeuTrackFirebasePlugin.kt dengan auto-detect application module via pluginManager.withPlugin
    status: completed
  - id: delete-firebase-app-plugin
    content: Hapus KeuTrackFirebaseAppPlugin.kt
    status: completed
  - id: update-convention-build
    content: Update convention/build.gradle.kts — hapus registrasi keutrackFirebaseAppPlugin, ubah ID plugin
    status: completed
  - id: update-version-catalog
    content: Update libs.versions.toml — ganti 2 entry menjadi 1 keutrack-firebase
    status: completed
  - id: update-app-build
    content: Update app/build.gradle.kts — ganti keutrack.firebase.app menjadi keutrack.firebase
    status: completed
  - id: update-core-data-build
    content: Update core/data/build.gradle.kts — ganti keutrack.firebase.lib menjadi keutrack.firebase
    status: completed
isProject: false
---

# Merge 2 Firebase Convention Plugins Menjadi 1

## Kondisi Saat Ini

Terdapat 2 plugin Firebase yang isinya hampir identik:

- `**KeuTrackFirebasePlugin**` (`keutrack.firebase.lib`) — dipakai di `core/data`, hanya menambah Firebase deps
- `**KeuTrackFirebaseAppPlugin**` (`keutrack.firebase.app`) — dipakai di `:app`, apply `google-services` + Firebase deps + Credentials API

Masalah tambahan: Credentials API (`androidx-credentials`, `googleid`) sudah dideklarasikan langsung di `[core/data/build.gradle.kts](core/data/build.gradle.kts)` baris 15-17, sehingga **duplikat** dengan yang ada di `KeuTrackFirebaseAppPlugin`.

## Strategi

Gabung menjadi 1 plugin `KeuTrackFirebasePlugin` dengan ID `keutrack.firebase`. Plugin ini:

- Selalu menambah Firebase dependencies (bom, auth, firestore, coroutines-play-services)
- Hanya apply `com.google.gms.google-services` jika module adalah **application** (bukan library)
- **Tidak** menyertakan Credentials API — biarkan tetap di `core/data` yang memang membutuhkannya

## Perubahan per File

### 1. Rewrite `[KeuTrackFirebasePlugin.kt](build-plugin/convention/src/main/kotlin/com/mascill/keutrack/buildplugin/convention/plugins/KeuTrackFirebasePlugin.kt)`

Ubah isinya menjadi:

```kotlin
class KeuTrackFirebasePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.application") {
                apply(plugin = "com.google.gms.google-services")
            }

            dependencies {
                add("implementation", platform(libs.findLibrary("firebase-bom").get()))
                add("implementation", libs.findLibrary("firebase-auth").get())
                add("implementation", libs.findLibrary("firebase-firestore").get())
                add("implementation", libs.findLibrary("kotlinx-coroutines-play-services").get())
            }
        }
    }
}
```

Menggunakan `pluginManager.withPlugin(...)` yang hanya trigger callback jika `com.android.application` sudah di-apply. Ini lebih aman dibanding `hasPlugin()` karena tidak bergantung pada urutan apply plugin.

### 2. Hapus `[KeuTrackFirebaseAppPlugin.kt](build-plugin/convention/src/main/kotlin/com/mascill/keutrack/buildplugin/convention/plugins/KeuTrackFirebaseAppPlugin.kt)`

File ini tidak diperlukan lagi.

### 3. Update `[convention/build.gradle.kts](build-plugin/convention/build.gradle.kts)`

- Hapus registrasi `keutrackFirebaseAppPlugin` (baris 100-107)
- Ubah registrasi `keutrackFirebasePlugin` agar menggunakan plugin ID baru `keutrack.firebase`

```kotlin
plugins.register("keutrackFirebasePlugin") {
    id = libs.plugins.keutrack.firebase.get().pluginId
    implementationClass =
        "com.mascill.keutrack.buildplugin.convention.plugins.KeuTrackFirebasePlugin"
}
```

### 4. Update `[gradle/libs.versions.toml](gradle/libs.versions.toml)`

- Hapus `keutrack-firebase-app` dan `keutrack-firebase-lib`
- Tambah satu entry:

```toml
keutrack-firebase = { id = "keutrack.firebase" }
```

### 5. Update `[app/build.gradle.kts](app/build.gradle.kts)` baris 11

```diff
- alias(libs.plugins.keutrack.firebase.app)
+ alias(libs.plugins.keutrack.firebase)
```

### 6. Update `[core/data/build.gradle.kts](core/data/build.gradle.kts)` baris 4

```diff
- alias(libs.plugins.keutrack.firebase.lib)
+ alias(libs.plugins.keutrack.firebase)
```

Credentials API deps di baris 15-17 **tetap dipertahankan** karena memang `core/data` yang membutuhkannya untuk Google Sign-In.

## Hasil Akhir

- 1 plugin `keutrack.firebase` yang bisa dipakai di module manapun
- `google-services` otomatis di-apply hanya di application module
- Tidak ada duplikasi dependency
- Credentials API tetap di `core/data` (tempat yang benar)
