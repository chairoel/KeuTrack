# Phase 2 — core:data (Room + Repository Implementation + Offline Sync)

> **Modul target:** `:core:data` (+ perubahan kecil di `gradle/libs.versions.toml`)
> **Estimasi:** ~3–4 hari
> **Prasyarat:** Phase 1 (selesai) — domain entity, enum, repository interface, use case sudah ada di `:core:domain`
> **Hasil akhir:** Persistence offline-first via Room, repository impl siap dipakai feature modules, dan jalur sync Firestore (WorkManager) untuk transaksi/wallet/budget — tanpa menyentuh UI feature

---

## Daftar Isi

1. [Konteks & Tujuan](#1-konteks--tujuan)
2. [Scope — Apa yang Dikerjakan](#2-scope--apa-yang-dikerjakan)
3. [Scope — Apa yang TIDAK Dikerjakan](#3-scope--apa-yang-tidak-dikerjakan)
4. [File Referensi (Read-Only)](#4-file-referensi-read-only)
5. [File yang TIDAK BOLEH Diubah](#5-file-yang-tidak-boleh-diubah)
6. [File yang BOLEH Diubah (Additive Only)](#6-file-yang-boleh-diubah-additive-only)
7. [Struktur File yang Akan Dibuat](#7-struktur-file-yang-akan-dibuat)
8. [Arsitektur Offline-First (Wajib Dipahami)](#8-arsitektur-offline-first-wajib-dipahami)
9. [Task Breakdown Detail](#9-task-breakdown-detail)
10. [Acceptance Criteria](#10-acceptance-criteria)
11. [Catatan Arsitektur & Konvensi](#11-catatan-arsitektur--konvensi)
12. [Dependency Graph](#12-dependency-graph)
13. [Risiko & Mitigasi](#13-risiko--mitigasi)
14. [Urutan Pengerjaan yang Disarankan](#14-urutan-pengerjaan-yang-disarankan)

---

## 1. Konteks & Tujuan

Phase 1 sudah mendefinisikan kontrak domain finansial di `:core:domain`:

| Layer | Status |
|-------|--------|
| Model (`Transaction`, `Wallet`, `Category`, `Budget`, `CategorySummary`, enums, `SyncStatus`) | ✅ Ada |
| Repository interface (`TransactionRepository`, `WalletRepository`, `CategoryRepository`, `BudgetRepository`, `SyncRepository`) | ✅ Ada |
| Use case (`AddTransactionUseCase`, `GetTransactionsUseCase`, dll.) | ✅ Ada |
| Implementasi di `:core:data` | ❌ Belum |
| Room database | ❌ Belum sama sekali |
| WorkManager sync | ❌ Belum |

Saat ini `:core:data` hanya punya pipeline auth:

- `UserRepositoryImpl` → Firebase Auth + Firestore `/users/{uid}` + Proto DataStore
- Tidak ada `@Entity`, `@Dao`, `AppDatabase`
- `SyncRepository` belum diimplementasikan
- Room/WorkManager **belum ada** di `gradle/libs.versions.toml`

Tanpa Phase 2, use case Phase 1 tidak bisa di-inject (Hilt tidak punya binding), dan feature modules tetap bergantung mock data.

**Tujuan Phase 2:**
- Tambah dependency Room + WorkManager ke version catalog dan `:core:data`
- Buat Room entity/DAO/`AppDatabase` yang mencerminkan domain model Phase 1
- Implementasikan semua repository interface finansial dengan pola **offline-first** (read dari Room, write ke Room dulu)
- Sediakan Firestore data source terpisah untuk sync (jangan campur dengan auth profile logic)
- Implementasikan `SyncRepository` + WorkManager worker
- Seed kategori default + personal wallet
- Wire Hilt `@Binds` / `@Provides` — siap dikonsumsi Phase 4+

---

## 2. Scope — Apa yang Dikerjakan

### A. Gradle / Dependencies

| # | Item | Deskripsi |
|---|------|-----------|
| 1 | `gradle/libs.versions.toml` | Tambah Room + WorkManager (+ room-ktx, room-compiler KSP, hilt-work jika dipakai) |
| 2 | `core/data/build.gradle.kts` | Tambah dependency Room, WorkManager, Moshi (untuk TypeConverter JSON summary) |

### B. Room Layer

| # | Item | Deskripsi |
|---|------|-----------|
| 3 | Type converters | `Instant` ↔ `Long`, enum ↔ `String`, `Map` breakdown ↔ JSON |
| 4 | Room entities | `TransactionEntity`, `WalletEntity`, `CategoryEntity`, `BudgetEntity`, `CategorySummaryEntity` |
| 5 | DAOs | CRUD + observe Flow + query pending sync |
| 6 | `AppDatabase` | Register entities + converters, version 1 |
| 7 | `DatabaseModule` | Hilt `@Provides` untuk DB + setiap DAO |

### C. Local Data Sources

| # | Item | Deskripsi |
|---|------|-----------|
| 8 | Local DS interfaces + Impl | Wrap DAO; repository tidak panggil DAO langsung |

### D. Firestore Sync Data Sources (Baru — Terpisah dari Auth)

| # | Item | Deskripsi |
|---|------|-----------|
| 9 | `TransactionFirestoreDataSource` | Upsert/delete transaction + batch write dengan side-effects |
| 10 | `WalletFirestoreDataSource` | Upsert/delete/increment balance |
| 11 | `BudgetFirestoreDataSource` | Upsert/delete/increment spent |
| 12 | `CategoryFirestoreDataSource` | Optional pull default categories (seed lokal tetap prioritas) |
| 13 | `CategorySummaryFirestoreDataSource` | Upsert monthly summary di subcollection user |

### E. Mappers

| # | Item | Deskripsi |
|---|------|-----------|
| 14 | Entity ↔ Domain mappers | Satu class per aggregate (`TransactionMapper`, dll.) |

### F. Repository Implementations

| # | Item | Deskripsi |
|---|------|-----------|
| 15 | `TransactionRepositoryImpl` | Observe dari Room; write lokal atomic + enqueue sync |
| 16 | `WalletRepositoryImpl` | Observe/CRUD wallet; create default personal wallet |
| 17 | `CategoryRepositoryImpl` | Observe + `seedDefaultCategories()` |
| 18 | `BudgetRepositoryImpl` | Observe budgets + monthly summaries |
| 19 | `SyncRepositoryImpl` | Push pending → Firestore → update `syncStatus` |

### G. WorkManager

| # | Item | Deskripsi |
|---|------|-----------|
| 20 | `SyncWorker` / `TransactionSyncWorker` | Background sync `syncAll()` / `syncPendingTransactions()` |
| 21 | `SyncScheduler` | Enqueue unique work setelah write lokal |
| 22 | Hilt WorkerFactory wiring | Agar worker bisa `@Inject` dependency |

### H. DI

| # | Item | Deskripsi |
|---|------|-----------|
| 23 | Extend `CommonRepositoryModule` | `@Binds` repository baru (additive) |
| 24 | Extend `CommonDataSourceModule` / module baru | Bind local + firestore DS |
| 25 | Extend `CommonMapperModule` / module baru | Provide/bind mappers |
| 26 | `DatabaseModule` + `SyncModule` | Room + WorkManager |

---

## 3. Scope — Apa yang TIDAK Dikerjakan

| Item | Alasan | Phase |
|------|--------|-------|
| Perubahan domain model / repository interface / use case | Kontrak Phase 1 sudah freeze | — (kecuali bug blocker, diskusikan dulu) |
| Wiring UI Dashboard / Transaction / Family / Settings | Feature-level | Phase 4–7 |
| Hapus mock dashboard / RouteRepository SMPOB | Cleanup feature | Phase 4 |
| Family group entity / invite / QR | Family scope | Phase 6 |
| Google Sheets sync | Future | Phase 7+ |
| Firestore Security Rules & composite indexes (deploy) | Infra Firebase console / rules file terpisah | Dokumentasikan di Phase 2, deploy boleh paralel |
| Forgot password / email verification | Auth future | — |
| Unit/instrumented tests penuh | Testing | Phase 9 (smoke compile wajib) |
| Modifikasi auth flow (login/register/splash) | Sudah production-stable | — |

---

## 4. File Referensi (Read-Only)

File berikut **dibaca sebagai sumber kebenaran**. Jangan diubah kecuali ada di Section 6.

### Desain & Dokumen

| File | Gunakan untuk |
|------|---------------|
| `docs/dev/phases/PHASE_1_DOMAIN_ENTITIES_AND_USE_CASES.md` | **Referensi utama kontrak domain** — field entity, enum values, repository method signatures, default categories, keputusan `Long`/`Instant`/`SyncStatus` |
| `docs/dev/Project_Assessment.md` | Gap analysis — apa yang missing di data layer, prioritas produk |
| `.cursor/rules/keutrack-data-layer.mdc` | Pola repository impl, CancellationException, mapper, `@Binds` |
| `.cursor/rules/keutrack-architecture.mdc` | Dependency rules, offline-first high-level |
| `.cursor/skills/keutrack-dev/SKILL.md` | Workflow implementasi repository + protected files list |
| `docs/firebase/FIRESTORE_LOGIN_INTEGRATION.md` | Pola Firestore field constants + error/rollback (untuk auth saja — jangan tiru upsert user di financial DS) |

> **Catatan:** `plans/KeuTrack_Data_Design.md` disebut di Phase 1 / skill, tetapi file tersebut **belum ada di repo saat dokumen ini ditulis**. Schema Firestore/Room di bawah Section 9 mengikuti Phase 1 + assessment. Jika Data Design ditambahkan nanti, selaraskan field names ke dokumen itu.

### Domain Contract (Phase 1 — Implement Exactly)

| File | Pelajari |
|------|----------|
| `core/domain/.../model/Transaction.kt` | Field yang harus di-mirror di Room entity |
| `core/domain/.../model/Wallet.kt` | Balance denormalized |
| `core/domain/.../model/Category.kt` | Default vs custom |
| `core/domain/.../model/Budget.kt` | `spent`, `month`, computed props (jangan persist computed) |
| `core/domain/.../model/CategorySummary.kt` | `byCategory: Map` — butuh TypeConverter / child table |
| `core/domain/.../model/SyncStatus.kt` | `PENDING` / `SYNCED` / `FAILED` |
| `core/domain/.../model/*Type.kt`, `BudgetPeriod.kt` | `value` + `fromValue()` untuk persist string |
| `core/domain/.../repository/TransactionRepository.kt` | Method yang wajib diimplementasikan |
| `core/domain/.../repository/WalletRepository.kt` | Termasuk `getPersonalWallet()` |
| `core/domain/.../repository/CategoryRepository.kt` | Termasuk `seedDefaultCategories()` |
| `core/domain/.../repository/BudgetRepository.kt` | Budgets + monthly summaries |
| `core/domain/.../repository/SyncRepository.kt` | `syncPending*` + `syncAll()` |
| `core/domain/.../usecase/AddTransactionUseCase.kt` | Ekspektasi: repo throw → use case `Result.failure` |

### Pola Existing di `:core:data` (Tiru Style-nya)

| File | Pelajari |
|------|----------|
| `core/data/.../repository/UserRepositoryImpl.kt` | `@Inject constructor`, `CancellationException` rethrow, orchestration multi-source |
| `core/data/.../datasource/FirestoreNetworkDataSource.kt` | Companion object constants untuk collection/field names |
| `core/data/.../datasource/UserProfileLocalDataSource.kt` + `Impl` | Pola interface + Impl untuk local source |
| `core/data/.../mapper/AuthUserMapper.kt` | Mapper sebagai class terpisah (bukan extension) |
| `core/data/.../di/CommonRepositoryModule.kt` | Pola `@Binds` |
| `core/data/.../di/CommonDataSourceModule.kt` | Pola bind data source |
| `core/data/.../di/CommonMapperModule.kt` | Pola provide/bind mapper |
| `core/data/.../di/FirebaseModule.kt` | Cara provide Firebase instances |
| `core/data/build.gradle.kts` | Baseline dependencies sebelum Room |

### UI Mock (Hanya untuk Validasi Field — Jangan Wire)

| File | Pelajari |
|------|----------|
| `features/dashboard/.../model/DashboardMockUi.kt` | Field yang nanti dikonsumsi dari repo (balance, recent tx) |
| `features/dashboard/.../model/BottomSheetUI.kt` | Category list + entry kind |

### Infra

| File | Pelajari |
|------|----------|
| `gradle/libs.versions.toml` | Tempat menambah Room/WorkManager aliases |
| `build-plugin/.../KeuTrackHiltPlugin.kt` | KSP sudah di-apply — Room compiler cukup `ksp(libs.androidx.room.compiler)` |
| `build-plugin/.../BuildAndroidConfig.kt` | `MIN_SDK_VERSION = 28` → `java.time.Instant` aman tanpa desugar khusus |

---

## 5. File yang TIDAK BOLEH Diubah

Perubahan pada file ini berisiko merusak auth yang sudah production / kontrak Phase 1.

### Domain Layer — Freeze (Phase 1 Contract)

| File / Area | Alasan |
|-------------|--------|
| Semua file di `core/domain/src/.../model/` | Kontrak entity/enum sudah dipakai use case; ubah = cascade ke semua mapper |
| Semua file di `core/domain/src/.../repository/` | Interface adalah API Phase 2; jangan ubah signature |
| Semua file di `core/domain/src/.../usecase/` | Business validation sudah final untuk MVP |
| `core/domain/build.gradle.kts` | Pure Kotlin module — jangan tarik Room/Android |

### Auth / User Pipeline — Production Stable

| File | Alasan |
|------|--------|
| `core/domain/.../model/User.kt` | Auth + DataStore + Firestore profile |
| `core/domain/.../model/AuthResult.kt` | Auth result types |
| `core/domain/.../model/TokenResult.kt` | Google credential flow |
| `core/domain/.../repository/UserRepository.kt` | Auth interface |
| `core/domain/.../usecase/SignInWithGoogleUseCase.kt` | Existing use case |
| `core/data/.../repository/UserRepositoryImpl.kt` | Orchestration auth kompleks + rollback |
| `core/data/.../datasource/AuthNetworkDataSource.kt` | Auth DS interface |
| `core/data/.../datasource/AuthNetworkDataSourceImpl.kt` | Firebase Auth |
| `core/data/.../datasource/FirestoreNetworkDataSource.kt` | **Khusus user profile** — jangan campur financial ops di sini |
| `core/data/.../datasource/UserProfileLocalDataSource.kt` | DataStore interface |
| `core/data/.../datasource/UserProfileLocalDataSourceImpl.kt` | DataStore impl |
| `core/data/.../mapper/AuthUserMapper.kt` | Auth mapper |
| `core/data/.../mapper/SignedInUserProtoMapper.kt` | Proto mapper |
| `core/data/.../model/AuthUserResponse.kt` | Auth response model |
| `core/data/.../di/FirebaseModule.kt` | Firebase Auth/Firestore providers — sudah cukup |
| `core/datastore/**` | Proto session cache — di luar scope finansial |

### Feature Modules & App Navigation

| File / Area | Alasan |
|-------------|--------|
| Semua file di `features/auth/` | Auth complete |
| Semua file di `features/splashscreen/` | Splash complete |
| Semua file di `features/dashboard/` | Wiring data = Phase 4 |
| Semua file di `features/transaction/` | Full flow = Phase 5 |
| Semua file di `features/family/` | Phase 6 |
| Semua file di `features/settings/` | Phase 7 |
| `app/src/main/kotlin/.../navigation/*` | Nav sudah jalan; Phase 2 tidak menambah route |

### Infra yang Harus Stabil

| File / Area | Alasan |
|-------------|--------|
| `build-plugin/` (semua file) | Convention plugins stable — **jangan** buat Room convention plugin di Phase 2 kecuali benar-benar perlu |
| `settings.gradle.kts` | Tidak perlu module baru |
| `build.gradle.kts` (root) | Tidak perlu |
| `gradle.properties` | Tidak perlu |
| `local.properties` | Secrets |

---

## 6. File yang BOLEH Diubah (Additive Only)

| File | Jenis perubahan yang diizinkan |
|------|--------------------------------|
| `gradle/libs.versions.toml` | **Additive:** versions + library aliases Room/WorkManager/(opsional) hilt-work |
| `core/data/build.gradle.kts` | **Additive:** `implementation` / `ksp` untuk Room, WorkManager, Moshi |
| `core/data/.../di/CommonRepositoryModule.kt` | **Additive:** `@Binds` untuk repo baru — **jangan ubah** `bindUserRepository` |
| `core/data/.../di/CommonDataSourceModule.kt` | **Additive:** bind DS baru, atau buat module baru jika file terlalu ramai |
| `core/data/.../di/CommonMapperModule.kt` | **Additive:** mapper finansial, atau module baru |
| File **baru** di bawah `core/data/src/...` | Semua entity/DAO/repo/sync/worker sesuai Section 7 |

**Aturan keras:**
- Jangan refactor / rename auth classes “sekalian”
- Jangan mengubah signature method existing di `FirestoreNetworkDataSource`
- Jangan menghapus binding Hilt yang sudah ada

---

## 7. Struktur File yang Akan Dibuat

```
core/data/src/main/kotlin/com/mascill/keutrack/core/data/
├── db/
│   ├── AppDatabase.kt                 ← BARU
│   ├── Converters.kt                  ← BARU (TypeConverters)
│   ├── entity/
│   │   ├── TransactionEntity.kt       ← BARU
│   │   ├── WalletEntity.kt            ← BARU
│   │   ├── CategoryEntity.kt          ← BARU
│   │   ├── BudgetEntity.kt            ← BARU
│   │   └── CategorySummaryEntity.kt   ← BARU
│   └── dao/
│       ├── TransactionDao.kt          ← BARU
│       ├── WalletDao.kt               ← BARU
│       ├── CategoryDao.kt             ← BARU
│       ├── BudgetDao.kt               ← BARU
│       └── CategorySummaryDao.kt      ← BARU
│
├── datasource/
│   ├── Auth* / UserProfile* / FirestoreNetworkDataSource.kt  ← EXISTING (jangan ubah)
│   ├── local/
│   │   ├── TransactionLocalDataSource.kt (+ Impl)
│   │   ├── WalletLocalDataSource.kt (+ Impl)
│   │   ├── CategoryLocalDataSource.kt (+ Impl)
│   │   ├── BudgetLocalDataSource.kt (+ Impl)
│   │   └── CategorySummaryLocalDataSource.kt (+ Impl)
│   └── firestore/
│       ├── TransactionFirestoreDataSource.kt
│       ├── WalletFirestoreDataSource.kt
│       ├── BudgetFirestoreDataSource.kt
│       ├── CategoryFirestoreDataSource.kt      ← opsional MVP
│       └── CategorySummaryFirestoreDataSource.kt
│
├── mapper/
│   ├── AuthUserMapper.kt / SignedInUserProtoMapper.kt  ← EXISTING (jangan ubah)
│   ├── TransactionMapper.kt
│   ├── WalletMapper.kt
│   ├── CategoryMapper.kt
│   ├── BudgetMapper.kt
│   └── CategorySummaryMapper.kt
│
├── repository/
│   ├── UserRepositoryImpl.kt          ← EXISTING (jangan ubah)
│   ├── TransactionRepositoryImpl.kt   ← BARU
│   ├── WalletRepositoryImpl.kt        ← BARU
│   ├── CategoryRepositoryImpl.kt      ← BARU
│   ├── BudgetRepositoryImpl.kt        ← BARU
│   └── SyncRepositoryImpl.kt          ← BARU
│
├── sync/
│   ├── SyncScheduler.kt               ← BARU
│   ├── SyncWorker.kt                  ← BARU
│   └── (opsional) SyncConstraints.kt
│
└── di/
    ├── FirebaseModule.kt              ← EXISTING (jangan ubah)
    ├── SystemServiceModule.kt         ← EXISTING
    ├── CommonRepositoryModule.kt      ← UPDATE additive
    ├── CommonDataSourceModule.kt      ← UPDATE additive (atau module baru)
    ├── CommonMapperModule.kt          ← UPDATE additive (atau module baru)
    ├── DatabaseModule.kt              ← BARU
    └── SyncModule.kt                  ← BARU (WorkManager + WorkerFactory)
```

Perkiraan: **~35–45 file baru** + 3–4 file config/DI yang di-extend secara additive.

---

## 8. Arsitektur Offline-First (Wajib Dipahami)

```
┌─────────────┐     ┌──────────┐     ┌────────────────────┐
│  UseCase    │────▶│ Repository│────▶│ Room (source of    │
│  (domain)   │     │  Impl     │     │ truth untuk READ)  │
└─────────────┘     └─────┬────┘     └─────────▲──────────┘
                          │ write              │ update syncStatus
                          ▼                    │
                   ┌──────────────┐     ┌──────┴───────┐
                   │ SyncScheduler│────▶│  WorkManager │
                   └──────────────┘     └──────┬───────┘
                                               ▼
                                        ┌──────────────┐
                                        │  Firestore   │
                                        │  batch write │
                                        └──────────────┘
```

### Aturan mutlak

1. **Semua read UI** → Room `Flow` saja. Jangan observe Firestore langsung dari repository finansial.
2. **Semua write user** → Room dulu (`syncStatus = PENDING`), UI update segera.
3. **Firestore** hanya dipanggil dari jalur sync (`SyncRepository` / Worker).
4. **Atomic local write** untuk `addTransaction` harus dalam satu Room `@Transaction`:
   - insert transaction
   - update wallet `balance` (+income / −expense)
   - update budget `spent` jika ada budget kategori+bulan yang match
   - upsert `CategorySummary` bulan berjalan
5. Setelah commit lokal → `SyncScheduler.enqueue()` (jangan block UI menunggu network).
6. Sync sukses → set `syncStatus = SYNCED`. Gagal berulang → `FAILED` (tetap bisa di-retry).
7. `id` transaksi/wallet = UUID lokal = Firestore document ID (idempotent retry).

### Batch write Firestore (saat sync satu transaksi)

Dalam **satu** `WriteBatch` / `runTransaction`:

1. `set` document `/transactions/{txnId}`
2. `FieldValue.increment` pada `/wallets/{walletId}.balance` (± amount)
3. Jika budget match: `increment` `/budgets/{budgetId}.spent`
4. Upsert `/users/{userId}/category_summaries/{yyyy-MM}` (total + byCategory)

> Side-effect balance/spent/summary **sudah diterapkan lokal** saat write. Sync Firestore harus memakai nilai yang konsisten (set absolute atau increment yang sama). Pilih **satu strategi** dan dokumentasikan di kode:
>
> - **Strategi A (disarankan MVP):** lokal pakai absolute update; sync Firestore pakai `set` document transaksi + `increment` remote untuk balance/spent (asumsi remote mulai dari seed yang sama).
> - **Strategi B (lebih aman multi-device):** lokal absolute; sync hanya `set` transaksi; cloud function menghitung ulang balance — **out of scope Phase 2**.
>
> Untuk single-device / family ringan: **Strategi A**. Pastikan wallet & budget di-create/seed ke Firestore sebelum increment.

---

## 9. Task Breakdown Detail

### Task 0: Prerequisites Check

```bash
# Pastikan Phase 1 compile
./gradlew :core:domain:compileDebugKotlin

# Pastikan domain file kontrak ada
ls core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/repository/
# Harus ada: TransactionRepository, WalletRepository, CategoryRepository,
#            BudgetRepository, SyncRepository, UserRepository
```

Jika domain belum lengkap → **hentikan Phase 2**, selesaikan Phase 1 dulu.

---

### Task 1: Version Catalog — Room + WorkManager

**File:** `gradle/libs.versions.toml`

Tambahkan (versi boleh disesuaikan dengan BOM AndroidX terbaru yang kompatibel AGP 8.11 / Kotlin 2.1):

```toml
# [versions]
androidxRoom = "2.7.2"
androidxWork = "2.10.2"
androidxHiltWork = "1.2.0"

# [libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "androidxRoom" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "androidxRoom" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "androidxRoom" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "androidxWork" }
androidx-hilt-work = { group = "androidx.hilt", name = "hilt-work", version.ref = "androidxHiltWork" }
androidx-hilt-compiler = { group = "androidx.hilt", name = "hilt-compiler", version.ref = "androidxHiltWork" }
```

**Jangan** menghapus / mengubah alias existing.

---

### Task 2: `core/data/build.gradle.kts`

```kotlin
dependencies {
    implementation(projects.core.datastore)
    implementation(projects.core.domain)
    implementation(projects.core.network)

    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Phase 2
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.moshi.kotlin) // TypeConverter CategorySummary.byCategory
    implementation(libs.kotlinx.coroutines.play.services) // jika belum transitif
}
```

> `keutrack.hilt` sudah apply KSP — cukup tambah `ksp(...)` Room/Hilt-Work.

---

### Task 3: TypeConverters

**File:** `core/data/.../db/Converters.kt`

Konversi yang dibutuhkan:

| Domain type | Room storage |
|-------------|--------------|
| `Instant` | `Long` (epoch milli) |
| `TransactionType` / `WalletType` / `CategoryType` / `BudgetPeriod` | `String` (`enum.value`) |
| `SyncStatus` | `String` (`enum.name`) |
| `Map<String, CategoryBreakdown>` | `String` JSON (Moshi) |

```kotlin
class Converters {
    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    // enum converters memakai fromValue() / value dari domain
    // SyncStatus memakai name / valueOf dengan fallback SYNCED atau PENDING
    // Map converter: Moshi adapter Map<String, CategoryBreakdown>
}
```

**Keputusan:**
- Jangan persist computed properties (`Budget.remaining`, `progressPercent`, `CategorySummary.netBalance`)
- JSON map cukup untuk MVP; normalisasi ke child table boleh ditunda jika query by-category breakdown belum dibutuhkan di SQL

---

### Task 4: Room Entities

Mirror 1:1 field domain (kecuali computed). Contoh inti:

**`TransactionEntity`**

```kotlin
@Entity(
    tableName = "transactions",
    indices = [
        Index("walletId"),
        Index("userId"),
        Index("categoryId"),
        Index("date"),
        Index("syncStatus"),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val walletId: String,
    val userId: String,
    val familyId: String?,
    val type: String,          // TransactionType.value
    val amount: Long,
    val categoryId: String,
    val note: String?,
    val dateEpochMs: Long,
    val addedByName: String,
    val syncStatus: String,    // SyncStatus.name
    val createdAtEpochMs: Long,
)
```

**`WalletEntity`** — table `wallets`, index `ownerId`, `type`, `syncStatus`

**`CategoryEntity`** — table `categories`, index `type`, `isDefault`

**`BudgetEntity`** — table `budgets`, index (`month`, `categoryId`), `syncStatus`

**`CategorySummaryEntity`** — table `category_summaries`

```kotlin
@Entity(
    tableName = "category_summaries",
    primaryKeys = ["period", "userId"],
)
data class CategorySummaryEntity(
    val period: String,          // "yyyy-MM"
    val userId: String,
    val familyId: String?,
    val totalIncome: Long,
    val totalExpense: Long,
    val byCategoryJson: String,  // Map JSON
    val topExpenseCategoryId: String?,
)
```

**Referensi field:** Phase 1 Task 2–6.

---

### Task 5: DAOs

Setiap DAO expose:

- `observe*` → `Flow<List<Entity>>` / `Flow<Entity?>`
- `getById` → `suspend`
- `insert` / `upsert` / `update` / `delete`
- `observePending()` / `getPending()` → `syncStatus = PENDING | FAILED` untuk sync worker

**`TransactionDao` query penting:**

```kotlin
@Query("""
    SELECT * FROM transactions
    WHERE (:walletId IS NULL OR walletId = :walletId)
      AND (:type IS NULL OR type = :type)
      AND (:categoryId IS NULL OR categoryId = :categoryId)
      AND (:startMs IS NULL OR dateEpochMs >= :startMs)
      AND (:endMs IS NULL OR dateEpochMs <= :endMs)
    ORDER BY dateEpochMs DESC
    LIMIT :limit
""")
fun observeFiltered(...): Flow<List<TransactionEntity>>

@Query("SELECT * FROM transactions ORDER BY dateEpochMs DESC LIMIT :limit")
fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

@Query("SELECT * FROM transactions WHERE syncStatus IN ('PENDING', 'FAILED')")
suspend fun getPending(): List<TransactionEntity>
```

**`WalletDao`:** `observeAll`, `observeByType`, `observeById`, `getPersonal()` (`type = 'personal' LIMIT 1`)

**`CategoryDao`:** `observeAll`, `observeByType` (include `BOTH` when filtering income/expense — put logic di local DS atau query `type IN (:type, 'both')`)

**`BudgetDao`:** `observeByMonth(month)`, pending sync

**`CategorySummaryDao`:** `observeByPeriod`, `observeByPeriods(months)`, upsert

Gunakan `@Transaction` DAO method atau repository-level `@androidx.room.Transaction` function di local DS untuk atomic multi-table write.

---

### Task 6: `AppDatabase` + `DatabaseModule`

```kotlin
@Database(
    entities = [
        TransactionEntity::class,
        WalletEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        CategorySummaryEntity::class,
    ],
    version = 1,
    exportSchema = true, // disarankan; siapkan folder schemas/ jika diaktifkan
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categorySummaryDao(): CategorySummaryDao
}
```

**`DatabaseModule`:**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDb(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "keutrack.db")
            .fallbackToDestructiveMigration() // OK untuk v1 pre-production; ganti Migration sebelum release
            .build()

    @Provides fun provideTransactionDao(db: AppDatabase) = db.transactionDao()
    // ... DAO lain
}
```

---

### Task 7: Local Data Sources

Pola (ikuti `UserProfileLocalDataSource`):

```kotlin
interface TransactionLocalDataSource {
    fun observeFiltered(...): Flow<List<TransactionEntity>>
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>
    suspend fun getById(id: String): TransactionEntity?
    suspend fun upsert(entity: TransactionEntity)
    suspend fun delete(id: String)
    suspend fun getPending(): List<TransactionEntity>
    suspend fun updateSyncStatus(id: String, status: SyncStatus)
}

class TransactionLocalDataSourceImpl @Inject constructor(
    private val dao: TransactionDao,
) : TransactionLocalDataSource { ... }
```

**Khusus write transaksi — method orchestration lokal:**

```kotlin
suspend fun applyNewTransactionAtomically(
    transaction: TransactionEntity,
    walletDelta: Long,                 // +income / -expense
    budgetIdToIncrement: String?,      // nullable
    summaryUpsert: CategorySummaryEntity,
)
```

Implementasi memakai `@Transaction` di DAO atau `withTransaction { }` Room.

---

### Task 8: Firestore Data Sources (Financial — File Baru)

**Jangan edit** `FirestoreNetworkDataSource.kt`.

Buat class baru dengan companion constants, contoh `TransactionFirestoreDataSource`:

```kotlin
@Singleton
class TransactionFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    suspend fun upsertTransactionWithSideEffects(
        transaction: Transaction,
        walletBalanceDelta: Long,
        budgetId: String?,
        budgetSpentDelta: Long,
        summary: CategorySummary,
    ) { /* WriteBatch */ }

    private companion object {
        const val COLLECTION_TRANSACTIONS = "transactions"
        const val COLLECTION_WALLETS = "wallets"
        const val COLLECTION_BUDGETS = "budgets"
        const val FIELD_AMOUNT = "amount"
        // ...
    }
}
```

#### Schema Firestore (selaras Phase 1)

**`/transactions/{txnId}`**

| Field | Type |
|-------|------|
| `id` | string |
| `walletId` | string |
| `userId` | string |
| `familyId` | string? |
| `type` | `"income"` \| `"expense"` |
| `amount` | number (Long) |
| `categoryId` | string |
| `note` | string? |
| `date` | timestamp |
| `addedByName` | string |
| `createdAt` | timestamp |

**`/wallets/{walletId}`** — mirror `Wallet` (tanpa `syncStatus`)

**`/budgets/{budgetId}`** — mirror `Budget` (tanpa computed / `syncStatus`)

**`/categories/{categoryId}`** — mirror `Category`

**`/users/{userId}/category_summaries/{yyyy-MM}`** — mirror `CategorySummary` (`byCategory` sebagai map)

Simpan timestamp Firestore sebagai `Timestamp` / `FieldValue.serverTimestamp()` di `createdAt` bila perlu; `date` transaksi pakai nilai dari domain `Instant`.

---

### Task 9: Mappers

Setiap mapper:

```kotlin
class TransactionMapper @Inject constructor() {
    fun toDomain(entity: TransactionEntity): Transaction = ...
    fun toEntity(domain: Transaction): TransactionEntity = ...
}
```

Aturan:
- Class terpisah, `@Inject constructor`
- Enum: `TransactionType.fromValue(entity.type)` / `domain.type.value`
- `Instant`: `ofEpochMilli` / `toEpochMilli`
- Jangan map ke tipe UI

---

### Task 10: `CategoryRepositoryImpl` + Seed Defaults

Implementasikan `seedDefaultCategories()` dengan 10 kategori dari Phase 1:

| id | name | type |
|----|------|------|
| `cat_makanan` | Makanan | EXPENSE |
| `cat_transport` | Transport | EXPENSE |
| `cat_tagihan` | Tagihan | EXPENSE |
| `cat_pendidikan` | Pendidikan | EXPENSE |
| `cat_hiburan` | Hiburan | EXPENSE |
| `cat_kesehatan` | Kesehatan | EXPENSE |
| `cat_belanja` | Belanja | EXPENSE |
| `cat_gaji` | Gaji | INCOME |
| `cat_investasi` | Investasi | INCOME |
| `cat_lainnya` | Lainnya | BOTH |

Aturan seed:
- Idempotent: jika row sudah ada, skip / upsert tanpa duplikasi
- `isDefault = true`, `userId = null`
- Icon/color string konsisten (boleh hardcoded hex + Material icon name)
- Dipanggil dari: app startup setelah login **atau** lazy saat `observeCategories()` pertama kali kosong — pilih satu, dokumentasikan

**Jangan** memanggil seed dari `UserRepositoryImpl` (file protected).

Opsi aman MVP: `CategoryRepositoryImpl.observeCategories()` melakukan one-shot seed jika count == 0 (guard dengan mutex/flag).

---

### Task 11: `WalletRepositoryImpl` + Default Personal Wallet

- `observe*` dari Room
- `createWallet` → insert lokal `PENDING` → enqueue sync
- `getPersonalWallet()` → DAO personal
- **Ensure default wallet:** jika user login dan belum ada personal wallet, create:

```kotlin
Wallet(
    id = UUID.randomUUID().toString(),
    ownerId = currentUserId, // dari auth uid — inject UserProfileLocalDataSource.observe / get once
    name = "Dompet Utama",
    type = WalletType.PERSONAL,
    balance = 0L,
    currency = "IDR",
    syncStatus = SyncStatus.PENDING,
)
```

Cara ambil `currentUserId` tanpa mengubah `UserRepositoryImpl`:
- Inject `UserProfileLocalDataSource` dan baca user tersimpan, **atau**
- Inject `FirebaseAuth.currentUser?.uid` via existing `AuthNetworkDataSource.getCurrentUser()`

Jangan menambah method ke `UserRepository` di Phase 2 kecuali benar-benar terpaksa (itu mengubah domain freeze).

---

### Task 12: `TransactionRepositoryImpl` (Paling Kritis)

```kotlin
class TransactionRepositoryImpl @Inject constructor(
    private val local: TransactionLocalDataSource,
    private val walletLocal: WalletLocalDataSource,
    private val budgetLocal: BudgetLocalDataSource,
    private val summaryLocal: CategorySummaryLocalDataSource,
    private val mapper: TransactionMapper,
    private val syncScheduler: SyncScheduler,
) : TransactionRepository
```

**`observe*` / `get*`:** map entity → domain dari Room saja.

**`addTransaction`:**

1. Pastikan `syncStatus = PENDING`
2. Hitung `walletDelta` = `+amount` jika INCOME else `-amount`
3. Cari budget aktif: `month = YearMonth.from(transaction.date)` format `"yyyy-MM"`, `categoryId` match
4. Hitung summary baru (increment totals + byCategory + topExpense)
5. `local.applyNewTransactionAtomically(...)`
6. `syncScheduler.enqueueSync()`
7. Jangan catch-all yang menelan `CancellationException`

**`updateTransaction` / `deleteTransaction`:** MVP boleh:
- Update/delete lokal + set PENDING + adjust balance/summary secara konservatif
- Atau batasi: delete hanya jika `SYNCED` belum — **dokumentasikan batasan** jika simplify

Untuk MVP Phase 2, prioritaskan **add + observe** solid; update/delete boleh implementasi sederhana (local adjust + resync) asalkan tidak corrupt balance.

---

### Task 13: `BudgetRepositoryImpl`

- `observeBudgets(month)` / CRUD lokal + enqueue sync untuk create/update/delete
- `observeMonthlySummary` / `observeMonthlySummaries` dari `CategorySummaryLocalDataSource`
- Create budget → `spent = 0`, `syncStatus = PENDING`

---

### Task 14: `SyncRepositoryImpl`

```kotlin
class SyncRepositoryImpl @Inject constructor(
    private val transactionLocal: TransactionLocalDataSource,
    private val walletLocal: WalletLocalDataSource,
    private val budgetLocal: BudgetLocalDataSource,
    private val transactionRemote: TransactionFirestoreDataSource,
    private val walletRemote: WalletFirestoreDataSource,
    private val budgetRemote: BudgetFirestoreDataSource,
    private val summaryLocal: CategorySummaryLocalDataSource,
    private val summaryRemote: CategorySummaryFirestoreDataSource,
    // mappers...
) : SyncRepository {

    override suspend fun syncPendingTransactions() { ... }
    override suspend fun syncPendingWallets() { ... }
    override suspend fun syncPendingBudgets() { ... }
    override suspend fun syncAll() {
        syncPendingWallets()
        syncPendingBudgets()
        syncPendingTransactions() // terakhir — butuh wallet/budget remote exist
    }
}
```

Per item pending:
1. Push ke Firestore
2. Sukses → `updateSyncStatus(SYNCED)`
3. Gagal network → biarkan `PENDING` / set `FAILED`, rethrow atau log; WorkManager akan retry
4. Selalu `catch (e: CancellationException) { throw e }`

---

### Task 15: WorkManager + `SyncScheduler`

**`SyncScheduler`:**

```kotlin
fun enqueueSync() {
    val request = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()

    WorkManager.getInstance(context)
        .enqueueUniqueWork(
            UNIQUE_SYNC_WORK,
            ExistingWorkPolicy.KEEP, // atau APPEND_OR_REPLACE
            request,
        )
}
```

**`SyncWorker`:** `@HiltWorker`, inject `SyncRepository`, panggil `syncAll()`, return `Result.retry()` on failure.

**Wiring Hilt:**
- `SyncModule` provide `WorkManager`
- Di `Application` class: konfigurasi `HiltWorkerFactory` — **cek dulu** apakah `KeuTrackApplication` sudah ada; jika perlu ubah Application, ini **satu-satunya** file `app/` yang boleh disentuh di Phase 2, dan hanya untuk WorkerFactory. Jangan ubah navigation.

Cari Application class:

```bash
rg -n "class .*Application" app/src
```

Jika belum pakai `@HiltAndroidApp` Configuration.Provider — tambahkan sesuai dokumentasi Hilt WorkManager.

---

### Task 16: Hilt DI Binding (Additive)

**`CommonRepositoryModule` — tambah saja:**

```kotlin
@Binds fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository
@Binds fun bindWalletRepository(impl: WalletRepositoryImpl): WalletRepository
@Binds fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository
@Binds fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository
@Binds fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository
```

Biarkan `bindUserRepository` utuh.

Bind data sources + mappers secara serupa (module existing atau `FinancialDataSourceModule` / `FinancialMapperModule` baru — lebih bersih).

---

### Task 17: Smoke Integration (Tanpa UI Feature)

Buat **sementara** hanya jika perlu debug — **jangan commit debug Activity**. Verifikasi lewat:

1. Compile `:core:data`
2. Instrumentasi manual opsional: unit test repository dengan Room in-memory (boleh ditunda Phase 9)
3. Pastikan Hilt graph resolve: `./gradlew assembleDevDebug` — jika ada missing binding, build akan gagal di KSP/Hilt

Tidak perlu menghubungkan Dashboard di Phase 2.

---

## 10. Acceptance Criteria

### Harus Terpenuhi Sebelum Phase 2 Dianggap Selesai

- [ ] **Room dependencies** ada di `libs.versions.toml` dan terpakai di `:core:data`
- [ ] **`AppDatabase` v1** berisi 5 entity finansial + TypeConverters
- [ ] **Semua repository interface Phase 1** punya `*Impl` + `@Binds` Hilt
- [ ] **`SyncRepositoryImpl`** mengimplementasikan keempat method
- [ ] **WorkManager** ter-enqueue setelah write transaksi (minimal) dan memanggil sync
- [ ] **Offline-first:** `observe*` hanya baca Room
- [ ] **`addTransaction` lokal atomic** meng-update wallet balance (+ summary; budget jika ada)
- [ ] **`seedDefaultCategories()`** idempotent, 10 kategori Phase 1
- [ ] **Default personal wallet** bisa dibuat saat belum ada
- [ ] **`FirestoreNetworkDataSource` (user)** tidak dimodifikasi
- [ ] **`UserRepositoryImpl` dan semua auth files** tidak dimodifikasi
- [ ] **Domain layer** tidak berubah
- [ ] **Feature modules** tidak berubah (kecuali Application WorkerFactory di `app` jika wajib)
- [ ] **CancellationException** selalu di-rethrow di repository/sync
- [ ] **amount/balance/spent** tetap `Long`
- [ ] Build sukses:

```bash
./gradlew :core:data:compileDebugKotlin
./gradlew assembleDevDebug
```

### Verification Steps

```bash
# 1. Compile data module
./gradlew :core:data:compileDebugKotlin

# 2. Full app (Hilt graph + Room KSP)
./gradlew assembleDevDebug

# 3. Pastikan tidak ada perubahan di domain / auth (expect clean)
git diff --stat -- core/domain features/auth features/splashscreen \
  core/data/src/main/kotlin/com/mascill/keutrack/core/data/repository/UserRepositoryImpl.kt \
  core/data/src/main/kotlin/com/mascill/keutrack/core/data/datasource/FirestoreNetworkDataSource.kt
```

---

## 11. Catatan Arsitektur & Konvensi

### Dependency Rules

| Aturan | Detail |
|--------|--------|
| Feature ↛ Room | Feature hanya kenal UseCase / domain repo interface |
| Data implements Domain | `*Impl` di `core/data`, interface di `core/domain` |
| Auth terpisah dari finansial | Jangan campur financial writes ke `FirestoreNetworkDataSource` |
| Reads lokal | Room = source of truth |

### Naming

| Tipe | Format | Contoh |
|------|--------|--------|
| Room entity | `NamaEntity` | `TransactionEntity` |
| DAO | `NamaDao` | `TransactionDao` |
| Local DS | `NamaLocalDataSource` | `TransactionLocalDataSource` |
| Firestore DS | `NamaFirestoreDataSource` | `WalletFirestoreDataSource` |
| Mapper | `NamaMapper` | `BudgetMapper` |
| Repo impl | `NamaRepositoryImpl` | `SyncRepositoryImpl` |

### Error Handling

```kotlin
try {
    // ...
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    // map / rethrow / set FAILED
}
```

### Financial Conventions (lanjutan Phase 1)

| Keputusan | Phase 2 implication |
|-----------|---------------------|
| `Long` amount | Column Room `INTEGER` |
| UUID id | Primary key = Firestore doc id |
| `SyncStatus` lokal | Column di entity; **tidak** dikirim ke Firestore |
| Denormalized balance/spent | Update lokal atomic + increment remote on sync |

---

## 12. Dependency Graph

```
:core:domain (Phase 1 — freeze)
    ▲ implements
:core:data
    ├── db/ (Room)
    ├── datasource/local  ──▶ DAO
    ├── datasource/firestore ──▶ FirebaseFirestore (financial collections)
    ├── mapper/
    ├── repository/*Impl
    └── sync/ (WorkManager → SyncRepositoryImpl)

Hilt graph (SingletonComponent)
    UserRepository → UserRepositoryImpl          (existing)
    TransactionRepository → TransactionRepositoryImpl
    WalletRepository → WalletRepositoryImpl
    CategoryRepository → CategoryRepositoryImpl
    BudgetRepository → BudgetRepositoryImpl
    SyncRepository → SyncRepositoryImpl

                    ▼ dikonsumsi nanti oleh ▼
Phase 4 Dashboard / Phase 5 Transaction / Phase 6 Family / Phase 7 Settings
(via UseCase — tanpa depend langsung ke Room)
```

---

## 13. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| Double-apply increment saat retry sync | Balance Firestore salah | Idempotency: tandai SYNCED hanya setelah batch sukses; atau simpan `syncedAt` / pakai transaction doc existence check sebelum increment |
| `CategorySummary.byCategory` JSON rapuh | Corrupt summary | Schema Moshi stabil; validasi parse dengan fallback empty map; pertimbangkan child table di phase berikutnya |
| `fallbackToDestructiveMigration` | Data hilang saat naik versi | Hanya untuk pre-release; sebelum produksi wajib `Migration` |
| Hilt WorkerFactory belum di Application | Worker crash di runtime | Wajib Task 15; verifikasi dengan enqueue manual di debug |
| Ambil `userId` tanpa ubah UserRepository | Bingung sumber uid | Pakai `AuthNetworkDataSource.getCurrentUser()` atau DataStore local user — jangan sentuh `UserRepositoryImpl` |
| Scope creep sync multi-device / conflict | Delay Phase 2 | Single-writer assumption MVP; conflict resolution ditunda |
| Edit `FirestoreNetworkDataSource` “biar cepat” | Regresi auth profile | Larang keras — file baru di `datasource/firestore/` |
| Room query filter null di SQLite | Query salah | Uji `IS NULL OR col = :param` pattern; tulis comment di DAO |
| Feature module mencoba akses DAO | Langgar arsitektur | Jangan export DAO di luar `core/data`; feature hanya UseCase |

---

## 14. Urutan Pengerjaan yang Disarankan

```
Step 1: Gradle
  └── libs.versions.toml → core/data/build.gradle.kts → sync/compile kosong

Step 2: Room foundation
  └── Converters → Entities → DAOs → AppDatabase → DatabaseModule

Step 3: Local DS + Mappers
  └── LocalDataSource interfaces/impls → Mappers entity↔domain

Step 4: Repository impl (local-only dulu)
  └── CategoryRepositoryImpl (+ seed)
  └── WalletRepositoryImpl (+ default wallet)
  └── BudgetRepositoryImpl
  └── TransactionRepositoryImpl (atomic write, scheduler no-op atau log dulu)

Step 5: Firestore DS + Sync
  └── *FirestoreDataSource
  └── SyncRepositoryImpl
  └── SyncScheduler + SyncWorker + Application WorkerFactory
  └── Wire enqueue dari TransactionRepositoryImpl / Wallet / Budget writes

Step 6: DI bindings
  └── CommonRepositoryModule (+ DataSource/Mapper/Sync modules)

Step 7: Verify
  └── ./gradlew :core:data:compileDebugKotlin
  └── ./gradlew assembleDevDebug
  └── git diff: pastikan auth + domain + features tidak berubah
```

### Definition of Ready untuk Phase 4

Phase 4 (Dashboard real data) boleh dimulai jika:
1. Hilt bisa inject semua UseCase Phase 1 tanpa missing binding
2. `AddTransactionUseCase` menulis ke Room dan muncul di `GetTransactionsUseCase` / `GetWalletSummaryUseCase` (bisa diverifikasi sementara lewat debug/test)
3. Sync worker tidak crash app (sukses atau retry dengan network)

---

## Ringkasan File Policy

| Kategori | Policy |
|----------|--------|
| `core/domain/**` | ❌ Jangan ubah |
| Auth pipeline (`UserRepository*`, `Auth*`, `FirestoreNetworkDataSource`, DataStore user) | ❌ Jangan ubah |
| `features/**`, nav app | ❌ Jangan ubah |
| `build-plugin/**` | ❌ Jangan ubah |
| `gradle/libs.versions.toml` | ✅ Additive only |
| `core/data/build.gradle.kts` | ✅ Additive only |
| `core/data/.../di/Common*Module.kt` | ✅ Additive binds only |
| `core/data/**` file baru (db/datasource/mapper/repository/sync) | ✅ Buat baru |
| `app/.../*Application*` | ⚠️ Hanya jika perlu Hilt WorkerFactory |

---

*Dokumen ini adalah referensi implementasi untuk Phase 2 KeuTrack. Setelah Phase 2 selesai, lanjut ke Phase 4 (Dashboard real data) atau Phase 5 (Transaction flow) — design system Phase 3 sudah ~90% selesai dan bisa dilewati.*

