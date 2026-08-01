# Phase 4 — features:dashboard (Real Data + Closed-Loop New Entry Sheet)

> **Modul target:** `:features:dashboard` (+ wiring tipis di `:app` `HomeShell` bila perlu navigasi Settings tab)
> **Estimasi:** ~2–2.5 hari
> **Prasyarat:** Phase 1 (domain) ✅ · Phase 2 (Room + repo + Hilt binds) ✅ DoR · Phase 3 (CurrencyFormat / money UI) **sangat disarankan**
> **Status baseline:** ~50% — UI Compose siap, data hampir seluruhnya mock; hanya nama + avatar user yang real
> **Hasil akhir:** Dashboard menampilkan wallet / income-expense / recent transactions dari Room via UseCase; SMPOB boilerplate dihapus; new-entry bottom sheet bisa **menyimpan** transaksi (loop tutup: add → observe → UI update) — tanpa memindahkan entry ke `:features:transaction` (itu Phase 5)

---

## Daftar Isi

1. [Konteks & Tujuan](#1-konteks--tujuan)
2. [Inventory — Apa yang Sudah Ada](#2-inventory--apa-yang-sudah-ada)
3. [Scope — Apa yang Dikerjakan](#3-scope--apa-yang-dikerjakan)
4. [Scope — Apa yang TIDAK Dikerjakan](#4-scope--apa-yang-tidak-dikerjakan)
5. [Prasyarat (Definition of Ready)](#5-prasyarat-definition-of-ready)
6. [File Referensi (Read-Only)](#6-file-referensi-read-only)
7. [File yang TIDAK BOLEH Diubah](#7-file-yang-tidak-boleh-diubah)
8. [File yang BOLEH Diubah / Dihapus](#8-file-yang-boleh-diubah--dihapus)
9. [Struktur File Target](#9-struktur-file-target)
10. [Pemetaan UI → Use Case](#10-pemetaan-ui--use-case)
11. [Task Breakdown Detail](#11-task-breakdown-detail)
12. [Acceptance Criteria](#12-acceptance-criteria)
13. [Catatan Arsitektur & Konvensi](#13-catatan-arsitektur--konvensi)
14. [Dependency Graph](#14-dependency-graph)
15. [Risiko & Mitigasi](#15-risiko--mitigasi)
16. [Urutan Pengerjaan yang Disarankan](#16-urutan-pengerjaan-yang-disarankan)

---

## 1. Konteks & Tujuan

Menurut `docs/dev/Project_Assessment.md`, Dashboard sudah punya UI kaya tetapi **belum jadi produk**:

| Area | Status sekarang |
|------|-----------------|
| Layout: top bar, wallet cards, income/expense, recent tx, FAB, bottom sheet | ✅ |
| `UserRepository.getCurrentUser()` → nama + avatar | ✅ |
| Financial data | ❌ `DefaultDashboardMockContent` |
| `RouteRepository` / SMPOB API | ❌ Boilerplate template, log-only |
| `onSettingsClick` / `onViewAllTransactions` | ❌ `{}` |
| FAB | ✅ Buka **local sheet** (bukan `TransactionRoute`) |
| Tombol “Add transaction” di sheet | ❌ Hanya `onDismiss` — **tidak persist** |
| `:features:transaction` | Placeholder `"New Transaction"` — **Phase 5** |

Phase 1–2 menyediakan kontrak + persistence. Phase 3 menyediakan formatter/komponen money UI. Phase 4 **menghubungkan** semuanya ke dashboard.

**Tujuan Phase 4:**
- Hapus SMPOB (`Route*`) dari modul dashboard
- Redesign `DashboardUIState` + `DashboardViewModel` berbasis UseCase Phase 1
- Bind Screen ke data real (loading / empty / content / error)
- Format uang dengan `CurrencyFormat` / `KeuTrackCurrencyText` (Phase 3)
- Wire **minimal save** dari new-entry sheet via `AddTransactionUseCase` agar loop offline-first terverifikasi di app
- Wire tipis `onSettingsClick` → tab Settings (via callback dari `:app`, tanpa feature↔feature dependency)

**Bukan tujuan Phase 4:**
- Memindahkan new-entry ke `TransactionRoute` / `NewEntryScreen` penuh
- Family insights / Settings persistence / invite
- Mengubah kontrak domain atau implementasi Room

---

## 2. Inventory — Apa yang Sudah Ada

### Presentation (pertahankan layout, ganti sumber data)

| File | Peran |
|------|-------|
| `presentation/DashboardScreen.kt` | `DashboardRouting` + `DashboardScreen`; sheet state lokal |
| `presentation/DashboardViewModel.kt` | User Flow + `fetchRoute()` SMPOB |
| `presentation/navigation/DashboardNavigation.kt` | `DashboardRoute`, `dashboardGraph()` |
| `presentation/model/DashboardMockUi.kt` | `DashboardMockContent`, `TransactionRowUi`, `DefaultDashboardMockContent` |
| `presentation/model/DashboardUIState.kt` | Hampir mati (`SignOutState`) — diganti/ diperluas |
| `presentation/model/BottomSheetUI.kt` | `EntryTransactionKind`, `NewEntryCategoryUI` |
| `presentation/components/DashboardTopBar.kt` | Greeting + avatar + settings icon |
| `presentation/components/WalletCards.kt` | Personal / family wallet cards |
| `presentation/components/DashboardStatCards.kt` | Income / expense row |
| `presentation/components/RecentTransactionsSection.kt` | List + View All |
| `presentation/components/TransactionRowCard.kt` | Satu baris transaksi |
| `presentation/components/NewEntryBottomSheetContent.kt` | Form entry (belum save) |

### SMPOB boilerplate (hapus di Phase 4)

| File | Alasan hapus |
|------|--------------|
| `di/DashboardModule.kt` | Provide Retrofit SMPOB |
| `domain/repository/RouteRepository.kt` | Kontrak asing |
| `domain/models/RouteDomain.kt` | Model asing |
| `data/service/RouteServices.kt` | `api/v1/smpob-mobile/routes` |
| `data/repositories/RouteRepositoryImpl.kt` | Impl asing |
| `data/models/RouteResponse.kt` | DTO asing |
| `data/mapper/RoutesMapper.kt` | Mapper asing |

### Gradle

`features/dashboard/build.gradle.kts` — via `keutrack.feature` sudah punya `:core:domain`, `:core:designsystem`, `:core:common`, `:core:network`. **Tidak** perlu depend `:core:data` (Hilt inject UseCase; impl di data module app classpath).

### Nav terkait (di luar dashboard, paham saja)

| File | Catatan |
|------|---------|
| `app/.../HomeShell.kt` | Nested tabs Dashboard / Family / Settings |
| `features/transaction/.../TransactionNavigation.kt` | `navigateToTransaction()` — **0 call site**; Phase 5 |
| `features/transaction/.../NewEntryScreen.kt` | Placeholder; Phase 5 |

---

## 3. Scope — Apa yang Dikerjakan

### A. Cleanup

| # | Item |
|---|------|
| 1 | Hapus seluruh stack SMPOB `Route*` + `DashboardModule` |
| 2 | Hapus `fetchRoute()` dan dependency `RouteRepository` dari ViewModel |

### B. State & ViewModel

| # | Item |
|---|------|
| 3 | Redesign `DashboardUIState` (loading, error, user greeting, wallets, monthly totals, recent rows, sheet categories, save-in-flight) |
| 4 | Inject UseCase: `GetWalletSummaryUseCase`, `GetTransactionsUseCase`, `GetMonthlySummaryUseCase`, `GetCategoriesUseCase`, `AddTransactionUseCase` + tetap `UserRepository` + `CommonDispatcher` |
| 5 | Combine Flows → satu `StateFlow<DashboardUIState>` (atau beberapa StateFlow yang di-combine di Routing — pilih satu pola, dokumentasikan) |
| 6 | Mapper domain → UI (`Transaction` + `Category` → `TransactionRowUi`, `Long` → label IDR) |

### C. Screen binding

| # | Item |
|---|------|
| 7 | `DashboardRouting` collect `uiState` real; `DefaultDashboardMockContent` **hanya** untuk `@Preview` |
| 8 | Empty state: belum ada wallet / belum ada transaksi |
| 9 | Loading awal (skeleton ringan atau spinner — jangan blokir seluruh app berlebihan) |
| 10 | Error ringan (snackbar / inline text) jika Flow gagal unexpected |

### D. New-entry sheet (closed loop — **masuk Phase 4**)

| # | Item |
|---|------|
| 11 | Categories dari `GetCategoriesUseCase` (filter by expense/income) |
| 12 | Default wallet = personal wallet dari `GetWalletSummaryUseCase` / `WalletSummary.personalWallet` |
| 13 | Tombol save → bangun `Transaction` → `AddTransactionUseCase` → sukses dismiss + biarkan Flow memperbarui list/balance |
| 14 | Validasi UI mirror use case (amount > 0, category dipilih, wallet ada) |

### E. Callbacks tipis

| # | Item |
|---|------|
| 15 | `onSettingsClick` → navigate ke `SettingsRoute` via callback dari `HomeShell` → `dashboardGraph(onSettingsClick)` |
| 16 | `onViewAllTransactions` → **stub sadar**: no-op dengan TODO Phase 5, **atau** navigate ke `TransactionRoute` hanya jika placeholder acceptable — **default rekomendasi: biarkan no-op / log**, jangan fake list screen |

### F. Design system consumption

| # | Item |
|---|------|
| 17 | Pakai `CurrencyFormat` / `KeuTrackCurrencyText` (Phase 3) untuk semua amount di dashboard |
| 18 | Pakai komponen sheet Phase 3 (`KeuTrackAmountKeypad`, dll.) jika sudah diekstrak; jika Phase 3 belum selesai, boleh tetap pakai local composable **asal** formatter `Long` konsisten |

---

## 4. Scope — Apa yang TIDAK Dikerjakan

| Item | Alasan | Phase |
|------|--------|-------|
| Pindah FAB → `navigateToTransaction()` + full `NewEntryScreen` | Transaction module productization | Phase 5 |
| Wallet picker / date picker / note field lengkap di sheet | Entry UX kaya | Phase 5 |
| Daftar transaksi penuh (“View All” screen) | Transaction / history | Phase 5 |
| Family insights real data | Family feature | Phase 6 |
| Settings: persist currency, family ID, wallets list | Settings | Phase 7 |
| Ubah kontrak `core/domain` UseCase/Repository | Freeze Phase 1 | — |
| Implementasi Room / Sync di `core/data` | Phase 2 | — |
| Auth / splash changes | Stable | — |
| Redesign visual layout dashboard besar | Design sudah ada | — |
| Google Sheets / invite / QR | Future | — |
| Unit test lengkap | Phase 9 (smoke compile wajib) |

---

## 5. Prasyarat (Definition of Ready)

Jangan mulai Phase 4 sebelum ini hijau (dari Phase 2 DoR):

1. Hilt bisa inject semua UseCase Phase 1 tanpa missing binding (`assembleDevDebug` sukses setelah Phase 2)
2. `AddTransactionUseCase` menulis Room dan muncul di `observe` wallet/transactions (terverifikasi debug/test)
3. Sync worker tidak crash app (boleh retry saat offline)
4. Default categories + personal wallet ter-seed (Phase 2 Task Category/Wallet)
5. **Direkomendasikan:** Phase 3 `CurrencyFormat` sudah ada — jika belum, implement formatter lokal sementara **hanya** di dashboard mapper, lalu ganti saat Phase 3 merge

```bash
# Sanity sebelum coding Phase 4
./gradlew :core:domain:compileDebugKotlin
./gradlew :core:data:compileDebugKotlin
./gradlew assembleDevDebug
```

---

## 6. File Referensi (Read-Only)

### Dokumen fase & assessment

| File | Gunakan untuk |
|------|---------------|
| `docs/dev/Project_Assessment.md` | Gap dashboard, prioritas missing functionality |
| `docs/dev/phases/PHASE_1_DOMAIN_ENTITIES_AND_USE_CASES.md` | Signature UseCase, `WalletSummary`, `Transaction` fields, validasi `AddTransactionUseCase` |
| `docs/dev/phases/PHASE_2_ROOM_AND_REPOSITORY_IMPLEMENTATION.md` | Offline-first, DoR Phase 4, seed wallet/categories |
| `docs/dev/phases/PHASE_3_DESIGN_SYSTEM.md` | `CurrencyFormat`, keypad/chip/FAB API |
| `.cursor/rules/keutrack-feature-module.mdc` | Routing/Screen/ViewModel/UIState patterns |
| `.cursor/rules/keutrack-architecture.mdc` | Feature ↛ feature; `KeuTrackTheme`; `Long` money |
| `.cursor/skills/keutrack-dev/SKILL.md` | Protected files, workflow feature screen |

### Domain contracts (consume only — jangan edit)

| File | Pelajari |
|------|----------|
| `core/domain/.../usecase/GetWalletSummaryUseCase.kt` | `WalletSummary` fields |
| `core/domain/.../usecase/GetTransactionsUseCase.kt` | `Params(limit = …)` |
| `core/domain/.../usecase/GetMonthlySummaryUseCase.kt` | `currentMonth: "yyyy-MM"` |
| `core/domain/.../usecase/GetCategoriesUseCase.kt` | Filter `CategoryType` |
| `core/domain/.../usecase/AddTransactionUseCase.kt` | Validasi + `Result` |
| `core/domain/.../model/Transaction.kt` | Field wajib saat create |
| `core/domain/.../model/Category.kt` | Join nama/icon untuk row UI |
| `core/domain/.../model/User.kt` | `uid`, `displayName`, `photoUrl` |
| `core/domain/.../repository/UserRepository.kt` | `getCurrentUser(): Flow<User?>` |

### Pola existing di dashboard / app

| File | Pelajari |
|------|----------|
| `features/dashboard/.../DashboardViewModel.kt` | Baseline Hilt + `CommonDispatcher` (pola dipertahankan, isi diganti) |
| `features/dashboard/.../DashboardScreen.kt` | `DashboardRouting` collect pattern |
| `features/dashboard/.../model/DashboardMockUi.kt` | Field UI yang harus dipetakan dari domain |
| `features/dashboard/.../components/NewEntryBottomSheetContent.kt` | State lokal sheet + tombol save saat ini = dismiss |
| `features/settings/.../SettingsViewModel.kt` | Contoh ViewModel feature yang sudah “real” (sign out / sync) |
| `app/.../HomeShell.kt` | Cara inject callback navigasi tab tanpa feature↔feature dep |
| `app/.../HomeNavDestination.kt` | `SettingsRoute` key |

### Design system (setelah Phase 3)

| File | Pakai untuk |
|------|-------------|
| `core/designsystem/.../format/CurrencyFormat.kt` | Format IDR |
| `core/designsystem/.../component/KeuTrackCurrencyText.kt` | Tampil amount |
| `core/designsystem/.../component/KeuTrackAmountKeypad.kt` dkk. | Sheet building blocks |
| `core/designsystem/.../component/KeuTrackEmptyState.kt` | Jika ada; jika tidak, empty UI lokal sederhana |

---

## 7. File yang TIDAK BOLEH Diubah

### Domain & data

| File / Area | Alasan |
|-------------|--------|
| Semua `core/domain/**` | Freeze kontrak Phase 1 — **consume only** |
| Semua `core/data/**` | Phase 2; jangan “fix” repo dari feature |
| `core/datastore/**`, auth Firestore user path | Production auth |

### Auth / splash / sibling features

| File / Area | Alasan |
|-------------|--------|
| `features/auth/**` | Complete |
| `features/splashscreen/**` | Complete |
| `features/family/**` | Phase 6 |
| `features/settings/**` (logic/UI data) | Phase 7 — jangan refactor Settings di Phase 4 |
| `features/transaction/**` | Phase 5 — jangan bangun NewEntryScreen di sini |

### Infra

| File / Area | Alasan |
|-------------|--------|
| `build-plugin/**` | Stable |
| `settings.gradle.kts`, root Gradle, `libs.versions.toml` | Tidak perlu dep baru untuk Phase 4 |
| `local.properties` | Secrets |

### Design system theme tokens

| File | Alasan |
|------|--------|
| `core/designsystem/.../theme/Colors.kt` hex / token rename | Phase 3 policy — jangan ubah dari dashboard work |

---

## 8. File yang BOLEH Diubah / Dihapus

### Hapus (SMPOB)

```
features/dashboard/src/.../di/DashboardModule.kt
features/dashboard/src/.../domain/repository/RouteRepository.kt
features/dashboard/src/.../domain/models/RouteDomain.kt
features/dashboard/src/.../data/service/RouteServices.kt
features/dashboard/src/.../data/repositories/RouteRepositoryImpl.kt
features/dashboard/src/.../data/models/RouteResponse.kt
features/dashboard/src/.../data/mapper/RoutesMapper.kt
```

Setelah hapus, folder `data/` / `domain/` / `di/` dashboard boleh kosong / dihapus jika tidak ada file lain.

### Ubah / perluas

| File | Perubahan |
|------|-----------|
| `DashboardViewModel.kt` | UseCase-driven state; hapus Route |
| `DashboardUIState.kt` | State produksi |
| `DashboardMockUi.kt` | Rename/keep models UI (`TransactionRowUi`); mock hanya Preview |
| `BottomSheetUI.kt` | Sesuaikan bila perlu untuk categories real |
| `DashboardScreen.kt` | Bind `uiState`; wiring save/categories sheet |
| `NewEntryBottomSheetContent.kt` | Props: categories, onSave, isSaving, errors |
| `WalletCards.kt` / `DashboardStatCards.kt` / `RecentTransactionsSection.kt` / `TransactionRowCard.kt` | Terima data real / empty; format Phase 3 |
| `DashboardNavigation.kt` | `dashboardGraph(onSettingsClick: () -> Unit = {})` |
| `app/.../HomeShell.kt` | Pass `onSettingsClick = { navController.navigate(SettingsRoute) { … } }` ke `dashboardGraph` |

### Buat baru (disarankan)

| File | Peran |
|------|-------|
| `presentation/model/DashboardUiMapper.kt` (atau `mapper/`) | Domain → UI mapping murni |
| `presentation/model/DashboardAction.kt` (opsional) | Sealed actions dari Screen → VM |

**Jangan** menambah `implementation(projects.core.data)` di dashboard Gradle kecuali ada alasan kuat (default: **tidak**).

---

## 9. Struktur File Target

```
features/dashboard/src/main/kotlin/com/mascill/keutrack/feature/dashboard/
├── presentation/
│   ├── DashboardScreen.kt              ← UPDATE (Routing + Screen)
│   ├── DashboardViewModel.kt           ← REWRITE deps + state
│   ├── navigation/
│   │   └── DashboardNavigation.kt      ← UPDATE (onSettingsClick)
│   ├── model/
│   │   ├── DashboardUIState.kt         ← REWRITE
│   │   ├── DashboardMockUi.kt          ← Preview-only mock + TransactionRowUi
│   │   ├── BottomSheetUI.kt            ← UPDATE ringan
│   │   ├── DashboardUiMapper.kt        ← BARU (opsional tapi disarankan)
│   │   └── DashboardAction.kt          ← BARU (opsional)
│   └── components/
│       ├── DashboardTopBar.kt
│       ├── WalletCards.kt
│       ├── DashboardStatCards.kt
│       ├── RecentTransactionsSection.kt
│       ├── TransactionRowCard.kt
│       └── NewEntryBottomSheetContent.kt  ← UPDATE save/categories
│
├── di/          ← HAPUS (DashboardModule)
├── domain/      ← HAPUS (Route*)
└── data/        ← HAPUS (Route*)
```

---

## 10. Pemetaan UI → Use Case

| Section UI | Sumber data | Mapping |
|------------|-------------|---------|
| Greeting first name | `UserRepository.getCurrentUser()` | `displayName` first token / email local-part (logic existing `greetingFirstNameOrFallback`) |
| Avatar | sama | `photoUrl` → `ProfileImage` |
| Personal wallet card | `GetWalletSummaryUseCase` | `totalPersonalBalance` / `personalWallet?.balance` → `CurrencyFormat` |
| Family wallet card | sama | `totalFamilyBalance`; subtitle “Shared with N” → **stub** (`familyWallets.size` atau copy tetap) sampai Phase 6 |
| Month change footer | `GetMonthlySummaryUseCase` (+ optional prior month) | Hitung % jika ada 2 periode; jika summary null → sembunyikan atau “—” |
| Income / expense stats | `GetMonthlySummaryUseCase(currentMonth)` | `totalIncome` / `totalExpense` |
| Recent transactions | `GetTransactionsUseCase(Params(limit = 5))` | Join category name/icon via categories map dari `GetCategoriesUseCase` |
| Sheet categories | `GetCategoriesUseCase(type)` | Map ke `NewEntryCategoryUI` |
| Sheet save | `AddTransactionUseCase` | Build `Transaction` lengkap |
| Budgets | — | **Tidak ada** di UI dashboard sekarang → skip |

### Membangun `Transaction` saat save (minimal)

```kotlin
Transaction(
    id = UUID.randomUUID().toString(),
    walletId = personalWallet.id,          // wajib ada; jika null → tampilkan error "Buat dompet dulu"
    userId = currentUser.uid,
    familyId = personalWallet.familyId,    // biasanya null untuk personal
    type = if (kind == Expense) EXPENSE else INCOME,
    amount = amountRupiah,                 // Long
    categoryId = selectedCategoryId,
    note = null,                           // Phase 5
    date = Instant.now(),                  // Phase 5: user-picked date
    addedByName = currentUser.displayName.ifBlank { currentUser.email },
    syncStatus = SyncStatus.PENDING,       // default domain
)
```

---

## 11. Task Breakdown Detail

### Task 1: Hapus SMPOB

1. Hapus 7 file Route* / `DashboardModule` (Section 8)
2. Bersihkan import ViewModel
3. Compile — pastikan tidak ada referensi tersisa:

```bash
rg -n "RouteRepository|RouteServices|smpob|DashboardModule" features/dashboard
# Expected: no matches
```

---

### Task 2: Redesign `DashboardUIState`

Contoh shape (sesuaikan nama field dengan komponen existing):

```kotlin
data class DashboardUIState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val userFirstName: String = "",
    val avatarUrl: String? = null,
    val personalBalance: Long = 0L,
    val familyBalance: Long = 0L,
    val familySharedSummary: String = "",
    val monthChangeLabel: String? = null,
    val incomeTotal: Long = 0L,
    val expenseTotal: Long = 0L,
    val recentTransactions: List<TransactionRowUi> = emptyList(),
    val categories: List<NewEntryCategoryUI> = emptyList(),
    val selectedEntryKind: EntryTransactionKind = EntryTransactionKind.Expense,
    val personalWalletId: String? = null,
    val isSavingTransaction: Boolean = false,
    val saveError: String? = null,
)
```

- Label string statis (“Current Balance”, “INCOME”) boleh tetap di composable / resources
- **Jangan** simpan formatted `"IDR …"` sebagai satu-satunya sumber — simpan `Long`, format di UI/mapper

Pertahankan `TransactionRowUi` untuk row, tetapi isi dari mapper real. `DefaultDashboardMockContent` hanya Preview.

---

### Task 3: Rewrite `DashboardViewModel`

```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val getWalletSummary: GetWalletSummaryUseCase,
    private val getTransactions: GetTransactionsUseCase,
    private val getMonthlySummary: GetMonthlySummaryUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val dispatcher: CommonDispatcher,
) : ViewModel()
```

**Pola observe (disarankan):**
- `init` / `viewModelScope.launch`: `combine(userFlow, walletFlow, txFlow, summaryFlow, categoriesFlow) { … }` → `_uiState`
- `currentMonth` = `YearMonth.now().toString()` → `"yyyy-MM"`
- `GetTransactionsUseCase(Params(limit = 5))`
- Categories: observe by type sesuai `selectedEntryKind` (saat kind berubah, update filter)

**Actions:**
- `onEntryKindChanged(kind)`
- `onSaveTransaction(amount, categoryId, kind)` → build domain → `addTransaction` → update `isSaving` / `saveError`
- Selalu `catch (e: CancellationException) { throw e }` di coroutine write

Jangan panggil repository finansial langsung — **UseCase only** (kecuali `UserRepository` yang memang existing pattern auth).

---

### Task 4: `DashboardUiMapper`

Tanggung jawab:
- `User` → first name / avatar
- `WalletSummary` → balances + `personalWalletId`
- `CategorySummary?` → income/expense totals; month-change label opsional
- `List<Transaction>` + `Map<categoryId, Category>` → `List<TransactionRowUi>`
  - `title`: `note` jika ada, else category name
  - `timeLabel`: format jam dari `date` (locale `id-ID`)
  - `amountLabel` / raw amount: prefer pass `Long` ke `KeuTrackCurrencyText` jika row di-refactor; jika row masih `String`, format via `CurrencyFormat.formatIdr`
  - `categoryIcon`: map icon string / fallback enum existing
- `List<Category>` → `List<NewEntryCategoryUI>`

---

### Task 5: Bind `DashboardRouting` / Screen

1. Collect `uiState`
2. Hapus `remember(currentUser) { DefaultDashboardMockContent.copy(...) }` sebagai sumber produksi
3. Pass field ke `WalletSummaryCard`, `DashboardStatCardsRow`, `RecentTransactionsSection`
4. Empty:
   - Tidak ada transaksi → section kosong + copy singkat
   - Tidak ada personal wallet → disable save + pesan di sheet
5. Preview: tetap pakai `DefaultDashboardMockContent` / fake `DashboardUIState`

---

### Task 6: New-entry sheet save loop

Update `NewEntryBottomSheetContent` signature (ilustratif):

```kotlin
@Composable
fun NewEntryBottomSheetContent(
    categories: List<NewEntryCategoryUI>,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (amount: Long, categoryId: String, kind: EntryTransactionKind) -> Unit,
    // kind/category selection boleh internal atau controlled
)
```

- Tombol primary: `onSave(...)` **bukan** langsung `onDismiss`
- Routing/VM: on success → `showNewEntrySheet = false` + clear save error
- On failure → tampilkan `saveError` di sheet
- Setelah sukses, **jangan** manual refresh list jika Flow Room sudah emit — verifikasi UI update otomatis

---

### Task 7: `onSettingsClick` via `:app`

Karena feature **tidak boleh** depend feature lain:

1. Ubah `dashboardGraph(onSettingsClick: () -> Unit = {})`
2. `DashboardRouting(onSettingsClick = onSettingsClick)`
3. Di `HomeShell` / `HomeNavHost`:

```kotlin
dashboardGraph(
    onSettingsClick = {
        homeNavController.navigate(SettingsRoute) {
            popUpTo(homeNavController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    },
)
```

Ini **satu-satunya** perubahan nav app yang diizinkan di Phase 4 (additive wiring). Jangan ubah splash/auth graph.

---

### Task 8: `onViewAllTransactions`

**Keputusan default Phase 4:** biarkan no-op atau tampilkan Toast/log `"Phase 5"` — **jangan** bangun list screen di dashboard.

Dokumentasikan di kode:

```kotlin
// TODO(Phase 5): navigate to full transaction history / TransactionRoute
```

---

### Task 9: Verify di device/emulator

Manual test plan:

1. Login (auth existing) → Dashboard load tanpa crash
2. Personal wallet balance tampil (0 jika baru)
3. Categories muncul di sheet (seed Phase 2)
4. Input amount + pilih category + save → sheet tutup
5. Recent transaction muncul; balance personal berubah (+income / −expense)
6. Kill app → buka lagi → data tetap (Room)
7. Settings icon → pindah tab Settings
8. Offline: save tetap berhasil lokal; sync tidak crash (Phase 2 worker)

---

## 12. Acceptance Criteria

### Harus Terpenuhi

- [ ] **Tidak ada** referensi SMPOB / `RouteRepository` / `RouteServices` di dashboard
- [ ] Dashboard **tidak** memakai `DefaultDashboardMockContent` sebagai sumber data runtime
- [ ] Wallet personal & family balance dari `GetWalletSummaryUseCase`
- [ ] Income/expense bulan berjalan dari `GetMonthlySummaryUseCase` (0 jika summary belum ada)
- [ ] Recent transactions (max 5) dari `GetTransactionsUseCase`
- [ ] Amount diformat IDR via Phase 3 `CurrencyFormat` (atau setara sementara yang diganti)
- [ ] Sheet categories dari `GetCategoriesUseCase`
- [ ] Save sheet memanggil `AddTransactionUseCase` dan memperbarui UI via Flow
- [ ] Validasi: amount ≤ 0 / category kosong / wallet null → error, tidak write
- [ ] `onSettingsClick` membuka tab Settings
- [ ] `features/transaction/**` tidak diubah untuk product flow
- [ ] `core/domain/**` & `core/data/**` & `features/auth/**` tidak diubah
- [ ] Build sukses:

```bash
./gradlew :features:dashboard:compileDevDebugKotlin
./gradlew assembleDevDebug
```

### Verification Commands

```bash
# Compile
./gradlew :features:dashboard:compileDevDebugKotlin
./gradlew assembleDevDebug

# Pastikan protected areas bersih
git diff --stat -- core/domain core/data features/auth features/splashscreen features/transaction features/family features/settings
# Expected: kosong (kecuali app/HomeShell + dashboard)
```

### Definition of Ready untuk Phase 5

Phase 5 boleh mulai jika:
1. Closed-loop save dari dashboard sheet sudah terbukti di device
2. Tim punya referensi UX sheet yang bisa dipindah/dikonsolidasi ke `NewEntryScreen`
3. `navigateToTransaction()` masih orphan — Phase 5 yang akan wire FAB opsional / ganti sheet

---

## 13. Catatan Arsitektur & Konvensi

### Feature rules

| Aturan | Penerapan Phase 4 |
|--------|-------------------|
| Feature ↛ feature | Navigasi Settings lewat callback `:app` |
| Feature ↛ Room/DAO | Hanya UseCase / `UserRepository` |
| Screen stateless | State + callbacks dari Routing/VM |
| `collectAsStateWithLifecycle` | Wajib |
| `CommonDispatcher` | IO untuk write; combine boleh main/default VM scope |
| `KeuTrackTheme` | Preview & runtime |

### Money

| Aturan | Detail |
|--------|--------|
| Domain `Long` | Jangan convert ke `Double` |
| Format di UI/mapper | Bukan di UseCase |
| IDR grouping | `CurrencyFormat.formatIdr` |

### Offline-first UX

- Save sukses = sukses **lokal**; jangan block UI menunggu Firestore
- Jangan tampilkan error sync sebagai kegagalan save (sync = background Phase 2)

### Batas Phase 4 vs 5

```
Phase 4                          Phase 5
─────────────────────────────    ─────────────────────────────
FAB → local bottom sheet    →    FAB → TransactionRoute (opsional)
Sheet save → AddTransaction →    NewEntryScreen penuh + ViewModel sendiri
Recent list (5 items)       →    View All / history
Defaults: wallet/date/now   →    Picker wallet, date, note, edit/delete
```

---

## 14. Dependency Graph

```
Phase 1 UseCases (domain)
        ▲ inject
DashboardViewModel
        │ StateFlow<DashboardUIState>
        ▼
DashboardRouting → DashboardScreen → components
        │
        ├── GetWalletSummaryUseCase ──▶ WalletRepository (data/Room)
        ├── GetTransactionsUseCase ───▶ TransactionRepository
        ├── GetMonthlySummaryUseCase ─▶ BudgetRepository (summaries)
        ├── GetCategoriesUseCase ─────▶ CategoryRepository
        ├── AddTransactionUseCase ────▶ TransactionRepository (write lokal + sync enqueue)
        └── UserRepository ───────────▶ DataStore/Auth (existing)

:app HomeShell ──onSettingsClick──▶ SettingsRoute (tab)
```

---

## 15. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| Phase 2 belum seed wallet/categories | Save selalu gagal | Pastikan DoR; tampilkan empty CTA jelas |
| `CategorySummary` masih kosong sampai transaksi pertama | Stats 0 | OK untuk MVP; pastikan `AddTransaction` Phase 2 meng-update summary |
| Join category icon jelek | Row kurang cantik | Fallback icon `Lainnya`; mapping sempurna bisa Phase 5/9 |
| `combine` banyak Flow rumit | Bug state | Mulai 2 Flow (wallet + tx), tambah summary/categories bertahap |
| Scope creep View All / TransactionRoute | Molor | Strict defer Phase 5 |
| Mengubah domain “supaya gampang” | Pecah kontrak | Larang; mapper di feature |
| Race double-tap save | Duplikat transaksi | Disable button saat `isSavingTransaction` |
| Settings navigate tanpa `saveState` | Tab state hilang | Ikuti pola `HomeShell` bottom nav options |
| Phase 3 belum merge | Duplikat format | Formatter lokal temporary + TODO ganti |

---

## 16. Urutan Pengerjaan yang Disarankan

```
Step 1: Cleanup
  └── Hapus Route*/DashboardModule → compile hijau

Step 2: State + Mapper + ViewModel reads
  └── DashboardUIState → Mapper → inject Get* UseCases → combine Flows
  └── Bind Screen (masih tanpa save)

Step 3: Empty / loading / Preview
  └── Pastikan Preview tetap pakai mock
  └── Runtime empty states

Step 4: Sheet categories + save
  └── GetCategoriesUseCase → sheet
  └── AddTransactionUseCase → closed loop
  └── Manual test di emulator

Step 5: Settings callback
  └── dashboardGraph(onSettingsClick) + HomeShell wiring

Step 6: Verify
  └── ./gradlew :features:dashboard:compileDevDebugKotlin
  └── ./gradlew assembleDevDebug
  └── git diff protected areas = bersih
```

---

## Ringkasan File Policy

| Kategori | Policy |
|----------|--------|
| `features/dashboard/**` (presentation) | ✅ Ubah / rewrite VM & state |
| SMPOB `Route*` / `DashboardModule` | ✅ Hapus |
| `app/.../HomeShell.kt` (+ nav terkait tipis) | ✅ Additive: pass `onSettingsClick` |
| `core/domain/**`, `core/data/**` | ❌ Jangan ubah |
| `features/auth/**`, `splashscreen/**` | ❌ Jangan ubah |
| `features/transaction/**` | ❌ Jangan ubah (Phase 5) |
| `features/family/**`, `features/settings/**` | ❌ Jangan ubah |
| `build-plugin/**`, version catalog | ❌ Jangan ubah |
| Design system hex/tokens | ❌ Jangan ubah; **boleh pakai** API Phase 3 |

---

## Estimasi Effort

| Bucket | Porsi |
|--------|-------|
| Hapus SMPOB + UIState/ViewModel reads + screen bind | ~45% |
| Mapper + empty/loading + format IDR | ~20% |
| Sheet categories + save closed-loop | ~25% |
| Settings nav wiring + verify device | ~10% |

---

*Dokumen ini adalah referensi implementasi untuk Phase 4 KeuTrack. Setelah Phase 4 selesai, user bisa menguji pencatatan dari dashboard sheet di app; lanjut Phase 5 untuk memindahkan/menyempurnakan transaction module + View All.*
