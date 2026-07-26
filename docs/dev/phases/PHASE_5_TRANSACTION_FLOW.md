# Phase 5 — features:transaction (Full New-Entry + History)

> **Modul target:** `:features:transaction` (+ wiring navigasi di `:app` dan entry-point tipis di `:features:dashboard`)
> **Estimasi:** ~2.5–3.5 hari
> **Prasyarat:** Phase 1–3 ✅ · Phase 4 ✅ (dashboard real data + closed-loop sheet save sudah ada di codebase)
> **Status baseline:** ~15% — route terdaftar di top-level NavHost, `NewEntryScreen` masih placeholder `"New Transaction"`, `navigateToTransaction()` **0 call site**
> **Hasil akhir:** Form new-entry penuh di modul transaction (wallet/date/note + DS money UI), history “View All”, FAB/View All ter-wire lewat `:app`; dashboard tetap overview (recent-5), bukan history screen

---

## Daftar Isi

1. [Konteks & Tujuan](#1-konteks--tujuan)
2. [Inventory — Apa yang Sudah Ada](#2-inventory--apa-yang-sudah-ada)
3. [Keputusan UX: Sheet vs Full Screen](#3-keputusan-ux-sheet-vs-full-screen)
4. [Scope — Apa yang Dikerjakan](#4-scope--apa-yang-dikerjakan)
5. [Scope — Apa yang TIDAK Dikerjakan](#5-scope--apa-yang-tidak-dikerjakan)
6. [Prasyarat (Definition of Ready)](#6-prasyarat-definition-of-ready)
7. [File Referensi (Read-Only)](#7-file-referensi-read-only)
8. [File yang TIDAK BOLEH Diubah](#8-file-yang-tidak-boleh-diubah)
9. [File yang BOLEH Diubah / Dibuat](#9-file-yang-boleh-diubah--dibuat)
10. [Struktur File Target](#10-struktur-file-target)
11. [Navigasi (Wajib Dipahami)](#11-navigasi-wajib-dipahami)
12. [Pemetaan UI → Use Case](#12-pemetaan-ui--use-case)
13. [Task Breakdown Detail](#13-task-breakdown-detail)
14. [Acceptance Criteria](#14-acceptance-criteria)
15. [Catatan Arsitektur & Konvensi](#15-catatan-arsitektur--konvensi)
16. [Dependency Graph](#16-dependency-graph)
17. [Risiko & Mitigasi](#17-risiko--mitigasi)
18. [Urutan Pengerjaan yang Disarankan](#18-urutan-pengerjaan-yang-disarankan)

---

## 1. Konteks & Tujuan

Menurut `docs/dev/Project_Assessment.md` dan handoff Phase 4:

| Area | Status sekarang (post–Phase 4) |
|------|--------------------------------|
| Dashboard overview + recent-5 + sync badge | ✅ Real data |
| Dashboard new-entry **bottom sheet** save | ✅ `AddTransactionUseCase` closed-loop |
| `RetryPendingSyncUseCase` saat dashboard open | ✅ |
| `:features:transaction` `NewEntryScreen` | ❌ Placeholder text saja |
| `navigateToTransaction()` | ❌ Tidak dipanggil dari mana pun |
| View All transactions | ❌ TODO Phase 5 di dashboard |
| Wallet / date picker / note di entry | ❌ Stub (`"Personal"` / `"Today"` / `note = null`) |
| Edit / delete transaksi | ❌ Repo method ada; UseCase belum |

Phase 4 sengaja **tidak** memindahkan entry ke transaction module agar loop offline-first cepat terverifikasi. Phase 5 menyelesaikan **product surface** transaksi.

**Tujuan Phase 5:**
- Productize `NewEntryScreen` + `NewEntryViewModel` (atau `TransactionViewModel`) dengan form lengkap
- Tambah **Transaction History** (“View All”) di modul transaction
- Wire FAB + View All dari dashboard → root nav via callback `:app` (tanpa feature↔feature dependency)
- Pakai DS Phase 3 (`CurrencyFormat`, keypad, segment, category chip, top bar, text field)
- (Opsional MVP+) edit/delete dengan UseCase domain **additive**

**Bukan tujuan Phase 5:**
- Family insights / invite / Settings persistence
- Redesign dashboard overview
- Mengubah kontrak entity Phase 1 secara besar
- Depend `:core:data` dari feature

---

## 2. Inventory — Apa yang Sudah Ada

### Modul transaction (sangat tipis)

| File | Peran |
|------|-------|
| `features/transaction/.../navigation/TransactionNavigation.kt` | `TransactionRoute`, `navigateToTransaction()`, `transactionGraph(onBack)` |
| `features/transaction/.../presentation/NewEntryScreen.kt` | Placeholder `"New Transaction"`; `onBack` unused |
| `features/transaction/build.gradle.kts` | `keutrack.feature` — deps transitive: domain, designsystem, common, network |

**Belum ada:** ViewModel, Routing, model/, components/, history screen, di/.

### App navigation

| File | Peran |
|------|-------|
| `app/.../navigation/KeuTrackNavHost.kt` | Top-level: Splash → Auth → `HomeRoute` → `transactionGraph(onBack = popBackStack)` |
| `app/.../navigation/HomeShell.kt` | Nested tabs; sudah pass `onSettingsClick` ke dashboard |

`TransactionRoute` adalah **sibling** `HomeRoute` (bukan di dalam tab NavHost) — cocok untuk full-screen entry / history dengan back ke Home.

### Dashboard entry surfaces (sumber UX yang harus di-port / share)

| File | Yang relevan untuk Phase 5 |
|------|----------------------------|
| `.../components/NewEntryBottomSheetContent.kt` | Form: segment, amount, categories, keypad, save; wallet/date chip masih stub |
| `.../model/BottomSheetUI.kt` | `EntryTransactionKind`, `NewEntryCategoryUI` |
| `.../DashboardViewModel.kt` | `onSaveTransaction`, categories, wallet id, user identity |
| `.../components/TransactionRowCard.kt` | Pola row + sync badge — **adaptasi** di history (jangan depend dashboard module) |
| `.../DashboardScreen.kt` | `onFabClick` → sheet lokal; `onViewAllTransactions` → TODO Phase 5 |

### Domain UseCase yang siap dipakai

| UseCase | Phase 5 |
|---------|---------|
| `AddTransactionUseCase` | Primary save |
| `GetTransactionsUseCase` | History (+ filter/limit) |
| `GetCategoriesUseCase` | Form categories |
| `GetWalletSummaryUseCase` | Wallet picker / default |
| `RetryPendingSyncUseCase` | Opsional on screen open |
| `GetMonthlySummaryUseCase` / `GetBudgetProgressUseCase` | Tidak wajib di Phase 5 |
| `Update*` / `Delete*` / `GetTransactionById*` UseCase | **Belum ada** — perlu additive jika edit/delete in-scope |

`TransactionRepository` sudah punya `getTransactionById`, `updateTransaction`, `deleteTransaction` — tinggal UseCase tipis di domain jika dibutuhkan.

### Design system (Phase 3)

| API | Path |
|-----|------|
| `CurrencyFormat` | `core/designsystem/.../format/CurrencyFormat.kt` |
| `KeuTrackCurrencyText` | `.../component/KeuTrackCurrencyText.kt` |
| `KeuTrackAmountKeypad` | `.../component/KeuTrackAmountKeypad.kt` |
| `KeuTrackSegmentedControl` | `.../component/KeuTrackSegmentedControl.kt` |
| `KeuTrackCategoryChip` | `.../component/KeuTrackCategoryChip.kt` |
| `KeuTrackStatusChip` | `.../component/KeuTrackStatusChip.kt` |
| `KeuTrackTopBar` / `KeuTrackTextField` / `KeuTrackButton` / `KeuTrackFab` / `KeuTrackModalBottomSheet` | existing |

**Belum di DS:** wallet picker list, date picker — boleh feature-local dulu; ekstrak ke DS hanya jika sangat generik.

---

## 3. Keputusan UX: Sheet vs Full Screen

Pilih **satu** primary path agar tidak ada dua form yang diverge. Rekomendasi dokumen ini:

| Keputusan | Detail |
|-----------|--------|
| **Primary entry** | FAB Dashboard → `navigateToTransaction()` → `NewEntryScreen` full-screen |
| **Dashboard sheet** | **Hapus atau nonaktifkan** setelah full-screen stabil (hindari dual save path). Alternatif: pertahankan sebagai “quick add” hanya jika form dishare 1:1 via shared composable — default Phase 5 = **hapus sheet path** |
| **View All** | → `TransactionHistoryRoute` di modul transaction |
| **Back** | `onBack` / `popBackStack` ke Home |

Jika ingin keep sheet sebagai shortcut: **wajib** ekstrak form body ke shared composable di `features/transaction` (atau package shared UI) supaya dashboard hanya host sheet — jangan copy-paste logic save kedua kalinya.

---

## 4. Scope — Apa yang Dikerjakan

### A. New Entry (wajib)

| # | Item |
|---|------|
| 1 | Rewrite `NewEntryScreen` + `NewEntryRouting` (pattern feature module) |
| 2 | `NewEntryViewModel` — inject UseCase + `UserRepository` + `CommonDispatcher` |
| 3 | Form: expense/income, amount keypad, category chips, **wallet picker**, **date picker**, **note** |
| 4 | Save via `AddTransactionUseCase`; sukses → `onBack` |
| 5 | Loading/saving/error/empty-wallet states |
| 6 | `@Preview` light + dark |

### B. History / View All (wajib)

| # | Item |
|---|------|
| 7 | Route baru mis. `TransactionHistoryRoute` |
| 8 | `TransactionHistoryScreen` + `HistoryViewModel` |
| 9 | List dari `GetTransactionsUseCase` (limit lebih besar, mis. 50–100) |
| 10 | Row UI (adaptasi dari dashboard `TransactionRowCard` pola — sync badge optional) |
| 11 | Empty / loading states |
| 12 | Wire `onViewAllTransactions` dari dashboard |

### C. Navigation wiring (wajib)

| # | Item |
|---|------|
| 13 | Perluas `transactionGraph` untuk history (+ optional detail) |
| 14 | `:app` `KeuTrackNavHost` / `HomeShell`: callback `onAddTransaction` / `onViewAllTransactions` dari dashboard → root `NavController` |
| 15 | FAB dashboard memanggil callback (bukan local sheet — sesuai keputusan Section 3) |

### D. Share / cleanup form (wajib jika hapus sheet)

| # | Item |
|---|------|
| 16 | Pindahkan model UI entry (`EntryTransactionKind`, category UI) ke transaction (atau package yang netral) |
| 17 | Hapus / slim `NewEntryBottomSheetContent` + save actions terkait di `DashboardViewModel` yang tidak terpakai |
| 18 | Pastikan dashboard compile & overview tetap jalan |

### E. Domain additive (opsional — edit/delete)

| # | Item |
|---|------|
| 19 | `GetTransactionByIdUseCase`, `UpdateTransactionUseCase`, `DeleteTransactionUseCase` di `core/domain` (**additive only**) |
| 20 | Detail/edit screen atau swipe-to-delete di history |

Jika waktu terbatas: **tunda E ke Phase 5.1 / Phase 9** — history read-only + create sudah cukup untuk “transaction flow” MVP.

### F. Polish

| # | Item |
|---|------|
| 21 | Opsional: `RetryPendingSyncUseCase` saat history/new-entry open |
| 22 | Filter history sederhana (tipe / bulan) — nice-to-have |

---

## 5. Scope — Apa yang TIDAK Dikerjakan

| Item | Alasan | Phase |
|------|--------|-------|
| Family insights / invite / QR | Family product | Phase 6 |
| Settings currency / family ID / wallets management UI | Settings | Phase 7 |
| Deep link / notification ke transaksi | Nav polish | Phase 8+ |
| Unit/UI test penuh | Testing | Phase 9 |
| Google Sheets export | Future | — |
| Redesign token Atelier / hex Colors | DS freeze | — |
| Auth / splash changes | Stable | — |
| Refactor besar Room/Sync di `core/data` | Phase 2 ownership | — kecuali bug blocker |
| Menjadikan dashboard sebagai history penuh | Melanggar batas Phase 4/5 | — |

---

## 6. Prasyarat (Definition of Ready)

Dari Phase 4 DoR + kenyataan codebase:

1. Closed-loop save dari dashboard sheet sudah terbukti di device (Phase 4 commits)
2. Hilt inject `AddTransactionUseCase`, `GetTransactionsUseCase`, `GetCategoriesUseCase`, `GetWalletSummaryUseCase` tanpa missing binding
3. Personal wallet + default categories ter-seed
4. Phase 3 money UI tersedia di designsystem
5. `TransactionRoute` sudah terdaftar di `KeuTrackNavHost`

```bash
./gradlew :features:dashboard:compileDevDebugKotlin
./gradlew :features:transaction:compileDevDebugKotlin
./gradlew assembleDevDebug
```

---

## 7. File Referensi (Read-Only)

### Dokumen fase

| File | Gunakan untuk |
|------|---------------|
| `docs/dev/Project_Assessment.md` | Gap transaction module & nav orphan |
| `docs/dev/phases/PHASE_1_DOMAIN_ENTITIES_AND_USE_CASES.md` | `Transaction` fields, `AddTransactionUseCase` validation |
| `docs/dev/phases/PHASE_2_ROOM_AND_REPOSITORY_IMPLEMENTATION.md` | Offline-first write; syncStatus; jangan block UI on network |
| `docs/dev/phases/PHASE_3_DESIGN_SYSTEM.md` | Komponen money UI yang wajib dikonsumsi |
| `docs/dev/phases/PHASE_4_DASHBOARD_REAL_DATA.md` | Batas Phase 4 vs 5; pola save; Settings callback pattern |
| `docs/dev/DESIGN_SYSTEM_ATELIER.md` | Katalog komponen (jika ada) |
| `.cursor/rules/keutrack-feature-module.mdc` | Screen / Routing / ViewModel / NavGraph conventions |
| `.cursor/rules/keutrack-architecture.mdc` | Feature ↛ feature; `Long` money |
| `.cursor/rules/keutrack-domain-layer.mdc` | Jika menambah UseCase update/delete |
| `.cursor/skills/keutrack-dev/SKILL.md` | Protected files list |

### Domain (consume; edit hanya jika Task E)

| File | Pelajari |
|------|----------|
| `core/domain/.../model/Transaction.kt` | Field create/update |
| `core/domain/.../model/TransactionType.kt` / `SyncStatus.kt` | Enum mapping |
| `core/domain/.../usecase/AddTransactionUseCase.kt` | Validasi + `Result` |
| `core/domain/.../usecase/GetTransactionsUseCase.kt` | `Params` filters/limit |
| `core/domain/.../usecase/GetCategoriesUseCase.kt` | Filter by type |
| `core/domain/.../usecase/GetWalletSummaryUseCase.kt` | `WalletSummary` |
| `core/domain/.../usecase/RetryPendingSyncUseCase.kt` | Optional retry |
| `core/domain/.../repository/TransactionRepository.kt` | `update` / `delete` / `getById` untuk UseCase baru |

### Pola implementasi yang ditiru

| File | Pelajari |
|------|----------|
| `features/dashboard/.../DashboardViewModel.kt` | Cara build `Transaction`, combine Flows, save error handling |
| `features/dashboard/.../components/NewEntryBottomSheetContent.kt` | UX form yang di-port |
| `features/dashboard/.../model/BottomSheetUI.kt` | Model UI entry |
| `features/dashboard/.../components/TransactionRowCard.kt` | Row + sync badge visual |
| `features/dashboard/.../presentation/model/DashboardUiMapper.kt` (jika ada) | Mapping category/transaction → UI |
| `features/settings/.../SettingsViewModel.kt` | Pola feature VM “real” |
| `app/.../HomeShell.kt` | Pola callback navigasi tab (template untuk FAB/View All) |
| `app/.../KeuTrackNavHost.kt` | Di mana `transactionGraph` hidup |

### Design system

| File | Pakai |
|------|-------|
| `CurrencyFormat.kt`, `KeuTrackCurrencyText.kt`, `KeuTrackAmountKeypad.kt`, `KeuTrackSegmentedControl.kt`, `KeuTrackCategoryChip.kt`, `KeuTrackTopBar.kt`, `KeuTrackTextField.kt`, `KeuTrackButton.kt`, `KeuTrackStatusChip.kt` | Form + history |

---

## 8. File yang TIDAK BOLEH Diubah

### Auth / splash / protected user stack

| File / Area | Alasan |
|-------------|--------|
| `features/auth/**` | Complete |
| `features/splashscreen/**` | Complete |
| `core/domain/.../model/User.kt`, `AuthResult.kt`, `TokenResult.kt` | Auth contracts |
| `core/domain/.../repository/UserRepository.kt` | Auth interface |
| `core/data/.../repository/UserRepositoryImpl.kt` + Auth/Firestore user DS | Production auth |
| `core/datastore/**` | Session |

### Infra

| File / Area | Alasan |
|-------------|--------|
| `build-plugin/**` | Stable |
| `settings.gradle.kts`, root `build.gradle.kts` | Tidak perlu module baru |
| `gradle/libs.versions.toml` | Tidak perlu lib baru (default) |
| `local.properties`, `gradle.properties` | Config / secrets |

### Sibling features (jangan “sekalian”)

| File / Area | Alasan |
|-------------|--------|
| `features/family/**` | Phase 6 |
| `features/settings/**` | Phase 7 |
| `core/designsystem/.../theme/Colors.kt` hex / token rename | Consume only |

### Domain / data — batasan

| Policy | Detail |
|--------|--------|
| Entity/enum existing | ❌ Jangan ubah field `Transaction` / enums kecuali bug blocker |
| Repository interface existing | ❌ Jangan ubah signature method yang sudah ada |
| UseCase existing | ❌ Jangan ubah perilaku `AddTransactionUseCase` validation tanpa alasan |
| `core/data` Room/Sync | ❌ Jangan refactor; hanya bugfix jika history/save rusak |
| UseCase **baru** update/delete | ✅ Additive saja (opsional Task E) |

---

## 9. File yang BOLEH Diubah / Dibuat

### Primary — `features/transaction/**`

| Aksi | File |
|------|------|
| UPDATE | `NewEntryScreen.kt`, `TransactionNavigation.kt` |
| BARU | `NewEntryRouting.kt`, `NewEntryViewModel.kt`, `model/*`, `components/*` |
| BARU | `TransactionHistoryScreen.kt`, `TransactionHistoryRouting.kt`, `TransactionHistoryViewModel.kt` |
| BARU | Routes: `TransactionHistoryRoute` (+ opsional `TransactionDetailRoute`) |
| BARU | Mapper UI di modul transaction |

### App nav (additive wiring)

| File | Perubahan |
|------|-----------|
| `app/.../KeuTrackNavHost.kt` | Pass navigate helpers ke `HomeShell` / pastikan graph history terdaftar |
| `app/.../HomeShell.kt` | Terima `onAddTransaction`, `onViewAllTransactions` → panggil root nav |

Pola yang disarankan: `KeuTrackNavHost` punya `rootNavController`; `HomeShell(onAddTransaction = { rootNavController.navigateToTransaction() }, onViewAllTransactions = { rootNavController.navigate(TransactionHistoryRoute) })`.

### Dashboard entry-point (tipis)

| File | Perubahan |
|------|-----------|
| `DashboardNavigation.kt` | `dashboardGraph(onSettingsClick, onAddTransaction, onViewAllTransactions)` |
| `DashboardScreen.kt` / Routing | FAB → `onAddTransaction`; View All → `onViewAllTransactions` |
| `NewEntryBottomSheetContent.kt` | Hapus pemakaian atau ganti host shared form |
| `DashboardViewModel.kt` | Hapus state/actions sheet save yang tidak terpakai (jangan sentuh wallet/stats/recent) |

### Domain (hanya jika edit/delete in-scope)

| File | Perubahan |
|------|-----------|
| `core/domain/.../usecase/GetTransactionByIdUseCase.kt` | BARU |
| `core/domain/.../usecase/UpdateTransactionUseCase.kt` | BARU (+ validasi mirip Add) |
| `core/domain/.../usecase/DeleteTransactionUseCase.kt` | BARU |

---

## 10. Struktur File Target

```
features/transaction/src/main/kotlin/com/mascill/keutrack/feature/transaction/
├── navigation/
│   └── TransactionNavigation.kt          ← UPDATE: multi-route graph + navigate helpers
│
└── presentation/
    ├── NewEntryRouting.kt                 ← BARU
    ├── NewEntryScreen.kt                  ← REWRITE
    ├── NewEntryViewModel.kt               ← BARU
    ├── history/
    │   ├── TransactionHistoryRouting.kt   ← BARU
    │   ├── TransactionHistoryScreen.kt    ← BARU
    │   └── TransactionHistoryViewModel.kt ← BARU
    ├── model/
    │   ├── NewEntryUIState.kt             ← BARU
    │   ├── HistoryUIState.kt              ← BARU
    │   ├── EntryTransactionKind.kt        ← PINDAH / mirror dari dashboard BottomSheetUI
    │   ├── TransactionRowUi.kt            ← BARU (atau shared shape mirip dashboard)
    │   └── TransactionUiMapper.kt         ← BARU
    └── components/
        ├── NewEntryFormContent.kt         ← BARU (body form reusable)
        ├── WalletPickerBottomSheet.kt     ← BARU
        ├── CategorySeeAllSheet.kt         ← BARU (opsional)
        ├── DatePickerField.kt             ← BARU (Material date picker wrapper)
        └── TransactionHistoryRow.kt       ← BARU
```

---

## 11. Navigasi (Wajib Dipahami)

### Graph sekarang

```
KeuTrackNavHost (root)
├── SplashRoute
├── LoginRoute / RegisterRoute
├── HomeRoute → HomeShell
│     └── nested NavHost
│           ├── DashboardRoute
│           ├── FamilyRoute
│           └── SettingsRoute
└── TransactionRoute → NewEntryScreen (placeholder, unreachable)
```

### Graph target Phase 5

```
KeuTrackNavHost (root)
├── … auth/home …
├── TransactionRoute              → NewEntryRouting / NewEntryScreen
└── TransactionHistoryRoute       → TransactionHistoryRouting / Screen
    └── (opsional) TransactionDetailRoute / edit
```

### Wiring tanpa langgar arsitektur

```
Dashboard FAB
  → onAddTransaction() callback
  → HomeShell / KeuTrackNavHost
  → rootNavController.navigateToTransaction()

Dashboard "View All"
  → onViewAllTransactions()
  → rootNavController.navigate(TransactionHistoryRoute)
```

**Dilarang:** `features/dashboard` import / navigate langsung ke class internal transaction kecuali melalui callback yang di-inject dari `:app` (dashboard boleh depend **hanya** pada function type `() -> Unit`, bukan pada `TransactionRoute` jika ingin ketat — **atau** dashboard memanggil extension `navigateToTransaction` hanya jika NavController root di-pass; paling bersih: callback dari app).

Rekomendasi ketat Phase 5:
- Dashboard **tidak** import `TransactionRoute`
- App yang tahu kedua route dan pass lambdas

---

## 12. Pemetaan UI → Use Case

### New Entry

| UI | Sumber |
|----|--------|
| Expense / Income toggle | Local UI state → `TransactionType` |
| Amount keypad | Local `Long` → `Transaction.amount` |
| Categories | `GetCategoriesUseCase(type)` |
| Wallet list / selected | `GetWalletSummaryUseCase` → personal + family wallets |
| Date | Local `Instant` / `LocalDate` → `Transaction.date` |
| Note | Local `String?` → `Transaction.note` |
| Save | `AddTransactionUseCase` |
| User id / addedByName | `UserRepository.getCurrentUser()` |

### History

| UI | Sumber |
|----|--------|
| List | `GetTransactionsUseCase(Params(limit = 50))` |
| Category label/icon | `GetCategoriesUseCase()` join by id |
| Amount display | `CurrencyFormat` / `KeuTrackCurrencyText` |
| Sync badge | `Transaction.syncStatus` → `KeuTrackStatusChip` (opsional) |

### Build `Transaction` (lebih kaya dari Phase 4)

```kotlin
Transaction(
    id = UUID.randomUUID().toString(),
    walletId = selectedWalletId,       // dari picker, bukan selalu personal
    userId = currentUser.uid,
    familyId = selectedWallet.familyId,
    type = type,
    amount = amount,
    categoryId = selectedCategoryId,
    note = note?.takeIf { it.isNotBlank() },
    date = selectedDateInstant,        // dari date picker
    addedByName = currentUser.displayName.ifBlank { currentUser.email },
    syncStatus = SyncStatus.PENDING,
)
```

---

## 13. Task Breakdown Detail

### Task 0: Putuskan dual-entry

Checklist keputusan di PR description:
- [ ] Primary = full-screen only (rekomendasi), sheet dihapus
- [ ] Atau: shared `NewEntryFormContent` dipakai sheet + screen

Jangan mulai coding dua path paralel tanpa keputusan.

---

### Task 1: Scaffold navigation routes

Update `TransactionNavigation.kt`:

```kotlin
@Serializable object TransactionRoute
@Serializable object TransactionHistoryRoute

fun NavController.navigateToTransaction(...)
fun NavController.navigateToTransactionHistory(...)

fun NavGraphBuilder.transactionGraph(onBack: () -> Unit) {
    composable<TransactionRoute> { NewEntryRouting(onBack = onBack) }
    composable<TransactionHistoryRoute> { TransactionHistoryRouting(onBack = onBack) }
}
```

Pastikan `KeuTrackNavHost` tetap `popBackStack` untuk `onBack`.

---

### Task 2: `NewEntryUIState` + `NewEntryViewModel`

State minimal:

```kotlin
data class NewEntryUIState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val kind: EntryTransactionKind = EntryTransactionKind.Expense,
    val amount: Long = 0L,
    val categories: List<NewEntryCategoryUI> = emptyList(),
    val selectedCategoryId: String? = null,
    val wallets: List<WalletOptionUi> = emptyList(),
    val selectedWalletId: String? = null,
    val selectedDate: Instant = Instant.now(),
    val note: String = "",
    val userId: String? = null,
    val addedByName: String = "",
)
```

Actions: `onKindChanged`, `onAmountChanged` / digit handlers, `onCategorySelected`, `onWalletSelected`, `onDateSelected`, `onNoteChanged`, `onSave`, `clearError`.

Inject: `AddTransactionUseCase`, `GetCategoriesUseCase`, `GetWalletSummaryUseCase`, `UserRepository`, `CommonDispatcher`.

---

### Task 3: `NewEntryFormContent` + Screen

1. Port UX dari `NewEntryBottomSheetContent` ke full-screen scaffold:
   - `KeuTrackTopBar` title “Transaksi Baru” + back
   - Segment, currency display, chips, keypad, note field, wallet/date rows
2. Wallet row → buka `WalletPickerBottomSheet` / dialog list
3. Date row → `DatePicker` (Material) → convert ke `Instant` (start of day lokal atau noon — dokumentasikan)
4. Note → `KeuTrackTextField` (opsional, max length mis. 120)
5. Primary button save → VM
6. Preview light/dark dengan fake state

---

### Task 4: History screen

1. `HistoryUIState(isLoading, items, error)`
2. `GetTransactionsUseCase(Params(limit = 50))` + categories join
3. LazyColumn rows; tap row → (opsional detail) atau no-op MVP
4. Empty: “Belum ada transaksi” + CTA ke new entry (callback `onAddTransaction`)
5. TopBar “Riwayat” + back

---

### Task 5: Wire `:app` + dashboard callbacks

1. `KeuTrackNavHost`:
   ```kotlin
   HomeShell(
       onSignOutSuccess = …,
       onAddTransaction = { navController.navigateToTransaction() },
       onViewAllTransactions = { navController.navigateToTransactionHistory() },
   )
   ```
2. `HomeShell` → `dashboardGraph(onSettingsClick, onAddTransaction, onViewAllTransactions)`
3. Dashboard FAB → `onAddTransaction`
4. View All → `onViewAllTransactions`
5. Hapus local `showNewEntrySheet` flow (jika keputusan Section 3 = full-screen only)

---

### Task 6: Cleanup dashboard sheet path

1. Hapus pemanggilan `NewEntryBottomSheetContent` dari `DashboardScreen`
2. Hapus / kurangi field state sheet di `DashboardUIState` & methods save di VM (`onSaveTransaction`, `dismissNewEntrySheet`, dll.) **hanya** yang terkait sheet
3. **Pertahankan** recent-5, wallet cards, monthly stats, retry sync
4. Compile dashboard + full app

---

### Task 7 (Opsional): Edit / Delete

1. Tambah UseCase domain additive (jangan ubah signature repo)
2. `UpdateTransactionUseCase`: validasi amount/wallet/category; panggil `updateTransaction`
3. `DeleteTransactionUseCase`: panggil `deleteTransaction`
4. UI: long-press / detail screen — sesuaikan kapasitas Room update (balance/summary side-effects harus sudah benar di Phase 2; jika belum aman, **jangan** ship edit di Phase 5)

> **Gate:** sebelum enable edit/delete di UI, verifikasi `TransactionRepositoryImpl.update/delete` mengoreksi wallet balance & category summary. Jika belum, batasi Phase 5 pada create + history read-only.

---

### Task 8: Manual QA

1. Dashboard FAB → New Entry screen (bukan sheet)
2. Ganti wallet (jika >1), ganti tanggal, isi note, save → back → recent-5 update
3. View All → history menampilkan transaksi yang sama (lebih banyak)
4. Back dari history → dashboard
5. Offline save tetap sukses lokal
6. Empty history copy + CTA
7. Auth/splash/settings/family tidak regres

---

## 14. Acceptance Criteria

### Harus Terpenuhi

- [ ] `NewEntryScreen` bukan placeholder; form lengkap (kind, amount, category, wallet, date, note)
- [ ] Save memanggil `AddTransactionUseCase`; sukses pop back
- [ ] `navigateToTransaction()` dipanggil dari alur FAB (via `:app`)
- [ ] `TransactionHistoryRoute` + screen list real data
- [ ] Dashboard “View All” membuka history
- [ ] Feature **tidak** depend `:core:data` atau feature lain
- [ ] Money UI memakai komponen Phase 3 (`CurrencyFormat` / keypad / chips / segment)
- [ ] Dashboard overview (balances, stats, recent-5) tetap berfungsi
- [ ] Dual-entry sheet dihilangkan **atau** form dishare tanpa duplikasi logic (sesuai Task 0)
- [ ] `features/auth/**`, `splashscreen/**`, Settings/Family logic tidak diubah
- [ ] Entity/repo signature existing tidak diubah (UseCase baru boleh)
- [ ] Build:

```bash
./gradlew :features:transaction:compileDevDebugKotlin
./gradlew :features:dashboard:compileDevDebugKotlin
./gradlew assembleDevDebug
```

### Verification

```bash
# Call site harus ada
rg -n "navigateToTransaction|TransactionHistoryRoute|onAddTransaction|onViewAllTransactions" \
  app features/dashboard features/transaction

# Protected areas
git diff --stat -- features/auth features/splashscreen features/family features/settings \
  core/data build-plugin
# Expected: kosong (domain hanya jika Task E UseCase baru)
```

### Definition of Ready untuk Phase 6

1. User bisa mencatat transaksi dari full-screen entry dengan wallet/date/note
2. User bisa melihat riwayat lebih dari 5 item
3. Dashboard tetap ringkas sebagai home overview

---

## 15. Catatan Arsitektur & Konvensi

### Feature module pattern

```
*Routing (hiltViewModel + collectAsStateWithLifecycle)
  → *Screen (stateless UI + callbacks)
  → *ViewModel (UseCase only)
```

- Routes `@Serializable`
- `NavGraphBuilder.transactionGraph(...)`
- Preview wajib `KeuTrackTheme` light + dark

### Offline-first

- Save sukses = sukses lokal; jangan await Firestore
- Sync badge di history opsional; jangan tampilkan kegagalan sync sebagai gagal save

### Money

- `Long` only; format di UI
- Hormati `MAX_AMOUNT_RUPIAH` dari DS jika ada

### Anti-duplikasi

| Jangan | Lakukan |
|--------|---------|
| Copy `NewEntryBottomSheetContent` mentah ke transaction lalu biarkan dashboard beda logic | Satu `NewEntryFormContent` sebagai sumber kebenaran form |
| Import class dashboard dari transaction | Adaptasi row UI di modul transaction |
| Feature depend feature | Callback dari `:app` |

---

## 16. Dependency Graph

```
:app KeuTrackNavHost
  ├── HomeShell
  │     └── dashboardGraph(
  │           onAddTransaction ──────────────┐
  │           onViewAllTransactions ─────────┤
  │           onSettingsClick → Settings tab │
  │         )                                │
  └── transactionGraph                       │
        ├── TransactionRoute → NewEntry*  ◄──┘
        └── TransactionHistoryRoute → History* ◄── (View All)

NewEntryViewModel
  ├── AddTransactionUseCase
  ├── GetCategoriesUseCase
  ├── GetWalletSummaryUseCase
  └── UserRepository

HistoryViewModel
  ├── GetTransactionsUseCase
  ├── GetCategoriesUseCase
  └── (opsional) RetryPendingSyncUseCase
```

---

## 17. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| Dua form (sheet + screen) diverge | Bug inkonsisten | Keputusan Task 0; hapus sheet atau share composable |
| Nested nav vs root nav salah | FAB tidak jalan / crash | Callback dari `KeuTrackNavHost` root; uji back stack |
| Date timezone salah | Transaksi “pindah hari” | Dokumentasikan konversi `LocalDate` → `Instant`; test batas tengah malam |
| Edit/delete tanpa koreksi balance di data layer | Saldo korup | Gate Task 7; default read-only history |
| Port row UI menarik dependensi dashboard | Langgar arsitektur | Duplikasi tipis / mapper sendiri di transaction |
| Scope creep filter/search/export | Molor | History list + create dulu |
| Menghapus sheet terlalu cepat sebelum screen siap | Regresi UX | Urutan: screen dulu → wire FAB → baru hapus sheet |
| Menambah UseCase dengan mengubah Add existing | Regresi dashboard | Additive files only |

---

## 18. Urutan Pengerjaan yang Disarankan

```
Step 1: Keputusan UX (sheet vs full-screen) + scaffold routes/history stubs

Step 2: NewEntryViewModel + FormContent + Screen (save works via temporary deep link/debug nav)

Step 3: Wire :app + dashboard FAB → TransactionRoute
        Verifikasi save end-to-end dari FAB

Step 4: History screen + View All wiring

Step 5: Cleanup dashboard sheet + dead VM state

Step 6: (Opsional) Edit/Delete UseCase + UI — hanya jika data layer aman

Step 7: Preview + QA device + assembleDevDebug
```

---

## Ringkasan File Policy

| Kategori | Policy |
|----------|--------|
| `features/transaction/**` | ✅ Primary work |
| `app/.../KeuTrackNavHost.kt`, `HomeShell.kt` | ✅ Additive nav callbacks |
| `features/dashboard/**` entry points + sheet cleanup | ✅ Tipis; jangan rusak overview |
| `core/domain/.../usecase/*` baru (update/delete/getById) | ⚠️ Opsional additive |
| Existing domain entities / repo signatures | ❌ Jangan ubah |
| `core/data/**` | ❌ Kecuali bug blocker |
| `features/auth/**`, `splashscreen/**`, `family/**`, `settings/**` | ❌ Jangan ubah |
| `build-plugin/**`, version catalog | ❌ Jangan ubah |
| DS theme hex/tokens | ❌ Jangan ubah; consume API |

---

## Estimasi Effort

| Bucket | Porsi |
|--------|-------|
| New entry form + VM + pickers (wallet/date/note) | ~45% |
| History screen + mapper/rows | ~20% |
| App/dashboard nav wiring + sheet cleanup | ~20% |
| Previews + QA + (opsional) edit/delete | ~15% |

---

## Phase 4 vs Phase 5 (Ringkas)

```
Phase 4 (done)                       Phase 5 (this doc)
─────────────────────────────────    ─────────────────────────────────
FAB → local bottom sheet        →    FAB → TransactionRoute (full screen)
Sheet save + defaults           →    Wallet/date/note pickers + richer form
Recent list (5)                 →    View All → TransactionHistoryRoute
Dashboard owns entry UX         →    :features:transaction owns entry + history
```

---

*Dokumen ini adalah referensi implementasi untuk Phase 5 KeuTrack. Setelah selesai, pencatatan dan riwayat transaksi berdiri di modul sendiri; lanjut Phase 6 (Family) atau Phase 7 (Settings persistence) sesuai prioritas produk.*
