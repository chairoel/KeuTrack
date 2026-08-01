# Phase Auth — Google Sign-In Credential Stability

> **Modul target:** `:features:auth` (+ sync copy di `docs/firebase/FIREBASE_AUTH_ERROR_MAPPING.md`)  
> **Estimasi:** ~0.5–1 hari  
> **Prasyarat:** Auth Google + email sudah wired (lihat `docs/firebase/PR_C_AUTH_UI_WIRING.md`, `docs/dev/Project_Assessment.md` — Auth COMPLETE)  
> **Status:** Done (implemented)  
> **Status baseline (sebelum fix):** `GoogleSignInTokenProvider` hanya memakai `GetGoogleIdOption` (One Tap / bottom sheet); intermittent `NoCredentialException` di cold start → UI bilang “No Google account found…” padahal akun ada; retry kedua sering berhasil  
> **Hasil akhir:** Tombol Sign in with Google memakai flow **explicit** (`GetSignInWithGoogleOption`) → UI **account picker dialog** (bukan bottom sheet One Tap); pesan error tidak menyesatkan; Login & Register tidak regresi  
> **Living docs:** [`FIRESTORE_LOGIN_INTEGRATION.md` — Google Sign-In UI](../../firebase/FIRESTORE_LOGIN_INTEGRATION.md#google-sign-in-ui-credential-manager), [`FIREBASE_AUTH_ERROR_MAPPING.md`](../../firebase/FIREBASE_AUTH_ERROR_MAPPING.md)

---

## Daftar Isi

1. [Konteks & Tujuan](#1-konteks--tujuan)
2. [Inventory — Apa yang Sudah Ada](#2-inventory--apa-yang-sudah-ada)
3. [Root Cause](#3-root-cause)
4. [Keputusan Desain](#4-keputusan-desain)
5. [Scope — Apa yang Dikerjakan](#5-scope--apa-yang-dikerjakan)
6. [Scope — Apa yang TIDAK Dikerjakan](#6-scope--apa-yang-tidak-dikerjakan)
7. [Prasyarat (Definition of Ready)](#7-prasyarat-definition-of-ready)
8. [File Referensi (Read-Only)](#8-file-referensi-read-only)
9. [File yang TIDAK BOLEH Diubah](#9-file-yang-tidak-boleh-diubah)
10. [File yang BOLEH Diubah / Dibuat](#10-file-yang-boleh-diubah--dibuat)
11. [Struktur File Target](#11-struktur-file-target)
12. [Desain Solusi](#12-desain-solusi)
13. [Task Breakdown Detail](#13-task-breakdown-detail)
14. [Acceptance Criteria](#14-acceptance-criteria)
15. [Catatan Arsitektur & Konvensi](#15-catatan-arsitektur--konvensi)
16. [Dependency Graph](#16-dependency-graph)
17. [Risiko & Mitigasi](#17-risiko--mitigasi)
18. [Urutan Pengerjaan yang Disarankan](#18-urutan-pengerjaan-yang-disarankan)
19. [Relasi ke Phase / Docs Lain](#19-relasi-ke-phase--docs-lain)
20. [Manual Test Plan](#20-manual-test-plan)

---

## 1. Konteks & Tujuan

Auth Google di KeuTrack sudah end-to-end (Credential Manager → idToken → Firebase Auth → Firestore profile → DataStore → Home). Bug yang dilaporkan adalah **reliability UX**, bukan missing feature:

| Gejala | Observasi |
|--------|-----------|
| Tap “Sign in with Google” segera setelah app launch | Kadang error “No Google account found on this device.” |
| Tap kedua tanpa ubah akun | Account picker muncul / login sukses |
| Logcat sekitar cold start | `GoogleApiManager` / `DEVELOPER_ERROR` / `Unknown calling package name 'com.google.android.gms'` (noise GMS; sering di emulator) |

**Tujuan phase ini:**
1. Samakan flow tombol Google dengan **best practice Android Identity**: explicit **Sign in with Google** (`GetSignInWithGoogleOption`).
2. Hilangkan false-negative “tidak ada akun Google” saat Credential Manager One Tap belum siap / tidak menemukan credential.
3. Perbarui copy error + dokumentasi mapping agar arti `NoCredential` akurat.

**Bukan tujuan:**
- Redesign UI Login/Register
- Mengubah kontrak `TokenResult` / `AuthResult` / `UserRepository`
- Mengganti Firebase Auth / menambahkan provider baru
- Memperbaiki noise Logcat GMS di emulator (di luar kontrol app)

---

## 2. Inventory — Apa yang Sudah Ada

### Credential pipeline

| Item | Lokasi | Perilaku hari ini |
|------|--------|-------------------|
| `GoogleSignInTokenProvider` | `features/auth/.../data/GoogleSignInTokenProvider.kt` | Hanya `GetGoogleIdOption` + `setFilterByAuthorizedAccounts(false)` |
| Deps Credential Manager | `features/auth/build.gradle.kts` | `credentials` 1.3.0 + `credentials-play-services-auth` + `googleid` 1.1.1 |
| Web client ID | `NetworkNativeWrapper.getGoogleServerClientId()` | Dipakai sebagai `serverClientId` |
| Login / Register VM | `LoginViewModel` / `RegisterViewModel` | `getGoogleIdToken` → `userRepository.signInWithGoogle` |
| UI wire | `LoginScreen` / `RegisterScreen` | `LocalContext.current` → `signInWithGoogle(context)` |
| Domain results | `TokenResult`, `AuthResult` | `Error.NoCredential` sudah ada |

### Mapping error (docs + UI)

| Sumber | Pesan Google `NoCredential` |
|--------|-----------------------------|
| `LoginViewModel` / `RegisterViewModel` | `"No Google account found on this device."` |
| `docs/firebase/FIREBASE_AUTH_ERROR_MAPPING.md` | Sama; mengartikan “Tidak ada akun Google di device” |

### Yang sudah benar

- `setFilterByAuthorizedAccounts(false)` — tidak membatasi ke akun yang pernah authorize app saja.
- `GetCredentialCancellationException` → `TokenResult.Cancelled` (user dismiss → Idle).
- Dependency `credentials-play-services-auth` sudah ada (wajib untuk GMS path).
- `serverClientId` dari native wrapper (harus Web client / `client_type: 3` di `google-services.json`).

---

## 3. Root Cause

```
User tap Google button (explicit intent)
  → GoogleSignInTokenProvider memakai GetGoogleIdOption (One Tap / bottomsheet)
  → Credential Manager / Play Services belum siap ATAU bottomsheet flow
     tidak menemukan credential “immediately available”
  → NoCredentialException
  → UI: "No Google account found on this device."   ← copy menyesatkan
  → Retry kedua: GMS warm → credential muncul → sukses
```

Poin penting (dari docs Android Identity + troubleshooting Credential Manager):

- `NoCredentialException` **bukan** bukti “device tidak punya Google account”.
- `GetGoogleIdOption` (One Tap) lebih sensitif terhadap: GMS cold start, Sign-in prompts disabled, akun perlu re-auth, quirk emulator.
- Untuk **tombol** “Sign in with Google”, API yang tepat adalah `GetSignInWithGoogleOption` (account picker / button flow).

Ini terkait (tapi terpisah dari) noise Logcat `GoogleApiManager` saat startup — keduanya menandakan GMS sering belum stabil di request pertama.

---

## 4. Keputusan Desain

### Keputusan utama (wajib)

**Tombol Google di Login & Register memakai `GetSignInWithGoogleOption` sebagai flow utama.**

**Implikasi UX (penting):** picker berubah dari One Tap **bottom sheet** (`GetGoogleIdOption`) menjadi SIWG **account picker dialog**. Ini perilaku sistem Credential Manager untuk button flow — bukan regresi layout app.

Alasan:
- User sudah menekan tombol eksplisit → cocok button flow, bukan One Tap oportunistik.
- Account picker lebih andal menampilkan akun device.
- Sesuai [Implement Sign in with Google](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation) untuk SIWG button.

### Keputusan sekunder (copy)

Ubah pesan UI untuk `TokenResult.Error.NoCredential` / Google path:

| Sebelum | Sesudah (usulan) |
|---------|------------------|
| `No Google account found on this device.` | `Unable to get a Google account. Please try again.` |

Cadangan jika ingin lebih spesifik setelah SIWG juga gagal:  
`No Google account available. Add a Google account in device Settings and try again.`

### Alternatif yang **tidak** dipilih sebagai default

| Alternatif | Kenapa ditunda |
|------------|----------------|
| Hybrid: `GetGoogleIdOption` dulu → fallback `GetSignInWithGoogleOption` | Valid (best practice One Tap + fallback), tapi untuk tombol KeuTrack menambah kompleksitas; SIWG-only sudah cukup |
| Soft-retry delay 300–500ms pada `NoCredentialException` | Workaround race; kurang clean dibanding ganti API |
| Naikkan `filterByAuthorizedAccounts(true)` dulu | Google pattern untuk One Tap; tidak mengatasi cold-start GMS; bisa justru lebih sering `NoCredential` di first-time |

Jika nanti ingin One Tap di splash/home (auto prompt), baru pertimbangkan hybrid di phase terpisah.

---

## 5. Scope — Apa yang Dikerjakan

1. Refactor `GoogleSignInTokenProvider.getGoogleIdToken` ke `GetSignInWithGoogleOption`.
2. Pastikan parsing credential tetap lewat `GoogleIdTokenCredential` / `CustomCredential` type yang sama.
3. Update copy error Google `NoCredential` di `LoginViewModel` + `RegisterViewModel`.
4. Sync tabel mapping di `docs/firebase/FIREBASE_AUTH_ERROR_MAPPING.md`.
5. Manual QA cold-start Google Sign-In (device fisik + emulator jika dipakai).

---

## 6. Scope — Apa yang TIDAK Dikerjakan

- Ubah `TokenResult`, `AuthResult`, `UserRepository`, `UserRepositoryImpl`
- Ubah Firebase Console / SHA-1 / `google-services.json` (kecuali QA membuktikan `DEVELOPER_ERROR` config sungguhan)
- Email login/register flow
- Splash `syncUserProfile`
- UI redesign tombol Google
- Unit test instrumented Credential Manager (opsional follow-up)
- Suppress / filter Logcat `GoogleApiManager`

---

## 7. Prasyarat (Definition of Ready)

- [ ] Flavor `dev` build jalan: `./gradlew assembleDevDebug`
- [ ] Device/emulator punya ≥1 Google account signed-in
- [ ] Firebase Auth Google provider enabled (project `keutrack-dev`)
- [ ] `getGoogleServerClientId()` mengembalikan **Web client ID** (`client_type: 3` di `app/src/dev/google-services.json`)
- [ ] SHA-1 debug keystore terdaftar di Firebase Console untuk `com.mascill.keutrack.dev` (lihat `./gradlew signingReport`)
- [ ] Baseline repro: cold start → tap Google sekali → (sering) error “No Google account…” → tap kedua sukses

---

## 8. File Referensi (Read-Only)

Baca dulu sebelum implementasi. Jangan diubah kecuali disebut di §10.

### Kode

| File | Kenapa relevan |
|------|----------------|
| `features/auth/.../data/GoogleSignInTokenProvider.kt` | Target utama refactor |
| `features/auth/.../presentation/LoginViewModel.kt` | Mapping `TokenResult` → `AuthState` |
| `features/auth/.../presentation/RegisterViewModel.kt` | Mapping sama untuk Google button |
| `features/auth/.../presentation/LoginScreen.kt` | `LocalContext.current` → pastikan Activity context |
| `features/auth/.../presentation/RegisterScreen.kt` | Idem |
| `core/domain/.../model/TokenResult.kt` | Kontrak hasil token — **jangan diubah** |
| `core/domain/.../model/AuthResult.kt` | Mapping Firebase — referensi saja |
| `core/network/.../NetworkNativeWrapper.kt` | Sumber `serverClientId` |
| `app/src/dev/google-services.json` | Verifikasi Web client ID + package `com.mascill.keutrack.dev` |
| `features/auth/build.gradle.kts` | Deps credentials / googleid |
| `gradle/libs.versions.toml` | Versi `credentials` / `googleId` |

### Docs proyek

| File | Kenapa relevan |
|------|----------------|
| `docs/firebase/PR_C_AUTH_UI_WIRING.md` | Baseline wiring Google button + AuthState mapping |
| `docs/firebase/FIREBASE_AUTH_ERROR_MAPPING.md` | Tabel pesan UI yang harus di-sync |
| `docs/firebase/FIRESTORE_LOGIN_INTEGRATION.md` | Alur Auth → Firestore end-to-end |
| `docs/firebase/PR_B_USER_REPOSITORY_ORCHESTRATION.md` | Kontrak `signInWithGoogle(idToken)` |
| `docs/dev/Project_Assessment.md` | Status Auth COMPLETE; konteks polish |
| `.cursor/skills/keutrack-dev/SKILL.md` | Catatan protected files — phase ini **sengaja** menyentuh `features/auth` |
| `.cursor/rules/keutrack-architecture.mdc` | Feature ↔ domain; CancellationException pattern |
| `.cursor/rules/keutrack-feature-module.mdc` | Struktur feature auth |

### External (Android Identity)

| Topik | URL |
|-------|-----|
| Implement Sign in with Google | https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation |
| Credential Manager troubleshooting (`NoCredentialException`) | https://developer.android.com/identity/sign-in/credential-manager-troubleshooting-guide |

---

## 9. File yang TIDAK BOLEH Diubah

| File / area | Alasan |
|-------------|--------|
| `core/domain/.../model/TokenResult.kt` | Kontrak stabil |
| `core/domain/.../model/AuthResult.kt` | Kontrak stabil |
| `core/domain/.../repository/UserRepository.kt` | Tidak perlu API baru |
| `core/data/.../UserRepositoryImpl.kt` | Downstream idToken tidak berubah |
| `core/data/.../datasource/**` Auth/Firestore | Di luar scope |
| `features/splashscreen/**` | Tidak terkait |
| `build-plugin/**`, `settings.gradle.kts` | Tidak terkait |
| Login/Register **visual** composable (layout/theme) | Hanya wiring/copy error jika perlu |

---

## 10. File yang BOLEH Diubah / Dibuat

| File | Perubahan |
|------|-----------|
| `features/auth/.../data/GoogleSignInTokenProvider.kt` | Ganti ke `GetSignInWithGoogleOption`; extract helper parse credential jika perlu |
| `features/auth/.../presentation/LoginViewModel.kt` | Update string `NoCredential` (Google path) |
| `features/auth/.../presentation/RegisterViewModel.kt` | Update string `NoCredential` (Google path) |
| `docs/firebase/FIREBASE_AUTH_ERROR_MAPPING.md` | Sync arti + copy `NoCredential` untuk Google |
| `docs/dev/Project_Assessment.md` | *(opsional)* 1 baris note: Google SIWG button flow |

Tidak perlu file baru kecuali helper private di dalam `GoogleSignInTokenProvider` (preferred: tetap satu class).

---

## 11. Struktur File Target

Tidak ada modul/folder baru. Tetap:

```
features/auth/src/main/kotlin/com/mascill/keutrack/feature/auth/
├── data/
│   └── GoogleSignInTokenProvider.kt   ← EDIT (inti)
└── presentation/
    ├── LoginViewModel.kt              ← EDIT (copy)
    └── RegisterViewModel.kt           ← EDIT (copy)
```

---

## 12. Desain Solusi

### 12.1 Flow target

```
Tap Google button
  → GetSignInWithGoogleOption(serverClientId = Web client ID)
  → CredentialManager.getCredential(activityContext, request)
  → GoogleIdTokenCredential.idToken
  → UserRepository.signInWithGoogle(idToken)   // unchanged
  → AuthState.Success → navigate Home
```

### 12.2 Sketch implementasi (`GoogleSignInTokenProvider`)

```kotlin
suspend fun getGoogleIdToken(context: Context): TokenResult {
    val serverClientId = nativeWrapper.getGoogleServerClientId()
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    return try {
        val result = CredentialManager.create(context)
            .getCredential(request = request, context = context)
        extractIdToken(result.credential)
    } catch (e: CancellationException) {
        throw e
    } catch (e: GetCredentialCancellationException) {
        TokenResult.Cancelled
    } catch (e: NoCredentialException) {
        TokenResult.Error.NoCredential
    } catch (e: GetCredentialException) {
        if (e.message?.contains("network", ignoreCase = true) == true) {
            TokenResult.Error.Network
        } else {
            TokenResult.Error.Unknown(e.message, e)
        }
    } catch (e: Exception) {
        TokenResult.Error.Unknown(e.message, e)
    }
}
```

Catatan API:
- Package: `com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption` (library `googleid` sudah di deps).
- Constructor / builder: `GetSignInWithGoogleOption.Builder(serverClientId)` — verifikasi signature di versi `1.1.1` saat implementasi; sesuaikan jika overload bernama `serverClientId =`.
- Parsing credential **reuse** logic `CustomCredential` + `GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL` yang sudah ada.

### 12.3 Context

- Tetap terima `Context` dari Compose `LocalContext.current` (Activity di Login/Register).
- Jangan ganti ke `Application` context — Credential Manager UI butuh Activity.
- Tidak perlu ubah signature `signInWithGoogle(context: Context)` di ViewModel.

### 12.4 Error copy (Login + Register, Google path saja)

```kotlin
is TokenResult.Error.NoCredential ->
    AuthState.Error("Unable to get a Google account. Please try again.")
```

Email path `AuthResult.Error.NoCredential` **jangan** diubah (sudah: “Unable to sign in…” / “Unable to create account…”).

### 12.5 Docs sync (`FIREBASE_AUTH_ERROR_MAPPING.md`)

Update baris yang mengklaim `NoCredential` Google = “Tidak ada akun Google di device” menjadi roughly:

- Arti: Credential Manager tidak mengembalikan Google ID token (picker kosong / tidak tersedia / GMS gagal).
- Pesan UI baru sesuai §4.
- Catatan: setelah SIWG, kasus ini jauh lebih jarang; jika tetap muncul → cek akun device + Play Services + SHA-1.

---

## 13. Task Breakdown Detail

### Task 0: Baseline repro

1. Install `devDebug` di device/emulator dengan Google account.  
2. Force-stop app → buka → di Login langsung tap Google.  
3. Catat: error pertama? Logcat `NoCredentialException` / `GoogleApiManager`?  
4. Tap kedua → sukses? (bukti baseline)

### Task 1: Refactor token provider

1. Ganti `GetGoogleIdOption` → `GetSignInWithGoogleOption`.  
2. Hapus opsi One Tap yang tidak terpakai (`filterByAuthorizedAccounts`, `autoSelectEnabled`) kecuali nanti hybrid.  
3. Pertahankan pola `CancellationException` rethrow.  
4. Compile: `./gradlew :features:auth:compileDevDebugKotlin`

### Task 2: Copy error ViewModels

1. Update string Google `NoCredential` di Login + Register.  
2. Pastikan email mapping tidak ikut berubah.

### Task 3: Docs

1. Update `FIREBASE_AUTH_ERROR_MAPPING.md` (tabel Login/Register + katalog arti `NoCredential`).  
2. Opsional: 1 kalimat di `Project_Assessment.md`.

### Task 4: QA

Jalankan [§20 Manual Test Plan](#20-manual-test-plan).

---

## 14. Acceptance Criteria

- [ ] Cold start → tap Google **pertama kali** menampilkan account picker (atau langsung sukses), **bukan** false error “No Google account found…”
- [ ] User cancel picker → kembali Idle (tidak stuck Loading; tidak error palsu)
- [ ] Sign-in sukses → Home (Firestore profile + session tetap jalan)
- [ ] Register screen Google button berperilaku sama
- [ ] Email login/register tidak regresi
- [ ] Pesan `NoCredential` Google sudah copy baru
- [ ] `FIREBASE_AUTH_ERROR_MAPPING.md` sinkron dengan UI
- [ ] `assembleDevDebug` hijau
- [ ] Diff terbatas ke file §10 (tidak menjalar ke `core/data` / splash / build-plugin)

```bash
./gradlew assembleDevDebug
git diff --stat
```

---

## 15. Catatan Arsitektur & Konvensi

- Tetap di `:features:auth` — credential UI adalah concern feature; domain hanya terima `idToken` string.
- Jangan tarik Firebase Auth ke feature; tetap `UserRepository.signInWithGoogle`.
- `CancellationException` wajib di-rethrow sebelum catch generik (aturan arsitektur KeuTrack).
- Financial/`Long` tidak relevan di phase ini.
- Protected-files di skill menyebut `features/auth/` — **phase ini adalah izin eksplisit** untuk mengubah provider + copy error Google saja.

---

## 16. Dependency Graph

```
Task 0 (repro)
  └── Task 1 (GoogleSignInTokenProvider SIWG)
        ├── Task 2 (ViewModel copy)
        │     └── Task 3 (docs mapping)
        └── Task 4 (QA)  ← butuh Task 1 + 2
```

Tidak ada perubahan Gradle dependency baru jika `GetSignInWithGoogleOption` sudah ada di `googleid:1.1.1`. Jika compile gagal (API belum ada), naikkan `googleId` di `libs.versions.toml` **minimal** — dokumentasikan di PR.

---

## 17. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| `GetSignInWithGoogleOption` API beda di `googleid` 1.1.1 | Compile error | Cek sumber library / bump patch `googleId`; jangan ganti ke Play Services Sign-In lama |
| Web client ID salah (Android client terpakai) | Tetap `NoCredential` / gagal token | Verifikasi `client_type: 3` + native wrapper |
| SHA-1 mismatch | `DEVELOPER_ERROR` sungguhan | `signingReport` + Firebase Console |
| Emulator tanpa Google Play | SIWG gagal total | QA di device fisik / emulator **with Play Store** |
| User menonaktifkan Sign-in prompts | One Tap gagal; SIWG biasanya tetap OK | Inilah alasan pilih SIWG |
| Hybrid One Tap diminta nanti | Scope creep | Tolak di phase ini; catat follow-up |

---

## 18. Urutan Pengerjaan yang Disarankan

1. Task 0 — rekam baseline (screenshot/log).  
2. Task 1 — SIWG di `GoogleSignInTokenProvider`.  
3. Task 2 — copy ViewModels.  
4. Task 3 — docs.  
5. Task 4 — QA cold start × 5 di device fisik.  
6. Commit saat diminta user, contoh message:  
   `[FIX] Stabilize Google Sign-In with GetSignInWithGoogleOption`

---

## 19. Relasi ke Phase / Docs Lain

| Dokumen | Relasi |
|---------|--------|
| `docs/firebase/PR_C_AUTH_UI_WIRING.md` | Parent wiring UI; phase ini memperbaiki reliability Google path yang sudah di-wire |
| `docs/firebase/FIREBASE_AUTH_ERROR_MAPPING.md` | Harus di-update bersama copy UI |
| `docs/firebase/FIRESTORE_LOGIN_INTEGRATION.md` | E2E Auth tidak berubah; hanya sumber idToken lebih stabil |
| `docs/firebase/PR_B_USER_REPOSITORY_ORCHESTRATION.md` | `signInWithGoogle(idToken)` tetap kontrak handoff |
| `docs/dev/Project_Assessment.md` | Auth tetap COMPLETE; polish reliability |
| Phase 6c / Phase 5 / dll. | **Tidak bergantung**; boleh dikerjakan paralel |
| Phase 9 (polish & tests) | Bisa digabung secara tema; phase ini lebih sempit & actionable |

---

## 20. Manual Test Plan

### Setup

- Build: `./gradlew installDevDebug` (atau Run `devDebug` dari IDE)
- Device: fisik dengan Google account (utama); emulator Play Store (sekunder)
- App state: signed out di Login

### Kasus

| # | Langkah | Expected |
|---|---------|----------|
| 1 | Force-stop → buka app → segera tap Google | Account picker muncul (bukan error palsu) |
| 2 | Ulangi kasus 1 sebanyak 5× cold start | ≥4/5 sukses picker di tap pertama |
| 3 | Picker → pilih akun | Login sukses → Home |
| 4 | Picker → Back / dismiss | Idle; bisa tap Google lagi |
| 5 | Dari Register → Google | Sama seperti Login |
| 6 | Email login valid | Tidak regresi |
| 7 | Airplane mode → tap Google | Network error atau credential error yang masuk akal; tidak crash |
| 8 | Device tanpa Google account (opsional) | Error jelas + copy baru; tidak crash |

### Logcat filter (debug)

```
tag:CredentialManager|GoogleSignInTokenProvider|GoogleApiManager|FirebaseAuth
```

`NoCredentialException` di tap pertama **setelah** fix harus jarang/tidak ada pada device sehat.

---

## Appendix A — Keputusan singkat untuk implementer

```
PRIMARY:  GetSignInWithGoogleOption(serverClientId)
KEEP:     TokenResult / UserRepository / Firebase sign-in unchanged
UPDATE:   Google NoCredential copy → "Unable to get a Google account. Please try again."
DOCS:     FIREBASE_AUTH_ERROR_MAPPING.md
SKIP:     One Tap hybrid, delay-retry, SHA/json unless QA proves config error
```

## Appendix B — Pseudo-diff fokus

```diff
- GetGoogleIdOption.Builder()
-   .setFilterByAuthorizedAccounts(false)
-   .setServerClientId(...)
-   .setAutoSelectEnabled(false)
-   .build()
+ GetSignInWithGoogleOption.Builder(serverClientId).build()
```

```diff
- "No Google account found on this device."
+ "Unable to get a Google account. Please try again."
```
