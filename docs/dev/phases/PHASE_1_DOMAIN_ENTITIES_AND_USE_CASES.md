# Phase 1 — core:domain (Entity, Enum, Repository Interface & UseCase)

> **Modul target:** `:core:domain`
> **Estimasi:** ~2 hari
> **Prasyarat:** Phase 0 (selesai) — build-plugin, gradle setup, module structure
> **Hasil akhir:** Semua domain model, enum, repository interface, dan use case untuk fitur finansial tersedia — siap diimplementasikan oleh `:core:data` di Phase 2

---

## Daftar Isi

1. [Konteks & Tujuan](#1-konteks--tujuan)
2. [Scope — Apa yang Dikerjakan](#2-scope--apa-yang-dikerjakan)
3. [Scope — Apa yang TIDAK Dikerjakan](#3-scope--apa-yang-tidak-dikerjakan)
4. [File Referensi (Read-Only)](#4-file-referensi-read-only)
5. [File yang TIDAK BOLEH Diubah](#5-file-yang-tidak-boleh-diubah)
6. [Struktur File yang Akan Dibuat](#6-struktur-file-yang-akan-dibuat)
7. [Task Breakdown Detail](#7-task-breakdown-detail)
8. [Acceptance Criteria](#8-acceptance-criteria)
9. [Catatan Arsitektur & Konvensi](#9-catatan-arsitektur--konvensi)
10. [Dependency Graph](#10-dependency-graph)
11. [Risiko & Mitigasi](#11-risiko--mitigasi)

---

## 1. Konteks & Tujuan

Saat ini modul `:core:domain` hanya berisi entity dan repository untuk **user authentication**:

- `User`, `AuthResult`, `TokenResult` (model)
- `UserRepository`, `SyncRepository` (repository interface)
- `SignInWithGoogleUseCase` (use case)

Belum ada domain model untuk **fitur inti finansial** — transaction, wallet, category, dan budget. Tanpa ini, semua feature module (dashboard, transaction, family, settings) masih menggunakan mock data dan tidak bisa menyimpan/membaca data keuangan yang sesungguhnya.

**Tujuan Phase 1:**
- Definisikan semua domain entity yang dibutuhkan fitur finansial
- Buat enum untuk tipe data yang memiliki nilai terbatas
- Definisikan repository interface sebagai kontrak antara domain dan data layer
- Buat use case yang mengenkapsulasi business logic spesifik
- Pastikan semua model konsisten dengan `KeuTrack_Data_Design.md`

---

## 2. Scope — Apa yang Dikerjakan

### A. Domain Model (Entity)

| # | File | Deskripsi |
|---|------|-----------|
| 1 | `Transaction.kt` | Entity transaksi keuangan (income/expense) |
| 2 | `Wallet.kt` | Entity dompet (personal/family) |
| 3 | `Category.kt` | Entity kategori transaksi |
| 4 | `Budget.kt` | Entity anggaran per kategori per bulan |
| 5 | `CategorySummary.kt` | Entity ringkasan pengeluaran/pemasukan per bulan |
| 6 | `SyncStatus.kt` | Enum status sinkronisasi offline-first |

### B. Enum

| # | File | Deskripsi |
|---|------|-----------|
| 7 | `TransactionType.kt` | `INCOME` / `EXPENSE` |
| 8 | `WalletType.kt` | `PERSONAL` / `FAMILY` |
| 9 | `BudgetPeriod.kt` | `MONTHLY` / `WEEKLY` |
| 10 | `CategoryType.kt` | `INCOME` / `EXPENSE` / `BOTH` |

### C. Repository Interface

| # | File | Deskripsi |
|---|------|-----------|
| 11 | `TransactionRepository.kt` | CRUD transaksi + query filter |
| 12 | `WalletRepository.kt` | CRUD wallet + balance query |
| 13 | `CategoryRepository.kt` | Query kategori (default + custom) |
| 14 | `BudgetRepository.kt` | CRUD budget + progress tracking |
| 15 | Update `SyncRepository.kt` | Tambah method untuk sync wallet & budget |

### D. Use Case

| # | File | Deskripsi |
|---|------|-----------|
| 16 | `AddTransactionUseCase.kt` | Validasi + simpan transaksi baru |
| 17 | `GetTransactionsUseCase.kt` | Ambil daftar transaksi dengan filter |
| 18 | `GetWalletSummaryUseCase.kt` | Ambil ringkasan wallet (balance + trend) |
| 19 | `GetCategoriesUseCase.kt` | Ambil daftar kategori by type |
| 20 | `GetMonthlySummaryUseCase.kt` | Ambil ringkasan bulanan (income/expense/net) |
| 21 | `GetBudgetProgressUseCase.kt` | Hitung progress budget vs actual spending |

---

## 3. Scope — Apa yang TIDAK Dikerjakan

| Item | Alasan | Phase |
|------|--------|-------|
| Room entity (`@Entity`, `@Dao`) | Implementasi data layer | Phase 2 |
| Repository implementation (`*Impl`) | Butuh Room/Firestore | Phase 2 |
| Firestore data source baru | Data layer | Phase 2 |
| Hilt DI module untuk repository baru | Data layer | Phase 2 |
| WorkManager sync worker | Sync infrastructure | Phase 2+ |
| UI perubahan di feature modules | Feature-level | Phase 4–7 |
| Unit test | Testing phase | Phase 9 (tapi boleh paralel) |
| Family group entity/repository | Family feature scope | Phase 6 |

---

## 4. File Referensi (Read-Only)

File-file berikut harus dibaca sebagai referensi saat mengerjakan Phase 1. **Jangan mengubah file ini**, tapi gunakan sebagai sumber kebenaran untuk desain data dan konvensi kode.

### Desain & Arsitektur

| File | Gunakan untuk |
|------|---------------|
| `plans/KeuTrack_Data_Design.md` | **Referensi utama** — Firestore collection schema, Room DB schema, field types, offline-first strategy, batch write flow |
| `plans/KeuTrack_Development_Plan.md` | Roadmap keseluruhan, dependency rule, naming convention |
| `plans/Project_Assessment.md` | Status terkini project, apa yang sudah dan belum ada |

### Kode Existing (Pola & Konvensi)

| File | Pelajari |
|------|----------|
| `core/domain/src/main/kotlin/.../model/User.kt` | Konvensi domain model — data class, default values, nullable fields |
| `core/domain/src/main/kotlin/.../model/AuthResult.kt` | Pola sealed class untuk result type |
| `core/domain/src/main/kotlin/.../model/TokenResult.kt` | Pola sealed class alternatif |
| `core/domain/src/main/kotlin/.../repository/UserRepository.kt` | Konvensi repository interface — suspend function, Flow return type |
| `core/domain/src/main/kotlin/.../repository/SyncRepository.kt` | Interface yang akan di-extend |
| `core/domain/src/main/kotlin/.../usecase/SignInWithGoogleUseCase.kt` | Konvensi use case — `@Inject constructor`, `operator fun invoke()` |
| `core/domain/build.gradle.kts` | Build config domain module (pure Kotlin + Hilt) |

### Mock Data (Untuk Memahami Kebutuhan UI)

| File | Pelajari |
|------|----------|
| `features/dashboard/src/.../model/DashboardMockUi.kt` | Field apa saja yang dibutuhkan dashboard — balance, income/expense, transaction list |
| `features/dashboard/src/.../model/BottomSheetUI.kt` | Entry transaction kind (Expense/Income), category UI model, numpad |
| `features/dashboard/src/.../model/DashboardUIState.kt` | Pola UI state yang dipakai |
| `features/dashboard/src/.../DashboardViewModel.kt` | Cara ViewModel consume repository — `Flow<User?>`, `StateFlow` |

### Data Layer Existing (Untuk Konsistensi Pola)

| File | Pelajari |
|------|----------|
| `core/data/src/.../repository/UserRepositoryImpl.kt` | Pola implementasi repository — `@Inject constructor`, error handling, CancellationException propagation |
| `core/data/src/.../datasource/FirestoreNetworkDataSource.kt` | Firestore field constants, mapping pattern |
| `core/data/src/.../mapper/AuthUserMapper.kt` | Pola mapper class |
| `core/data/src/.../di/CommonRepositoryModule.kt` | Pola Hilt `@Binds` module |
| `core/data/build.gradle.kts` | Dependencies data module |

### Gradle & Infra

| File | Pelajari |
|------|----------|
| `gradle/libs.versions.toml` | Library versions yang tersedia (belum ada Room!) |
| `settings.gradle.kts` | Module registration |

---

## 5. File yang TIDAK BOLEH Diubah

File-file berikut sudah stabil dan **TIDAK BOLEH diubah** selama Phase 1. Perubahan pada file ini bisa merusak fitur auth yang sudah berjalan.

### Core Domain (Existing — Stable)

| File | Alasan |
|------|--------|
| `core/domain/src/.../model/User.kt` | Digunakan oleh auth flow, DataStore, Firestore — sudah production |
| `core/domain/src/.../model/AuthResult.kt` | Sealed class result untuk auth — digunakan semua auth ViewModel |
| `core/domain/src/.../model/TokenResult.kt` | Google Sign-In token result — digunakan oleh credential flow |
| `core/domain/src/.../repository/UserRepository.kt` | Interface auth — diimplementasikan `UserRepositoryImpl` yang sudah production |
| `core/domain/src/.../usecase/SignInWithGoogleUseCase.kt` | Use case auth — existing |

### Core Data (Existing — Stable)

| File | Alasan |
|------|--------|
| `core/data/src/.../repository/UserRepositoryImpl.kt` | Implementasi auth repository — complex logic, tested manually |
| `core/data/src/.../datasource/AuthNetworkDataSource.kt` | Auth data source interface |
| `core/data/src/.../datasource/AuthNetworkDataSourceImpl.kt` | Firebase Auth implementation |
| `core/data/src/.../datasource/FirestoreNetworkDataSource.kt` | Firestore user profile operations |
| `core/data/src/.../datasource/UserProfileLocalDataSource.kt` | DataStore interface |
| `core/data/src/.../datasource/UserProfileLocalDataSourceImpl.kt` | DataStore implementation |
| `core/data/src/.../mapper/AuthUserMapper.kt` | Auth user mapper |
| `core/data/src/.../mapper/SignedInUserProtoMapper.kt` | Proto DataStore mapper |
| `core/data/src/.../model/AuthUserResponse.kt` | Auth response model |
| `core/data/src/.../di/FirebaseModule.kt` | Firebase DI (Auth + Firestore instances) |
| `core/data/src/.../di/CommonRepositoryModule.kt` | UserRepository binding — akan di-extend (bukan diubah) di Phase 2 |
| `core/data/src/.../di/CommonDataSourceModule.kt` | DataSource bindings |
| `core/data/src/.../di/CommonMapperModule.kt` | Mapper provisions |
| `core/data/src/.../di/SystemServiceModule.kt` | System service DI |
| `core/data/build.gradle.kts` | Dependency config — Phase 2 yang akan menambah Room |

### Feature Modules (Existing — Stable)

| File | Alasan |
|------|--------|
| Semua file di `features/auth/` | Auth flow complete, jangan disentuh |
| Semua file di `features/splashscreen/` | Splash routing complete |
| `app/src/main/kotlin/.../navigation/*` | Navigation working — Phase 1 tidak menyentuh navigasi |

### Infra & Config

| File | Alasan |
|------|--------|
| `build.gradle.kts` (root) | Root build config |
| `settings.gradle.kts` | Module registration — tidak perlu perubahan |
| `gradle/libs.versions.toml` | Version catalog — Room akan ditambah di Phase 2, bukan Phase 1 |
| `build-plugin/` (semua file) | Convention plugins sudah stable |
| `gradle.properties` | Gradle config |
| `local.properties` | Local config (secrets) |

---

## 6. Struktur File yang Akan Dibuat

```
core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/
├── model/
│   ├── User.kt                    ← EXISTING (jangan ubah)
│   ├── AuthResult.kt              ← EXISTING (jangan ubah)
│   ├── TokenResult.kt             ← EXISTING (jangan ubah)
│   ├── Transaction.kt             ← BARU
│   ├── Wallet.kt                  ← BARU
│   ├── Category.kt                ← BARU
│   ├── Budget.kt                  ← BARU
│   ├── CategorySummary.kt         ← BARU
│   ├── TransactionType.kt         ← BARU
│   ├── WalletType.kt              ← BARU
│   ├── BudgetPeriod.kt            ← BARU
│   ├── CategoryType.kt            ← BARU
│   └── SyncStatus.kt              ← BARU
│
├── repository/
│   ├── UserRepository.kt          ← EXISTING (jangan ubah)
│   ├── SyncRepository.kt          ← EXISTING (update: tambah method)
│   ├── TransactionRepository.kt   ← BARU
│   ├── WalletRepository.kt        ← BARU
│   ├── CategoryRepository.kt      ← BARU
│   └── BudgetRepository.kt        ← BARU
│
└── usecase/
    ├── SignInWithGoogleUseCase.kt  ← EXISTING (jangan ubah)
    ├── AddTransactionUseCase.kt    ← BARU
    ├── GetTransactionsUseCase.kt   ← BARU
    ├── GetWalletSummaryUseCase.kt  ← BARU
    ├── GetCategoriesUseCase.kt     ← BARU
    ├── GetMonthlySummaryUseCase.kt ← BARU
    └── GetBudgetProgressUseCase.kt ← BARU
```

---

## 7. Task Breakdown Detail

### Task 1: Domain Enums

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/model/TransactionType.kt`

```kotlin
package com.mascill.keutrack.core.domain.model

enum class TransactionType(val value: String) {
    INCOME("income"),
    EXPENSE("expense");

    companion object {
        fun fromValue(value: String): TransactionType =
            entries.firstOrNull { it.value == value } ?: EXPENSE
    }
}
```

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/model/WalletType.kt`

```kotlin
package com.mascill.keutrack.core.domain.model

enum class WalletType(val value: String) {
    PERSONAL("personal"),
    FAMILY("family");

    companion object {
        fun fromValue(value: String): WalletType =
            entries.firstOrNull { it.value == value } ?: PERSONAL
    }
}
```

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/model/BudgetPeriod.kt`

```kotlin
package com.mascill.keutrack.core.domain.model

enum class BudgetPeriod(val value: String) {
    MONTHLY("monthly"),
    WEEKLY("weekly");

    companion object {
        fun fromValue(value: String): BudgetPeriod =
            entries.firstOrNull { it.value == value } ?: MONTHLY
    }
}
```

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/model/CategoryType.kt`

```kotlin
package com.mascill.keutrack.core.domain.model

enum class CategoryType(val value: String) {
    INCOME("income"),
    EXPENSE("expense"),
    BOTH("both");

    companion object {
        fun fromValue(value: String): CategoryType =
            entries.firstOrNull { it.value == value } ?: BOTH
    }
}
```

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/model/SyncStatus.kt`

```kotlin
package com.mascill.keutrack.core.domain.model

enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED
}
```

**Catatan desain:**
- Setiap enum yang disimpan ke Firestore memiliki `value: String` yang sesuai dengan format Firestore (lowercase)
- `fromValue()` companion function untuk deserialisasi dari Firestore/Room string
- `SyncStatus` tidak perlu `value` karena hanya digunakan secara lokal di Room

---

### Task 2: Domain Entity — Transaction

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/model/Transaction.kt`

```kotlin
package com.mascill.keutrack.core.domain.model

import java.time.Instant

data class Transaction(
    val id: String,
    val walletId: String,
    val userId: String,
    val familyId: String? = null,
    val type: TransactionType,
    val amount: Long,
    val categoryId: String,
    val note: String? = null,
    val date: Instant,
    val addedByName: String,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Instant = Instant.now(),
)
```

**Keputusan desain:**
- `amount: Long` — bukan `Double`, karena menyimpan dalam satuan terkecil (IDR tanpa desimal) menghindari floating-point error. Ini mengikuti best practice untuk financial data
- `id: String` — UUID yang dihasilkan di device, digunakan sebagai Firestore document ID (`localId` di Data Design)
- `date: Instant` — tanggal transaksi yang dipilih user, berbeda dari `createdAt`
- `familyId` nullable — hanya diisi jika wallet adalah family wallet
- `syncStatus` default `PENDING` — setiap transaksi baru belum tersinkron

**Referensi:** `KeuTrack_Data_Design.md` Section 3.3 (`/transactions/{txnId}`)

---

### Task 3: Domain Entity — Wallet

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/model/Wallet.kt`

```kotlin
package com.mascill.keutrack.core.domain.model

import java.time.Instant

data class Wallet(
    val id: String,
    val ownerId: String,
    val familyId: String? = null,
    val name: String,
    val type: WalletType,
    val balance: Long,
    val currency: String = "IDR",
    val icon: String? = null,
    val color: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val createdAt: Instant = Instant.now(),
)
```

**Keputusan desain:**
- `balance: Long` — denormalized, diupdate via atomic increment saat transaksi disimpan (bukan dihitung ulang dari aggregasi)
- `syncStatus` default `SYNCED` — wallet biasanya dibuat saat first-time setup dan langsung disync
- `color: String?` — hex color string, dikonversi ke `Color` di UI layer

**Referensi:** `KeuTrack_Data_Design.md` Section 3.2 (`/wallets/{walletId}`)

---

### Task 4: Domain Entity — Category

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/model/Category.kt`

```kotlin
package com.mascill.keutrack.core.domain.model

data class Category(
    val id: String,
    val userId: String? = null,
    val familyId: String? = null,
    val name: String,
    val icon: String,
    val color: String,
    val type: CategoryType,
    val isDefault: Boolean = false,
)
```

**Keputusan desain:**
- `userId: String? = null` — null berarti kategori bawaan/global yang tersedia untuk semua user
- `icon: String` — nama ikon Material Design, resolved di UI layer
- `isDefault: Boolean` — membedakan kategori bawaan vs buatan user

**Kategori default** yang harus disediakan (di-seed di data layer):
1. Makanan (`cat_makanan`) — Expense
2. Transport (`cat_transport`) — Expense
3. Tagihan (`cat_tagihan`) — Expense
4. Pendidikan (`cat_pendidikan`) — Expense
5. Hiburan (`cat_hiburan`) — Expense
6. Kesehatan (`cat_kesehatan`) — Expense
7. Belanja (`cat_belanja`) — Expense
8. Gaji (`cat_gaji`) — Income
9. Investasi (`cat_investasi`) — Income
10. Lainnya (`cat_lainnya`) — Both

**Referensi:** `KeuTrack_Data_Design.md` Section 3.5 (`/categories/{categoryId}`)

---

### Task 5: Domain Entity — Budget

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/model/Budget.kt`

```kotlin
package com.mascill.keutrack.core.domain.model

import java.time.Instant

data class Budget(
    val id: String,
    val userId: String,
    val familyId: String? = null,
    val categoryId: String,
    val limit: Long,
    val spent: Long = 0,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val month: String,
    val walletId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val createdAt: Instant = Instant.now(),
) {
    val remaining: Long get() = limit - spent
    val progressPercent: Float get() = if (limit > 0) (spent.toFloat() / limit) else 0f
    val isOverBudget: Boolean get() = spent > limit
}
```

**Keputusan desain:**
- `spent: Long` — denormalized, diupdate via atomic increment (sama seperti wallet balance)
- `month: String` — format `"2026-03"`, digunakan sebagai query key
- Computed properties (`remaining`, `progressPercent`, `isOverBudget`) ada di domain layer karena ini business logic

**Referensi:** `KeuTrack_Data_Design.md` Section 3.4 (`/budgets/{budgetId}`)

---

### Task 6: Domain Entity — CategorySummary

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/model/CategorySummary.kt`

```kotlin
package com.mascill.keutrack.core.domain.model

data class CategorySummary(
    val period: String,
    val userId: String,
    val familyId: String? = null,
    val totalIncome: Long,
    val totalExpense: Long,
    val byCategory: Map<String, CategoryBreakdown>,
    val topExpenseCategoryId: String? = null,
) {
    val netBalance: Long get() = totalIncome - totalExpense
}

data class CategoryBreakdown(
    val name: String,
    val totalExpense: Long,
    val totalIncome: Long,
    val transactionCount: Int,
) {
    fun percentOfTotal(totalExpense: Long): Float =
        if (totalExpense > 0) (this.totalExpense.toFloat() / totalExpense * 100) else 0f
}
```

**Keputusan desain:**
- Precomputed summary — menghindari aggregasi mahal di Firestore (1 doc read vs 1000 reads)
- `percentOfTotal` dihitung sebagai function (bukan stored field) agar selalu akurat setelah recalculation
- `byCategory: Map<String, CategoryBreakdown>` — key adalah `categoryId`

**Referensi:** `KeuTrack_Data_Design.md` Section 4.2 (`/users/{userId}/category_summaries/{yyyy-MM}`)

---

### Task 7: Repository Interface — TransactionRepository

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/repository/TransactionRepository.kt`

```kotlin
package com.mascill.keutrack.core.domain.repository

import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface TransactionRepository {

    fun observeTransactions(
        walletId: String? = null,
        type: TransactionType? = null,
        categoryId: String? = null,
        startDate: Instant? = null,
        endDate: Instant? = null,
        limit: Int = 50,
    ): Flow<List<Transaction>>

    fun observeRecentTransactions(limit: Int = 5): Flow<List<Transaction>>

    suspend fun getTransactionById(id: String): Transaction?

    suspend fun addTransaction(transaction: Transaction)

    suspend fun updateTransaction(transaction: Transaction)

    suspend fun deleteTransaction(id: String)
}
```

**Keputusan desain:**
- `observe*` return `Flow` — UI selalu mendapat update terbaru saat data berubah (reactive)
- `add/update/delete` adalah `suspend` — write operation, one-shot
- Filter parameters opsional di `observeTransactions` — memungkinkan query fleksibel dari satu method
- Default `limit = 50` untuk pagination, `observeRecentTransactions` default 5 untuk dashboard

---

### Task 8: Repository Interface — WalletRepository

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/repository/WalletRepository.kt`

```kotlin
package com.mascill.keutrack.core.domain.repository

import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import kotlinx.coroutines.flow.Flow

interface WalletRepository {

    fun observeWallets(): Flow<List<Wallet>>

    fun observeWalletsByType(type: WalletType): Flow<List<Wallet>>

    fun observeWalletById(walletId: String): Flow<Wallet?>

    suspend fun getPersonalWallet(): Wallet?

    suspend fun createWallet(wallet: Wallet)

    suspend fun updateWallet(wallet: Wallet)

    suspend fun deleteWallet(walletId: String)
}
```

**Keputusan desain:**
- `getPersonalWallet()` — shortcut yang sering dipakai, mengambil wallet pertama bertipe `PERSONAL`
- `observeWalletById` return `Flow<Wallet?>` — untuk Settings screen dan detail view

---

### Task 9: Repository Interface — CategoryRepository

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/repository/CategoryRepository.kt`

```kotlin
package com.mascill.keutrack.core.domain.repository

import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun observeCategories(): Flow<List<Category>>

    fun observeCategoriesByType(type: CategoryType): Flow<List<Category>>

    suspend fun getCategoryById(id: String): Category?

    suspend fun seedDefaultCategories()
}
```

**Keputusan desain:**
- `seedDefaultCategories()` — dipanggil sekali saat user pertama kali setup, menyisipkan 10 kategori default ke Room
- Tidak ada `create/update/delete` untuk sekarang — custom category adalah fitur future

---

### Task 10: Repository Interface — BudgetRepository

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/repository/BudgetRepository.kt`

```kotlin
package com.mascill.keutrack.core.domain.repository

import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.CategorySummary
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {

    fun observeBudgets(month: String): Flow<List<Budget>>

    suspend fun getBudgetById(id: String): Budget?

    suspend fun createBudget(budget: Budget)

    suspend fun updateBudget(budget: Budget)

    suspend fun deleteBudget(budgetId: String)

    fun observeMonthlySummary(month: String): Flow<CategorySummary?>

    fun observeMonthlySummaries(months: List<String>): Flow<List<CategorySummary>>
}
```

**Keputusan desain:**
- `observeBudgets(month)` — filter by month karena budget selalu ditampilkan per periode
- `observeMonthlySummary/Summaries` — ada di `BudgetRepository` karena tightly coupled dengan budget tracking
- `observeMonthlySummaries` untuk trend chart (misal 3 bulan terakhir)

---

### Task 11: Update SyncRepository

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/repository/SyncRepository.kt`

Perubahan dari:
```kotlin
interface SyncRepository {
    suspend fun syncPendingTransactions()
}
```

Menjadi:
```kotlin
package com.mascill.keutrack.core.domain.repository

interface SyncRepository {
    suspend fun syncPendingTransactions()
    suspend fun syncPendingWallets()
    suspend fun syncPendingBudgets()
    suspend fun syncAll()
}
```

**Catatan:** Ini satu-satunya file existing yang diubah. Perubahannya hanya **additive** (menambah method baru), tidak mengubah atau menghapus method yang sudah ada. Tidak akan break implementasi yang belum ada.

---

### Task 12: Use Case — AddTransactionUseCase

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/usecase/AddTransactionUseCase.kt`

```kotlin
package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(transaction: Transaction): Result<Unit> {
        if (transaction.amount <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than 0"))
        }
        if (transaction.walletId.isBlank()) {
            return Result.failure(IllegalArgumentException("Wallet must be selected"))
        }
        if (transaction.categoryId.isBlank()) {
            return Result.failure(IllegalArgumentException("Category must be selected"))
        }
        return try {
            transactionRepository.addTransaction(transaction)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Keputusan desain:**
- Return `Result<Unit>` — Kotlin stdlib Result, konsisten dan ringan
- Validasi business rules di use case (bukan di ViewModel atau repository):
  - `amount > 0`
  - `walletId` tidak kosong
  - `categoryId` tidak kosong
- Repository hanya bertanggung jawab persistence, use case handle validation

---

### Task 13: Use Case — GetTransactionsUseCase

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/usecase/GetTransactionsUseCase.kt`

```kotlin
package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    data class Params(
        val walletId: String? = null,
        val type: TransactionType? = null,
        val categoryId: String? = null,
        val startDate: Instant? = null,
        val endDate: Instant? = null,
        val limit: Int = 50,
    )

    operator fun invoke(params: Params = Params()): Flow<List<Transaction>> =
        transactionRepository.observeTransactions(
            walletId = params.walletId,
            type = params.type,
            categoryId = params.categoryId,
            startDate = params.startDate,
            endDate = params.endDate,
            limit = params.limit,
        )
}
```

**Keputusan desain:**
- `Params` data class — mengelompokkan filter parameters, lebih readable daripada banyak parameter
- Return `Flow` langsung (bukan `suspend`) — observer pattern untuk reactive UI
- Default `Params()` tanpa filter = semua transaksi terbaru

---

### Task 14: Use Case — GetWalletSummaryUseCase

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/usecase/GetWalletSummaryUseCase.kt`

```kotlin
package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class WalletSummary(
    val personalWallet: Wallet?,
    val familyWallets: List<Wallet>,
    val totalPersonalBalance: Long,
    val totalFamilyBalance: Long,
)

class GetWalletSummaryUseCase @Inject constructor(
    private val walletRepository: WalletRepository,
) {
    operator fun invoke(): Flow<WalletSummary> =
        walletRepository.observeWallets().map { wallets ->
            val personal = wallets.filter { it.type == WalletType.PERSONAL }
            val family = wallets.filter { it.type == WalletType.FAMILY }
            WalletSummary(
                personalWallet = personal.firstOrNull(),
                familyWallets = family,
                totalPersonalBalance = personal.sumOf { it.balance },
                totalFamilyBalance = family.sumOf { it.balance },
            )
        }
}
```

**Keputusan desain:**
- `WalletSummary` — aggregated view yang dibutuhkan dashboard (personal + family balance)
- Business logic (grouping, summing) ada di use case, bukan di ViewModel

---

### Task 15: Use Case — GetCategoriesUseCase

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/usecase/GetCategoriesUseCase.kt`

```kotlin
package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.CategoryType
import com.mascill.keutrack.core.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    operator fun invoke(type: CategoryType? = null): Flow<List<Category>> =
        if (type != null) {
            categoryRepository.observeCategoriesByType(type)
        } else {
            categoryRepository.observeCategories()
        }
}
```

---

### Task 16: Use Case — GetMonthlySummaryUseCase

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/usecase/GetMonthlySummaryUseCase.kt`

```kotlin
package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.CategorySummary
import com.mascill.keutrack.core.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class MonthlySummaryResult(
    val currentMonth: CategorySummary?,
    val trend: List<CategorySummary>,
)

class GetMonthlySummaryUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
) {
    operator fun invoke(
        currentMonth: String,
        trendMonths: List<String> = emptyList(),
    ): Flow<MonthlySummaryResult> =
        budgetRepository.observeMonthlySummaries(
            listOf(currentMonth) + trendMonths
        ).map { summaries ->
            MonthlySummaryResult(
                currentMonth = summaries.firstOrNull { it.period == currentMonth },
                trend = summaries.sortedBy { it.period },
            )
        }
}
```

**Keputusan desain:**
- Menggabungkan current month summary + trend dalam satu query
- Dipakai oleh Dashboard (income/expense summary) dan Family Insights (trend chart)

---

### Task 17: Use Case — GetBudgetProgressUseCase

**File:** `core/domain/src/main/kotlin/com/mascill/keutrack/core/domain/usecase/GetBudgetProgressUseCase.kt`

```kotlin
package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBudgetProgressUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
) {
    operator fun invoke(month: String): Flow<List<Budget>> =
        budgetRepository.observeBudgets(month)
}
```

---

## 8. Acceptance Criteria

### Harus Terpenuhi Sebelum Phase 1 Dianggap Selesai

- [ ] **Build success** — `:core:domain` harus compile tanpa error (`./gradlew :core:domain:compileDebugKotlin`)
- [ ] **Tidak ada breaking change** — semua fitur existing (auth, splash, navigation) tetap berjalan normal
- [ ] **Semua file baru** sesuai struktur di Section 6
- [ ] **Enum `fromValue()`** mengembalikan default yang masuk akal untuk value yang tidak dikenali
- [ ] **Domain model** konsisten dengan field di `KeuTrack_Data_Design.md`
- [ ] **`amount` dan `balance`** menggunakan `Long` (bukan `Double` atau `BigDecimal`)
- [ ] **Repository interface** menggunakan `Flow` untuk observe dan `suspend` untuk one-shot operations
- [ ] **Use case** menggunakan pola `@Inject constructor` + `operator fun invoke()`
- [ ] **`AddTransactionUseCase`** memvalidasi: amount > 0, walletId tidak kosong, categoryId tidak kosong
- [ ] **`SyncRepository`** hanya additive — method `syncPendingTransactions()` existing tidak berubah signature
- [ ] **Tidak ada import Android framework** di domain model/enum — harus pure Kotlin (kecuali `javax.inject`)
- [ ] **`build.gradle.kts` domain** — hanya tambah dependency jika benar-benar perlu (kemungkinan tidak perlu perubahan)

### Verification Steps

```bash
# 1. Compile check
./gradlew :core:domain:compileDebugKotlin

# 2. Full build (pastikan tidak ada yang break)
./gradlew assembleDevDebug

# 3. Count new files (harus ~17 file baru)
find core/domain/src/main/kotlin -name "*.kt" | wc -l
# Expected: 23 (6 existing + 17 new)
```

---

## 9. Catatan Arsitektur & Konvensi

### Clean Architecture Rules (dari Development Plan)

1. **`:core:domain` tidak boleh depend ke modul lain** — pure Kotlin, no Android framework imports (kecuali `javax.inject` dari Hilt)
2. **Repository interface di domain** — implementasi di `:core:data`
3. **Use case menerima repository via constructor injection** — pattern `@Inject constructor`

### Naming Convention (dari Development Plan)

| Tipe | Format | Contoh |
|------|--------|--------|
| Entity | `Nama.kt` (data class) | `Transaction.kt` |
| Enum | `NamaTipe.kt` | `TransactionType.kt` |
| Repository | `NamaRepository.kt` (interface) | `TransactionRepository.kt` |
| Use Case | `KataKerjaSesuatuUseCase.kt` | `AddTransactionUseCase.kt` |

### Financial Data Convention

| Keputusan | Alasan |
|-----------|--------|
| `amount: Long` (bukan `Double`) | Menghindari floating-point precision error pada perhitungan uang. IDR tidak memiliki desimal, jadi Long cukup |
| `balance: Long` denormalized | Diupdate via `FieldValue.increment()` — tidak dihitung dari aggregasi transaksi |
| UUID sebagai `id` | Idempotency untuk offline-first sync — `localId` = Firestore document ID |
| `Instant` untuk timestamp | Java 8 Time API, lebih presisi dan timezone-safe daripada `Long` epoch |
| `syncStatus` di domain model | Domain perlu aware status sync untuk menampilkan indicator di UI |

### Pola yang Diikuti dari Kode Existing

| Pola | Contoh di Codebase | Diikuti di |
|------|---------------------|-----------|
| Data class dengan default values | `User(currency = "IDR")` | Semua entity baru |
| Nullable `String?` untuk optional fields | `User(familyId = null)` | `Transaction.note`, `Wallet.icon`, dll |
| `Flow<T>` untuk reactive reads | `UserRepository.getCurrentUser(): Flow<User?>` | Semua `observe*` methods |
| `suspend` untuk one-shot operations | `UserRepository.signOut()` | Semua write operations |
| `sealed class` untuk result types | `AuthResult` | `Result<Unit>` di use case (Kotlin stdlib) |
| `@Inject constructor` use case | `SignInWithGoogleUseCase` | Semua use case baru |
| `operator fun invoke()` | `SignInWithGoogleUseCase.invoke(idToken)` | Semua use case baru |

---

## 10. Dependency Graph

```
Phase 1 Output (core:domain)
│
├── model/
│   ├── Transaction ─────────── uses ──▶ TransactionType, SyncStatus
│   ├── Wallet ──────────────── uses ──▶ WalletType, SyncStatus
│   ├── Category ────────────── uses ──▶ CategoryType
│   ├── Budget ──────────────── uses ──▶ BudgetPeriod, SyncStatus
│   └── CategorySummary ─────── uses ──▶ CategoryBreakdown
│
├── repository/
│   ├── TransactionRepository ── uses ──▶ Transaction, TransactionType
│   ├── WalletRepository ────── uses ──▶ Wallet, WalletType
│   ├── CategoryRepository ──── uses ──▶ Category, CategoryType
│   ├── BudgetRepository ────── uses ──▶ Budget, CategorySummary
│   └── SyncRepository ──────── (standalone, no model deps)
│
└── usecase/
    ├── AddTransactionUseCase ──── depends ──▶ TransactionRepository
    ├── GetTransactionsUseCase ─── depends ──▶ TransactionRepository
    ├── GetWalletSummaryUseCase ── depends ──▶ WalletRepository
    ├── GetCategoriesUseCase ───── depends ──▶ CategoryRepository
    ├── GetMonthlySummaryUseCase ─ depends ──▶ BudgetRepository
    └── GetBudgetProgressUseCase ─ depends ──▶ BudgetRepository

                    ▼ Phase 2 akan implement ▼

core:data/
├── Room entities + DAOs (mirror domain models)
├── TransactionRepositoryImpl ── implements ──▶ TransactionRepository
├── WalletRepositoryImpl ─────── implements ──▶ WalletRepository
├── CategoryRepositoryImpl ───── implements ──▶ CategoryRepository
├── BudgetRepositoryImpl ─────── implements ──▶ BudgetRepository
└── SyncRepositoryImpl ────────── implements ──▶ SyncRepository
```

---

## 11. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| `java.time.Instant` membutuhkan API 26+ | Crash di device lama | KeuTrack `minSdk` kemungkinan sudah 26+. Verifikasi di `build-plugin/convention`. Jika tidak, gunakan `kotlinx-datetime` sebagai alternative |
| Domain model tidak match dengan Room entity nanti | Refactor di Phase 2 | Desain domain model sedekat mungkin dengan `KeuTrack_Data_Design.md` schema — mapper di data layer yang menjembatani |
| `Long` untuk amount mungkin tidak cukup untuk currency lain | Data overflow | `Long.MAX_VALUE` = 9.2 quintillion — cukup untuk IDR. Jika multi-currency nanti, evaluasi ulang |
| Use case terlalu banyak untuk MVP | Over-engineering | `GetBudgetProgressUseCase` dan `GetMonthlySummaryUseCase` boleh ditunda jika deadline ketat — prioritaskan `AddTransaction`, `GetTransactions`, `GetWalletSummary` |
| `SyncRepository` interface change break existing | Compile error | Tidak ada implementasi existing — perubahan aman. Sudah diverifikasi di assessment |

---

## Urutan Pengerjaan yang Disarankan

```
Step 1: Enum (5 file)
  └── TransactionType → WalletType → BudgetPeriod → CategoryType → SyncStatus

Step 2: Entity (5 file)
  └── Category → Wallet → Transaction → Budget → CategorySummary

Step 3: Repository Interface (5 file)
  └── CategoryRepository → WalletRepository → TransactionRepository
      → BudgetRepository → update SyncRepository

Step 4: Use Case (6 file)
  └── GetCategoriesUseCase → GetWalletSummaryUseCase → AddTransactionUseCase
      → GetTransactionsUseCase → GetMonthlySummaryUseCase → GetBudgetProgressUseCase

Step 5: Verify
  └── ./gradlew :core:domain:compileDebugKotlin
  └── ./gradlew assembleDevDebug
```

---

*Dokumen ini adalah referensi implementasi untuk Phase 1 KeuTrack. Setelah Phase 1 selesai, lanjut ke Phase 2 (Room + Repository Implementation).*
