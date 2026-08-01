# Firebase Auth Error Mapping

Dokumentasi mapping error Firebase Authentication → domain `AuthResult` → pesan UI di KeuTrack.

Sumber implementasi:

- `core/data/.../repository/UserRepositoryImpl.kt` → `mapAuthFailure()` / `mapFirestoreFailure()`
- `core/domain/.../model/AuthResult.kt`
- `features/auth/.../LoginViewModel.kt` / `RegisterViewModel.kt`
- `features/auth/.../data/GoogleSignInTokenProvider.kt` → Credential Manager SIWG

### Catatan UX Google picker

Tombol Google memakai `GetSignInWithGoogleOption` → UI **account picker dialog**, bukan One Tap **bottom sheet** (`GetGoogleIdOption`). Ini trade-off disengaja demi reliability cold start; detail di [`FIRESTORE_LOGIN_INTEGRATION.md`](./FIRESTORE_LOGIN_INTEGRATION.md#google-sign-in-ui-credential-manager) dan phase [`PHASE_AUTH_GOOGLE_SIGNIN_CREDENTIAL_STABILITY.md`](../dev/phases/PHASE_AUTH_GOOGLE_SIGNIN_CREDENTIAL_STABILITY.md).

---

## Legenda status

| Tanda | Status | Arti |
|-------|--------|------|
| ✅ | **Implemented** | Ada constant + `when` branch khusus di `UserRepositoryImpl` (atau handle lewat tipe exception / flow Google) |
| ❌ | **Not implemented** | Belum ada branch khusus; jika muncul → `AuthResult.Error.Unknown(e.message)` (kecuali tertangkap sebagai `FirebaseNetworkException`) |

Fallback default di kode (untuk semua ❌):

```kotlin
else -> AuthResult.Error.Unknown(e.message)
```

---

## Ringkasan cepat

| Status | Jumlah (errorCode / case) | Keterangan |
|--------|---------------------------|------------|
| ✅ Implemented | 7 `errorCode` Auth + beberapa exception/flow non-string | Dipakai di login/register email & Google |
| ❌ Not implemented | Sisanya di katalog di bawah | Belum di-mapping spesifik |

### ✅ Implemented — Firebase Auth `errorCode`

| Status | Android `errorCode` | `AuthResult` |
|--------|---------------------|--------------|
| ✅ | `ERROR_USER_NOT_FOUND` | `UserNotFound` |
| ✅ | `ERROR_USER_DISABLED` | `InvalidCredential` |
| ✅ | `ERROR_WRONG_PASSWORD` | `InvalidCredential` |
| ✅ | `ERROR_INVALID_CREDENTIAL` | `InvalidCredential` |
| ✅ | `ERROR_INVALID_EMAIL` | `InvalidCredential` |
| ✅ | `ERROR_EMAIL_ALREADY_IN_USE` | `InvalidCredential` |
| ✅ | `ERROR_WEAK_PASSWORD` | `InvalidCredential` |

### ✅ Implemented — exception / flow (bukan `errorCode` string)

| Status | Case | `AuthResult` |
|--------|------|--------------|
| ✅ | `FirebaseNetworkException` | `Network` |
| ✅ | `FirebaseFirestoreException` `UNAVAILABLE` | `Network` |
| ✅ | `FirebaseFirestoreException` `DEADLINE_EXCEEDED` | `Network` |
| ✅ | Google Credential Manager: no credential | `NoCredential` |
| ✅ | User cancel Google picker | `Cancelled` |

---

## Alur singkat

```
Firebase Exception
        │
        ▼
UserRepositoryImpl.mapAuthFailure() / mapFirestoreFailure()
        │
        ▼
AuthResult.Error (domain)
        │
        ▼
LoginViewModel / RegisterViewModel → AuthState.Error(message)
```

Di Android, `FirebaseAuthException.errorCode` / `getErrorCode()` mengembalikan **String** (contoh: `"ERROR_WRONG_PASSWORD"`).

---

## Domain: `AuthResult.Error`

| Variant | Arti di KeuTrack |
|---------|------------------|
| `Network` | Tidak ada koneksi / backend sementara tidak tersedia |
| `NoCredential` | Credential Manager tidak mengembalikan Google ID token (picker kosong / tidak tersedia / GMS gagal). Bukan bukti mutlak “device tanpa akun Google”. |
| `InvalidCredential` | Email/password/credential tidak valid, atau konflik akun (email sudah dipakai, password lemah, akun disabled) |
| `UserNotFound` | Akun tidak ditemukan di Auth |
| `Unknown(message?)` | Error lain yang belum di-mapping secara spesifik (semua ❌) |

---

## Mapping: Firebase Auth → `AuthResult`

Implementasi di `UserRepositoryImpl.mapAuthFailure()`.

| Status | Exception | `errorCode` | `AuthResult` | Keterangan |
|--------|-----------|-------------|--------------|------------|
| ✅ | `FirebaseNetworkException` | — | `Error.Network` | Koneksi gagal |
| ✅ | `FirebaseAuthException` | `ERROR_USER_NOT_FOUND` | `Error.UserNotFound` | Email belum terdaftar / user dihapus |
| ✅ | `FirebaseAuthException` | `ERROR_WRONG_PASSWORD` | `Error.InvalidCredential` | Password salah |
| ✅ | `FirebaseAuthException` | `ERROR_INVALID_CREDENTIAL` | `Error.InvalidCredential` | Credential tidak valid; sering muncul saat email enumeration protection aktif |
| ✅ | `FirebaseAuthException` | `ERROR_INVALID_EMAIL` | `Error.InvalidCredential` | Format email tidak valid |
| ✅ | `FirebaseAuthException` | `ERROR_EMAIL_ALREADY_IN_USE` | `Error.InvalidCredential` | Register dengan email yang sudah dipakai |
| ✅ | `FirebaseAuthException` | `ERROR_WEAK_PASSWORD` | `Error.InvalidCredential` | Password terlalu lemah |
| ✅ | `FirebaseAuthException` | `ERROR_USER_DISABLED` | `Error.InvalidCredential` | Akun dinonaktifkan di Firebase Console |
| ❌ | `FirebaseAuthException` | *(lainnya)* | `Error.Unknown(message)` | Belum di-handle khusus |
| ❌ | *(exception lain)* | — | `Error.Unknown(message)` | Fallback |

Constant string yang **sudah** dipakai di repository (✅):

```kotlin
ERROR_USER_NOT_FOUND
ERROR_WRONG_PASSWORD
ERROR_INVALID_CREDENTIAL
ERROR_INVALID_EMAIL
ERROR_EMAIL_ALREADY_IN_USE
ERROR_WEAK_PASSWORD
ERROR_USER_DISABLED
```

---

## Mapping: Firestore → `AuthResult`

Implementasi di `UserRepositoryImpl.mapFirestoreFailure()`.

| Status | Exception | Code | `AuthResult` |
|--------|-----------|------|--------------|
| ✅ | `FirebaseNetworkException` | — | `Error.Network` |
| ✅ | `FirebaseFirestoreException` | `UNAVAILABLE` | `Error.Network` |
| ✅ | `FirebaseFirestoreException` | `DEADLINE_EXCEEDED` | `Error.Network` |
| ❌ | `FirebaseFirestoreException` | *(lainnya, mis. `PERMISSION_DENIED`)* | `Error.Unknown(message)` |
| ❌ | *(exception lain)* | — | `Error.Unknown(message)` |

---

## Mapping: `AuthResult` → pesan UI

Semua variant domain di bawah sudah di-wire di ViewModel (✅ UI). Yang ❌ adalah `errorCode` Firebase mentah yang belum di-map ke variant spesifik.

### Login (`LoginViewModel`)

| Status UI | `AuthResult.Error` | Google Sign-In message | Email login message |
|-----------|--------------------|------------------------|---------------------|
| ✅ | `Network` | No internet connection. Please try again. | No internet connection. Please try again. |
| ✅ | `NoCredential` | Unable to get a Google account. Please try again. | Unable to sign in. Please try again. |
| ✅ | `InvalidCredential` | Invalid credential. Please try again. | Invalid email or password. |
| ✅ | `UserNotFound` | Account not found. Please try again. | Account not found. Please try again. |
| ✅ | `Unknown` | An unexpected error occurred. / message jika ada | An unexpected error occurred. / message jika ada |

### Register (`RegisterViewModel`)

| Status UI | `AuthResult.Error` | Google Sign-In message | Email register message |
|-----------|--------------------|------------------------|------------------------|
| ✅ | `Network` | No internet connection. Please try again. | No internet connection. Please try again. |
| ✅ | `NoCredential` | Unable to get a Google account. Please try again. | Unable to create account. Please try again. |
| ✅ | `InvalidCredential` | Invalid credential. Please try again. | Unable to create account. Email may already be in use or password is too weak. |
| ✅ | `UserNotFound` | Account not found. Please try again. | Unable to create account. Please try again. |
| ✅ | `Unknown` | An unexpected error occurred. | An unexpected error occurred. |

---

## Katalog error Firebase

Kolom **Status**:

- ✅ = Implemented di `mapAuthFailure()` / path terkait
- ❌ = Not implemented (fallback `Unknown`, kecuali network exception)

Kolom **Prioritas** (hanya relevan untuk ❌):

- **P0** — sangat mungkin muncul di login/register email atau Google
- **P1** — relevan saat fitur account management / linking ditambah
- **P2** — edge case / config / platform
- **P3** — fitur yang belum ada di app (phone, MFA, Game Center, dll.)

### Credential, email/password, provider

| Status | Android `errorCode` | Situasi | Prioritas | `AuthResult` / saran |
|--------|---------------------|---------|-----------|----------------------|
| ✅ | `ERROR_USER_NOT_FOUND` | User tidak ada / sudah dihapus | — | `UserNotFound` |
| ✅ | `ERROR_USER_DISABLED` | User disabled di console | — | `InvalidCredential` |
| ✅ | `ERROR_WRONG_PASSWORD` | Password salah | — | `InvalidCredential` |
| ✅ | `ERROR_INVALID_CREDENTIAL` | Credential generik tidak valid | — | `InvalidCredential` |
| ✅ | `ERROR_INVALID_EMAIL` | Format email salah | — | `InvalidCredential` |
| ✅ | `ERROR_EMAIL_ALREADY_IN_USE` | Email sudah dipakai akun lain | — | `InvalidCredential` |
| ✅ | `ERROR_WEAK_PASSWORD` | Password terlalu lemah | — | `InvalidCredential` |
| ❌ | `ERROR_TOO_MANY_REQUESTS` | Rate limit / terlalu banyak percobaan | P0 | Saran: pesan "Too many attempts" atau variant baru |
| ❌ | `ERROR_OPERATION_NOT_ALLOWED` | Provider disabled di console | P0 | Saran: `Unknown` + pesan config |
| ❌ | `ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL` | Email sudah ada via provider lain | P0 | Saran: `InvalidCredential` |
| ❌ | `ERROR_CREDENTIAL_ALREADY_IN_USE` | Credential sudah ter-link ke akun lain | P1 | Saran: `InvalidCredential` |
| ❌ | `ERROR_INVALID_CUSTOM_TOKEN` | Custom token tidak valid | P2 | Saran: `InvalidCredential` / `Unknown` |
| ❌ | `ERROR_CUSTOM_TOKEN_MISMATCH` | Service account / API key beda project | P2 | Saran: `Unknown` |
| ❌ | `ERROR_REJECTED_CREDENTIAL` | Credential ditolak (malformed / mismatch) | P1 | Saran: `InvalidCredential` |
| ❌ | `ERROR_MISSING_EMAIL` | Email wajib tapi tidak dikirim | P1 | Saran: `InvalidCredential` |
| ❌ | `ERROR_USER_MISMATCH` | Re-auth dengan user yang bukan current user | P1 | Saran: `InvalidCredential` |
| ❌ | `ERROR_PROVIDER_ALREADY_LINKED` | Provider sudah ter-link ke akun | P1 | Saran: `InvalidCredential` / `Unknown` |
| ❌ | `ERROR_NO_SUCH_PROVIDER` | Unlink provider yang tidak ter-link | P1 | Saran: `Unknown` |
| ❌ | `ERROR_INVALID_PROVIDER_ID` | Provider id tidak valid | P2 | Saran: `Unknown` |

### Session / token / re-auth

| Status | Android `errorCode` | Situasi | Prioritas | `AuthResult` / saran |
|--------|---------------------|---------|-----------|----------------------|
| ❌ | `ERROR_REQUIRES_RECENT_LOGIN` | Operasi sensitif butuh login ulang | P1 | Saran: arahkan re-login |
| ❌ | `ERROR_USER_TOKEN_EXPIRED` | Token revoked (mis. password diganti di device lain) | P0 | Saran: signed-out → minta login lagi |
| ❌ | `ERROR_INVALID_USER_TOKEN` | Token user malformed / tidak valid | P0 | Saran: signed-out → minta login lagi |
| ❌ | `ERROR_NULL_USER` | Expected non-null user, tapi null | P1 | Saran: `UserNotFound` atau signed-out |

### Network / app config

| Status | Android `errorCode` / case | Situasi | Prioritas | `AuthResult` / saran |
|--------|----------------------------|---------|-----------|----------------------|
| ✅ | `FirebaseNetworkException` | Koneksi gagal | — | `Network` |
| ❌ | `ERROR_NETWORK_REQUEST_FAILED` | Timeout / host unreachable (via `errorCode`) | P0 | Saran: `Network` (sering sudah lewat `FirebaseNetworkException`) |
| ❌ | `ERROR_APP_NOT_AUTHORIZED` | Package name / SHA-1 / API key tidak authorized | P0 | Saran: `Unknown` + cek Firebase Console |
| ❌ | `ERROR_INVALID_API_KEY` | API key tidak valid | P2 | Saran: `Unknown` |
| ❌ | `ERROR_INTERNAL_ERROR` | Internal Auth error | P2 | Saran: `Unknown` |
| ❌ | `ERROR_ADMIN_RESTRICTED_OPERATION` | Operasi dibatasi admin | P2 | Saran: `Unknown` |
| ❌ | `ERROR_TENANT_ID_MISMATCH` | Tenant ID tidak cocok | P3 | Saran: `Unknown` |
| ❌ | `ERROR_UNSUPPORTED_TENANT_OPERATION` | Operasi tidak support multi-tenancy | P3 | Saran: `Unknown` |

### Email action / password reset / OOB code

Fitur reset/verify email belum ada di app → semua ❌ untuk saat ini.

| Status | Android `errorCode` | Situasi | Prioritas | Saran mapping |
|--------|---------------------|---------|-----------|---------------|
| ❌ | `ERROR_EXPIRED_ACTION_CODE` | OOB / action code kedaluwarsa | P1 | `InvalidCredential` / "Link expired" |
| ❌ | `ERROR_INVALID_ACTION_CODE` | OOB / action code tidak valid | P1 | `InvalidCredential` |
| ❌ | `ERROR_INVALID_MESSAGE_PAYLOAD` | Payload email template tidak valid | P2 | `Unknown` |
| ❌ | `ERROR_INVALID_SENDER` | Sender email template tidak valid | P2 | `Unknown` |
| ❌ | `ERROR_INVALID_RECIPIENT_EMAIL` | Recipient email tidak valid | P1 | `InvalidCredential` |
| ❌ | `ERROR_UNAUTHORIZED_DOMAIN` | Continue URL domain tidak di-allowlist | P2 | `Unknown` |
| ❌ | `ERROR_INVALID_CONTINUE_URI` | Continue URI tidak valid | P2 | `Unknown` |
| ❌ | `ERROR_MISSING_CONTINUE_URI` | Continue URI wajib tapi hilang | P2 | `Unknown` |
| ❌ | `ERROR_MISSING_ANDROID_PACKAGE_NAME` | Android package name hilang | P2 | `Unknown` |
| ❌ | `ERROR_MISSING_IOS_BUNDLE_ID` | iOS bundle ID hilang | P3 | `Unknown` |
| ❌ | `ERROR_EMAIL_CHANGE_NEEDS_VERIFICATION` | MFA user wajib email verified saat ganti email | P2 | `Unknown` |
| ❌ | `ERROR_UNVERIFIED_EMAIL` | Email belum diverifikasi padahal required | P1 | `Unknown` + arahkan verify |

### Google / federated / web sign-in flow

| Status | Case / `errorCode` | Situasi | Prioritas | `AuthResult` / saran |
|--------|--------------------|---------|-----------|----------------------|
| ✅ | Google: no credential | Credential Manager tidak mengembalikan ID token (SIWG picker kosong / GMS gagal). Setelah button flow `GetSignInWithGoogleOption` kasus ini jarang; jika tetap muncul cek akun device + Play Services + SHA-1 | — | `NoCredential` |
| ✅ | Google: user cancel picker | User batalkan | — | `Cancelled` |
| ❌ | `ERROR_WEB_CONTEXT_ALREADY_PRESENTED` | Web context sudah terbuka | P2 | `Unknown` / `Cancelled` |
| ❌ | `ERROR_WEB_CONTEXT_CANCELED` | User membatalkan web flow | P1 | `Cancelled` |
| ❌ | `ERROR_WEB_NETWORK_REQUEST_FAILED` | Network gagal di web view | P1 | `Network` |
| ❌ | `ERROR_WEB_INTERNAL_ERROR` | Internal error di web view | P2 | `Unknown` |
| ❌ | `ERROR_WEB_STORAGE_UNSUPPORTED` | Web storage / cookie issue | P2 | `Unknown` |
| ❌ | `ERROR_INVALID_CLIENT_ID` | Client ID OAuth tidak valid | P2 | `Unknown` |
| ❌ | `ERROR_INVALID_HOSTING_LINK_DOMAIN` | Hosting link domain tidak owned project | P3 | `Unknown` |
| ❌ | `ERROR_MISSING_OR_INVALID_NONCE` | Nonce hilang/invalid (OIDC) | P2 | `InvalidCredential` |
| ❌ | `ERROR_MISSING_CLIENT_IDENTIFIER` | Client identifier hilang | P2 | `Unknown` |

### Phone authentication

Fitur phone auth belum ada → semua ❌.

| Status | Android `errorCode` | Situasi | Prioritas | Saran (jika nanti ada phone auth) |
|--------|---------------------|---------|-----------|-----------------------------------|
| ❌ | `ERROR_MISSING_PHONE_NUMBER` | Nomor telepon kosong | P3 | `InvalidCredential` |
| ❌ | `ERROR_INVALID_PHONE_NUMBER` | Format nomor salah (bukan E.164) | P3 | `InvalidCredential` |
| ❌ | `ERROR_MISSING_VERIFICATION_CODE` | SMS code kosong | P3 | `InvalidCredential` |
| ❌ | `ERROR_INVALID_VERIFICATION_CODE` | SMS code salah | P3 | `InvalidCredential` |
| ❌ | `ERROR_MISSING_VERIFICATION_ID` | Verification ID kosong | P3 | `InvalidCredential` |
| ❌ | `ERROR_INVALID_VERIFICATION_ID` | Verification ID tidak valid | P3 | `InvalidCredential` |
| ❌ | `ERROR_SESSION_EXPIRED` | SMS code kedaluwarsa | P3 | `InvalidCredential` + "Code expired" |
| ❌ | `ERROR_QUOTA_EXCEEDED` | Kuota SMS project habis | P3 | `Unknown` |
| ❌ | `ERROR_CAPTCHA_CHECK_FAILED` | reCAPTCHA phone flow gagal | P3 | `Unknown` |
| ❌ | `ERROR_MISSING_APP_CREDENTIAL` | App credential hilang | P3 | `Unknown` |
| ❌ | `ERROR_INVALID_APP_CREDENTIAL` | App credential tidak valid | P3 | `Unknown` |
| ❌ | `ERROR_APP_NOT_VERIFIED` | App verification gagal | P3 | `Unknown` |

### Multi-factor authentication

Fitur MFA belum ada → semua ❌.

| Status | Android `errorCode` | Situasi | Prioritas | Saran (jika nanti ada MFA) |
|--------|---------------------|---------|-----------|----------------------------|
| ❌ | `ERROR_SECOND_FACTOR_REQUIRED` | Butuh second factor | P3 | Handle MFA challenge (jangan `InvalidCredential`) |
| ❌ | `ERROR_MISSING_MULTI_FACTOR_SESSION` | Multi-factor session hilang | P3 | Restart sign-in |
| ❌ | `ERROR_MISSING_MULTI_FACTOR_INFO` | Second factor identifier tidak dikirim | P3 | `InvalidCredential` |
| ❌ | `ERROR_INVALID_MULTI_FACTOR_SESSION` | Multi-factor session tidak valid | P3 | Restart sign-in |
| ❌ | `ERROR_MULTI_FACTOR_INFO_NOT_FOUND` | Second factor tidak cocok | P3 | `InvalidCredential` |
| ❌ | `ERROR_SECOND_FACTOR_ALREADY_ENROLLED` | Second factor sudah terdaftar | P3 | `Unknown` |
| ❌ | `ERROR_MAXIMUM_SECOND_FACTOR_COUNT_EXCEEDED` | Jumlah second factor melebihi limit | P3 | `Unknown` |
| ❌ | `ERROR_UNSUPPORTED_FIRST_FACTOR` | First factor tidak didukung untuk MFA | P3 | `Unknown` |

### reCAPTCHA Enterprise

| Status | Android `errorCode` | Situasi | Prioritas | Saran |
|--------|---------------------|---------|-----------|-------|
| ❌ | `ERROR_RECAPTCHA_NOT_ENABLED` | reCAPTCHA Enterprise belum aktif | P2 | `Unknown` |
| ❌ | `ERROR_MISSING_RECAPTCHA_TOKEN` | Token reCAPTCHA hilang | P2 | `Unknown` |
| ❌ | `ERROR_INVALID_RECAPTCHA_TOKEN` | Token reCAPTCHA tidak valid | P2 | `Unknown` |
| ❌ | `ERROR_INVALID_RECAPTCHA_ACTION` | Action reCAPTCHA tidak valid | P2 | `Unknown` |
| ❌ | `ERROR_MISSING_CLIENT_TYPE` | Client type hilang | P2 | `Unknown` |
| ❌ | `ERROR_MISSING_RECAPTCHA_VERSION` | Versi reCAPTCHA hilang | P2 | `Unknown` |
| ❌ | `ERROR_INVALID_RECAPTCHA_VERSION` | Versi reCAPTCHA tidak valid | P2 | `Unknown` |
| ❌ | `ERROR_INVALID_REQ_TYPE` | Request type tidak valid | P2 | `Unknown` |

### Lain-lain / platform spesifik

| Status | Android `errorCode` | Situasi | Prioritas | Catatan / saran |
|--------|---------------------|---------|-----------|-----------------|
| ❌ | `ERROR_LOCAL_PLAYER_NOT_AUTHENTICATED` | Game Center: local player belum auth | P3 | iOS/Game Center |
| ❌ | `ERROR_GAME_KIT_NOT_LINKED` | GameKit framework belum di-link | P3 | iOS/Game Center |
| ❌ | `ERROR_KEYCHAIN_ERROR` | Gagal akses keychain | P3 | iOS |
| ❌ | `ERROR_DYNAMIC_LINK_NOT_ACTIVATED` | Dynamic Links (legacy) | P3 | Jarang relevan |
| ❌ | `ERROR_BLOCKING_FUNCTION` / Cloud Functions blocking | Blocking Auth function menolak request | P2 | `Unknown` + message dari function |
| ❌ | JWT / malformed token parse issues | JWT gagal di-parse | P2 | `Unknown` / signed-out |

> Nama string Android di beberapa edge case bisa sedikit berbeda antar versi SDK. Selalu log `e.errorCode` dari `FirebaseAuthException` (sudah ada di `logFailure()`) lalu sesuaikan constant di repository.

---

## Prioritas implementasi berikutnya (hanya yang ❌)

Urutan usulan jika ingin memperketat mapping:

1. **P0:** `ERROR_TOO_MANY_REQUESTS`, `ERROR_OPERATION_NOT_ALLOWED`, `ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL`, `ERROR_USER_TOKEN_EXPIRED`, `ERROR_INVALID_USER_TOKEN`, `ERROR_APP_NOT_AUTHORIZED`, pastikan `ERROR_NETWORK_REQUEST_FAILED` → `Network`
2. **P1:** `ERROR_REQUIRES_RECENT_LOGIN`, `ERROR_CREDENTIAL_ALREADY_IN_USE`, action-code errors (saat reset/verify email ditambah), `ERROR_WEB_CONTEXT_CANCELED` → `Cancelled`
3. **P2+:** Phone, MFA, reCAPTCHA Enterprise — baru saat fitur tersebut masuk roadmap

Tidak wajib menambah sealed class domain baru untuk setiap `errorCode`. Banyak kasus cukup:

- map ke `InvalidCredential` / `Network` / `UserNotFound` / `Cancelled`, **atau**
- tetap `Unknown` tapi UI memakai `message` / pesan yang lebih spesifik.

Setelah diimplementasi: ubah tanda ❌ → ✅ di tabel terkait, dan update constant di `UserRepositoryImpl`.

---

## Catatan: Email Enumeration Protection

Jika **email enumeration protection** aktif di project Firebase:

- Login gagal sering mengembalikan `ERROR_INVALID_CREDENTIAL` generik (✅ sudah di-map)
- Bukan selalu `ERROR_USER_NOT_FOUND` atau `ERROR_WRONG_PASSWORD` yang spesifik

Karena itu mapping KeuTrack mengelompokkan sebagian besar credential error ke `InvalidCredential`, dan UI login menampilkan pesan generik:

> Invalid email or password.

Jangan bergantung pada perbedaan `USER_NOT_FOUND` vs `WRONG_PASSWORD` untuk messaging yang sangat spesifik jika protection ini aktif.

Dokumentasi resmi: [Password Auth – Email enumeration protection](https://firebase.google.com/docs/auth/android/password-auth)

---

## Dokumentasi resmi Firebase / Google

| Topik | URL |
|-------|-----|
| `FirebaseAuthException` | https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseAuthException |
| Invalid user | https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseAuthInvalidUserException |
| Invalid credentials | https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseAuthInvalidCredentialsException |
| User collision | https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseAuthUserCollisionException |
| Weak password | https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseAuthWeakPasswordException |
| Identity Platform error codes (tabel Android) | https://cloud.google.com/identity-platform/docs/error-codes |
| AuthErrorCode lengkap (iOS, numeric) | https://firebase.google.com/docs/reference/swift/firebaseauth/api/reference/Enums/AuthErrorCode |
| Password auth Android | https://firebase.google.com/docs/auth/android/password-auth |

---

## Cara menambah error code baru (❌ → ✅)

1. Tambahkan constant string di `UserRepositoryImpl` companion object.
2. Extend `when (e.errorCode)` di `mapAuthFailure()` ke variant `AuthResult` yang sesuai.
3. Pastikan `LoginViewModel` / `RegisterViewModel` sudah punya pesan UI untuk variant tersebut (atau tambah variant domain baru bila perlu).
4. Di dokumen ini: ubah kolom **Status** dari ❌ menjadi ✅, dan isi kolom `AuthResult` dengan mapping final (bukan saran).
