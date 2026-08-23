# Phase 14 — Payday Cycle: Default Monthly Period di Settings

> **Modul target:** `:features:settings` (UI + persist) → `:core:datastore` / `:core:domain` / `:core:common` → konsumsi di `:features:family`, `:features:transaction` (history preset), `:features:dashboard` (income/expense)  
> **Estimasi:** ~2–3.5 hari · **14a** ~1.5–2.5 hari (pref + filter ikut siklus) · **14b** ~0.5–1 hari (matching `Budget.spent` / `monthKey` tulis)  
> **Prasyarat:** Phase 13 ✅ **selesai** (13a Family month stepper + 13b History date range + `PeriodBounds`)  
> **Status:** Planning — **jangan mulai sebelum Phase 13 AC hijau**.  
> **Hasil akhir:** User set **hari mulai siklus** di Settings (default tanggal 1 = bulan kalender). “Bulan ini” di Family / History / kartu income-expense Dashboard mengikuti jendela gajian yang berulang (contoh: 25 Jul – 24 Agu, lalu 25 Agu – 24 Sep). Saldo wallet **tetap** kumulatif.

---

## Daftar Isi

1. [Konteks & Tujuan](#1-konteks--tujuan)
2. [Inventory — Apa yang Sudah Ada (setelah Phase 13)](#2-inventory--apa-yang-sudah-ada-setelah-phase-13)
3. [Keputusan Produk](#3-keputusan-produk)
4. [Scope — Apa yang Dikerjakan](#4-scope--apa-yang-dikerjakan)
5. [Scope — Apa yang TIDAK Dikerjakan](#5-scope--apa-yang-tidak-dikerjakan)
6. [Prasyarat (Definition of Ready)](#6-prasyarat-definition-of-ready)
7. [File Referensi (Read-Only)](#7-file-referensi-read-only)
8. [File yang TIDAK BOLEH Diubah](#8-file-yang-tidak-boleh-diubah)
9. [File yang BOLEH Diubah / Dibuat](#9-file-yang-boleh-diubah--dibuat)
10. [Struktur File Target](#10-struktur-file-target)
11. [Desain UX Settings](#11-desain-ux-settings)
12. [Desain Domain: Siklus, Label, Kunci Budget](#12-desain-domain-siklus-label-kunci-budget)
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

Phase 13 mengunci **satu bulan kalender** (`1 … last day`, `YearMonth`). Itu benar untuk banyak user, tapi tidak untuk gajian: “bulan keuangan” seseorang bisa **25 Juli – 24 Agustus**, lalu berulang.

User **tidak** mengisi range baru setiap bulan. Satu aturan di Settings:

> Siklus dimulai tanggal **25**

App menggeser jendela itu otomatis:

| Hari ini (contoh) | Periode “berjalan” |
|-------------------|-------------------|
| 23 Agu 2026 | 25 Jul – 24 Agu |
| 25 Agu 2026 | 25 Agu – 24 Sep |
| `startDay = 1` | 1 Agu – 31 Agu (identik Phase 13) |

**Tujuan 14a:**

1. Kartu/tombol di `SettingsScreen` untuk set `cycleStartDay` (1–28), default **1**.
2. Persist Proto DataStore (device, user lokal).
3. `PeriodBounds` / `FinancePeriod` memakai `cycleStartDay`.
4. Family stepper, History chip “Bulan ini” / “Periode ini”, Dashboard income/expense + `% this month` memakai **periode siklus**, bukan `YearMonth` kalender (kecuali `startDay == 1`).
5. Authoring budget: “periode berjalan” = periode yang **mengandung hari ini**, bukan `YearMonth.now()` kalender.

**Tujuan 14b:**

6. Saat add/sync expense, `Budget.month` / increment `spent` memakai **kunci periode siklus** yang sama dengan UI, supaya Room `Budget.spent` tidak nyangkut di bulan kalender.

**Bukan tujuan Phase 14:**

- User mengisi dua tanggal mutlak setiap bulan
- Siklus beda per wallet / per kategori
- Sync pref ke Firestore / antar device (MVP lokal DataStore)
- Siklus **family-wide** di `FamilyGroup` (anggota lain otomatis sama) — future
- Copy-forward limit otomatis
- Range custom 13b dihapus (tetap ada)
- Reset transaksi tiap gajian

---

## 2. Inventory — Apa yang Sudah Ada (setelah Phase 13)

Asumsi DoR: 13a + 13b sudah di kode.

| Item | Peran vs Phase 14 |
|------|-------------------|
| `PeriodBounds.ofYearMonth` / `ofLocalDates` | Kalender & custom range History; **belum** ada `cycleStartDay` |
| Family stepper `YearMonth` | Harus jadi stepper **FinancePeriod** |
| History preset `CurrentMonth` | Harus jadi periode siklus (“Periode ini”) |
| `Budget.month` | String `yyyy-MM` — **tetap schema**; arti kunci disepakati §12.3 |
| `TransactionRepositoryImpl.monthKey` | `YearMonth` dari `tx.date` — salah untuk gajian sampai 14b |
| `FamilyUiMapper.toBudgetRows` | Spent tampilan dari **txs terfilter** — 14a sudah bisa jujur di UI |
| `GetMonthlySummaryUseCase` / `CategorySummary.period` | Agregat **kalender**; Dashboard 14a jangan andalkan ini untuk siklus ≠ 1 |
| `WalletUiPreferences` proto | Hanya visibility saldo; pola persist siap ditambah field atau proto baru |
| `SettingsScreen` | Profile, Family Network, Wallets, Sheets, Sign out — **belum** periode |
| Saldo wallet | Kumulatif — **jangan** diikat siklus |

---

## 3. Keputusan Produk

| # | Keputusan | Pilihan | Alasan |
|---|-----------|---------|--------|
| P1 | Kapan dikerjakan | **Setelah Phase 13 selesai** | 13 membangun query range + UI filter; 14 hanya mengganti definisi “satu bulan” |
| P2 | Yang disimpan | **Satu angka** `cycleStartDay` (1–28), default 1 | Bukan from–to mutlak; jendela bergeser tiap periode |
| P3 | Cap 28 | Tidak izinkan 29–31 | Februari; clamp `min(startDay, lengthOfMonth)` |
| P4 | Persist | DataStore lokal (additive proto) | Sama pola Phase 7 visibility; belum perlu Firestore |
| P5 | Siapa yang punya pref | **Per akun di device** | Family tab memakai pref user yang login. Bukan satu siklus resmi keluarga di MVP |
| P6 | Label UI `startDay == 1` | `MMMM yyyy` (id-ID) seperti Phase 13 | Tidak pecah UX kalender |
| P7 | Label UI `startDay != 1` | `d MMM – d MMM yyyy` (satu atau dua tahun) | Jujur: 25 Jul – 24 Agu 2026 |
| P8 | Family prev/next | Geser **satu siklus** (±1 periode), bukan `YearMonth.minusMonths` buta | Next tidak melewati periode yang mengandung today |
| P9 | History “Bulan ini” | Rename/copy **Periode ini** = siklus current; chip 7 hari / Semua / Custom **tetap** | Custom 13b tidak diubah |
| P10 | Dashboard income/expense | Jumlah txs di siklus current (bukan `CategorySummary` kalender) | Settings “default monthly” harus kelihatan di home |
| P11 | Dashboard saldo | Tidak diubah | Kumulatif |
| P12 | Budget **baca** (14a) | `observeBudgets(periodKey)` + spent UI dari txs siklus | Mapper Family sudah spent-from-txs |
| P13 | `periodKey` | `yyyy-MM` = **bulan kalender tanggal akhir periode** (inclusive) | `startDay=1` → Agustus = `2026-08` (kompatibel 13). Siklus 25 Jul–24 Agu → kunci `2026-08` |
| P14 | Authoring | Hanya jika selected period **contains today** | Ganti `selected == YearMonth.now()` |
| P15 | Budget **tulis** (14b) | `monthKey(tx)` = periodKey dari siklus yang mengandung `tx.date` | Tanpa 14b, `Budget.spent` Room bisa salah; bar UI 14a tetap OK |
| P16 | Ganti `startDay` di tengah jalan | Pref baru berlaku immediately; budget lama kunci kalender **tidak dimigrasi** | Dokumentasikan; user yang pindah 1→25 mungkin lihat limit di kunci lain |
| P17 | Proto | Field baru di `WalletUiPreferences` **atau** proto `PeriodPreferences` terpisah | Terpisah lebih bersih; field `cycle_start_day = 3` di proto existing lebih sedikit file. **Pilih proto terpisah** `period_preferences.proto` agar visibility wallet tidak tercampur konsep gajian |
| P18 | Dashboard `% this month` | Banding net siklus current vs prior siklus | Bukan `CategorySummary` kalender |

---

## 4. Scope — Apa yang Dikerjakan

### 14a — Pref + filter ikut siklus

1. Domain `PeriodPreferences(cycleStartDay: Int)` + validasi 1–28.
2. Proto DataStore + repository + `ObservePeriodPreferencesUseCase` / `SetCycleStartDayUseCase`.
3. `FinancePeriod` + perluasan `PeriodBounds` (`containing`, `plusPeriods`, `toInstantRange`, `periodKey`).
4. Settings: section + kartu + sheet pilih tanggal 1–28 + preview rentang.
5. Family VM: stepper + query + authoring guard memakai `FinancePeriod`, bukan `YearMonth` mentah.
6. History: preset current = siklus now; copy chip.
7. Dashboard: `incomeTotal` / `expenseTotal` / `monthChangeLabel` dari txs siklus (observe `GetTransactionsUseCase` range), **bukan** ganti schema `CategorySummary`.
8. Tes util + mapper + Settings VM + regresi Family/History `startDay=1`.

### 14b — Tulis budget selaras siklus

9. `TransactionRepositoryImpl.monthKey` (dan setara sync rebuild summary **tidak** wajib diubah jika Dashboard sudah tidak pakai summary untuk kartu ini).
10. Matching `findBudgetForExpense(month = periodKey, …)`.
11. Tes: tx 26 Jul + `startDay=25` → kunci `2026-08` (periode 25 Jul–24 Agu).

---

## 5. Scope — Apa yang TIDAK Dikerjakan

- Migrasi massal baris `Budget.month` lama
- `FamilyGroup.cycleStartDay` + rules Firestore
- Pref di cloud / restore antar device
- Multi-cycle (personal 25, family 1)
- UI minggu (`BudgetPeriod.WEEKLY`)
- Menghapus Custom range History
- Mengubah New Entry (tanggal transaksi tetap kalender)
- Pagination / pull Firestore by cycle

---

## 6. Prasyarat (Definition of Ready)

- [ ] Phase 13a: Family query `startDate`/`endDate`, stepper, authoring hanya “periode current”
- [ ] Phase 13b: History presets + Custom
- [ ] `PeriodBounds` tes kalender hijau
- [ ] `GetTransactionsUseCase.Params` range dipakai Family & History
- [ ] Settings Phase 7 (DataStore visibility) sebagai pola persist

Jika 13 belum merge, **jangan** implementasi 14.

---

## 7. File Referensi (Read-Only)

| File | Kenapa |
|------|--------|
| `docs/dev/phases/PHASE_13_FAMILY_AND_HISTORY_PERIOD_FILTER.md` | Kontrak filter yang 14 override definisi bulan |
| `PeriodBounds.kt` (hasil 13) | Titik perluasan siklus |
| `FamilyViewModel.kt` | Ganti `YearMonth` selected → `FinancePeriod` |
| `TransactionHistoryViewModel.kt` | Preset current month |
| `DashboardViewModel.kt` | Income/expense dari summary kalender |
| `wallet_ui_preferences.proto` | Pola DataStore; **jangan** campur kecuali P17 dibatalkan |
| `SettingsScreen.kt` | Titik pasang kartu |
| `Budget.kt` / `TransactionRepositoryImpl.monthKey` | 14b |
| Phase 11 P3/P4 | Satu budget per kunci bulan + authoring periode berjalan |

---

## 8. File yang TIDAK BOLEH Diubah

- `features/auth/**`, `features/splashscreen/**`
- Domain user/auth + `UserRepositoryImpl`
- `build-plugin/**`, `settings.gradle.kts`, `gradle.properties`, `local.properties`
- New Entry amount/keypad (Phase 12)
- Firestore rules (pref lokal)

Boleh sentuh Dashboard **hanya** sumber income/expense / monthChangeLabel, bukan saldo.

---

## 9. File yang BOLEH Diubah / Dibuat

### Persistensi & domain

| Path | Aksi |
|------|------|
| `core/datastore/.../period_preferences.proto` | **Baru** `cycle_start_day` (int32, 0 = unset → 1) |
| Serializer + `PeriodPreferencesDataStoreModule` | **Baru** (pola wallet prefs) |
| `core/domain/.../model/PeriodPreferences.kt` | **Baru** |
| `PeriodPreferencesRepository` + impl + DataSource | **Baru** |
| `ObservePeriodPreferencesUseCase` / `SetCycleStartDayUseCase` | **Baru** |
| `core/common/.../PeriodBounds.kt` + `FinancePeriod.kt` | Perluas |
| Tes `PeriodBounds` siklus (23 Agu + day 25, 1 Feb + day 28, day 1 = kalender) | **Baru/ubah** |

### Settings

| Path | Aksi |
|------|------|
| `SettingsUIState` / mapper / VM | Field + save |
| `SettingsScreen.kt` / Routing | Section + callback |
| `components/SettingsPeriodCycleCard.kt` | **Baru** |
| `components/SettingsPeriodCycleSheet.kt` | **Baru** — pilih 1–28 |
| Tes `SettingsViewModel` | Save/observe |

### Konsumen filter (14a)

| Path | Aksi |
|------|------|
| `FamilyViewModel` / mapper / stepper / UIState label | `FinancePeriod` |
| `TransactionHistoryViewModel` / `HistoryPeriod` copy | Current = siklus |
| `DashboardViewModel` | Income/expense dari txs range |
| Tes Family / History / Dashboard terkait periode | Ubah |

### 14b

| Path | Aksi |
|------|------|
| `TransactionRepositoryImpl.monthKey` | Pakai `PeriodBounds.periodKey(tx.date, startDay)` |
| Inject `PeriodPreferences` / baca DataStore di repo | Hindari `YearMonth` buta |
| Tes repo: 26 Jul + startDay 25 → `2026-08` | **Baru** |

`monthKey` di data layer butuh `cycleStartDay`. Jangan hard-code 1. Baca pref di addTransaction (suspend) atau inject store. Jika repo tidak boleh block: `PeriodPreferencesRepository.observe().first()`.

---

## 10. Struktur File Target

```
core/datastore/src/main/proto/.../period_preferences.proto     ← BARU
core/common/.../utils/
├── PeriodBounds.kt                                            ← UBAH (siklus)
└── FinancePeriod.kt                                           ← BARU (data class start/end)

core/domain/.../model/PeriodPreferences.kt                     ← BARU
core/domain/.../repository/PeriodPreferencesRepository.kt      ← BARU
core/domain/.../usecase/SetCycleStartDayUseCase.kt             ← BARU
core/domain/.../usecase/ObservePeriodPreferencesUseCase.kt     ← BARU

features/settings/.../presentation/
├── SettingsScreen.kt / SettingsViewModel.kt                   ← UBAH
└── components/SettingsPeriodCycleCard.kt + Sheet.kt           ← BARU
```

Family / History / Dashboard: ubah file Phase 13 + `DashboardViewModel.kt`.

---

## 11. Desain UX Settings

Letak: section **Periode keuangan** setelah Connected Wallets, sebelum Google Sheets (pref app, bukan family network).

Kartu (`SettingsPeriodCycleCard`):

- Title: `Siklus bulanan`
- Subtitle default: `Tanggal 1 – akhir bulan (kalender)`
- Subtitle custom: `Tanggal 25 – sehari sebelum tanggal 25 berikutnya`
- Trailing: chevron / “Atur”
- Tap → sheet

Sheet (`SettingsPeriodCycleSheet`):

- Judul: `Mulai siklus`
- Penjelasan 1–2 baris: “Pemasukan gajian sering tidak di tanggal 1. Pilih hari mulai, filter Family dan riwayat mengikuti rentang ini setiap periode.”
- Picker 1–28 (list / `LazyColumn` / slider — Material 2, token tema)
- Preview live: `Periode berjalan: 25 Jul 2026 – 24 Agu 2026`
- Simpan / Batal
- Disabled 29–31

Tidak ada date picker from–to.

Accessibility: kartu `contentDescription` termasuk angka hari.

---

## 12. Desain Domain: Siklus, Label, Kunci Budget

### 12.1 `FinancePeriod`

```kotlin
data class FinancePeriod(
    val start: LocalDate, // inclusive
    val end: LocalDate,   // inclusive
) {
    val periodKey: String get() = YearMonth.from(end).toString() // P13
}
```

### 12.2 Algoritma `containing(today, startDay)`

1. `anchor = clamp(startDay, 1, 28)`.
2. `startThis = date.withDayOfMonth(min(anchor, date.lengthOfMonth()))`.
3. Jika `date >= startThis` → `start = startThis`; else `start = startThis.minusMonths(1)` lalu clamp hari lagi.
4. `end = start.plusMonths(1).minusDays(1)` (bukan “hari 24 tetap” jika start di-clamp Februari).

Contoh:

| today | startDay | start | end | periodKey |
|-------|----------|-------|-----|-----------|
| 2026-08-23 | 1 | 2026-08-01 | 2026-08-31 | `2026-08` |
| 2026-08-23 | 25 | 2026-07-25 | 2026-08-24 | `2026-08` |
| 2026-08-25 | 25 | 2026-08-25 | 2026-09-24 | `2026-09` |
| 2026-02-10 | 28 | 2026-01-28 | 2026-02-27 | `2026-02` |
| 2026-02-28 | 28 | 2026-02-28 | 2026-03-27 | `2026-03` |

`plusPeriods(+1 / -1)`: `start.plusMonths(n)` + rumus end yang sama.

Instant range: `PeriodBounds.ofLocalDates(start, end)` (sudah ada dari 13).

### 12.3 Kompatibilitas `startDay = 1`

Identik Phase 13: Family label “Agustus 2026”, History “bulan ini”, budget key `yyyy-MM` kalender. **Regresi wajib.**

### 12.4 Budget vs siklus

**14a (baca):** Family `getBudgetProgress(selected.periodKey)`; spent bar dari txs di `[start, end]`. Limit dari baris `Budget.month == periodKey`.

**14b (tulis):** `monthKey(tx)` = `FinancePeriod.containing(txDate, startDay).periodKey`.

Tanpa 14b: user `startDay=25`, expense 26 Jul masuk budget kalender `2026-07` di Room, sementara UI periode current key `2026-08` — limit Agustus tidak naik `spent` tersimpan, tapi **bar UI 14a tetap** dari txs. 14b menutup gap footnote / sync Firestore `spent`.

Data lama Phase 11: tetap `yyyy-MM` kalender. Ganti ke 25 tidak memindahkan limit. Copy Settings: “Limit lama mengikuti bulan kalender; siklus baru memakai kunci bulan tanggal akhir periode.”

### 12.5 CategorySummary Dashboard

Jangan remap `category_summaries` di 14a. Kartu income/expense = `sum` txs personal+family? **Dashboard hari ini:** summary **user** period kalender, bukan split wallet.

Tetap satu total income/expense di Dashboard (seperti sekarang), tapi window = siklus current untuk **semua tx yang masuk summary lama** (semua wallet user). Implementasi: `GetTransactionsUseCase.Params(start, end, limit cukup besar)` lalu sum type — atau use case baru `GetPeriodTotalsUseCase` di domain (lebih bersih, tes mudah).

`limit`: pakai 200+ atau tanpa limit jika DAO mendukung; jika LIMIT memotong, total salah. Pertimbangkan query **tanpa limit** / limit tinggi khusus totals, atau DAO `SUM` (additive). **Keputusan 14a:** `GetPeriodTotalsUseCase` → repo `observeTotals(start, end)` **atau** observe list dengan limit 500 + dokumentasi risiko. Lebih baik DAO:

```sql
SELECT type, SUM(amount) FROM transactions
WHERE dateEpochMs BETWEEN :start AND :end
GROUP BY type
```

Additive `TransactionDao`, tidak pecah observe list.

Prior siklus untuk `%`: totals periode `selected - 1`.

---

## 13. Pemetaan UI → State / Use Case

### Settings

| UI | State / use case |
|----|------------------|
| Subtitle kartu | `ObservePeriodPreferences` → format |
| Simpan sheet | `SetCycleStartDayUseCase(day)` validasi 1–28 |
| Preview sheet | `FinancePeriod.containing(today, draftDay)` |

### Family

| UI | Sumber |
|----|--------|
| Label stepper | `format(period, startDay)` P6/P7 |
| Query txs | Instant range `period` + prior period |
| Budgets | `period.periodKey` |
| `canEditBudgets` | owner ∧ wallet ∧ `period.contains(today)` |
| Next enabled | `period < currentPeriod` (banding `start` date) |

### History

| Chip | Range |
|------|--------|
| Semua | null–null |
| 7 hari | tetap |
| Periode ini | siklus containing today |
| Custom | tetap 13b |

### Dashboard

| Field | Sumber 14a |
|-------|------------|
| `personalBalance` / `familyBalance` | `GetWalletSummary` — tidak berubah |
| `incomeTotal` / `expenseTotal` | totals siklus current |
| `monthChangeLabel` | net current vs prior siklus; copy boleh “periode ini” jika `startDay != 1` |

---

## 14. Task Breakdown Detail

**Jangan mulai sebelum 13 merge.**

### 14a — Task 1: `FinancePeriod` + `PeriodBounds` siklus + tes

- `containing`, `plusPeriods`, `periodKey`, clamp Feb.
- Tes tabel §12.2.
- `startDay=1` ≡ `ofYearMonth`.

### 14a — Task 2: DataStore + use case

- Proto, serializer, module Hilt, mapper 0→1.
- `SetCycleStartDayUseCase`: reject <1 or >28.
- Tes mapper + use case.

### 14a — Task 3: Settings UI

- Kartu + sheet + preview.
- Preview light/dark.
- Tes VM save.

### 14a — Task 4: Family konsumsi pref

- `flatMapLatest` prefs + selected period.
- Default selected = `containing(today)`.
- Guard authoring `contains(today)`.
- Tes: startDay 25, today 23 Agu → label 25 Jul–24 Agu; next disabled; prev = 25 Jun–24 Jul.

### 14a — Task 5: History preset

- Current = siklus; copy chip.
- Tes Params Instant.

### 14a — Task 6: Dashboard totals

- DAO sum atau use case list.
- Tes totals; saldo tidak berubah.

### 14a — Task 7: `startDay=1` regresi

- Manual + tes: perilaku = Phase 13.

### 14b — Task 8: `monthKey` siklus

- Repo baca `cycleStartDay`.
- Tes 26 Jul + 25 → `2026-08`; startDay 1 + 26 Jul → `2026-07`.

### 14a/14b — Task 9: compile

```
./gradlew :core:common:testDebugUnitTest
./gradlew :core:domain:testDebugUnitTest
./gradlew :core:data:testDevDebugUnitTest
./gradlew :features:settings:testDevDebugUnitTest
./gradlew :features:family:testDevDebugUnitTest
./gradlew :features:transaction:testDevDebugUnitTest
./gradlew :features:dashboard:testDevDebugUnitTest
./gradlew assembleDevDebug
```

---

## 15. Acceptance Criteria

### 15.1 14a

- [ ] Default install: `cycleStartDay=1`; Family/History/Dashboard **sama** Phase 13.
- [ ] Settings: set 25, persist; kill app; tetap 25.
- [ ] Invalid 0/29 ditolak, pref tidak rusak.
- [ ] 23 Agu 2026 + day 25: Family = 25 Jul–24 Agu; expense 26 Jul masuk donut; expense 25 Agu **tidak**.
- [ ] Next tidak masuk 25 Agu–24 Sep sebelum 25 Agu.
- [ ] Owner hanya bisa Adjust Targets di periode yang mengandung today.
- [ ] History “Periode ini” = range yang sama.
- [ ] Dashboard income/expense ikut siklus; **saldo** tidak berubah saat ganti day.
- [ ] Custom / 7 hari History tidak rusak.
- [ ] Sheet Settings preview sesuai today + draft day.
- [ ] Rotation di sheet: draft tidak hilang (`rememberSaveable`).

### 15.2 14b

- [ ] Add expense 26 Jul, startDay 25 → budget `2026-08` (family matching tetap familyId).
- [ ] startDay 1 → tetap `2026-07` untuk tx 26 Jul.

### 15.3 Non-regresi

- [ ] Sign out / family membership Settings tidak rusak.
- [ ] New Entry tanggal tidak terpengaruh.
- [ ] Visibility saldo wallet tetap.

---

## 16. Catatan Arsitektur & Konvensi

- Features tidak saling depend: pref di `:core:domain` + DataStore; Family/History/Dashboard observe use case yang sama.
- `cycleStartDay` **Int**, bukan `LocalDate`.
- Zone: `systemDefault()`, sama Phase 13.
- `CancellationException` rethrow di save pref.
- Screen Settings stateless; sheet boleh local draft sampai Save.
- Jangan hitung siklus di Composable (selain preview sheet dari callback/state).

Repo transaksi (14b) membaca pref: tetap di `:core:data`, bukan feature.

---

## 17. Dependency Graph

```
Settings sheet → SetCycleStartDayUseCase → DataStore
                                            │
ObservePeriodPreferences ───────────────────┤
                                            ▼
                              PeriodBounds.containing(startDay)
                                            │
                    ┌───────────────────────┼───────────────────────┐
                    ▼                       ▼                       ▼
              Family stepper          History “Periode ini”    Dashboard totals
              + budget periodKey
                    │
                    └─ 14b monthKey(tx) ─► Budget.month
```

---

## 18. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| Mulai 14 sebelum 13 | Dobel definisi bulan | DoR ketat |
| `YearMonth.minusMonths` vs siklus 25–24 | Prev/next salah 1 hari | Stepper hanya `plusPeriods` |
| Hari 31 | Crash Feb | Cap 28 + clamp |
| Budget key vs kalender lama | Limit “hilang” setelah ganti 1→25 | Copy Settings; tidak migrate |
| Dashboard masih `CategorySummary` | Angka beda dari Family | 14a totals dari txs/DAO sum |
| LIMIT 50 pada totals | Income/expense terpotong | DAO `SUM` atau limit tinggi khusus |
| Pref beda antar anggota family | Donut beda di dua HP | Dokumentasikan MVP; future family field |
| 14b tanpa inject pref | monthKey tetap kalender | Task 8 wajib baca store |
| Label “Agustus” untuk 25 Jul–24 Agu | Menyesatkan | P7 range dates |

---

## 19. Urutan Pengerjaan yang Disarankan

1. Selesai & QA Phase 13.
2. Task 1 util (tes dulu, tanpa UI).
3. Task 2 persist.
4. Task 3 Settings (user sudah bisa set, filter belum berubah → default 1).
5. Task 4–6 konsumen.
6. Task 7 regresi `startDay=1`.
7. Task 8 (14b) matching tulis.
8. `assembleDevDebug`.

Jangan ship Settings yang menulis pref jika Family masih `YearMonth` kalender-only — user pilih 25 tidak kelihatan efeknya. Minimal Task 4+5 dalam PR yang sama dengan Settings, **atau** hide kartu sampai konsumen siap. **Keputusan:** satu PR 14a (Settings + konsumen); 14b boleh PR kedua segera setelahnya.

---

## 20. Relasi ke Phase Lain

| Phase | Relasi |
|-------|--------|
| **13** | **Prasyarat wajib.** 13 = filter kalender + range History. 14 = definisi “bulan” yang bisa bergeser |
| 7 | Pola DataStore Settings |
| 11 | `Budget.month` tetap string `yyyy-MM`; 14 hanya mengubah **arti kunci** + matching tulis |
| 4 | Dashboard totals ikut siklus; saldo tidak |
| 5 | History preset current |
| Future | `FamilyGroup.cycleStartDay`, sync Firestore pref, migrasi budget |

Phase 13 P2 (“Family = bulan penuh kalender”) **di-override** oleh 14 hanya jika `cycleStartDay != 1`.

---

## 21. Rencana Commit

```
[FEAT] Add FinancePeriod cycle bounds and PeriodPreferences store
[FEAT] Add payday cycle picker on settings
[FEAT] Drive Family History and Dashboard filters from cycle start day
[FEAT] Match budget monthKey to payday cycle
[TEST] Cover cycle containing dates and startDay 1 regression
[DOCS] Mark Phase 14 payday cycle implemented
```

---

## 22. Manual Test Plan

### 22.1 Settings + default 1

| # | Langkah | Expected |
|---|---------|----------|
| 1 | Install / pref kosong | Kartu “tanggal 1”; Family = bulan kalender Phase 13 |
| 2 | Buka sheet, pilih 25, jangan simpan | Pref tetap 1 |
| 3 | Simpan 25, force-stop | Tetap 25 |
| 4 | Pilih 1 lagi | Kembali kalender penuh |

### 22.2 Siklus 25 (tanggal uji ~23 Agu 2026; geser jika QA di bulan lain)

| # | Langkah | Expected |
|---|---------|----------|
| 5 | Family stepper | `25 Jul – 24 Agu 2026` (atau setara today) |
| 6 | Tx bertanggal 26 Jul | Masuk donut/history Family periode ini |
| 7 | Tx 25 Agu | **Tidak** masuk periode 25 Jul–24 Agu |
| 8 | Next | Disabled sampai today ≥ 25 Agu |
| 9 | Prev | 25 Jun – 24 Jul |
| 10 | History chip Periode ini | Sama dengan range Family current |
| 11 | History Custom | Tidak berubah |
| 12 | Dashboard income/expense | Ikut siklus; **saldo** sama seperti sebelum ganti day |
| 13 | Owner Adjust Targets di current | Buka; `Budget.month` = `periodKey` akhir (14b: `2026-08` untuk 25 Jul–24 Agu) |
| 14 | Dark theme sheet | Token benar |

### 22.3 14b

| # | Langkah | Expected |
|---|---------|----------|
| 15 | startDay 25, add expense 26 Jul family | Progress budget kunci Agustus siklus, bukan hanya bar dari mapper |

### Verify

Lihat perintah Task 9.
