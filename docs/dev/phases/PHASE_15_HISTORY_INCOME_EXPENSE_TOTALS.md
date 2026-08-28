# Phase 15 — Pindah Ringkasan Income / Expense ke Transaction History

> **Modul target:** `:core:domain` + `:core:data` (SUM query scoped) → `:features:transaction` (header History) → `:features:dashboard` (cabut kartu)  
> **Estimasi:** ~1–1.5 hari · **15a** ~0.3 hari (query) · **15b** ~0.5–0.7 hari (History UI) · **15c** ~0.2–0.3 hari (cabut Dashboard)  
> **Prasyarat:** Phase 13b ✅ (History period chips) · Phase 14a ✅ (`GetPeriodTotalsUseCase` + DAO `SUM` + payday cycle)  
> **Status:** **Not started**  
> **Hasil akhir:** Kartu Income / Expense **tidak** lagi di Dashboard. History menampilkan total pemasukan & pengeluaran yang **mengikuti chip periode + scope** (All / Personal / Family), dihitung `SUM` Room **tanpa LIMIT**. Saldo wallet Dashboard **tetap** kumulatif. Footer `% this month` / `% periode ini` di kartu personal **tetap**.  
> **Override Phase 14 P10:** “Settings siklus kelihatan di home” setelah 15 hanya lewat `monthChangeLabel`, bukan kartu income/expense.

---

## Daftar Isi

