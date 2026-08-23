# Phase 13 — Period Filter: Family Month Picker lalu Transaction History Date Range

> **Modul target:** `:features:family` (13a) → `:features:transaction` (13b) · util tipis `:core:common` · pull budget additive `:core:data` (opsional 13a+)  
> **Estimasi:** ~2.5–4 hari total · **13a** ~1.5–2 hari · **13b** ~1–1.5 hari  
> **Prasyarat:** Phase 5 ✅ (history + `GetTransactionsUseCase.Params.startDate/endDate`) · Phase 6 ✅ (Family insights) · Phase 11 ✅ (budget per `yyyy-MM`, authoring bulan berjalan)  
> **Status:** **13a implemented** (Family month picker + `PeriodBounds`) — siap commit. **13b belum** (history date-range filter). Jangan mulai 13b sebelum AC §15.1 + manual §22.1.  
> **Hasil akhir:** Tab Family bisa pilih **bulan**; breakdown / shared budgets / history / insight mengikuti bulan itu. History transaksi bisa filter **rentang tanggal** (preset + custom). Saldo wallet **tidak** ikut filter.  
> **13a as-shipped:** `PeriodBounds.ofYearMonth` / `ofLocalDates`; `FamilyMonthStepper`; `SavedStateHandle["selectedMonth"]`; query `GetTransactionsUseCase.Params(startDate, endDate, limit=200)`; authoring hanya `YearMonth.now()` + owner. Hydrate budget remote bulan lama (**Task 6**) **tidak** dikerjakan.

---

## Daftar Isi

