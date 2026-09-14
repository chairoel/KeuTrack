# Phase 19 — Filter History by Created-By User

> **Modul target:** `:core:domain` + `:core:data` (param `userId` di observe list + SUM) → `:features:transaction` (chip penulis + VM)  
> **Estimasi:** ~0.8–1.2 hari · **19a** ~0.3 hari (query) · **19b** ~0.4–0.6 hari (VM + chip) · **19c** ~0.2 hari (tes + preview)  
> **Prasyarat:** Phase 13b ✅ (chip periode + empty filter) · Phase 15 ✅ (totals `SUM` mirror filter list) · Phase 17e ✅ (`userId` = penulis; `addedByName` = label) · Phase 18 swipe **tidak** memblokir  
> **Status:** **19a + 19b done** (2026-09-13) — query + chip penulis History. **19c** tes VM + `assembleDevDebug` belum.  
> **Hasil akhir:** Di Riwayat, user bisa memfilter transaksi menurut **penulis** (`Transaction.userId`). Chip **Semua** / **Saya** / nama anggota. List **dan** kartu PEMASUKAN / PENGELUARAN memakai filter yang sama di Room. Default tetap Semua penulis.  
> **Asal-usul:** History keluarga menampilkan tx banyak anggota (`authorLabel` Phase 17e; breakdown Family sudah group by `userId`). Periode (13b) sudah ada; filter penulis belum. Phase 16 §1 menunda “filter tipe/kategori” — **bukan** filter penulis; 19 mengisi gap atribusi.  
> **Tidak memblokir Phase 18c:** swipe / hapus list tetap. `assembleDevDebug` 18c boleh digabung QA 19.

---

## Progress

| Slice | Task | Status |
|-------|------|--------|
| 19a | Task 1–2 — `userId` di DAO / repo / use case + tes forward param | **Done** (2026-09-13) |
| 19b | Task 3–5 — VM + `HistoryAuthorBar` + empty copy | **Done** (2026-09-13) |
| 19c | Task 6 — tes VM + preview + `assembleDevDebug` | **Not started** |

**Terakhir dikerjakan:** 19b — chip Semua / Saya / nama di History (P9), persist `authorUserId`, empty copy penulis/periode, list + totals memakai `userId` yang sama.