1. [Konteks & Tujuan](#1-konteks--tujuan)
2. [Inventory — Apa yang Sudah Ada (setelah Phase 14)](#2-inventory--apa-yang-sudah-ada-setelah-phase-14)
3. [Keputusan Produk](#3-keputusan-produk)
4. [Scope — Apa yang Dikerjakan](#4-scope--apa-yang-dikerjakan)
5. [Scope — Apa yang TIDAK Dikerjakan](#5-scope--apa-yang-tidak-dikerjakan)
6. [Prasyarat (Definition of Ready)](#6-prasyarat-definition-of-ready)
7. [File Referensi (Read-Only)](#7-file-referensi-read-only)
8. [File yang TIDAK BOLEH Diubah](#8-file-yang-tidak-boleh-diubah)
9. [File yang BOLEH Diubah / Dibuat](#9-file-yang-boleh-diubah--dibuat)
10. [Struktur File Target](#10-struktur-file-target)
11. [Desain UX History](#11-desain-ux-history)
12. [Desain Query: SUM Scoped](#12-desain-query-sum-scoped)
13. [Pemetaan UI → State / Use Case](#13-pemetaan-ui--state--use-case)
14. [Task Breakdown Detail](#14-task-breakdown-detail)
15. [Acceptance Criteria](#15-acceptance-criteria)
16. [Catatan Arsitektur & Konvensi](#16-catatan-arsitektur--konvensi)
17. [Dependency Graph](#17-dependency-graph)
18. [Risiko & Mitigasi](#18-risiko--mitigasi)
19. [Urutan Pengerjaan yang Disarankan](#19-urutan-pengerjaan-yang-disarankan)
20. [Relasi ke Phase Lain](#20-relasi-ke-phase-lain)
21. [Rencana Commit](#21-rencana-commit)
22. [Manual Test Plan](#22-manual-test-plan)

---

## 1. Konteks & Tujuan

Dashboard hari ini menumpuk tiga cerita berbeda:

1. **Saldo** — kartu Personal / Family (kumulatif).
2. **Aliran periode** — kartu Income / Expense (`GetPeriodTotalsUseCase` siklus current, **semua wallet**, tanpa label periode).
3. **Aktivitas** — recent transactions.

Kartu (2) lemah di home: tidak ada chip/label “Periode ini”, tidak split personal vs family, dan hampir duplikat `monthChangeLabel` di footer kartu personal. User yang ingin “berapa masuk/keluar di rentang ini” sudah ada di History (chip Semua / 7 hari / Periode ini / Custom + scope All / Personal / Family) — tapi History **tidak** menampilkan total.

History **tidak boleh** jumlahkan `uiState.items`. List dipotong `HISTORY_LIMIT = 50` (Family `200`). Total yang jujur = `SUM` DAO tanpa `LIMIT`, dengan filter yang **sama** dengan query list.

**Tujuan 15a — Query:**

1. `observePeriodTotals` / `observeSumsByType` menerima `walletId` / `familyId` opsional + `startDate` / `endDate` **nullable** (Semua = all-time).
2. `GetPeriodTotalsUseCase` memakai `Params` (pola `GetTransactionsUseCase`), bukan dua `Instant` wajib.
3. Dashboard `monthChangeLabel` tetap memakai totals **unscoped** siklus current vs prior (hanya ganti call-site ke `Params`).

**Tujuan 15b — History:**

4. Header sticky di bawah `HistoryPeriodBar`: dua kartu **PEMASUKAN** / **PENGELUARAN** + caption `periodSummaryLabel`.
5. Angka mengikuti scope + chip; tampil juga saat list kosong (Rp 0).
6. Sembunyi hanya saat `isLoading` initial.

**Tujuan 15c — Dashboard:**

7. Cabut `DashboardStatCardsRow` dan field `incomeTotal` / `expenseTotal`.
8. Saldo, recent list, FAB, `monthChangeLabel` **tidak** berubah.

**Bukan tujuan Phase 15:**

- Pagination / “showing 50 of N”
- Net (income − expense) sebagai kartu ketiga
- Filter type/category di History
- Kartu yang sama di tab Family (sudah punya expense bulan terpilih)
- Mengubah default chip History (tetap **Semua**)
- Sync / Firestore agregat

---

## 2. Inventory — Apa yang Sudah Ada (setelah Phase 14)

| Item | Lokasi | Status vs Phase 15 |
|------|--------|-------------------|
| `DashboardStatCardsRow` | `features/dashboard/.../DashboardStatCards.kt` | Income/Expense home; **15c hapus pemakaian** |
| `incomeTotal` / `expenseTotal` | `DashboardUIState` + `DashboardViewModel` | Siklus current, **semua wallet**, tanpa label |
| `monthChangeLabel` | Kartu personal footer | Net current vs prior; **tetap** |
| `GetPeriodTotalsUseCase` | `core/domain/.../usecase` | `invoke(startDate, endDate)` — **wajib Instant, tanpa wallet/family** |
| `TransactionRepository.observePeriodTotals` | domain + impl | Sama: dua Instant wajib |
| `TransactionDao.observeSumsByType` | Room | `WHERE dateEpochMs >= :startMs AND <= :endMs` — **tanpa** wallet/family, **tanpa** null range |
| `observeFiltered` | DAO list | Sudah punya wallet/family + nullable start/end + **LIMIT** — pola SQL yang 15a tiru **tanpa LIMIT** |
| `HistoryPeriodBar` | transaction components | Chip Semua / 7 hari / Periode ini / Custom |
| `TransactionHistoryViewModel.transactionParams` | History VM | Sudah hitung `walletId`/`familyId`/`startDate`/`endDate` — **reuse untuk totals** |
| `HistoryUIState` | transaction model | Belum ada income/expense |
| `HISTORY_LIMIT` / `FAMILY_HISTORY_LIMIT` | History VM companion | 50 / 200 — alasan **jangan** list-sum |
| `HistoryPeriodLabels.summary` | transaction model | Caption siap (“Semua”, “7 hari”, range siklus, custom) |
| Copy History | `TransactionHistoryScreen` | Bahasa Indonesia |
| Copy Dashboard kartu | `INCOME` / `EXPENSE` | Inggris — **jangan** dibawa mentah ke History |
| Feature graph | `:features:*` | **Tidak** boleh import composable dashboard dari transaction |
| Tab Family | `monthlyTotalExpense` dll. | Sudah punya angka periode; **jangan** duplikat kartu 15b |

`DashboardViewModelTest` stub: `every { getPeriodTotals(any(), any()) }` — pecah saat signature jadi `Params`.

---

## 3. Keputusan Produk

| # | Keputusan | Pilihan | Alasan |
|---|-----------|---------|--------|
| P1 | Rumah kartu | **History**, cabut dari Dashboard | Home = saldo; History = aliran + list |
| P2 | Sumber angka | DAO `SUM` tanpa LIMIT | List History terpotong 50/200 |
| P3 | Filter totals | **Sama** dengan list: scope AND periode | Header menjelaskan list, bukan angka “global” |
| P4 | Chip **Semua** | Tetap tampilkan totals **all-time** (start/end null) + caption “Semua” | Jangan sembunyikan; jangan diam-diam “bulan ini” |
| P5 | Chip **Periode ini** | Siklus `containing(today)` — sama 14a | Satu definisi bulan dengan Settings |
| P6 | Caption | Selalu `periodSummaryLabel` di bawah row | Kartu Dashboard hari ini **tanpa** periode; jangan ulangi kesalahan itu |
| P7 | Label kartu | **PEMASUKAN** / **PENGELUARAN** | History sudah ID; Dashboard EN tidak ikut pindah |
| P8 | Empty list | Row tetap, Rp 0 | Jujur: filter aktif tapi tidak ada tx |
| P9 | Loading | Sembunyi saat `isLoading` | Hindari flash 0 sebelum Room emit |
| P10 | Sticky | Column di atas `LazyColumn` (sejajar `HistoryPeriodBar`) | Total tidak ikut scroll baris |
| P11 | Visual | Clone `DashboardStatCardsRow` di `:features:transaction` | Feature tidak saling depend; jangan naik `:core:designsystem` (satu konsumen) |
| P12 | Dashboard sisa | `monthChangeLabel` **tetap**; saldo **tetap** | Override 14 P10: siklus di home hanya lewat % |
| P13 | Default chip | **Tidak** diubah (Semua) | Scope Phase 13b |
| P14 | Family tab | Tidak menambah kartu income/expense | Sudah ada expense periode |
| P15 | Personal tanpa wallet | Totals 0, jangan panggil query (sama list) | Konsisten `familyId`/`walletId` blank |
| P16 | Format uang | `CurrencyFormat.formatIdr` | Sama Dashboard / row History |
| P17 | Ship | **15a + 15b + 15c satu PR** (atau 15c langsung setelah 15b di branch yang sama) | Jangan live-kan SUM baru tanpa UI, atau UI tanpa SUM |

---

## 4. Scope — Apa yang Dikerjakan

### 15a — SUM scoped

1. DAO `observeSumsByType(walletId, familyId, startMs, endMs)` — keempatnya nullable; `GROUP BY type`; **tanpa LIMIT**.
2. Data source + `TransactionRepository.observePeriodTotals` + `GetPeriodTotalsUseCase.Params`.
3. Dashboard `monthChangeLabel`: ganti call ke `Params(startDate, endDate)` unscoped; **belum** cabut field income/expense (supaya 15a compile sendiri jika di-commit terpisah — lihat P17).
4. Tes repo/DAO atau mapper rows: wallet filter, family filter, null range = all-time, range memotong tx di luar jendela.

### 15b — History header

5. `HistoryUIState.incomeTotal` / `expenseTotal` (`Long`).
6. VM `combine` list + `getPeriodTotals(params yang sama dengan list)` (tanpa `limit`).
7. `HistoryPeriodTotalsRow` + Preview light/dark.
8. Pasang di `TransactionHistoryScreen` antara bar dan list/empty.
9. Tes VM: All + Semua; Personal `walletId`; Family `familyId`; ganti chip → Params totals ikut; jangan `sum(items)`.

### 15c — Cabut Dashboard

10. Hapus item `DashboardStatCardsRow` dari `DashboardScreen`.
11. Hapus `incomeTotal` / `expenseTotal` dari `DashboardUIState` / mock / mapper preview.
12. Hapus `DashboardStatCards.kt` jika tidak terpakai.
13. Tes Dashboard: `monthChangeLabel` + saldo masih benar; tidak assert kartu income/expense.

---

## 5. Scope — Apa yang TIDAK Dikerjakan

- Infinite scroll / count total baris
- Kartu **Net**
- Breakdown per kategori di header History
- Memindahkan `StatMiniCard` ke design system
- Mengubah New Entry, Settings siklus, Family stepper
- Filter Firestore by date (tetap Room-only)
- Mengubah `HISTORY_LIMIT`
- i18n string resources (tetap const di file, pola existing)

---

## 6. Prasyarat (Definition of Ready)

- [x] Phase 13b: `HistoryPeriodBar` + `GetTransactionsUseCase.Params` range
- [x] Phase 14a: `GetPeriodTotalsUseCase` + `observeSumsByType` + chip **Periode ini** = siklus
- [x] `observeFiltered` sudah nullable start/end + wallet/family (pola SQL)
- [x] History scope All / Personal / Family dari route args
- [x] `CurrencyFormat.formatIdr` di design system

---

## 7. File Referensi (Read-Only)

| File | Kenapa |
|------|--------|
| `docs/dev/phases/PHASE_13_FAMILY_AND_HISTORY_PERIOD_FILTER.md` | Kontrak chip + scope |
| `docs/dev/phases/PHASE_14_PAYDAY_CYCLE_PERIOD_PREFERENCE.md` | P10 Dashboard totals; siklus; `observeSumsByType` as-shipped |
| `TransactionDao.observeFiltered` | Pola `(:x IS NULL OR …)` yang ditiru 15a |
| `TransactionHistoryViewModel.transactionParams` / `instantRange` | Range chip yang totals **wajib** mirror |
| `DashboardStatCards.kt` | Referensi visual clone (bukan import) |
| `GetTransactionsUseCase.Params` | Pola `Params` untuk totals |
| `HistoryPeriodLabels` | Caption P6 |
| `DashboardViewModel.periodTotalsFlow` | Tetap untuk `monthChangeLabel` saja |

---

## 8. File yang TIDAK BOLEH Diubah

- `features/auth/**`, `features/splashscreen/**`
- Domain user/auth + `UserRepositoryImpl`
- `build-plugin/**`, `settings.gradle.kts`, `gradle.properties`, `local.properties`
- New Entry / amount keypad (Phase 12)
- Settings siklus UI (Phase 14) — pref tidak berubah
- Family screen / budget authoring
- Firestore rules
- Schema Room **tabel** `transactions` (hanya query baru / signature query)

Boleh sentuh Dashboard **hanya** kartu income/expense + field UIState terkait + call-site `GetPeriodTotalsUseCase`. Jangan ubah saldo, visibility, sync, recent list.

---

## 9. File yang BOLEH Diubah / Dibuat

### 15a — core

| Path | Aksi |
|------|------|
| `core/data/.../db/dao/TransactionDao.kt` | Perluas `observeSumsByType` |
| `core/data/.../datasource/local/TransactionLocalDataSource.kt` + Impl | Signature ikut DAO |
| `core/domain/.../repository/TransactionRepository.kt` | `observePeriodTotals` + filter opsional |
| `core/data/.../repository/TransactionRepositoryImpl.kt` | Teruskan param |
| `core/domain/.../usecase/GetPeriodTotalsUseCase.kt` | `data class Params` |
| `core/data/src/test/.../TransactionRepositoryImplTest.kt` (atau tes DAO jika ada) | Filter wallet / family / null range |
| `features/dashboard/.../DashboardViewModel.kt` | `getPeriodTotals(Params(start, end))` unscoped — compile 15a |
| `features/dashboard/src/test/.../DashboardViewModelTest.kt` | Stub `getPeriodTotals(any())` |

### 15b — transaction

| Path | Aksi |
|------|------|
| `features/transaction/.../model/HistoryUIState.kt` | `incomeTotal` / `expenseTotal` default `0L` |
| `features/transaction/.../history/TransactionHistoryViewModel.kt` | Combine totals |
| `features/transaction/.../components/HistoryPeriodTotalsRow.kt` | **Baru** |
| `features/transaction/.../history/TransactionHistoryScreen.kt` | Pasang row |
| `features/transaction/.../history/TransactionHistoryRouting.kt` | Tidak wajib ubah (tidak ada callback baru) |
| `features/transaction/src/test/.../TransactionHistoryViewModelTest.kt` | Totals vs Params; bukan sum list |

### 15c — dashboard

| Path | Aksi |
|------|------|
| `DashboardScreen.kt` | Hapus item stat row + const label |
| `DashboardUIState.kt` | Hapus `incomeTotal` / `expenseTotal` |
| `DashboardViewModel.kt` | Jangan map field itu ke UIState |
| `DashboardMockUi.kt` | Hapus income/expense mock jika hanya untuk kartu |
| `DashboardStatCards.kt` | **Hapus file** jika 0 referensi |
| Tes Dashboard terkait field yang dihapus | Rapikan |

Tidak perlu file baru di `:core:designsystem`.

---

## 10. Struktur File Target

```
core/domain/.../usecase/GetPeriodTotalsUseCase.kt
    └── data class Params(walletId, familyId, startDate, endDate)

core/data/.../db/dao/TransactionDao.kt
    └── observeSumsByType(walletId?, familyId?, startMs?, endMs?)

features/transaction/.../presentation/
├── components/
│   ├── HistoryPeriodBar.kt          ← tidak pindah; totals di bawahnya
│   └── HistoryPeriodTotalsRow.kt    ← BARU
├── history/
│   ├── TransactionHistoryScreen.kt  ← sisip row
│   └── TransactionHistoryViewModel.kt
└── model/
    └── HistoryUIState.kt            ← incomeTotal, expenseTotal

features/dashboard/.../presentation/
├── DashboardScreen.kt               ← tanpa DashboardStatCardsRow
├── DashboardViewModel.kt            ← monthChangeLabel only dari totals
└── components/DashboardStatCards.kt ← DIHAPUS
```

---

## 11. Desain UX History

Urutan vertikal `TransactionHistoryScreen` setelah 15b:

```
KeuTrackTopBar (Riwayat / Personal / Keluarga)
HistoryPeriodBar (chips)
HistoryPeriodTotalsRow          ← BARU, sticky
  caption: periodSummaryLabel
  [ PEMASUKAN Rp … ] [ PENGELUARAN Rp … ]
LazyColumn rows  |  empty  |  spinner
```

### 11.1 Visual

Clone token `DashboardStatCardsRow`: `success.s100` / `danger.d100`, icon trending up/down, `bodyBold10` label, `headingBold20` amount, radius `radiusLg`, gap 12.dp.

Caption: `typography.bodyRegular12` + `textColors.body`, padding top 8.dp (sama caption Custom di `HistoryPeriodBar`). **Jangan** duplikasi caption Custom di dua tempat — pindahkan / biarkan satu sumber:

- **Pilih:** caption range hanya di bawah **totals row** (P6). Caption Custom di `HistoryPeriodBar` boleh tetap untuk error range (`periodRangeError`); label “12–20 Agu 2026” cukup di totals.

Kalau Custom caption di bar **dan** di totals sama persis, user lihat dua kali. **Keputusan:** `HistoryPeriodBar` tetap tampilkan `customRangeLabel` seperti sekarang (sudah shipped 13b); totals caption tetap `periodSummaryLabel` untuk Semua / 7 hari / Periode ini / Custom. Duplikat Custom diterima (kecil) **atau** sembunyikan caption totals saat preset Custom. **Pilih yang kedua:** caption totals **hidden** jika `periodPreset == Custom` (bar sudah menampilkan range); selain itu selalu tampil.

### 11.2 Empty & error

| State | Totals row |
|-------|------------|
| `isLoading` | Tidak di-render |
| List kosong, filter off | Render, Rp 0, caption “Semua” |
| List kosong, filter on | Render, Rp 0, caption periode; CTA “Ubah ke Semua” tetap |
| `errorMessage` snackbar | Row ikut state terakhir yang berhasil; jangan blank kecuali loading |

### 11.3 Dashboard setelah 15c

```
Greeting + title
Personal wallet (+ monthChangeLabel)
Family wallet
Recent transactions
```

Tidak ada spacer “bekas” kartu. Spacing `DASH_LIST_SECTION_SPACING` antar item yang tersisa.

---

## 12. Desain Query: SUM Scoped

### 12.1 SQL target

Samakan predikat dengan `observeFiltered`, **tanpa** `type` / `categoryId` / `ORDER BY` / `LIMIT`:

```sql
SELECT type AS type, SUM(amount) AS total FROM transactions
WHERE (:walletId IS NULL OR walletId = :walletId)
  AND (:familyId IS NULL OR familyId = :familyId)
  AND (:startMs IS NULL OR dateEpochMs >= :startMs)
  AND (:endMs IS NULL OR dateEpochMs <= :endMs)
GROUP BY type
```

Room: `Long?` untuk `startMs`/`endMs`. Mapper impl tetap lipat `AmountByTypeRow` → `PeriodTotals` (income/expense 0 jika type tidak muncul).

### 12.2 `GetPeriodTotalsUseCase.Params`

```kotlin
data class Params(
    val walletId: String? = null,
    val familyId: String? = null,
    val startDate: Instant? = null,
    val endDate: Instant? = null,
)

operator fun invoke(params: Params = Params()): Flow<PeriodTotals> =
    transactionRepository.observePeriodTotals(
        walletId = params.walletId,
        familyId = params.familyId,
        startDate = params.startDate,
        endDate = params.endDate,
    )
```

Hapus overload `(Instant, Instant)` supaya mockk satu bentuk. Dashboard:

```kotlin
getPeriodTotals(
    GetPeriodTotalsUseCase.Params(
        startDate = currentRange.start,
        endDate = currentRange.endInclusive,
    ),
)
```

`walletId`/`familyId` tetap null → perilaku 14a untuk `%`.

### 12.3 Mirror range History

Reuse `instantRange(period, cycleStartDay)` yang sudah ada:

| Preset | start/end totals |
|--------|------------------|
| Semua | null / null |
| 7 hari | `PeriodBounds.ofLocalDates(today-6, today)` |
| Periode ini | `PeriodBounds.containing(today, cycleStartDay).toInstantRange()` |
| Custom | `ofLocalDates(from, to)` setelah clamp |

Scope:

| Scope | walletId | familyId |
|-------|----------|----------|
| All | null | null |
| Personal | `summary.personalWallet.id` | null |
| Family | null | `user.familyId` |

Jika Personal wallet id blank atau Family `familyId` blank: **jangan** panggil totals dengan filter “semua tx” — emit `PeriodTotals()` (0), sama seperti list `flowOf(emptyList())`.

### 12.4 Jangan list-sum

Salah:

```kotlin
items.filter { !it.isExpense }.sumOf { parse(it.amountLabel) }  // terpotong LIMIT + parse UI
```

Benar: Flow kedua `getPeriodTotals(Params(...))` combine ke `HistoryUIState`.

Header **boleh** lebih besar dari jumlah baris tampil (mis. 80 tx di Room, list 50). Jangan tambah copy “50 terbaru” di phase ini (lihat §5). Dokumentasikan di risiko §18.

### 12.5 Timezone

Sama Phase 13/14: `PeriodBounds` + `ZoneId.systemDefault()`. Jangan hitung range di Composable.

---

## 13. Pemetaan UI → State / Use Case

### History (`HistoryUIState` additive)

| Field | Sumber |
|-------|--------|
| `items` | `GetTransactionsUseCase` + LIMIT (tidak berubah) |
| `incomeTotal` / `expenseTotal` | `GetPeriodTotalsUseCase` **tanpa** limit, Params mirror list |
| `periodSummaryLabel` | `HistoryPeriodLabels.summary` (sudah ada) → caption P6/P11.1 |
| `scope` / `periodPreset` | tidak berubah |

Screen: tidak ada callback baru. Routing tidak berubah.

### Dashboard setelah 15c

| Field | Sumber |
|-------|--------|
| `personalBalance` / `familyBalance` | `GetWalletSummary` |
| `monthChangeLabel` | `GetPeriodTotalsUseCase` current vs prior, unscoped |
| `incomeTotal` / `expenseTotal` | **dihapus** |
| `recentTransactions` | `GetTransactionsUseCase(limit=RECENT_TX_LIMIT)` |

---

## 14. Task Breakdown Detail

Kerjakan 15a → 15b → 15c. **Jangan** mulai 15b sebelum SUM menerima wallet/family + null range (kalau tidak, Personal History akan tampilkan total semua wallet).

### 15a — Task 1: DAO + data source + repo + use case

- Perluas signature sepanjang stack.
- Mapper `PeriodTotals` tetap di `TransactionRepositoryImpl`.
- Verify: `./gradlew :core:domain:compileDebugKotlin :core:data:compileDebugKotlin`

### 15a — Task 2: tes query

Minimal:

- Dua wallet: filter `walletId` hanya menjumlah wallet itu.
- `familyId` tidak mencampur tx personal.
- Tx di luar `[start, end]` tidak masuk.
- `startMs=null, endMs=null` = all-time.
- Hanya income → `expenseTotal=0`.

Kalau tidak ada tes DAO instrumented, tes `TransactionRepositoryImpl` dengan mock `TransactionLocalDataSource.observeSumsByType` **dan** tes unit mapper rows (income/expense folding) sudah ada di impl — tambah tes impl bahwa param diteruskan. Ideal: Room in-memory jika modul sudah punya pola itu; **jangan** buat test infra baru yang besar. Cukup repo + mock local **plus** satu tes folding jika belum ada.

### 15a — Task 3: Dashboard compile

- `DashboardViewModel.periodTotalsFlow` pakai `Params`.
- Update `DashboardViewModelTest` stub `getPeriodTotals(any())`.
- Verify: `./gradlew :features:dashboard:testDevDebugUnitTest`

### 15b — Task 4: UIState + VM

- Field Long default 0.
- `combine` existing + `getPeriodTotals`. Hati-hati arity `combine` Kotlin (max 5 flow) — History sekarang 5 flow; totals = ke-6. Pecah nested `combine` seperti Dashboard, atau `combine(transactionsFlow, totalsFlow, …)`.
- Personal/Family blank → `flowOf(PeriodTotals())`.
- Tes: verify `getPeriodTotals(match { walletId == "w-p" && startDate != null })` saat Personal + Last7Days.
- Tes: All + Semua → `walletId==null && startDate==null`.
- Tes: 3 tx di stub list vs totals **beda angka** (list 1 item, totals income 1_000_000) → state memakai totals, bukan sum item.

### 15b — Task 5: `HistoryPeriodTotalsRow` + Screen

- Preview light/dark + empty zeros.
- Sisip di Column setelah `HistoryPeriodBar`.
- Caption rule §11.1.
- Empty content: row **di atas** empty column, bukan di dalam `HistoryEmptyContent` (supaya layout loading/empty/list konsisten).

Struktur:

```kotlin
HistoryPeriodBar(...)
if (!uiState.isLoading) {
    HistoryPeriodTotalsRow(...)
}
when {
    isLoading -> spinner
    items.isEmpty() -> HistoryEmptyContent
    else -> LazyColumn
}
```

### 15b — Task 6: tes + compile transaction

```
./gradlew :features:transaction:testDevDebugUnitTest
./gradlew :features:transaction:compileDevDebugKotlin
```

### 15c — Task 7: cabut Dashboard UI

- Screen, UIState, mock, hapus `DashboardStatCards.kt`.
- Komentari ulang kdoc Screen (“wallet summary, income/expense overview” → tanpa income/expense).
- Tes mapper/VM yang masih set `incomeTotal` — rapikan.
- Verify: `./gradlew :features:dashboard:testDevDebugUnitTest`

### Task 8: assemble

```
./gradlew :core:data:testDevDebugUnitTest
./gradlew :features:transaction:testDevDebugUnitTest
./gradlew :features:dashboard:testDevDebugUnitTest
./gradlew assembleDevDebug
```

Flavor tes: `testDevDebugUnitTest` (bukan `testDebugUnitTest`).

---

## 15. Acceptance Criteria

### 15.1 15a

- [ ] `SUM` hormat `walletId` / `familyId` / range nullable
- [ ] Tanpa LIMIT pada query totals
- [ ] Dashboard `monthChangeLabel` masih emit setelah ganti `Params`
- [ ] `startDay=1` vs `25`: jendela Periode ini tetap dari `PeriodBounds.containing` (regresi 14)

### 15.2 15b

- [ ] History All + Semua: totals = all-time semua wallet di Room
- [ ] History Personal: totals hanya wallet personal; Family history tidak masuk
- [ ] History Family: totals `familyId`; personal tidak masuk
- [ ] Chip 7 hari / Periode ini / Custom mengubah **keduanya** list dan totals
- [ ] Caption “Semua” / “7 hari” / range siklus terlihat; Custom tidak dobel (rule §11.1)
- [ ] Empty + filter: row Rp 0, bukan hidden
- [ ] Loading awal: row belum tampil
- [ ] >50 tx di periode: header = SUM penuh, list tetap 50
- [ ] Preview light/dark totals row
- [ ] Copy PEMASUKAN / PENGELUARAN (bukan INCOME / EXPENSE)

### 15.3 15c

- [ ] Dashboard tidak merender kartu income/expense
- [ ] Saldo personal/family tidak berubah vs sebelum 15
- [ ] `monthChangeLabel` masih ada di kartu personal bila prior net ≠ 0
- [ ] Recent transactions + FAB + History buttons wallet tetap
- [ ] File `DashboardStatCards.kt` tidak tersisa tanpa referensi

### 15.4 Non-regresi

- [ ] Chip Custom from>to tetap error 13b
- [ ] Settings `cycleStartDay` tetap menggeser “Periode ini” **dan** totals
- [ ] Sign-out / Family tab / New Entry tidak rusak
- [ ] Visibility mata saldo Dashboard tidak rusak

---

## 16. Catatan Arsitektur & Konvensi

- `:features:transaction` **tidak** depend `:features:dashboard`. Clone visual, hapus sumber Dashboard.
- `GetPeriodTotalsUseCase` tetap di `:core:domain`; implementasi Room di `:core:data`.
- Amounts `Long` (IDR). Format hanya di UI.
- `CancellationException` tidak relevan di observe Flow; biarkan `.catch` History yang sudah ada.
- Screen stateless; totals masuk `HistoryUIState`, bukan `remember { items.sum() }`.
- Jangan hitung siklus di Composable.

---

## 17. Dependency Graph

```
HistoryPeriodBar (chip)
        │
        ▼
TransactionHistoryViewModel
        │
        ├─ GetTransactionsUseCase(Params + limit) ──► list
        │
        └─ GetPeriodTotalsUseCase(Params, no limit) ──► income/expense
                    │
                    ▼
              observeSumsByType (Room SUM)

DashboardViewModel
        └─ GetPeriodTotalsUseCase(Params unscoped, current+prior) ──► monthChangeLabel only
```

---

## 18. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| Sum `items` di VM | Totals salah setelah 50 tx | AC “list 1 vs totals besar”; code review larang parse amountLabel |
| 15b sebelum 15a | Personal History = total semua wallet | DoR: 15a dulu |
| `combine` > 5 Flow | Tidak compile | Nested combine |
| Caption Custom dobel | Noise | §11.1 hide caption totals jika Custom |
| User kira Dashboard “rusak” | Hilang kartu hijau/merah | `monthChangeLabel` tetap; History 1 tap dari wallet |
| Header ≠ jumlah baris | Bingung | Diterima; pagination future. Jangan klaim “total list ini” |
| `observeSumsByType` breaking | Tes Dashboard `any(), any()` merah | Task 3 wajib |
| Feature import DashboardStatCards | Langgar arsitektur | Clone + hapus |
| Family tab vs History Family angka beda | Dua definisi periode | Family = stepper selected; History = chip. Jangan samakan di 15 |

---

## 19. Urutan Pengerjaan yang Disarankan

1. Task 1–2 query + tes (tanpa UI).
2. Task 3 Dashboard compile (`Params`).
3. Task 4–6 History header + tes VM.
4. Task 7 cabut Dashboard.
5. Task 8 `assembleDevDebug` + QA §22.

Jangan merge 15a ke `main` tanpa 15b: API lebih luas tanpa konsumen History. Jangan merge 15b tanpa 15a: angka Personal/Family salah. **Keputusan (P17):** satu PR 15a+15b+15c.

---

## 20. Relasi ke Phase Lain

| Phase | Relasi |
|-------|--------|
| **13b** | Chip + range list; 15 menempel totals ke kontrak yang sama |
| **14a** | `GetPeriodTotalsUseCase` + siklus; 15 **memindahkan** konsumen kartu, **memperluas** filter. **P10 di-override:** home tidak lagi menampilkan income/expense siklus |
| **14** `monthChangeLabel` | Tetap di Dashboard — satu-satunya sisa “periode” di home |
| 4 | Dashboard real data; 15c menyusutkan UI Phase 4 |
| 5 | History list; 15 menambah header |
| 6 / 11 | Family insights/budget **tidak** diubah |
| Future | Pagination History; kartu Net; design-system stat row jika ≥2 feature |

---

## 21. Rencana Commit

Ikuti tag repo (`[FEAT]` / `[TEST]` / `[DOCS]`). Satu PR boleh beberapa commit:

```
[FEAT] Scope period totals query by wallet family and range
[FEAT] Show income and expense totals on transaction history
[FEAT] Remove income and expense cards from dashboard
[TEST] Cover scoped period totals and history header state
[DOCS] Add Phase 15 history income expense totals plan
```

Commit `[DOCS]` untuk file plan ini boleh **sekarang** (sebelum implementasi). Commit FEAT hanya saat kode menyusul.

---

## 22. Manual Test Plan

Tanggal uji menyesuaikan `today`. Contoh di bawah mengunci `cycleStartDay` di Settings.

### 22.1 Dashboard (15c)

| # | Langkah | Expected |
|---|---------|----------|
| 1 | Buka home | Tidak ada kartu INCOME / EXPENSE |
| 2 | Kartu Personal | Saldo kumulatif; footer `%` masih ada jika prior ≠ 0 |
| 3 | Kartu Family | Saldo + shared footer; History > tetap |
| 4 | Recent + FAB | Tidak berubah |

### 22.2 History All — chip

| # | Langkah | Expected |
|---|---------|----------|
| 5 | History (View all / recent) default Semua | Caption “Semua”; totals = semua tx Room |
| 6 | Chip 7 hari | List + totals hanya 7 hari inclusive |
| 7 | Chip Periode ini (`startDay=1`) | Sama bulan kalender |
| 8 | Settings siklus 25, kembali Periode ini | Totals = jendela gajian; caption range (bukan “Semua”) |
| 9 | Custom 1 hari | List + totals hari itu; caption totals **tidak** dobel di bawah row |
| 10 | Periode tanpa tx | Empty copy + totals Rp 0 + CTA filter |

### 22.3 Scope Personal / Family

| # | Langkah | Expected |
|---|---------|----------|
| 11 | History > di kartu Personal | Totals ≠ All jika ada tx family di periode yang sama |
| 12 | History > di kartu Family | Hanya tx `familyId`; personal salary tidak masuk PEMASUKAN |
| 13 | User tanpa family, buka Family history | Empty + totals 0 (tidak bocor ke personal) |

### 22.4 LIMIT vs SUM

| # | Langkah | Expected |
|---|---------|----------|
| 14 | >50 tx personal di “Semua” | List 50; PEMASUKAN+PENGELUARAN = seluruh Room personal (boleh > jumlah baris) |

### 22.5 Regresi

| # | Langkah | Expected |
|---|---------|----------|
| 15 | Custom from > to | Error range; totals tidak pakai range invalid |
| 16 | Ganti `cycleStartDay` 1 ↔ 25 | Dashboard saldo sama; History Periode ini + totals bergeser |
| 17 | New Entry simpan expense | History totals naik tanpa buka ulang (Flow) |
| 18 | Rotation History | Chip + totals tetap (`SavedStateHandle` periode) |

---

## Appendix — Call-site ringkas

**History VM (15b), sketsa:**

```kotlin
val totalsFlow = periodContext.flatMapLatest { (selection, startDay) ->
    getPeriodTotals(
        GetPeriodTotalsUseCase.Params(
            walletId = /* same as list */,
            familyId = /* same as list */,
            startDate = instantRange(selection, startDay)?.start,
            endDate = instantRange(selection, startDay)?.endInclusive,
        ),
    )
}
```

Personal/Family: `flatMapLatest` yang sama dengan `transactionsFlow` (wallet/family resolved dulu), lalu `getPeriodTotals` — jangan All-scope totals di Personal.

**Dashboard VM (15c):** hapus assignment `incomeTotal` / `expenseTotal`; biarkan `periodTotalsFlow` untuk `monthChangeLabel` saja.