1. [Konteks & Tujuan](#1-konteks--tujuan)
2. [Inventory — Apa yang Sudah Ada](#2-inventory--apa-yang-sudah-ada)
3. [Keputusan Produk](#3-keputusan-produk)
4. [Scope — Apa yang Dikerjakan](#4-scope--apa-yang-dikerjakan)
5. [Scope — Apa yang TIDAK Dikerjakan](#5-scope--apa-yang-tidak-dikerjakan)
6. [Prasyarat (Definition of Ready)](#6-prasyarat-definition-of-ready)
7. [File Referensi (Read-Only)](#7-file-referensi-read-only)
8. [File yang TIDAK BOLEH Diubah](#8-file-yang-tidak-boleh-diubah)
9. [File yang BOLEH Diubah / Dibuat](#9-file-yang-boleh-diubah--dibuat)
10. [Struktur File Target](#10-struktur-file-target)
11. [Desain UX](#11-desain-ux)
12. [Desain Domain, Query & Timezone](#12-desain-domain-query--timezone)
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

**Sebelum 13a:** angka “bulan ini” di Family **hard-code** `YearMonth.now()` di `FamilyViewModel`. User tidak bisa melihat Juli jika sekarang Agustus. History Family di tab itu juga **tidak** ikut bulan (5 transaksi terbaru apa pun tanggalnya).

History full-screen (`TransactionHistoryScreen`) mengambil N transaksi terbaru tanpa `startDate`/`endDate`, padahal DAO/use case **sudah** mendukung range.

Bug laten yang phase ini perbaiki di Family: transaksi di-query `limit = 200` **tanpa** tanggal, lalu difilter `YearMonth` di mapper. Jika 200 transaksi terbaru semuanya bulan ini, **bulan lalu tampak kosong** meski datanya ada di Room.

**Tujuan 13a — Family:**

1. Kontrol **bulan** di `FamilyScreen` (default `YearMonth.now()`).
2. Breakdown, Shared Budgets, history 5 baris, insight, dan label sheet budget mengikuti bulan terpilih.
3. Query Room pakai `startDate`/`endDate` bulan itu (plus bulan − 1 untuk insight), bukan filter in-memory setelah limit global.
4. Authoring budget (Adjust Targets / save / delete) **tetap hanya bulan kalender berjalan** (Phase 11 P4).
5. Saldo wallet, membership, invite, FAB **tidak** berubah.

**Tujuan 13b — History (setelah 13a hijau):**

1. Bar filter periode di `TransactionHistoryScreen` (semua scope: All / Personal / Family).
2. Query `GetTransactionsUseCase` dengan `startDate`/`endDate`.
3. Empty state membedakan “belum pernah ada transaksi” vs “tidak ada di periode ini”.

**Bukan tujuan Phase 13:**

- Filter tanggal di Dashboard income/expense
- Reset / hapus transaksi tiap akhir bulan
- Copy-forward budget otomatis
- Range tanggal bebas di Family (tetap 1 bulan penuh)
- Authoring budget untuk bulan lampau
- Infinite scroll / pagination history
- Composite index Firestore + pull by date (hydrate remote untuk bulan sangat lama)
- `:features:family` import composable dari `:features:transaction` (larangan antar-feature)

---

## 2. Inventory — Apa yang Sudah Ada

### Family (13a)

| Item | Lokasi | Status vs filter |
|------|--------|------------------|
| Bulan hard-code | `FamilyViewModel` `currentMonth` / `currentMonthKey` | Tidak bisa diubah user |
| Transaksi family | `getTransactions(Params(familyId, limit=200))` | Tanpa `startDate`/`endDate` |
| Filter bulan di mapper | `FamilyUiMapper.toUiState` `currentMonthTxs` / `priorMonthTxs` | In-memory setelah limit |
| Budget | `getBudgetProgress(currentMonthKey)` | Hanya `yyyy-MM` sekarang |
| History 5 baris | `familyTransactions.take(HISTORY_LIMIT)` | **Tidak** filter bulan |
| Authoring | `upsertFamilyBudget(..., month = currentMonthKey)` | Benar untuk MVP: bulan berjalan |
| Pull budget remote | `SyncRepositoryImpl.familyBudgetMonths()` | Hanya current + prior |
| Pull tx family | `getByFamilyId(..., limit=200)` | Terbaru dulu, tanpa tanggal |
| Top bar | `KeuTrackTopBar(title = "Family Insights")` | Tidak ada trailing / chip bulan |
| `canEditBudgets` | owner + family wallet | Tidak cek bulan terpilih *(baseline sebelum 13a)* |

**Setelah 13a (kode staged):** stepper `FamilyMonthStepper` setelah banner, sebelum hero. `selectedMonth` di VM + `SavedStateHandle` key `"selectedMonth"`. Query Room pakai `PeriodBounds.ofYearMonth` → `startDate`/`endDate`. History 5 baris, breakdown, shared budgets, insight, `budgetMonthLabel` ikut bulan terpilih. `canEditBudgets` juga butuh `isCurrentCalendarMonth`. Prev cap `FamilyUiMapper.MONTH_LOOK_BACK_LIMIT` (24). Saldo / join / invite / FAB tidak ikut bulan. Pull budget remote tetap current+prior saja.

### History (13b)

| Item | Lokasi | Status vs filter |
|------|--------|------------------|
| `TransactionHistoryScreen` | list / empty / error | Tidak ada UI periode |
| `TransactionHistoryViewModel` | `Params(walletId\|familyId, limit)` | `startDate`/`endDate` = null |
| Scope All / Personal / Family | `TransactionHistoryRoute` | Filter sumber, bukan tanggal |
| `GetTransactionsUseCase.Params` | domain | `startDate`/`endDate` **sudah ada** |
| `TransactionDao.observeFiltered` | `dateEpochMs >= startMs AND <= endMs` | Siap dipakai |
| `DatePickerDialogHost` | `features/transaction/.../DatePickerField.kt` | Boleh dipakai 13b; **jangan** diimpor Family |

### Shared / core

| Item | Catatan |
|------|---------|
| `YearMonth.toString()` | `"yyyy-MM"` — sama dengan `Budget.month` |
| Zone | `ZoneId.systemDefault()` — sama `FamilyUiMapper.toYearMonth` & New Entry |
| `:features:*` tidak boleh saling depend | Chip Family di `:features:family`; bar History di `:features:transaction` |
| Util tanggal | `PeriodBounds` di `:core:common` (**13a done**) — `ofYearMonth` / `ofLocalDates` → `ClosedRange<Instant>` inclusive. 13b memakai `ofLocalDates` untuk preset + custom. |

---

## 3. Keputusan Produk

| # | Keputusan | Pilihan | Alasan |
|---|-----------|---------|--------|
| P1 | Urutan | **13a Family dulu**, 13b History kemudian | Family lebih ketat (budget `yyyy-MM`); History lebih bebas |
| P2 | Family = **bulan penuh**, bukan range hari | `YearMonth` | Satu budget = satu kategori + satu bulan (Phase 11 P3) |
| P3 | History = **range tanggal** + preset | Semua / 7 hari / Bulan ini / Custom dari–sampai | Layar log; tidak terikat `Budget.month` |
| P4 | Default Family | `YearMonth.now()` | Perilaku sekarang (breakdown bulan ini) |
| P5 | Default History | **Semua** (`startDate`/`endDate` null) | Tidak mengecilkan list vs baseline Phase 5 |
| P6 | Data Family yang **ikut** bulan | Breakdown, Shared Budgets, history 5 baris, insight, `budgetMonthLabel` | Satu sumber kebenaran visual |
| P7 | Data Family yang **tidak** ikut | Saldo wallet, membership, invite, kategori picker, FAB | Saldo kumulatif; identitas bukan periode |
| P8 | Authoring budget | Hanya jika `selectedMonth == YearMonth.now()` **dan** owner | Phase 11 P4; bulan lampau = read-only |
| P9 | Insight | Bandingkan **bulan terpilih** vs **bulan terpilih − 1** | Bukan selalu “kalender sekarang vs kemarin” |
| P10 | Navigasi bulan | Prev bebas (sampai ada data / batas 24 bulan); **Next tidak melewati bulan sekarang** | Tidak ada “masa depan” |
| P11 | Query Family | Room `startDate`/`endDate` untuk bulan terpilih **dan** bulan prior (dua observe, atau satu window 2 bulan lalu split) | Hindari limit 200 menelan bulan lama |
| P12 | Empty Family | Kartu tetap render 0 / list kosong; **jangan** anggap user belum join | Beda dengan `showJoinBanner` |
| P13 | Empty History + filter aktif | Copy khusus periode | Jangan suruh “catat transaksi pertama” jika data ada di bulan lain |
| P14 | Timezone | `ZoneId.systemDefault()` | Konsisten dengan mapper & New Entry |
| P15 | Inclusive range | `start` = awal hari/bulan; `end` = akhir hari/bulan (`<= endMs`) | DAO memakai `<= endMs` |
| P16 | Persist filter | `SavedStateHandle` (rotation / process death) | Screen stateless |
| P17 | Shared UI kit | **Tidak** dipaksa di 13a | Family: stepper bulan. 13b: chips + date picker. Extract ke designsystem hanya jika visual benar-benar sama (opsional, akhir 13b) |
| P18 | Remote hydrate bulan lama | **Di luar 13a wajib** | Pull tx 200 terbaru + budget current/prior. 13a+ boleh pull budget `selectedMonth` on demand. Tx Firestore by date = future |
| P19 | Limit query | Family tetap 200 **per window**; History 50 / Family history 200 **dalam range** | Pagination bukan scope |
| P20 | Dashboard | Tidak disentuh | Income/expense dashboard tetap `GetMonthlySummaryUseCase` bulan berjalan |

---

## 4. Scope — Apa yang Dikerjakan

### 13a — FamilyScreen

1. State `selectedMonth: YearMonth` di `FamilyViewModel` (default now; persist handle). **Done.**
2. Util `PeriodBounds` di `:core:common` (start/end Instant dari `YearMonth` / `LocalDate`). **Done.**
3. `flatMapLatest` transaksi + budget berdasarkan `selectedMonth`. **Done.**
4. Mapper: history ikut transaksi bulan terpilih; insight vs prior; `canEditBudgets` && bulan berjalan. **Done.**
5. UI: `FamilyMonthStepper` setelah banner membership/wallet, sebelum hero. **Done.**
6. `onPreviousMonth` / `onNextMonth`; disable Adjust Targets + row-edit di bulan lampau. **Done.**
7. `upsert`/`delete` tetap `YearMonth.now()`; guard di VM jika selected ≠ now. **Done.**
8. Preview + tes mapper/VM. **Done** (unit tes hijau; manual §22.1 masih QA device).
9. (Opsional 13a+) `hydrateFamilyBudgets(familyId, month)` saat pilih bulan di luar current/prior. **Skip** — 13a+ jika QA butuh budget bulan −2 dari cloud.

### 13b — TransactionHistoryScreen

1. State periode: preset + optional `from`/`to` `LocalDate`.
2. `flatMapLatest` `GetTransactionsUseCase` dengan Instant range (null–null untuk Semua).
3. UI chips + custom dua `DatePickerDialogHost`.
4. Validasi `from <= to`; `to` tidak di masa depan.
5. Empty state dua mode.
6. Tes VM (verify `Params.startDate`/`endDate`) + preview.

---

## 5. Scope — Apa yang TIDAK Dikerjakan

- Filter Dashboard / New Entry
- Hapus data tiap ganti bulan
- Weekly `BudgetPeriod` UI
- Shared composable wajib di `:core:designsystem` pada 13a
- Deep link `?month=` / `?from=`
- Grouping header “Agustus 2026” di list history (nice-to-have, bukan DoD)
- Edit/delete transaksi dari history
- WorkManager job reset

---

## 6. Prasyarat (Definition of Ready)

- [x] `GetTransactionsUseCase.Params.startDate` / `endDate` ada dan di-forward ke Room
- [x] `BudgetDao.observeByMonth(month)` ada
- [x] Family insights + budget authoring (Phase 6 + 11) di device
- [x] History All / Personal / Family route hidup
- [x] Tidak menambah dependensi `:features:family` ↔ `:features:transaction`

---

## 7. File Referensi (Read-Only)

Baca sebelum coding; jangan diubah kecuali kolom §9.

| File | Kenapa |
|------|--------|
| `FamilyViewModel.kt` | Hard-code bulan; dua flow yang harus `flatMapLatest` |
| `FamilyUiMapper.kt` | Split current/prior; history unfiltered |
| `FamilyUIState.kt` | Field baru label bulan / `canEditBudgets` |
| `FamilyScreen.kt` | Titik pasang kontrol bulan |
| `FamilyBudgetTargetSheet.kt` | Sudah terima `monthLabel` |
| `TransactionHistoryViewModel.kt` | Params tanpa tanggal |
| `TransactionHistoryScreen.kt` | Top bar + list |
| `GetTransactionsUseCase.kt` | Kontrak range |
| `TransactionDao.kt` | Semantik `startMs`/`endMs` |
| `SyncRepositoryImpl.kt` | `familyBudgetMonths()` hanya 2 bulan |
| `DatePickerDialogHost` | Hanya 13b |
| Phase 11 P3/P4 | Budget = satu bulan; authoring bulan berjalan |

---

## 8. File yang TIDAK BOLEH Diubah

Sesuai skill KeuTrack + batas phase:

- `features/auth/**`, `features/splashscreen/**`
- `core/domain/.../User.kt`, `AuthResult.kt`, `TokenResult.kt`, `UserRepository.kt`
- `core/data/.../UserRepositoryImpl.kt`
- `build-plugin/**`, `settings.gradle.kts`, `gradle.properties`, `local.properties`
- `DashboardViewModel.kt` / `DashboardScreen.kt` (bukan filter dashboard)
- `NewEntryViewModel.kt` (date transaksi baru tetap field form)
- Firestore rules (tidak perlu index baru untuk query Room)

---

## 9. File yang BOLEH Diubah / Dibuat

### 13a

| Path | Aksi |
|------|------|
| `core/common/.../utils/PeriodBounds.kt` | **Done** — `ofYearMonth`, `ofLocalDates` |
| `core/common/src/test/.../PeriodBoundsTest.kt` | **Done** |
| `features/family/.../model/FamilyUIState.kt` | **Done** — `selectedMonthLabel`, `canSelectNextMonth`, `canSelectPreviousMonth` |
| `features/family/.../model/FamilyUiMapper.kt` | **Done** — `canEditBudgets(..., isCurrentCalendarMonth)`; `toUiState(selectedMonthTxs, priorMonthTxs, selectedMonth)` |
| `features/family/.../FamilyViewModel.kt` | **Done** — `selectedMonth` flow; query range; guard authoring |
| `features/family/.../FamilyScreen.kt` | **Done** — `FamilyMonthStepper` + `onPreviousMonth` / `onNextMonth` |
| `features/family/.../components/FamilyMonthStepper.kt` | **Done** — prev / label / next; a11y “Bulan sebelumnya” / “Bulan berikutnya” |
| `features/family/.../model/FamilyInsightsMockContent.kt` | **Done** — preview label bulan |
| `features/family/src/test/.../FamilyUiMapperTest.kt` | **Done** |
| `features/family/src/test/.../FamilyViewModelTest.kt` | **Done** |
| `core/data/.../SyncRepository.kt` + impl | **Skip 13a** — hanya 13a+ pull budget bulan terpilih |
| `docs/dev/phases/PHASE_13_*.md` | Status 13a (file ini) |

### 13b

| Path | Aksi |
|------|------|
| `features/transaction/.../model/HistoryUIState.kt` | Preset, from/to, `hasActivePeriodFilter` |
| `features/transaction/.../model/HistoryPeriod.kt` | **Baru** — sealed/enum preset |
| `features/transaction/.../history/TransactionHistoryViewModel.kt` | Period state + Params range |
| `features/transaction/.../history/TransactionHistoryScreen.kt` | Bar filter + empty copy |
| `features/transaction/.../history/TransactionHistoryRouting.kt` | Callback filter |
| `features/transaction/.../components/HistoryPeriodBar.kt` | **Baru** |
| `features/transaction/src/test/.../TransactionHistoryViewModelTest.kt` | Verify Instant range |
| `DatePickerDialogHost` | Pakai ulang; ubah hanya jika butuh `maxDate` |

---

## 10. Struktur File Target

```
core/common/src/main/kotlin/.../utils/
└── PeriodBounds.kt                         ← 13a DONE (dipakai 13b)

features/family/.../presentation/
├── FamilyViewModel.kt                      ← 13a DONE
├── FamilyScreen.kt                         ← 13a DONE
├── components/
│   └── FamilyMonthStepper.kt               ← 13a DONE
└── model/
    ├── FamilyUIState.kt                    ← 13a DONE
    └── FamilyUiMapper.kt                   ← 13a DONE

features/transaction/.../presentation/
├── history/
│   ├── TransactionHistoryViewModel.kt      ← UBAH (13b)
│   ├── TransactionHistoryScreen.kt         ← UBAH (13b)
│   └── TransactionHistoryRouting.kt        ← UBAH (13b)
├── components/
│   └── HistoryPeriodBar.kt                 ← BARU (13b)
└── model/
    ├── HistoryUIState.kt                   ← UBAH (13b)
    └── HistoryPeriod.kt                    ← BARU (13b)
```

---

## 11. Desain UX

### 13a — Family

Letak: **item pertama** `LazyColumn` setelah banner membership/wallet (jika ada), **sebelum** hero breakdown — atau di bawah `KeuTrackTopBar` full-width. Jangan di dalam kartu donut (sulit ketemu).

Kontrol `FamilyMonthStepper`:

```
[ ‹ ]     Agustus 2026     [ › ]
```

- Label: `DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID"))` — sama `formatBudgetMonth`.
- `‹` = `selectedMonth.minusMonths(1)`.
- `›` enabled hanya jika `selectedMonth < YearMonth.now()`.
- Tidak ada lompat ke masa depan.
- Accessibility: contentDescription “Bulan sebelumnya” / “Bulan berikutnya”.
- Sheet budget: `monthLabel` = bulan terpilih; jika bulan lampau, sheet **tidak** dibuka (`onAdjustTargetsClick` / `onBudgetRowClick` no-op seperti non-owner).
- CTA insight “Atur Target”: `showCta = canEditBudgets` (sudah false di bulan lampau).

Empty bulan tanpa transaksi:

- Breakdown total Rp 0, segment kosong.
- Shared Budgets: baris limit jika ada budget bulan itu; jika tidak, kartu kosong/porsi 0 seperti sekarang.
- History: list kosong di section (bukan banner join).
- Insight: hidden jika `priorExpense <= 0` (logika `savingTogetherInsight` tetap).

### 13b — History

Letak: di bawah top bar, **sticky** (item pertama LazyColumn, atau `Column` { bar; LazyColumn }) supaya filter tidak ikut scroll hilang.

Chips (Material 2, token `KeuTrackTheme`; boleh `FilterChip` / `KeuTrackButton` ghost / `KeuTrackSegmentedControl` jika muat 3 opsi + Custom):

| Chip | Meaning |
|------|---------|
| Semua | `start`/`end` null — default |
| 7 hari | `today-6` 00:00 … `today` 23:59:59.999 |
| Bulan ini | `YearMonth.now()` full range |
| Custom | buka dari, lalu sampai (dua dialog berurutan) |

Custom: tampilkan ringkasan `12 Agu – 20 Agu 2026`. Tap lagi untuk ubah.

Empty:

| Kondisi | Title | Body | CTA |
|---------|-------|------|-----|
| Tidak ada tx di Room untuk scope, filter Semua | copy existing (“Belum ada transaksi…”) | existing | Tambah transaksi |
| Ada kemungkinan data di periode lain / filter aktif | “Tidak ada transaksi di periode ini” | “Coba ubah filter tanggal.” | **Ubah ke Semua** (bukan wajib Tambah) |

Tombol Tambah tetap boleh di empty filter (user memang mau catat).

---

## 12. Desain Domain, Query & Timezone

### 12.1 `PeriodBounds` (`:core:common`)

Pure Kotlin, tanpa Android.

```kotlin
object PeriodBounds {
    fun ofYearMonth(
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ClosedRange<Instant> {
        val start = month.atDay(1).atStartOfDay(zone).toInstant()
        val endExclusive = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
        val endInclusive = endExclusive.minusNanos(1)
        return start..endInclusive
    }

    fun ofLocalDates(
        from: LocalDate,
        to: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ClosedRange<Instant> { ... } // to inclusive end-of-day
}
```

DAO: `dateEpochMs >= startMs AND dateEpochMs <= endMs`. Jangan kirim `endExclusive` tanpa menyesuaikan query.

Tes: Agustus 2026 WIB — start = `2026-08-01T00:00+07:00`, end < `2026-09-01T00:00+07:00`.

### 12.2 Family query (13a)

`selectedMonth` = `MutableStateFlow` / `SavedStateHandle`.

**Transaksi bulan terpilih** (breakdown, budgets spent, history 5):

```
GetTransactionsUseCase.Params(
  familyId = familyId,
  startDate = range.start,
  endDate = range.endInclusive,
  limit = FAMILY_TX_LIMIT, // 200
)
```

**Transaksi bulan prior** (insight saja): params sama dengan `selectedMonth.minusMonths(1)`.

Gabung di mapper: `toUiState(..., selectedMonthTxs, priorMonthTxs, selectedMonth, ...)`.

Hapus filter `it.date.toYearMonth() == currentMonth` **sebagai sumber kebenaran** (boleh defensive filter). History: `selectedMonthTxs.take(5)` bukan `all.take(5)`.

**Budget:**

```
selectedMonth.flatMapLatest { getBudgetProgress(it.toString()) }
```

`getBudgetProgress("2026-07")` → `observeBudgets("2026-07")`.

### 12.3 Authoring (13a)

```kotlin
private fun isCurrentCalendarMonth(selected: YearMonth): Boolean =
    selected == YearMonth.now()

canEditBudgets = FamilyUiMapper.canEditBudgets(user, hasWallet) && isCurrentCalendarMonth

fun onSaveBudget() {
    if (!isCurrentCalendarMonth(selectedMonth.value)) return
    upsert(..., month = YearMonth.now().toString()) // jangan pakai bulan lampau
}
```

`onAdjustTargetsClick` / `onBudgetRowClick` sudah no-op jika `!canEditBudgets`.

### 12.4 History query (13b)

| Preset | startDate | endDate |
|--------|-----------|---------|
| Semua | null | null |
| 7 hari | `PeriodBounds.ofLocalDates(today-6, today).start` | end inclusive |
| Bulan ini | `PeriodBounds.ofYearMonth(YearMonth.now())` | |
| Custom | `ofLocalDates(from, to)` | |

Scope **tetap** AND-ed: Family → `familyId`; Personal → `walletId`; All → keduanya null.

`to` custom > hari ini → clamp ke today. `from > to` → error di bar / swap / ignore submit.

### 12.5 Data yang mungkin kosong di bulan lama (P18)

| Sumber | Isi khas | Efek pilih Juni jika sekarang Agustus |
|--------|----------|----------------------------------------|
| Room txs | Last 200 family pull + local writes | Juni mungkin ada jika masih dalam 200 |
| Room budgets | Hydrate current + prior saja | Juni **hampir pasti** tanpa limit remote |
| Wallet.balance | Kumulatif | Tidak berubah (sengaja) |

Dokumen di UI **jangan** klaim “semua sejarah cloud”. 13a+ opsional: saat `selectedMonth` bukan current/prior, panggil `budgetRemote.getByFamilyId(familyId, month)` lalu upsert Room (skip PENDING/FAILED lokal) — pola sama `hydrateFamilyBudgets`.

---

## 13. Pemetaan UI → State / Use Case

### 13a FamilyUIState (additive)

| Field | Sumber |
|-------|--------|
| `selectedMonthLabel` | `FamilyUiMapper.formatBudgetMonth(selectedMonth)` |
| `canSelectNextMonth` | `selectedMonth < YearMonth.now()` |
| `canSelectPreviousMonth` | `selectedMonth > YearMonth.now().minusMonths(FamilyUiMapper.MONTH_LOOK_BACK_LIMIT)` |
| `monthlyTotalExpense` | expense txs bulan terpilih |
| `spendSegments` | txs bulan terpilih |
| `budgetRows` | budgets `observeByMonth(selected)` + spent txs bulan terpilih |
| `historyRows` | 5 txs bulan terpilih |
| `insight*` | selected vs selected−1 |
| `budgetMonthLabel` | sama `selectedMonthLabel` |
| `canEditBudgets` | owner ∧ wallet ∧ selected == now |
| `hasFamilyWallet` / invite / join | **tidak** dari bulan |

Callback Screen:

- `onPreviousMonth` / `onNextMonth` (atau `onMonthSelected(YearMonth)`)

Routing: `onPreviousMonth = viewModel::onPreviousMonth`, dst.

### 13b HistoryUIState (additive)

| Field | Sumber |
|-------|--------|
| `periodPreset` | `HistoryPeriodPreset` |
| `customFrom` / `customTo` | `LocalDate?` |
| `periodSummaryLabel` | “Semua” / “7 hari” / “Agustus 2026” / “12–20 Agu” |
| `hasActivePeriodFilter` | preset != Semua |
| `items` | query range |
| `scope` | tidak berubah |

Callback: `onPeriodPresetSelected`, `onCustomRangeConfirmed(from, to)`, `onClearPeriodFilter`.

---

## 14. Task Breakdown Detail

Kerjakan berurutan. **Jangan mulai 13b sebelum 13a AC §15.1 hijau** (kecuali util `PeriodBounds` yang memang shared).

### 13a — Task 1: `PeriodBounds` + tes — **Done**

- File baru di `:core:common`.
- Tes: bulan 31 hari, Februari non-kabisat, range 1 hari, `from == to`.
- Verify: `./gradlew :core:common:testDevDebugUnitTest`

### 13a — Task 2: Mapper + UIState — **Done**

- `canEditBudgets(..., isCurrentCalendarMonth: Boolean = true)` — default true agar tes lama tetap lolos, lalu tes baru `false` → tidak edit.
- `toUiState` terima `selectedMonthTxs` (atau tetap filter jika VM masih kirim gabungan — **lebih bersih** VM kirim list sudah ter-scope).
- History dari list bulan terpilih.
- Tes: tx Juli + Agustus → selected Juli → history/segments hanya Juli; insight vs Juni.

### 13a — Task 3: ViewModel query — **Done**

- `SavedStateHandle` key `"selectedMonth"` string `yyyy-MM`.
- `selectedMonth: StateFlow<YearMonth>`.
- `familyTransactionsFlow` = `combine(userFlow, selectedMonth).flatMapLatest`.
- `priorMonthTransactionsFlow` terpisah.
- `getBudgetProgress` `flatMapLatest` dari `selectedMonth`.
- `onPreviousMonth` / `onNextMonth` (no-op next jika already now).
- Tes: `onPreviousMonth` → `verify { getTransactions(match { startDate != null && month == prior }) }`; `getBudgetProgress(priorKey)`.
- Tes: `onNextMonth` dari now tidak geser.

Mockk: `getTransactions` / `getBudgetProgress` harus `every { invoke(any()) } answers { ... }` longgar **atau** stub per Params. Lebih rapat: `slot` / `match`.

### 13a — Task 4: UI stepper — **Done**

- `FamilyMonthStepper` + Preview light/dark.
- Pasang di `FamilyScreen`.
- Next grey/disabled di bulan sekarang (`KeuTrackTheme` / alpha, bukan warna mentah di luar token).

### 13a — Task 5: Authoring guard — **Done**

- Tes VM: selected prior → `onAdjustTargetsClick` tidak buka sheet; `onSaveBudget` tidak panggil upsert.
- UI: `canEdit = false` di kartu budget.

### 13a — Task 6 (opsional): hydrate budget bulan terpilih — **Skip**

- Signature additive `SyncRepository.syncFamilyBudgets(familyId, month: String)` atau perluas `hydrateFamilyBudgets`.
- Panggil dari `FamilyViewModel` saat month change (IO, best-effort, jangan tutup UI).
- Tes `SyncRepositoryImpl` bulan arbitrary.
- Tidak masuk commit 13a. Kerjakan hanya jika QA butuh Shared Budgets untuk bulan di luar current/prior dari cloud.

### 13a — Task 7: compile + tes family — **Done** (unit tes)

```
./gradlew :core:common:testDevDebugUnitTest
./gradlew :features:family:testDevDebugUnitTest
./gradlew :features:family:compileDevDebugKotlin
```

### 13b — Task 8: `HistoryPeriod` + UIState

- Enum/sealed: `All`, `Last7Days`, `CurrentMonth`, `Custom`.
- `hasActivePeriodFilter`.

### 13b — Task 9: ViewModel range

- `period` di `SavedStateHandle` (preset name + optional epoch days).
- `transactionsFlow` `flatMapLatest` period.
- Tes: Last7Days → non-null start/end; All → null/null; Custom from>to ditolak.

### 13b — Task 10: `HistoryPeriodBar` + Screen

- Chips; Custom → dua `DatePickerDialogHost`.
- Empty copy cabang `hasActivePeriodFilter`.
- Preview: filled, empty default, empty filtered, dark.

### 13b — Task 11: compile + tes history

```
./gradlew :features:transaction:testDevDebugUnitTest
./gradlew :features:transaction:compileDevDebugKotlin
```

---

## 15. Acceptance Criteria

### 15.1 13a Family (wajib sebelum 13b)

- [x] Default buka tab Family = bulan kalender sekarang; angka sama seperti sebelum phase (regresi breakdown/budget). *(unit tes VM; regresi visual = §22.1)*
- [x] Tap prev → label bulan − 1; donut, budgets, history 5, insight, sheet label ikut. *(unit tes mapper + VM query range)*
- [x] Tap next di bulan sekarang: tidak pindah.
- [x] Setelah prev, next kembali ke sekarang. *(clamp + `canSelectNextMonth`)*
- [x] Owner di bulan sekarang: Adjust Targets tetap jalan; save menulis `Budget.month = yyyy-MM` sekarang.
- [x] Owner di bulan lampau: tidak ada CTA atur; tap row tidak buka sheet; tidak ada write budget.
- [x] Member: tetap tidak bisa edit (semua bulan).
- [ ] Join banner / saldo / invite **tidak** berubah saat ganti bulan. *(manual §22.1 #9)*
- [ ] ≥ 200 tx bulan ini + beberapa tx bulan lalu di Room: prev ke bulan lalu **masih menampilkan** tx bulan lalu (bukti query range, bukan filter setelah limit). *(manual §22.1 #12)*
- [x] Rotation: bulan terpilih persist. *(SavedStateHandle restore test)*
- [x] Preview light/dark stepper + screen. *(composable Preview ada; cek visual di IDE/device)*
- [x] Unit tes mapper + VM di atas hijau.

Sisa sebelum 13b: manual §22.1 di device.

### 15.2 13b History

- [ ] Default **Semua**: list sama baseline (limit 50/200).
- [ ] **7 hari** / **Bulan ini** memotong list; tx di luar range hilang.
- [ ] Custom `from <= to` inclusive kedua ujung.
- [ ] Personal/Family/All: filter tanggal **dan** scope.
- [ ] Empty + Semua: copy lama + CTA tambah.
- [ ] Empty + filter: copy periode; bisa kembali ke Semua.
- [ ] Rotation: preset + custom persist.
- [ ] Tes VM Params Instant; preview.

### 15.3 Non-regresi

- [ ] Dashboard saldo kumulatif; income/expense tetap bulan berjalan.
- [ ] New Entry date picker tidak rusak.
- [ ] Sync WorkManager / add transaction tidak berubah semantik.

---

## 16. Catatan Arsitektur & Konvensi

- Screen stateless: filter di ViewModel, bukan `remember` sebagai sumber kebenaran (boleh `remember` hanya untuk dialog visibility).
- `CancellationException` rethrow di pull on-demand.
- Amount tetap `Long`.
- `KeuTrackTheme` only.
- Jangan `features:family` depend `features:transaction`.
- `YearMonth.now()` di VM: batas diketahui — app terbuka melewati 1 bulan tanpa recreate VM tetap bulan lama sampai process/tab recreate. Boleh dokumentasikan; jangan Clock injection kecuali tes membutuhkannya (opsional `java.time.Clock` di ctor default `Clock.systemDefaultZone()`).

---

## 17. Dependency Graph

```
PeriodBounds (:core:common)
        │
        ├─► FamilyViewModel ─► GetTransactions(start,end) ─► Room
        │                 └─► GetBudgetProgress(yyyy-MM) ─► Room
        │                 └─► FamilyMonthStepper ─► FamilyScreen
        │
        └─► TransactionHistoryViewModel (13b)
                          └─► GetTransactions(start,end)
                          └─► HistoryPeriodBar ─► TransactionHistoryScreen
```

Tidak ada panah family → transaction.

---

## 18. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| Limit 200 tanpa range (status quo) | Bulan lalu “kosong” palsu | **Wajib** range di query 13a |
| Budget remote hanya 2 bulan | Shared Budgets Juli kosong di September | Copy jujur; 13a+ hydrate; jangan anggap bug UI |
| Tx cloud > 200 | History Family sangat lama tidak di Room | Dokumentasikan; future pull-by-date |
| `endMs` salah (start of next month inclusive) | Tx 1 Sep masuk Agustus | `endInclusive = nextMonthStart - 1ns`; tes PeriodBounds |
| Timezone device vs Firestore | Border bulan geser | Satu `ZoneId.systemDefault()`; sama New Entry |
| Authoring ke bulan lampau | Rusak P4 | Guard VM + `canEditBudgets` |
| Chip History penuh di narrow | Overflow | `horizontalScroll` pada row chips |
| `SavedStateHandle` parse gagal | Crash restore | `YearMonth.parse` try/catch → now |
| Tes VM stub Params ketat | Flaky verify | `match { }` pada Instant di bulan yang sama, bukan eq Instant.now() |
| 13b mulai sebelum 13a | Duplikasi Instant hack | Shared `PeriodBounds` dari Task 1 |

---

## 19. Urutan Pengerjaan yang Disarankan

1. Task 1 `PeriodBounds` (fondasi 13a **dan** 13b).
2. Task 2–5 Family (mapper → VM → stepper → guard) — **shippable 13a**.
3. Manual test §22.1.
4. Task 6 hanya jika QA butuh budget bulan −2 dari cloud.
5. Task 8–10 History.
6. Manual test §22.2.
7. `assembleDevDebug`.

Jangan parallel 13a UI dan 13b UI di PR yang sama kecuali 13a sudah review-able; boleh satu branch `feat/period-filter` dengan dua commit (§21).

---

## 20. Relasi ke Phase Lain

| Phase | Relasi |
|-------|--------|
| 5 | History list; Params tanggal sudah ada, belum di-UI |
| 6 | Insights Family; history 5 baris sekarang unfiltered |
| 11 | Budget `yyyy-MM`; authoring bulan berjalan **dipertahankan** |
| 4 | Dashboard **tidak** ikut phase ini |
| 10 | Restore/pull 200 tx — batas data bulan lama |
| **14** | **Setelah 13 selesai.** Siklus gajian (`cycleStartDay` di Settings), bukan kalender 1–akhir bulan. Lihat `PHASE_14_PAYDAY_CYCLE_PERIOD_PREFERENCE.md` |
| Future | Pagination, Firestore query by date, Dashboard month picker, copy-forward budget |

Phase 11 P4 (“hanya bulan berjalan di MVP”) **tidak dibatalkan**: yang baru adalah **baca** bulan lain, bukan **tulis** limit ke bulan lampau.

---

## 21. Rencana Commit

Ikuti `[TAG]` repo.

```
[FEAT] Add family calendar-month picker for insights
[FEAT] Filter transaction history by date range presets
[TEST] Cover history period Params
```

Commit 13a (kode staged + update status di file ini): PeriodBounds, Family month stepper, mapper/VM, tes. **Jangan** campur 13b.

Commit 13b nanti: history date-range saja.

---

## 22. Manual Test Plan

### 22.1 Family (13a)

Build: `./gradlew :features:family:compileDevDebugKotlin` lalu Run `devDebug`.

| # | Langkah | Expected |
|---|---------|----------|
| 1 | Buka tab Family | Stepper = bulan sekarang; breakdown/budget seperti sebelum patch |
| 2 | Catat 1 expense family bulan ini | Donut & history 5 naik |
| 3 | Prev ke bulan lalu (tanpa tx) | Total 0; history kosong; bukan banner “belum join” |
| 4 | Seed / catat tx bertanggal bulan lalu (New Entry date picker) | Prev: tx muncul di history & donut |
| 5 | Owner + bulan sekarang | Adjust Targets buka; save limit OK |
| 6 | Prev, tap Adjust / row budget | Tidak buka sheet |
| 7 | Next sampai sekarang | Edit kembali aktif |
| 8 | Member akun | Tidak edit di bulan mana pun |
| 9 | Ganti bulan, cek kartu wallet Family di Dashboard | Saldo **sama** |
| 10 | Rotate di bulan lalu | Tetap bulan lalu |
| 11 | Dark theme | Stepper token benar; disabled next terbaca |
| 12 | (Jika data banyak bulan ini) Prev ke bulan dengan tx lama di Room | Tidak kosong palsu |

### 22.2 History (13b)

| # | Langkah | Expected |
|---|---------|----------|
| 1 | Riwayat Semua, chip Semua | List = sebelum patch |
| 2 | Chip 7 hari | Tx > 7 hari hilang |
| 3 | Chip Bulan ini | Hanya `YearMonth.now()` |
| 4 | Custom 1 hari | Hanya tanggal itu |
| 5 | Custom from > to | Tidak apply / error |
| 6 | Personal + Bulan ini | Hanya personal bulan ini |
| 7 | Family + 7 hari | Hanya family 7 hari |
| 8 | Filter ketat, kosong | Copy “periode ini”, bukan “pertamamu” |
| 9 | Kembali Semua | List penuh lagi |
| 10 | Rotate di Custom | Range persist |
| 11 | Empty Semua (akun baru) | Copy + CTA tambah seperti lama |

### Verify compile

```
./gradlew :core:common:testDevDebugUnitTest
./gradlew :features:family:testDevDebugUnitTest
./gradlew :features:transaction:testDevDebugUnitTest
./gradlew assembleDevDebug
```