**Berikutnya:** 19c — tes VM §14 Task 6 + `assembleDevDebug`. Preview bar / Family / empty author sudah ada.

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
12. [Desain Query & Sumber Anggota](#12-desain-query--sumber-anggota)
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

History sudah bisa:

| Permukaan | Perilaku (sekarang) |
|-----------|---------------------|
| Scope All / Personal / Family | Route args → `walletId` / `familyId` |
| Chip periode | Semua / 7 hari / Periode ini / Custom → `startDate` / `endDate` |
| Totals | `GetPeriodTotalsUseCase` mirror filter list, **tanpa** LIMIT |
| Attribution row | `authorLabel` = `addedByName` jika `userId != currentUser` |
| ACL | Swipe / Ubah / Hapus hanya `canEdit` (`userId == currentUid`) |

Yang belum: “tampilkan hanya transaksi yang **dicatat oleh** anggota X”.

`GetTransactionsUseCase.Params` sudah punya `type` / `categoryId` (belum dipakai History). **Tidak** ada `userId`. DAO `observeFiltered` / `observeSumsByType` juga belum.

Pelajaran Phase 13: **jangan** `items.filter { it.userId == … }` setelah `LIMIT` 50/200. Tx lama penulis itu hilang dari list padahal ada di Room. Totals Phase 15 juga harus ikut — kalau hanya list yang difilter, kartu PEMASUKAN / PENGELUARAN tetap angka semua orang.

**Tujuan 19a — Query:**

1. `observeFiltered` dan `observeSumsByType` menerima `userId: String?` (`null` = semua penulis).
2. `GetTransactionsUseCase.Params` dan `GetPeriodTotalsUseCase.Params` meneruskan field yang sama.
3. Pemanggil lama (`userId` default `null`) tidak berubah semantik.

**Tujuan 19b — History UI:**

4. Chip bar kedua **penulis** di bawah `HistoryPeriodBar` (pola visual yang sama).
5. Opsi: **Semua** (default) + **Saya** + nama anggota keluarga.
6. Pilihan masuk `SavedStateHandle` (rotation).
7. Empty state membedakan “belum ada transaksi” vs “tidak ada dari penulis / periode ini”.
8. `revealedId` / dialog hapus tetap reset jika id hilang dari `items` (sudah ada `LaunchedEffect(visibleItemIds)`).

**Tujuan 19c — Verifikasi:**

9. Tes VM: pilih penulis → **kedua** use case dapat `userId`; Semua → `null`; restore handle; Personal tanpa bar.
10. Preview: Family + chip terpilih; empty filtered-by-author.
11. `assembleDevDebug`.

**Bukan tujuan Phase 19:**

- Field baru `createdBy` / kolom Room / Firestore
- Filter `addedByName` (string)
- Filter tipe / kategori (utang Phase 16 — phase terpisah)
- Pagination / “showing N of M”
- Chip penulis di Dashboard recent atau Family insights list
- Query Firestore `whereEqualTo("userId")` untuk History (tetap Room-only)

---

## 2. Inventory — Apa yang Sudah Ada

### Domain / data

| Item | Lokasi | Status vs Phase 19 |
|------|--------|-------------------|
| `Transaction.userId` | `core/domain/.../model/Transaction.kt` | Penulis stabil; **kunci filter** |
| `Transaction.addedByName` | sama | Snapshot nama; **label saja** |
| `Transaction.createdAt` | sama | Timestamp; **bukan** user |
| `GetTransactionsUseCase.Params` | domain | wallet / family / type / category / range / limit — **tanpa** `userId` |
| `GetPeriodTotalsUseCase.Params` | domain | wallet / family / range — **tanpa** `userId` |
| `TransactionDao.observeFiltered` | Room | `(:x IS NULL OR …)` untuk wallet/family/type/category/date + LIMIT |
| `TransactionDao.observeSumsByType` | Room | Mirror tanpa type/category/LIMIT — **tanpa** `userId` |
| `TransactionEntity` | Room | `Index("userId")` sudah ada; **jangan** ubah schema tabel |
| `FamilyGroup.memberIds` / `memberNames` | domain | Sumber chip |
| `FamilyRepository.observeCurrentFamily()` | domain | Dipakai Family / Dashboard / Settings; History **belum** |

### Feature transaction

| Item | Lokasi | Status vs Phase 19 |
|------|--------|-------------------|
| `TransactionHistoryViewModel` | `.../history/` | `HistoryQuery` = wallet / family / period; persist periode |
| `HistoryPeriodBar` | components | Pola chip yang ditiru |
| `HistoryPeriodTotalsRow` | components | Harus ikut `userId` (via VM, bukan UI) |
| `HistoryUIState` | model | `hasActivePeriodFilter`; belum author |
| `HistoryEmptyContent` | Screen | Copy periode saja |
| `TransactionUiMapper.toTransactionRows` | model | `canEdit` + `authorLabel` — **tidak** perlu diubah untuk filter |
| `TransactionHistoryViewModelTest` | test | Verify `Params` periode / scope; stub `getTransactions(any())` |

### Family (baca pola, jangan diubah)

| Item | Catatan |
|------|---------|
| `FamilyUiMapper.memberKey` | `userId` dulu, baru `addedByName` |
| `resolveMemberLabel` | `addedByName` terbaru → `memberNames` → `"Anggota"` |
| Feature graph | `:features:transaction` **tidak** boleh import composable Family |

---

## 3. Keputusan Produk

| # | Keputusan | Pilihan | Alasan |
|---|-----------|---------|--------|
| P1 | Kunci filter | **`Transaction.userId`** | Sama ACL 17e; `addedByName` bisa bentrok/berubah; `createdAt` = waktu |
| P2 | Field baru | **Tidak** (`createdBy` dilarang) | Data sudah ada |
| P3 | Tempat filter | Room `(:userId IS NULL OR userId = :userId)` | Hindari limit-lalu-filter (13) |
| P4 | Totals | **Sama** `userId` di `observeSumsByType` | Kontrak 15 P3: header menjelaskan list |
| P5 | UI | Chip bar kedua, clone `HistoryPeriodBar` | Konsisten; jangan dropdown M3 |
| P6 | Default | **Semua** (`userId = null`) | Tidak mengecilkan list vs baseline |
| P7 | Opsi chip | Semua → **Saya** → anggota lain A–Z | “Saya” selalu `currentUser.uid` |
| P8 | Label | Semua / Saya / `memberNames` | Fallback §12.3; jangan tampilkan raw UID |
| P9 | Kapan tampil | `scope != Personal` **dan** family hidup **dan** ada ≥ 1 anggota selain diri | Personal hampir selalu 1 penulis; 1-orang family = chip sia-sia |
| P10 | Persist | `SavedStateHandle["authorUserId"]` | Sama periode (13 P16) |
| P11 | Anggota keluar | Jika `selectedUserId` tidak di `memberIds` + bukan self → reset Semua | Hindari chip hantu |
| P12 | Empty | Copy khusus penulis / periode / keduanya | Jangan “catat transaksi pertama” jika data ada di penulis lain |
| P13 | Clear | Tombol terpisah: “Semua penulis” vs “Ubah ke Semua” (periode) | Jangan samakan semantik |
| P14 | Sumber anggota | `FamilyRepository.observeCurrentFamily()` | Sama feature lain; **bukan** distinct `userId` dari list terbatas |
| P15 | Use case family | **Tidak** wajib `ObserveCurrentFamilyUseCase` | History sudah inject `UserRepository` langsung; selaras Dashboard/Settings |
| P16 | Scope All + filter Budi | Semua wallet di device milik `userId=Budi` (biasanya tx family Budi) | Jujur terhadap Room; personal Budi tidak di-pull ke device A |
| P17 | Swipe / ACL | Tidak berubah | Filter tidak mengizinkan edit tx orang lain |
| P18 | Copy | Hardcoded ID di file (bukan `strings.xml`) | Pola History existing |
| P19 | Cloud / rules | **Tidak** ada kerja Firestore | History tetap baca Room |
| P20 | Ship | **19a + 19b + 19c satu PR** | Jangan merge param `userId` tanpa konsumen History |
| P21 | Komponen | Feature-local `HistoryAuthorBar` | Jangan naik `:core:designsystem` (satu konsumen) |
| P22 | Filter tipe/kategori | **Bukan 19** | Utang 16 tetap phase sendiri |

Phase 13 P5 (default Semua tanggal) **tetap**. Phase 15 P3 (totals = filter list) **diperluas** dengan `userId`.

---

## 4. Scope — Apa yang Dikerjakan

### 19a — Query `userId`

1. DAO: tambah `userId` nullable di `observeFiltered` dan `observeSumsByType`.
2. `TransactionLocalDataSource` + Impl, `TransactionRepository` + Impl.
3. `GetTransactionsUseCase.Params.userId` dan `GetPeriodTotalsUseCase.Params.userId` (default `null`).
4. Tes: param diteruskan; default `null` = semua penulis. Update `verify` mockk yang enumerasi argumen (breaking signature).

### 19b — History

5. `selectedUserId: MutableStateFlow<String?>` + persist handle.
6. `HistoryQuery.userId`; `transactionParams` / `periodTotalsParams` meneruskan.
7. Combine `observeCurrentFamily()` → `authorOptions` di `HistoryUIState`.
8. `HistoryAuthorBar` + Screen + Routing callbacks.
9. Empty copy P12/P13; `hasActiveAuthorFilter`.
10. `onAuthorSelected` / `onClearAuthorFilter`; reset handle saat clear.

### 19c — Tes + preview

11. Tes VM §14 Task 6.
12. Preview Screen: Family + chip Budi; empty author filter.
13. Preview `HistoryAuthorBar` light/dark.
14. `assembleDevDebug`.

---

## 5. Scope — Apa yang TIDAK Dikerjakan

| Item | Alasan |
|------|--------|
| Kolom / field `createdBy` | P2 |
| Filter `addedByName` LIKE | P1 |
| Filter type / category UI | P22 |
| Pagination / ubah `HISTORY_LIMIT` | 13 P19 |
| Chip di Dashboard recent / Family 5-row | Satu permukaan: History full-screen |
| Firestore query / index baru | P19 |
| Rules Console | Tidak berubah |
| `ObserveCurrentFamilyUseCase` wajib | P15 |
| i18n `strings.xml` | P18 |
| Auth / splash / Settings / Family invite | Protected / di luar |
| Mengubah `canEdit` / swipe 18 | P17 |
| Deep link `?author=` | Nice-to-have, bukan DoD |

---

## 6. Prasyarat (Definition of Ready)

- [x] Phase 13b: `HistoryPeriodBar` + `SavedStateHandle` periode + empty filter
- [x] Phase 15: `GetPeriodTotalsUseCase.Params` + `observeSumsByType` scoped
- [x] `observeFiltered` pola `(:x IS NULL OR …)`
- [x] `Transaction.userId` + `Index("userId")`
- [x] `FamilyGroup.memberIds` / `memberNames` + `observeCurrentFamily()`
- [x] History scope All / Personal / Family
- [x] Phase 17e `authorLabel` / `canEdit` (label row; bukan filter)
- [ ] **Bukan DoR:** Phase 18c `assembleDevDebug` — 19 boleh paralel

---

## 7. File Referensi (Read-Only)

| File | Kenapa |
|------|--------|
| [`PHASE_13_FAMILY_AND_HISTORY_PERIOD_FILTER.md`](./PHASE_13_FAMILY_AND_HISTORY_PERIOD_FILTER.md) | Limit-lalu-filter; persist; empty copy |
| [`PHASE_15_HISTORY_INCOME_EXPENSE_TOTALS.md`](./PHASE_15_HISTORY_INCOME_EXPENSE_TOTALS.md) | Totals **wajib** mirror filter list |
| [`PHASE_17_TRANSACTION_FIRESTORE_UPDATE_AND_DELETE.md`](./PHASE_17_TRANSACTION_FIRESTORE_UPDATE_AND_DELETE.md) | `userId` = penulis |
| `TransactionDao.observeFiltered` | Pola SQL yang ditiru |
| `TransactionHistoryViewModel.HistoryQuery` / `transactionParams` | Titik sisip `userId` |
| `HistoryPeriodBar.kt` | Visual chip |
| `FamilyRepository.observeCurrentFamily()` | Sumber anggota |
| `FamilyUiMapper.resolveMemberLabel` | Fallback nama (baca; **jangan** import) |
| `TransactionHistoryViewModelTest` | Pola turbine + `verify { getTransactions(match { … }) }` |

---

## 8. File yang TIDAK BOLEH Diubah

- `features/auth/**`, `features/splashscreen/**`
- Domain user/auth + `UserRepository` / `UserRepositoryImpl` (boleh **baca** `getCurrentUser()` seperti sekarang)
- `User.kt` / `AuthResult.kt` / `TokenResult.kt`
- `build-plugin/**`, `settings.gradle.kts`, `gradle.properties`, `local.properties`
- Schema tabel Room `transactions` (hanya **parameter query**)
- Firestore DS / rules / sync / outbox
- `features/dashboard/**`, `features/family/**`, `features/settings/**` (Family hanya dibaca lewat `:core:domain`)
- New Entry / keypad / swipe komponen (kecuali Screen sisip bar)
- `TransactionUiMapper.toTransactionRows` — kecuali bug label yang ketemu (seharusnya tidak)
- Token warna / `Colors.kt` Atelier

Boleh menambah **parameter default** di repository/use case. Jangan pecah pemanggil Dashboard `getPeriodTotals` selain menambah argumen default `userId = null` (call-site Dashboard **tidak** wajib diubah jika default di `Params`).

---

## 9. File yang BOLEH Diubah / Dibuat

### 19a — core

| Path | Aksi |
|------|------|
| `core/data/.../db/dao/TransactionDao.kt` | `userId` di kedua query |
| `core/data/.../datasource/local/TransactionLocalDataSource.kt` + Impl | Signature ikut |
| `core/domain/.../repository/TransactionRepository.kt` | `observeTransactions` + `observePeriodTotals` + `userId` |
| `core/data/.../repository/TransactionRepositoryImpl.kt` | Teruskan param |
| `core/domain/.../usecase/GetTransactionsUseCase.kt` | `Params.userId` |
| `core/domain/.../usecase/GetPeriodTotalsUseCase.kt` | `Params.userId` |
| `core/domain/src/test/.../GetTransactionsUseCaseTest.kt` | Forward + update `verify` named args |
| `core/domain/src/test/.../GetPeriodTotalsUseCaseTest.kt` | Sama |
| `core/data/src/test/.../TransactionRepositoryImplTest.kt` | `observeFiltered` / `observeSumsByType` dapat `userId` |

### 19b — transaction

| Path | Aksi |
|------|------|
| `.../model/HistoryUIState.kt` | `authorUserId`, `authorOptions`, `hasActiveAuthorFilter` |
| `.../model/HistoryAuthorOption.kt` (atau di `HistoryPeriod.kt` / UIState file) | **Baru** — `userId: String?`, `label: String` |
| `.../history/TransactionHistoryViewModel.kt` | State + family flow + params |
| `.../history/TransactionHistoryScreen.kt` | Bar + empty copy |
| `.../history/TransactionHistoryRouting.kt` | `onAuthorSelected` / `onClearAuthorFilter` |
| `.../components/HistoryAuthorBar.kt` | **Baru** |

### 19c

| Path | Aksi |
|------|------|
| `TransactionHistoryViewModelTest.kt` | Tes author + handle; mock `FamilyRepository` |
| Preview History / `HistoryAuthorBar` | Light/dark + empty author |

Tidak ada route baru. Argumen nav `familyOnly` / `personalOnly` **tidak** berubah.

---

## 10. Struktur File Target

```
core/domain/.../usecase/
├── GetTransactionsUseCase.kt     ← Params.userId
└── GetPeriodTotalsUseCase.kt     ← Params.userId

core/data/.../db/dao/TransactionDao.kt
    └── observeFiltered / observeSumsByType + userId

features/transaction/.../presentation/
├── components/
│   ├── HistoryPeriodBar.kt       ← tidak pindah
│   ├── HistoryAuthorBar.kt       ← BARU
│   └── HistoryPeriodTotalsRow.kt ← tidak berubah (angka dari state)
├── history/
│   ├── TransactionHistoryScreen.kt
│   ├── TransactionHistoryRouting.kt
│   └── TransactionHistoryViewModel.kt
└── model/
    ├── HistoryUIState.kt
    └── HistoryAuthorOption.kt    ← BARU (boleh digabung file UIState)
```

Tidak ada file di `:app`, `:features:family`, Firestore, atau schema Room baru.

---

## 11. Desain UX

Urutan vertikal `TransactionHistoryScreen` setelah 19b:

```
KeuTrackTopBar (Riwayat / Personal / Keluarga)
HistoryPeriodBar          ← tidak pindah
HistoryAuthorBar          ← BARU; hide jika P9 false
HistoryPeriodTotalsRow    ← angka ikut penulis
list / empty / spinner
```

`HistoryAuthorBar` **sticky** di Column (sejajar periode + totals), tidak di dalam `LazyColumn`.

### 11.1 Chip

Visual = `PeriodChip` (pill, `bodyBold12`, selected = `semantic.primary` + teks putih). **Boleh** extract `HistoryFilterChip` privat bersama jika duplikasi mengganggu; **jangan** wajib di 19a. Default: copy-paste pola `HistoryPeriodBar` ke `HistoryAuthorBar` (satu konsumen, cepat).

```
[ Semua ]  [ Saya ]  [ Budi ]  [ Siti ]
```

- Horizontal scroll, spacing `8.dp`.
- Tidak ada caption di bawah bar penulis (nama sudah di chip).
- Personal / tanpa family / family 1 orang: **jangan** render bar (P9).

### 11.2 Empty

Perluas `HistoryEmptyContent`. `filtered` sekarang:

```
hasActivePeriodFilter || hasActiveAuthorFilter
```

| Kondisi | Judul | Body | CTA filter |
|---------|-------|------|------------|
| Tidak ada filter | copy scope existing | existing | — |
| Periode saja | `Tidak ada transaksi di periode ini` | `Coba ubah filter tanggal.` | `Ubah ke Semua` → `onClearPeriodFilter` |
| Penulis saja | `Tidak ada transaksi dari {label}` | `Coba ubah filter penulis.` | `Semua penulis` → `onClearAuthorFilter` |
| Periode + penulis | `Tidak ada transaksi dari {label} di periode ini` | `Coba ubah filter penulis atau tanggal.` | Kedua tombol, periode dulu lalu penulis |
| Selalu | — | — | `Tambah transaksi` tetap (Tertiary jika ada CTA filter) |

`{label}` = label chip terpilih (`Saya` / nama), bukan UID.

Preview wajib: empty author-only (tambahan ke preview empty periode existing).

### 11.3 Totals

Tidak ada caption penulis di `HistoryPeriodTotalsRow`. Angka **berubah** saat chip penulis berubah. Caption periode (15) **tetap**.

### 11.4 Swipe

Tidak berubah. Ganti filter → `items` berubah → `LaunchedEffect(visibleItemIds)` menutup reveal / dialog jika id hilang.

### 11.5 a11y

`contentDescription` tidak wajib di chip (teks terlihat). Jangan ikon-only.

---

## 12. Desain Query & Sumber Anggota

### 12.1 SQL

Tambah **satu** klausa di kedua query:

```sql
AND (:userId IS NULL OR userId = :userId)
```

Posisi: setelah `familyId`, sebelum `type` (list) / sebelum `startMs` (SUM). Semantik sama filter existing.

`null` Kotlin → bind SQL NULL → klausa lolos (semua penulis).

Jangan `AND userId = :userId` tanpa guard NULL — itu mematahkan default Semua.

### 12.2 Signature (sketsa)

```kotlin
// GetTransactionsUseCase.Params — field baru di akhir, default null
val userId: String? = null,

// GetPeriodTotalsUseCase.Params
val userId: String? = null,

fun observeTransactions(
    walletId: String? = null,
    familyId: String? = null,
    type: TransactionType? = null,
    categoryId: String? = null,
    startDate: Instant? = null,
    endDate: Instant? = null,
    limit: Int = 50,
    userId: String? = null,   // akhir, default null
)
```

`userId` di **akhir** meminimalkan pecah named-arg di tes. Tetap update `verify { observeTransactions(..., userId = null) }` yang menulis semua parameter.

Blank string: VM mengirim `null` (`takeIf { it.isNotBlank() }`). DAO tidak perlu treat `""` sebagai Semua.

### 12.3 Chip options

```kotlin
data class HistoryAuthorOption(
    val userId: String?, // null = Semua
    val label: String,
)
```

Bangun di VM (bukan Screen):

1. Jika P9 false → `authorOptions = emptyList()`; paksa `selectedUserId = null`.
2. Else:
   - `Semua` (`userId = null`, label `Semua`)
   - `Saya` (`currentUser.uid`, label `Saya`)
   - `family.memberIds` minus current uid, distinct, skip blank
   - Label anggota: `memberNames[id]?.takeIf { it.isNotBlank() } ?: "Anggota"`
   - Sort anggota lain by `label` (locale `id-ID`)

Jangan pakai `addedByName` dari list History terbatas untuk membangun chip (limit 50/200 = anggota yang hanya punya tx lama tidak muncul). `memberNames` adalah sumber keanggotaan.

Jika `memberNames` kosong (family lama): chip tetap ada (id dari `memberIds`) dengan label `Anggota`. Dua orang tanpa nama = dua chip “Anggota” — diterima MVP; jangan disambiguasi UID di UI.

### 12.4 `HistoryQuery`

```kotlin
private data class HistoryQuery(
    val walletId: String? = null,
    val familyId: String? = null,
    val userId: String? = null,
    val period: HistoryPeriod,
    val cycleStartDay: Int,
    val canQuery: Boolean,
)
```

`queryContext` menambah `selectedUserId` ke combine yang sudah ada (user / wallet / period). Tetap `flatMapLatest` ke `getTransactions` + `getPeriodTotals`.

Personal/Family blank: `canQuery == false` → list kosong + totals 0; **jangan** query unscoped.

### 12.5 Persist

```
KEY_AUTHOR_USER_ID = "authorUserId"
```

- Semua: hapus key atau simpan `null` / empty.
- Baca: blank → `null`.
- Jangan persist label.

### 12.6 Timezone

Tidak berubah. Range tanggal tetap `PeriodBounds` + `instantRange`.

---

## 13. Pemetaan UI → State / Use Case

### `HistoryUIState` (additive)

| Field | Sumber | Default |
|-------|--------|---------|
| `authorUserId` | `selectedUserId` | `null` |
| `authorOptions` | family + current user (P9) | `emptyList()` |
| `hasActiveAuthorFilter` | `authorUserId != null` | `false` |
| `incomeTotal` / `expenseTotal` | totals use case **+ userId** | `0L` |
| field existing | tidak berubah | — |

Jangan masukkan `FamilyGroup` utuh ke UIState.

`hasActivePeriodFilter` **tetap** hanya periode. Empty memakai OR (P12).

### Screen / Routing

| Event | Aksi |
|-------|------|
| Tap chip Semua | `onAuthorSelected(null)` |
| Tap Saya / nama | `onAuthorSelected(option.userId)` |
| Empty “Semua penulis” | `onClearAuthorFilter` (= `onAuthorSelected(null)`) |
| Empty “Ubah ke Semua” | `onClearPeriodFilter` (existing) |
| Periode chip | tidak berubah |

```kotlin
// TransactionHistoryRouting
onAuthorSelected = viewModel::onAuthorSelected,
onClearAuthorFilter = viewModel::onClearAuthorFilter,
```

### ViewModel API

```kotlin
fun onAuthorSelected(userId: String?)
fun onClearAuthorFilter()
```

`onAuthorSelected`: trim blank → `null`; persist; tidak sentuh periode.

---

## 14. Task Breakdown Detail

Kerjakan **19a → 19b → 19c**. Jangan 19b sebelum compile domain/data.

### 19a — Task 1: DAO + data + use case

- Klausa SQL P3 di **kedua** query.
- Default `null` di seluruh stack.
- Verify: `./gradlew :core:domain:compileDebugKotlin :core:data:compileDebugKotlin`

### 19a — Task 2: tes query

Minimal:

- `GetTransactionsUseCase` / `GetPeriodTotalsUseCase`: `userId = "u-2"` diteruskan; default invoke → `userId = null`.
- `TransactionRepositoryImplTest`: `observeFiltered` / `observeSumsByType` menerima `userId`.
- Update setiap `verify { observeTransactions(…)` / `observePeriodTotals(…)` yang menulis daftar argumen lengkap.

```
./gradlew :core:domain:testDebugUnitTest --tests "*GetTransactions*" --tests "*GetPeriodTotals*"
./gradlew :core:data:testDevDebugUnitTest --tests "*TransactionRepositoryImpl*"
```

(Sesuaikan flavor tes modul jika berbeda — pola repo: data/feature = `testDevDebugUnitTest`.)

### 19b — Task 3: UIState + VM

- Inject `FamilyRepository`.
- `selectedUserId` + `readAuthor` / `persistAuthor`.
- `queryContext` + params.
- P9 / P11 di builder options.
- Hati-hati arity `combine` (max 5) — nest seperti totals 15.

### 19b — Task 4: `HistoryAuthorBar` + Screen

- Sisip setelah `HistoryPeriodBar`, sebelum totals.
- `if (uiState.authorOptions.isNotEmpty()) HistoryAuthorBar(...)`.
- Empty §11.2.
- Routing wire.

### 19b — Task 5: compile feature

```
./gradlew :features:transaction:compileDevDebugKotlin
```

### 19c — Task 6: tes + preview + assemble

Di `TransactionHistoryViewModelTest`:

1. Family scope + pilih `user-2` → `getTransactions` **dan** `getPeriodTotals` `userId == "user-2"`; `familyId` tetap.
2. Chip Semua setelah filter → kedua use case `userId == null`.
3. Restore `SavedStateHandle["authorUserId"] = "user-2"` → state + params.
4. Personal only → `authorOptions` kosong; `getTransactions` tanpa `userId` (null); bar tidak relevan.
5. Family tanpa `familyId` → tidak query; options kosong.
6. Tes periode / delete / `NotOwner` **jangan** pecah (constructor + mock `FamilyRepository.observeCurrentFamily()`).

Stub default: `every { familyRepo.observeCurrentFamily() } returns flowOf(null)` di `stub()` supaya tes lama hijau. Tes family author: `FamilyGroup` dengan `memberIds` + `memberNames`.

Preview:

- `HistoryAuthorBar` Semua vs Saya selected.
- History Family + options.
- History empty author filter.

```
./gradlew :features:transaction:testDevDebugUnitTest --tests "*TransactionHistoryViewModel*"
./gradlew assembleDevDebug
```

---

## 15. Acceptance Criteria

### 15.1 19a

- [x] `observeFiltered` / `observeSumsByType` hormat `userId` nullable
- [x] Default `null` = semua penulis (semantik lama)
- [x] Use case meneruskan `userId` ke repo
- [x] Tes domain/data hijau setelah signature baru

### 15.2 19b

- [ ] Riwayat Keluarga (2+ anggota): bar Semua / Saya / nama
- [ ] Riwayat All + family 2+ anggota: bar yang sama
- [ ] Riwayat Personal: **tidak** ada bar penulis
- [ ] Default Semua: list + totals = baseline 15 (semua penulis dalam scope/periode)
- [ ] Tap Saya: hanya `userId == currentUid`; totals ikut
- [ ] Tap anggota lain: hanya tx penulis itu; totals ikut; swipe tetap `canEdit == false`
- [ ] Ganti chip periode **tetap** berlaku bersama penulis (AND)
- [ ] Empty penulis saja / periode saja / keduanya — copy §11.2
- [ ] Rotation: chip penulis tetap
- [ ] Anggota terpilih keluar family → reset Semua
- [ ] Swipe / hapus / totals / periode tidak regresi

### 15.3 19c

- [ ] Tes VM §14 Task 6 hijau
- [x] Preview light/dark bar + empty author
- [ ] `assembleDevDebug`
- [ ] Auth / splash / Settings / Family invite tidak disentuh

### Sengaja belum

- [ ] Filter tipe / kategori
- [ ] Chip penulis di Dashboard / Family tab
- [ ] Pagination
- [ ] Disambiguasi dua “Anggota” tanpa `memberNames`

---

## 16. Catatan Arsitektur & Konvensi

- Offline-first: baca Room saja. Firestore hanya sync (tidak disentuh 19).
- Feature → UseCase → Repository. Screen tidak filter list.
- `:features:transaction` **tidak** depend `:features:family`. Anggota lewat `FamilyRepository` di `:core:domain`.
- Amounts `Long`. Filter tidak parse `amountLabel`.
- `CancellationException` tidak relevan di observe; `.catch` History tetap.
- Screen stateless; filter di VM + `HistoryUIState`.
- Kotlin only. `KeuTrackTheme` di semua `@Preview`. Material 2.
- Protected skill: `User*` model/repo impl, auth, splash, build-plugin, gradle root. `UserRepository` **inject existing** di History VM boleh tetap.
- Jangan i18n resources.

---

## 17. Dependency Graph

```
HistoryPeriodBar (tanggal)
HistoryAuthorBar (penulis)          ← BARU
        │
        ▼
TransactionHistoryViewModel
        │
        ├─ UserRepository.getCurrentUser()
        ├─ FamilyRepository.observeCurrentFamily()   ← chip + P9
        ├─ GetTransactionsUseCase(Params + userId + limit) ──► list
        └─ GetPeriodTotalsUseCase(Params + userId, no limit) ──► totals
                    │
                    ▼
              Room observeFiltered / observeSumsByType
              AND (:userId IS NULL OR userId = :userId)
```

Tidak ada method sync baru. Tidak ada route baru.

---

## 18. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| Filter in-memory setelah LIMIT | Tx penulis hilang | P3; review PR larang `items.filter` |
| Totals tanpa `userId` | Header ≠ cerita list | P4; tes VM kedua use case |
| Filter `addedByName` | Dua “Budi” / ganti nama | P1 |
| `verify` mockk pecah | Tes merah massal | Task 2 wajib; `userId` di akhir + default |
| `combine` > 5 Flow | Tidak compile | Nested combine (sudah pola 15/18) |
| Chip dari list terbatas | Anggota tanpa tx baru hilang | P14 `memberIds` |
| Personal + chip Saya | UX kosong/membingungkan | P9 hide |
| Raw UID di chip | Jelek | P8 |
| Family 1 orang | Semua ≡ Saya | P9 hide jika tidak ada anggota lain |
| Signature Dashboard totals | Compile break | Default `userId = null`; jangan ubah Dashboard |
| 19 dicampur swipe 18 | Review kabur | Commit terpisah; boleh satu branch |

---

## 19. Urutan Pengerjaan yang Disarankan

1. Task 1–2 query + tes core (tanpa UI).
2. Task 3 VM + options (boleh tanpa bar dulu; state tesable).
3. Task 4–5 bar + empty + routing.
4. Task 6 tes + preview + `assembleDevDebug` + QA §22.

Jangan merge 19a ke `main` tanpa 19b (API lebih luas tanpa UI). **Keputusan (P20):** satu PR 19a+19b+19c.

Jangan satukan commit dengan swipe 18 / Publish rules 17e.

---

## 20. Relasi ke Phase Lain

| Phase | Relasi |
|-------|--------|
| **13b** | Periode AND penulis; empty filter diperluas, tidak diganti |
| **15** | Totals **wajib** dapat `userId` yang sama dengan list |
| **16** | “Filter tipe/kategori” **tetap** di luar; 19 = penulis saja |
| **17e** | `userId` = penulis; 19 **membaca** field yang sama, tidak longgarkan ACL |
| **18** | Swipe tidak berubah; ganti filter mereset reveal via `visibleItemIds` |
| **6 / 11** | Family `memberNames` / breakdown — pola label; UI Family **tidak** diubah |
| **5 / 12** | New Entry tidak berubah (`userId` tetap di-set saat create) |
| **9** | Tes Compose penuh tidak wajib; 19 wajib tes VM + compile |
| Future | Filter type/category; pagination; hydrate remote by author |

---

## 21. Rencana Commit

Ikuti tag repo. Branch kerja: `feat/history-author-filter`.

```
[DOCS] Add Phase 19 history author filter plan
[FEAT] Add optional userId filter to transaction queries
[FEAT] Filter transaction history by author chips
[TEST] Cover history author filter in view model
```

Commit `[DOCS]` untuk file ini boleh **sekarang** (sebelum implementasi). Commit FEAT hanya saat kode menyusul.

Satu PR boleh beberapa commit di atas. Jangan campur `[FEAT]` swipe 18.

---

## 22. Manual Test Plan

Pakai akun A di keluarga dengan anggota B yang punya ≥ 1 tx family. Idealnya 2 device (A + B). Offline cukup: Room sudah berisi tx kedua penulis (sync 6c/17).

### 22.1 Visibility

| # | Langkah | Expected |
|---|---------|----------|
| 1 | Riwayat Personal (kartu Personal → History) | Tidak ada chip penulis |
| 2 | Riwayat Keluarga, 2+ anggota | Chip Semua / Saya / nama B |
| 3 | Riwayat All (View all) + family 2+ | Chip penulis tampil |
| 4 | User tanpa family, Riwayat All | Tidak ada chip penulis |

### 22.2 Filter + totals

| # | Langkah | Expected |
|---|---------|----------|
| 5 | Default Semua | List + totals = semua penulis (sama sebelum 19) |
| 6 | Tap Saya | Hanya tx A; PEMASUKAN/PENGELUARAN turun sesuai |
| 7 | Tap nama B | Hanya tx B; `authorLabel` B; swipe B tidak reveal (17e) |
| 8 | Chip 7 hari + penulis B | AND: hanya tx B di 7 hari; totals jendela itu |
| 9 | Kembali Semua penulis, periode tetap 7 hari | List semua penulis di 7 hari |

### 22.3 Empty

| # | Langkah | Expected |
|---|---------|----------|
| 10 | Penulis C tanpa tx di periode | Judul “Tidak ada transaksi dari {C}…”; CTA “Semua penulis” |
| 11 | Periode tanpa tx, penulis Semua | Copy periode existing; CTA “Ubah ke Semua” |
| 12 | Tap “Semua penulis” | Baris muncul lagi jika ada tx penulis lain |

### 22.4 Persist + edge

| # | Langkah | Expected |
|---|---------|----------|
| 13 | Pilih B, rotasi | Chip B tetap; list tetap |
| 14 | >50 tx family, filter Saya | List ≤ 50 milik A; totals = SUM penuh A (boleh > jumlah baris) |
| 15 | Swipe hapus tx sendiri saat filter Saya | Row hilang; totals A terkoreksi |
| 16 | New Entry (A) simpan expense family | Muncul di Saya + Semua; tidak di chip B |

### 22.5 Regresi

| # | Langkah | Expected |
|---|---------|----------|
| 17 | Custom from > to | Error range; author tidak ikut rusak |
| 18 | Swipe Ubah / Hapus milik sendiri | Sama Phase 18 |
| 19 | Dashboard / Family tab / Settings | Tidak berubah |
| 20 | Auth / splash | Tidak berubah |

---

## Appendix — Call-site ringkas

**DAO (ide, bukan copy-paste wajib):**

```sql
SELECT * FROM transactions
WHERE (:walletId IS NULL OR walletId = :walletId)
  AND (:familyId IS NULL OR familyId = :familyId)
  AND (:userId IS NULL OR userId = :userId)
  AND (:type IS NULL OR type = :type)
  AND (:categoryId IS NULL OR categoryId = :categoryId)
  AND (:startMs IS NULL OR dateEpochMs >= :startMs)
  AND (:endMs IS NULL OR dateEpochMs <= :endMs)
ORDER BY dateEpochMs DESC
LIMIT :limit
```

`observeSumsByType`: klausa `userId` yang sama; tetap `GROUP BY type`; tanpa LIMIT / type / category.

**History VM params:**

```kotlin
GetTransactionsUseCase.Params(
    walletId = walletId,
    familyId = familyId,
    startDate = range?.start,
    endDate = range?.endInclusive,
    limit = if (scope == HistoryScope.Family) FAMILY_HISTORY_LIMIT else HISTORY_LIMIT,
    userId = userId,
)
GetPeriodTotalsUseCase.Params(
    walletId = walletId,
    familyId = familyId,
    startDate = range?.start,
    endDate = range?.endInclusive,
    userId = userId,
)
```

**Jangan:**

```kotlin
uiState.items.filter { it.authorLabel == selectedName }  // salah: LIMIT + label
transactions.filter { it.addedByName == name }           // salah: bukan userId
```
