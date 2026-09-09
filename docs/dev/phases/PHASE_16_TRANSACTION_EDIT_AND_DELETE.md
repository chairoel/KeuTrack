# Phase 16 — Edit & Delete Transaksi di History

> **Modul target:** `:core:data` (atomic update/delete) → `:core:domain` (use case additive) → `:features:transaction` (form edit + tap History) → `:app` (nav callback)  
> **Estimasi:** ~2–2.5 hari · **16a** ~0.8–1 hari (Room side-effects) · **16b** ~0.3 hari (use case) · **16c** ~0.7–1 hari (UI)  
> **Prasyarat:** Phase 5 ✅ (create + history read-only) · Phase 11 ✅ (`findBudgetForExpense` + increment budget) · Phase 12 ✅ (New Entry keypad) · Phase 15 ✅ (History totals Flow)  
> **Status:** **16a + 16b + 16c done**  
> **Hasil akhir:** User bisa **mengubah** dan **menghapus** item di Riwayat. Tap row membuka form yang sama (`NewEntryScreen`) dalam mode edit. Save mempertahankan `id`. Hapus lewat tombol di form + dialog konfirmasi. Saldo wallet, budget `spent`, category summary, dan totals History **ikut terkoreksi di Room**.  
> **Gate Phase 5 Task 7:** UI edit/delete **dilarang** hidup sebelum 16a hijau. Ini dokumen pengganti “tunda E ke Phase 5.1”.

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
11. [Desain Mutasi Lokal (16a)](#11-desain-mutasi-lokal-16a)
12. [Desain UX](#12-desain-ux)
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

Phase 5 menutup create + history **read-only**. Section E / Task 7 (use case + UI edit/delete) ditunda karena `TransactionRepositoryImpl.update/delete` **belum** mengoreksi side-effect.

Hari ini:

| Permukaan | Perilaku |
|-----------|----------|
| History row | Tampil saja; **tidak** clickable |
| `TransactionRoute(transactionId)` | Route + deep link sudah ada |
| `NewEntryRouting` | `transactionId` di-suppress; form **create-only** |
| `NewEntryViewModel.onSave` | Selalu `UUID` baru + `AddTransactionUseCase` |
| `updateTransaction` | `upsert` + `PENDING` — **tanpa** reverse/apply wallet/budget/summary |
| `deleteTransaction` | Reverse **wallet saja**; budget + summary stale |
| Firestore sync | Strategy A: skip increment jika dokumen sudah ada — edit yang `SYNCED` tidak ter-push dengan benar |

History list dan `HistoryPeriodTotalsRow` (Phase 15) membaca Room langsung, jadi jumlah baris/total **kelihatan** benar setelah delete. Yang rusak: saldo Dashboard, budget Family, category summary.

**Tujuan 16a — Write lokal benar (wajib dulu):**

1. `updateTransaction` atomic: reverse efek transaksi lama, apply efek baru, upsert row `PENDING`.
2. `deleteTransaction` atomic: reverse efek lama, hapus row.
3. Wallet, budget `spent`, dan category summary terkoreksi untuk ganti amount / tipe / kategori / wallet / tanggal (termasuk pindah `monthKey`).
4. Tes repo menutup kasus di atas. **Gate:** 16c tidak mulai sebelum tes ini hijau.

**Tujuan 16b — Use case additive:**

5. `GetTransactionByIdUseCase`, `UpdateTransactionUseCase`, `DeleteTransactionUseCase` di `:core:domain`.
6. Validasi update cermin `Add`. Add / Update / Delete return `TransactionWriteResult` (bukan `Result<Unit>`). Signature repo **tidak** berubah. VM `when` pada sealed itu.

**Tujuan 16c — UI:**

7. Tap row History → `navigateToTransaction(id)`.
8. Form prefill; judul **Edit Transaksi**; save lewat `UpdateTransactionUseCase` (id tetap).
9. Tombol hapus + `AlertDialog` konfirmasi → `DeleteTransactionUseCase` → pop back.
10. List + totals History + saldo Dashboard refresh via Flow Room (tanpa reload manual).

**Bukan tujuan Phase 16:**

- Swipe-to-delete / long-press menu di list
- Screen detail terpisah
- ACL “hanya penulis yang boleh edit”
- Firestore update/delete yang benar (tombstone, reverse remote increment) — **16d / follow-up**
- Pagination History, filter tipe/kategori
- Edit dari Dashboard recent (opsional tipis; jangan blokir)

---

## 2. Inventory — Apa yang Sudah Ada

### Domain / data

| Item | Lokasi | Status vs Phase 16 |
|------|--------|-------------------|
| `TransactionRepository.getTransactionById` | domain + impl | Siap; 16b bungkus use case |
| `updateTransaction` | impl | **Tidak aman** — upsert only. Komentar impl: prefer delete+add sampai di-harden |
| `deleteTransaction` | impl | Wallet reverse saja; **bukan** atomic dengan delete row |
| `addTransaction` | impl | Pola yang **ditiru**: `applyNewTransactionAtomically` + `findBudgetForExpense` + `buildUpdatedSummary` |
| `applyNewTransactionAtomically` | `TransactionLocalDataSource` | Hanya path create |
| `WalletLocalDataSource.applyBalanceDelta` | data | Delta bertanda; dipakai reverse |
| `BudgetLocalDataSource.applySpentDelta` | data | Delta bertanda; 16a wajib pakai angka negatif |
| `findBudgetForExpense` | `BudgetExpenseLookup.kt` | Family vs personal |
| `monthKey` / `PeriodBounds.periodKey` | `TransactionRepositoryImpl` | Siklus payday; edit tanggal bisa pindah period |
| `CategorySummary` keyed `(period, userId)` | existing | Bukan per-wallet; 16a jangan ubah model |
| Use case `Add` / `GetTransactions` | domain | Ada; Add return `TransactionWriteResult` |
| `TransactionWriteResult` | domain model | Ada — sealed Success / Error |
| `Get*` / `Update*` / `Delete*` transaction use case | domain | Ada (16b) |
| Tes `TransactionRepositoryImpl` | `core/data/src/test` | Cover `add` + observe; **tidak** ada update/delete |
| `TransactionFirestoreDataSource.upsertTransactionWithSideEffects` | data | Skip jika doc exists |
| `deleteTransaction` Firestore | data | Hapus dokumen saja; sync **tidak** dipanggil dari delete lokal (row sudah hilang → `getPending()` kosong) |

### Feature transaction

| Item | Lokasi | Status vs Phase 16 |
|------|--------|-------------------|
| `TransactionRoute(transactionId)` | `TransactionNavigation.kt` | Siap |
| `navigateToTransaction(id)` | sama | Siap; History **belum** memanggil |
| Deep link `keutrack://transaction/{transactionId}` | sama | Hidup setelah 16c baca id |
| `NewEntryRouting` | presentation | `@Suppress UNUSED_PARAMETER` |
| `NewEntryViewModel` | presentation | Create-only; tidak baca `SavedStateHandle` |
| `NewEntryUIState` | model | Tidak ada `isEditMode` |
| `NewEntryScreen` judul | `"Transaksi Baru"` | Hardcoded |
| Save CTA | `"Add transaction"` | Hardcoded di form |
| `TransactionHistoryRow` | components | Tidak ada `onClick` |
| `TransactionHistoryScreen` / Routing | history | Tidak ada `onTransactionClick` |
| `KeuTrackCard.onClick` | design system | Sudah ada (Phase 12) |
| Family budget delete CTA | `FamilyBudgetTargetSheet` | Pola `TextButton` error — **adaptasi** |
| History totals Flow | Phase 15 | Otomatis update setelah Room write |

### App nav

| Item | Status |
|------|--------|
| `KeuTrackNavHost.transactionGraph(onBack, onAddTransaction)` | Perlu callback `onEditTransaction` |
| HomeShell → History / FAB | Tidak perlu ubah kecuali recent-tap (opsional) |

---

## 3. Keputusan Produk

| # | Keputusan | Pilihan | Alasan |
|---|-----------|---------|--------|
| P1 | Entry edit | **Reuse `NewEntryScreen`**, bukan detail screen baru | Route + form sudah ada; hindari dua form diverge |
| P2 | Trigger edit | **Tap row** History | Paling jelas; Material 2 belum punya swipe rapi |
| P3 | Trigger delete | **Tombol di form edit** + dialog | Satu tempat mutasi; History VM tetap read-only |
| P4 | Swipe / long-press list | **Tidak** di 16 | Polish belakangan |
| P5 | Identitas row | `id` **tetap** saat update | Sync / deep link / history key |
| P6 | Field yang dipertahankan | `id`, `createdAt`, `userId`, `addedByName` | Edit mengubah isi, bukan kepemilikan |
| P7 | Field yang boleh berubah | type, amount, category, wallet (`familyId` ikut wallet), date, note | Form yang sama dengan create |
| P8 | Tx tidak ketemu | Error + `navigateBack` | Jangan buka form kosong seolah create |
| P9 | Konfirmasi hapus | `AlertDialog` M2: judul + body + Batal / Hapus | Destructive; pola Family budget |
| P10 | Copy UI | Campuran ID/EN seperti form sekarang; judul edit **ID** (`Edit Transaksi`) | Konsisten History; jangan i18n resources |
| P11 | ACL family | Siapa pun yang melihat row boleh edit/hapus | Phase 6C menunda ACL; 16 jangan tambah |
| P12 | Strategi update | **Reverse + apply**, bukan delete+add | Pertahankan `id` (P5); hapus+buat UUID baru merusak deep link |
| P13 | Atomicity | Satu `db.withTransaction` untuk update dan untuk delete | Delete hari ini pecah 2 call |
| P14 | Ship slice | **16a → 16b → 16c**. 16c dilarang tanpa 16a hijau | Gate Phase 5 |
| P15 | Firestore | **Di luar 16.** Lokal benar; cloud boleh stale sampai 16d | `getPending` tidak melihat row yang sudah dihapus; upsert skip-if-exists |
| P16 | Dashboard recent tap | **Opsional** 16c; jangan blokir | History adalah AC utama |
| P17 | Summary helper | Ekstrak delta bertanda dari `buildUpdatedSummary` | Add/update/delete share satu rumus |
| P18 | Period pindah | Jika `monthKey(old) != monthKey(new)`: reverse summary/budget periode lama, apply periode baru | Payday cycle (Phase 14) |
| P19 | Wallet pindah | Reverse saldo wallet lama, apply wallet baru | Dua `applyBalanceDelta` |
| P20 | Merge PR | Satu PR 16a+16b+16c **atau** 16a dulu (boleh merge sendiri), 16b+16c menyusul | Jangan merge 16c tanpa 16a |

---

## 4. Scope — Apa yang Dikerjakan

### 16a — Atomic local write

1. Helper signed-delta: wallet, budget, summary (support `+1` / `-1`).
2. `applyUpdatedTransactionAtomically` + `applyDeletedTransactionAtomically` di local data source (Room `withTransaction`).
3. `TransactionRepositoryImpl.updateTransaction` / `deleteTransaction` memakai helper itu; `addTransaction` boleh refaktor ke helper yang sama **tanpa** ubah perilaku add.
4. Tes repo: amount, tipe, kategori, wallet, period, delete, missing id.

### 16b — Domain

5. `GetTransactionByIdUseCase` — `suspend operator fun invoke(id: String): Transaction?`
6. `UpdateTransactionUseCase` — validasi = Add; `getById` null → `Error.NotFound`; panggil `updateTransaction`; return `TransactionWriteResult`
7. `DeleteTransactionUseCase` — id blank → `Error.MissingId`; else `deleteTransaction(id)`; return `TransactionWriteResult`
8. `AddTransactionUseCase` ikut return `TransactionWriteResult` (satu kontrak write)
9. Tes unit assert sealed variant (bukan `IllegalArgumentException` / `message`)

### 16c — UI + nav

10. `NewEntryViewModel` baca `transactionId` dari `SavedStateHandle`; load; prefill; save/update/delete.
11. `NewEntryUIState` + copy judul/CTA/delete.
12. Dialog hapus di `NewEntryScreen`.
13. `TransactionHistoryRow.onClick` → Routing → `navigateToTransaction(id)`.
14. `transactionGraph` + `KeuTrackNavHost` callback `onEditTransaction`.
15. Tes VM: prefill, update path, delete path, missing id.
16. Preview edit (judul + tombol hapus).

---

## 5. Scope — Apa yang TIDAK Dikerjakan

| Item | Alasan |
|------|--------|
| Swipe-to-delete / contextual menu list | P4 |
| `TransactionDetailRoute` / screen baru | P1 |
| Schema Room baru / kolom tombstone | 16d |
| Ubah Strategy A Firestore / `SyncRepositoryImpl.syncPendingTransactions` | 16d |
| ACL penulis vs member | P11 |
| Edit recurring / split / transfer antar-wallet sebagai tipe baru | Tidak ada di domain |
| i18n `strings.xml` | Pola existing: const di file |
| Auth / splash / `UserRepository` | Protected |
| Ubah `HISTORY_LIMIT` / chip periode / totals query | Phase 13–15 |
| Family budget authoring | Phase 11 |
| Google Sheets export | Future |

---

## 6. Prasyarat (Definition of Ready)

- [x] Phase 5: create form + history list + `TransactionRoute(transactionId)`
- [x] Phase 11: `findBudgetForExpense` + increment `spent` saat add expense
- [x] Phase 12: keypad sheet; `KeuTrackCard.onClick`
- [x] Phase 14: `monthKey` pakai `PeriodBounds.periodKey` + `cycleStartDay`
- [x] Phase 15: History totals dari `GetPeriodTotalsUseCase` (bukan sum `items`)
- [x] `applyNewTransactionAtomically` + tes add di `TransactionRepositoryImplTest`
- [x] Family delete CTA sebagai referensi visual

---

## 7. File Referensi (Read-Only)

| File | Kenapa |
|------|--------|
| `docs/dev/phases/PHASE_5_TRANSACTION_FLOW.md` §E / Task 7 | Gate + use case yang ditunda |
| `docs/dev/phases/PHASE_11_FAMILY_SHARED_BUDGET_AUTHORING.md` | Match budget family vs personal |
| `docs/dev/phases/PHASE_14_PAYDAY_CYCLE_PERIOD_PREFERENCE.md` | `monthKey` siklus |
| `TransactionRepositoryImpl.addTransaction` | Pola write yang 16a tiru |
| `TransactionLocalDataSourceImpl.applyNewTransactionAtomically` | Pola `db.withTransaction` |
| `AddTransactionUseCase` + tes | Cermin 16b |
| `FamilyBudgetTargetSheet` delete `TextButton` | Pola CTA destructive |
| `TransactionNavigation.kt` | Route + deep link sudah siap |
| `NewEntryViewModelTest` | Pola tes form |

---

## 8. File yang TIDAK BOLEH Diubah

- `features/auth/**`, `features/splashscreen/**`
- Domain user/auth + `UserRepository` / `UserRepositoryImpl`
- `build-plugin/**`, `settings.gradle.kts`, `gradle.properties`, `local.properties`
- Schema tabel Room `transactions` / `wallets` / `budgets` / `category_summaries` (hanya method write baru)
- Firestore rules
- Settings siklus UI
- Family invite / QR / budget authoring (kecuali **baca** pola delete)
- `GetPeriodTotalsUseCase` / DAO `observeSumsByType` (Phase 15)

Boleh sentuh Dashboard **hanya** jika mengerjakan P16 (recent tap): callback `onTransactionClick` + `KeuTrackCard.onClick`. Jangan ubah saldo / FAB / sync.

---

## 9. File yang BOLEH Diubah / Dibuat

### 16a — core:data

| Path | Aksi |
|------|------|
| `core/data/.../datasource/local/TransactionLocalDataSource.kt` + Impl | Method atomic update + delete |
| `core/data/.../repository/TransactionRepositoryImpl.kt` | Harden `update` / `delete`; ekstrak helper delta |
| `core/data/src/test/.../TransactionRepositoryImplTest.kt` | Tes reverse/apply |

Interface `TransactionRepository` **tidak** wajib berubah (signature sudah ada).

### 16b — core:domain

| Path | Aksi |
|------|------|
| `core/domain/.../model/TransactionWriteResult.kt` | **Baru** — sealed Success / Error (pola `AuthResult`) |
| `core/domain/.../usecase/AddTransactionUseCase.kt` | Return `TransactionWriteResult` |
| `core/domain/.../usecase/GetTransactionByIdUseCase.kt` | **Baru** |
| `core/domain/.../usecase/UpdateTransactionUseCase.kt` | **Baru** |
| `core/domain/.../usecase/DeleteTransactionUseCase.kt` | **Baru** |
| `core/domain/src/test/.../usecase/*UseCaseTest.kt` | **Baru** + update tes Add |
| `NewEntryViewModel.kt` (create path) | `fold` → `when (TransactionWriteResult)` |

Tidak perlu Hilt `@Binds` baru — `@Inject constructor` seperti Add.

### 16c — feature + app

| Path | Aksi |
|------|------|
| `NewEntryViewModel.kt` | `SavedStateHandle`, load, update, delete |
| `NewEntryUIState.kt` | `isEditMode` / `editingTransactionId` |
| `NewEntryRouting.kt` | Pakai id via VM; wiring `onDelete` |
| `NewEntryScreen.kt` / `NewEntryFormContent.kt` | Judul, CTA, tombol hapus, dialog |
| `TransactionHistoryRow.kt` | `onClick` |
| `TransactionHistoryScreen.kt` / Routing | `onTransactionClick` |
| `TransactionNavigation.kt` | Graph terima `onEditTransaction` |
| `app/.../KeuTrackNavHost.kt` | `navigateToTransaction(id)` |
| Tes `NewEntryViewModelTest` | Prefill / update / delete / missing |
| Tes History (jika ada) | Row click **tidak** wajib di VM (nav saja) |

---

## 10. Struktur File Target

```
core/domain/.../model/
└── TransactionWriteResult.kt             ← BARU (Add / Update / Delete)

core/domain/.../usecase/
├── AddTransactionUseCase.kt              ← return TransactionWriteResult
├── GetTransactionByIdUseCase.kt          ← BARU
├── UpdateTransactionUseCase.kt           ← BARU
└── DeleteTransactionUseCase.kt           ← BARU

core/data/.../datasource/local/
├── TransactionLocalDataSource.kt         ← + applyUpdated / applyDeleted
└── TransactionLocalDataSourceImpl.kt

core/data/.../repository/
└── TransactionRepositoryImpl.kt          ← harden update/delete

features/transaction/.../presentation/
├── NewEntryViewModel.kt                  ← create + edit
├── NewEntryScreen.kt                     ← judul + dialog hapus
├── NewEntryRouting.kt                    ← onDelete
├── components/
│   ├── NewEntryFormContent.kt            ← CTA + tombol hapus
│   └── TransactionHistoryRow.kt          ← onClick
├── history/
│   ├── TransactionHistoryScreen.kt
│   └── TransactionHistoryRouting.kt
└── navigation/TransactionNavigation.kt   ← onEditTransaction

app/.../navigation/KeuTrackNavHost.kt     ← wire id
```

Tidak perlu route baru.

---

## 11. Desain Mutasi Lokal (16a)

Semua angka uang `Long`. Jangan `Double`.

### 11.1 Rumus dasar

```
walletDelta(tx) =
    INCOME  → +amount
    EXPENSE → -amount

budgetDelta(tx) =
    EXPENSE + budget ketemu → +amount   (spent naik)
    selain itu              → 0

summary: income/expense/count per category bertanda ±
```

**Delete:** apply `-walletDelta(old)`, `-budgetDelta(old)`, summary `sign = -1`, lalu hapus row.

**Update:** delete-effects(old) + add-effects(new) + upsert `new.copy(syncStatus = PENDING)` dalam **satu** transaksi Room.

Jangan implementasi update sebagai “hapus row + `addTransaction`” — itu ganti `id` kecuali dipaksa. P12: **id tetap**, jadi reverse/apply in-place.

### 11.2 Kasus wajib

| # | Perubahan | Wallet | Budget | Summary |
|---|-----------|--------|--------|---------|
| 1 | Amount 100 → 150, expense, wallet/kategori/tanggal sama | −50 lagi | `spent` +50 | expense kategori +50 |
| 2 | Expense ↔ Income, amount sama | 2× amount (reverse expense + apply income) | reverse spent lama; income tidak increment budget | pindah income/expense breakdown |
| 3 | Kategori A → B | tidak | reverse budget A (jika ada), apply budget B | count/total A turun, B naik |
| 4 | Wallet personal → family (atau sebaliknya) | reverse wallet lama, apply baru; `familyId` ikut wallet baru | lookup budget **baru** memakai `familyId` baru + `monthKey` baru | summary tetap `(period, userId)` — hanya delta kategori |
| 5 | Tanggal pindah `monthKey` | wallet sama (kecuali #4) | budget period lama vs baru | **dua** summary period: reverse lama, apply baru |
| 6 | Note-only | semua delta 0 | 0 | 0; tetap upsert + `PENDING` |
| 7 | Delete expense ber-budget | reverse wallet | `spent` −amount | summary − |
| 8 | `getById` null pada update/delete | no-op delete (seperti sekarang); update → boleh no-op **atau** lempar. **Pilih:** delete no-op; update no-op + use case tetap `success` **atau** failure “not found”. **Pilih failure** di use case jika repo tidak menulis (cek `getById` dulu) |
| 9 | Add (regresi) | perilaku `applyNewTransactionAtomically` **identik** | | |

### 11.3 API local (usulan)

Nama boleh disesuaikan; kontrak yang penting: **satu** `withTransaction` per operasi.

```kotlin
suspend fun applyUpdatedTransactionAtomically(
    updated: TransactionEntity,
    oldWalletId: String,
    oldWalletDelta: Long,          // reverse = -walletDelta(old)
    newWalletDelta: Long,
    oldBudgetId: String?,
    oldBudgetDelta: Long,          // biasanya -old.amount jika expense
    newBudgetId: String?,
    newBudgetDelta: Long,
    summaryUpserts: List<CategorySummaryEntity>, // 1 atau 2 period
)

suspend fun applyDeletedTransactionAtomically(
    id: String,
    walletId: String,
    walletDelta: Long,             // reverse
    budgetId: String?,
    budgetDelta: Long,
    summaryUpsert: CategorySummaryEntity?,
)
```

`add` tetap memanggil `applyNewTransactionAtomically` (atau delegasi ke updated dengan old-delta 0). Jangan pecah update jadi banyak DAO call di luar `withTransaction`.

### 11.4 Helper di `TransactionRepositoryImpl`

Ekstrak dari `addTransaction` / `buildUpdatedSummary`:

- `walletDeltaFor(tx)`
- `budgetMatch(tx)` → `findBudgetForExpense(monthKey(tx), category, familyId)` hanya jika `EXPENSE`
- `summaryAfterDelta(base, tx, sign)` — `sign = +1` add, `-1` reverse
- `monthKey(tx)` sudah ada

`transactionCount` tidak boleh negatif; jika 0 dan total kategori 0, boleh biarkan entry 0 (jangan over-engineer hapus key). `topExpenseCategoryId` dihitung ulang dari map.

### 11.5 Sync setelah write

Tetap `syncScheduler.enqueueSync()` seperti add. **Ingat P15:** enqueue tidak memperbaiki update remote (skip-if-exists) atau delete remote (row sudah hilang). Jangan anggap 16a “selesai cloud”.

### 11.6 Yang dilarang di 16a

- Menambah kolom `isDeleted` / tabel outbox (itu 16d)
- Memanggil Firestore dari repository
- Mengubah signature `TransactionRepository`

---

## 12. Desain UX

### 12.1 History

```
KeuTrackTopBar
HistoryPeriodBar
HistoryPeriodTotalsRow
LazyColumn
  TransactionHistoryRow    ← tap seluruh kartu
```

- `KeuTrackCard.onClick` di row (sudah didukung DS).
- Jangan icon pensil / overflow di 16 — tap cukup.
- Disabled state tidak perlu (semua row bisa dibuka).

### 12.2 Form edit

Sama seperti create, plus:

| Elemen | Create | Edit |
|--------|--------|------|
| Top bar | `Transaksi Baru` | `Edit Transaksi` |
| Primary CTA | `Add transaction` | `Simpan perubahan` |
| Hapus | tidak ada | `TextButton` error di bawah primary |
| Prefill | default wallet / today / amount 0 | nilai transaksi |
| Loading awal | categories/wallets | + `getById` |

Dialog hapus (M2 `AlertDialog`):

- Judul: `Hapus transaksi?`
- Body: `Transaksi ini akan dihapus dari riwayat. Saldo dan anggaran akan disesuaikan.`
- Dismiss: `Batal`
- Confirm: `Hapus` (`semantic.error`)
- Saat `isSaving` / `isDeleting`: tombol dialog + form disabled

Setelah save atau delete sukses: `navigateBack` (sudah ada pola `navigateBack` flag).

### 12.3 Missing / error

| Kasus | UI |
|-------|-----|
| `transactionId` set, load null | snackbar + pop |
| Validasi amount/wallet/category | snackbar existing (`ERR_AMOUNT` dll.) |
| Repo throw | snackbar `Gagal menyimpan` / `Gagal menghapus` |
| History tap | selalu navigate; error ditangani di form |

### 12.4 Back stack

History → Edit → Back / save / delete → History (satu `popBackStack`).  
Dashboard FAB → Create tetap `navigateToTransaction()` tanpa id.

---

## 13. Pemetaan UI → State / Use Case

### `NewEntryUIState` (additive)

| Field | Sumber |
|-------|--------|
| `isEditMode` | `editingTransactionId != null` |
| `editingTransactionId` | `SavedStateHandle["transactionId"]` |
| field form yang sudah ada | prefill dari `GetTransactionByIdUseCase` |
| `isSaving` | save **atau** delete in-flight (satu flag cukup) |

Jangan expose `Transaction` domain ke Screen.

### `NewEntryViewModel`

```
init / first combine:
  id = savedStateHandle["transactionId"]
  if (!id.isNullOrBlank()) load GetTransactionById
    success → FormDraft(kind, amount, category, wallet, date, note)
              + preserved id, createdAt, userId, addedByName
    null    → error + navigateBack

onSave:
  result = if (isEditMode) UpdateTransactionUseCase(tx dengan id lama)
           else            AddTransactionUseCase(UUID baru)
  when (result) { Success → pop; Error.* → snackbar; NotFound/MissingId → snackbar + pop (edit) }

onDelete:
  only if isEditMode → when (DeleteTransactionUseCase(id)) { … }
```

`familyId` pada save: dari **wallet terpilih** (`WalletOptionUi.familyId`), sama create — bukan dari transaksi lama jika wallet diganti.

### History

Tidak inject use case tulis. Hanya callback nav.

```
transactionGraph(onEditTransaction = { id -> nav.navigateToTransaction(id) })
  HistoryRouting(onTransactionClick = onEditTransaction)
```

---

## 14. Task Breakdown Detail

Kerjakan **16a → 16b → 16c**. Jangan buka klik History sebelum 16a hijau.

### 16a — Task 1: Helper + atomic local API

- Ekstrak signed-delta; jangan duplikasi rumus di 3 tempat.
- Implement `applyUpdated` / `applyDeleted` dengan `db.withTransaction`.
- `updateTransaction`: `getById` → hitung old/new → atomic.
- `deleteTransaction`: `getById` ?: return → atomic reverse + delete.
- Verify: `./gradlew :core:data:compileDebugKotlin`

### 16a — Task 2: Tes repo (gate)

Minimal di `TransactionRepositoryImplTest` (mock local + verify arg, pola tes add yang ada):

1. Update amount expense → `oldWalletDelta` / `newWalletDelta` benar; budget ±.
2. Update expense → income → budget reverse, wallet 2×.
3. Update walletId → dua wallet id di atomic call.
4. Update date pindah `monthKey` (`cycleStartDay` 25, tanggal 26 Jul vs 20 Jul) → dua summary period **atau** verify `monthKey` beda ter-pass ke helper.
5. Delete expense → reverse wallet + budget + summary; `delete` id.
6. Delete missing id → tidak `enqueueSync` (atau tidak tulis).
7. Add tetap: tes existing **jangan** pecah.

Kalau helper murni sulit di-mock, tes impl dengan mock `TransactionLocalDataSource.applyUpdated…` + verify parameter. Jangan bangun test infra Room in-memory baru kecuali sudah ada pola.

Verify: `./gradlew :core:data:testDevDebugUnitTest --tests "*TransactionRepositoryImpl*"`

### 16b — Task 3: Use case + tes

- `TransactionWriteResult` + Add/Update/Delete return sealed itu.
- Tes: validasi update, id blank delete → `MissingId`, getById null → `NotFound`, `CancellationException` rethrow.
- Create path `NewEntryViewModel` `when` pada result (jangan `fold` `Result`).
- Verify: `./gradlew :core:domain:testDevDebugUnitTest --tests "*Transaction*UseCase*"`

### 16c — Task 4: ViewModel edit/delete

- `SavedStateHandle` key harus sama dengan nav arg `transactionId`.
- Save/delete: `when (TransactionWriteResult)` (helper `applyWriteResult` sudah ada di create).
- Tes: tanpa id → add path; dengan id → prefill + update; delete; missing → navigateBack.

### 16c — Task 5: Form UI

- Judul, CTA, tombol hapus, dialog, Preview light/dark edit.
- `BackHandler` overlay (keypad/wallet) **tetap**; dialog hapus di atas overlay.

### 16c — Task 6: History tap + nav

- Row `onClick`.
- Graph + `KeuTrackNavHost`.
- Verify: `./gradlew :features:transaction:testDevDebugUnitTest assembleDevDebug`

### 16c — Task 7 (opsional): Dashboard recent

- `onTransactionClick: (String) -> Unit` dari HomeShell. Jangan import `TransactionRoute` di dashboard.

---

## 15. Acceptance Criteria

### Harus terpenuhi

- [ ] `updateTransaction` mengoreksi wallet + budget + summary untuk kasus §11.2 #1–#6
- [ ] `deleteTransaction` atomic dan mengoreksi wallet + budget + summary
- [ ] Tes 16a Task 2 hijau
- [x] Use case 16b ada (`TransactionWriteResult`); feature **tidak** panggil `TransactionRepository` langsung untuk tulis
- [ ] Tap item History membuka form ter-prefill (amount, tipe, kategori, wallet, tanggal, note)
- [ ] Save edit **tidak** membuat row baru (jumlah list sama; `id` sama)
- [ ] Hapus + konfirmasi: row hilang; totals History berubah; saldo wallet berubah
- [ ] Create path (FAB) **tidak** regresi
- [ ] Preview edit light + dark
- [ ] Auth / splash / Settings / Family invite tidak disentuh

### Sengaja belum

- [ ] Transaksi yang sudah `SYNCED` lalu diedit: **lokal** benar; Firestore dokumen boleh stale (P15)
- [ ] Hapus tx yang sudah `SYNCED`: **lokal** hilang; dokumen Firestore boleh masih ada sampai 16d
- [ ] Swipe-to-delete

---

## 16. Catatan Arsitektur & Konvensi

- Feature → UseCase → Repository. Jangan inject `TransactionRepository` di ViewModel untuk update/delete (New Entry create hari ini sudah lewat `AddTransactionUseCase`).
- Write use case return `TransactionWriteResult`. ViewModel `when (result)` — jangan `fold` / parse `exception.message`.
- `:features:transaction` **tidak** depend dashboard; Dashboard **tidak** import class internal transaction — hanya callback dari `:app`.
- `CancellationException` selalu di-rethrow sebelum `catch (e: Exception)`.
- `CommonDispatcher` di ViewModel.
- Screen stateless; Routing `hiltViewModel()` + `collectAsStateWithLifecycle()`.
- Uang `Long`; tanggal domain `Instant`; form date `LocalDate` lewat mapper existing.
- Kotlin only; `KeuTrackTheme`; Material 2 (`AlertDialog` dari `androidx.compose.material`).
- Jangan sentuh file protected skill (`User*`, auth, splash, build-plugin, gradle root).

---

## 17. Dependency Graph

```
History tap
  → NavController.navigateToTransaction(id)
      → NewEntryViewModel (SavedStateHandle)
          ├─ GetTransactionByIdUseCase → TransactionRepository.getTransactionById
          ├─ UpdateTransactionUseCase  → updateTransaction
          └─ DeleteTransactionUseCase  → deleteTransaction
                → TransactionRepositoryImpl
                    → applyUpdated / applyDeleted (Room withTransaction)
                    → SyncScheduler.enqueueSync()   // best-effort; cloud 16d
```

History list/totals **tidak** di-refresh manual — `observeFiltered` / `observePeriodTotals` emit ulang.

---

## 18. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| UI sebelum 16a | Saldo/budget salah diam-diam | P14 gate; AC tes repo |
| Delete+add (UUID baru) | Duplikat / deep link mati | P5 / P12 |
| Summary period salah saat ganti tanggal | Budget + insight bulan lain | P18 + tes `monthKey` |
| Wallet pindah tanpa reverse lama | Saldo dobel di satu wallet | P19 + tes #3 |
| `buildUpdatedSummary` hanya `+` | Reverse merusak count | P17 signed delta |
| Update pecah di luar `withTransaction` | Crash mid-write | P13 |
| User kira hapus sudah di cloud | Family member lain masih lihat tx | Dokumentasikan P15; 16d; badge sync tidak cukup untuk delete |
| `SavedStateHandle` key ≠ nav arg | Prefill tidak jalan | Task 4: pakai nama `transactionId` |
| Combine Flow + load edit race | Form kedip default lalu prefill | Load id **sebelum** user edit; `isLoading` sampai prefill **atau** draft terisi |
| Family member edit tx orang lain | Diterima 16 (P11) | Jangan tambah ACL diam-diam |
| Tes add pecah karena refaktor helper | Regresi create | Task 2 #7 wajib |

---

## 19. Urutan Pengerjaan yang Disarankan

1. Task 1–2 (16a) — **berhenti jika tes merah.**
2. Task 3 (16b) use case.
3. Task 4–5 form edit/delete (belum tap History juga boleh diuji lewat deep link / `navigateToTransaction(id)`).
4. Task 6 tap History + nav.
5. Task 7 opsional Dashboard.
6. `assembleDevDebug` + QA §22.

Jangan merge 16c ke `main` tanpa 16a. 16b boleh satu commit dengan 16c.

---

## 20. Relasi ke Phase Lain

| Phase | Relasi |
|-------|--------|
| **5** | Create + history; 16 menyelesaikan §E / Task 7 |
| **11** | Budget increment; 16 **wajib** reverse `spent` |
| **12** | Form + keypad; 16 reuse, jangan rollback keypad |
| **13 / 15** | History filter + totals; 16 konsumen Flow yang sama |
| **14** | `monthKey` siklus; 16a kasus tanggal |
| **6C** | Shared family data; 16 **tidak** menambah ACL / sync pull conflict |
| **10** | Personal restore; 16d nanti harus kompatibel pull vs delete lokal |
| **Future 16d** | Firestore update (bukan skip-if-exists) + tombstone/outbox delete + reverse remote increment |
| **9** | Tes UI lebih luas; 16 wajib tes repo + VM saja |

---

## 21. Rencana Commit

Ikuti tag repo (`[FEAT]` / `[TEST]` / `[DOCS]`). Satu PR boleh beberapa commit:

```
[FEAT] Harden transaction update and delete side effects
[TEST] Cover wallet budget summary deltas on update delete
[FEAT] Add get update and delete transaction use cases
[FEAT] Enable edit and delete from transaction history
[TEST] Cover new entry edit and delete view model
[DOCS] Add Phase 16 transaction edit and delete plan
```

Commit `[DOCS]` untuk file plan ini boleh **sekarang** (sebelum implementasi). Commit FEAT hanya saat kode menyusul.

---

## 22. Manual Test Plan

Pakai dua wallet (personal + family) jika memungkinkan. Catat saldo + budget `spent` **sebelum** tiap langkah.

### 22.1 Create tidak regresi

| # | Langkah | Expected |
|---|---------|----------|
| 1 | FAB → isi expense → save | Kembali; row baru di History; saldo turun; totals naik |
| 2 | FAB → income → save | Saldo naik |

### 22.2 Edit dari History

| # | Langkah | Expected |
|---|---------|----------|
| 3 | Tap row | Form judul `Edit Transaksi`; field terisi |
| 4 | Ubah note saja → simpan | `id` sama; saldo/budget **tidak** berubah |
| 5 | Ubah amount expense 100rb → 150rb | Saldo −50rb lagi; totals +50rb; budget `spent` +50rb jika match |
| 6 | Ubah expense → income (amount sama) | Saldo +2× amount; totals pindah kolom; budget `spent` turun |
| 7 | Ganti kategori | Budget lama turun, budget baru naik (jika keduanya ada) |
| 8 | Ganti wallet personal → family | Saldo personal balik; family terserap; `familyId` ikut |
| 9 | Ganti tanggal menyeberang siklus (`cycleStartDay` 25) | Budget/summary period lama vs baru; History chip “Periode ini” mengikuti |
| 10 | Back tanpa simpan | Tidak ada perubahan |

### 22.3 Delete

| # | Langkah | Expected |
|---|---------|----------|
| 11 | Edit → Hapus → Batal | Row tetap |
| 12 | Hapus → konfirmasi | Pop ke History; row hilang; totals turun; saldo balik |
| 13 | Hapus satu-satunya tx di filter | Empty state + totals 0 |

### 22.4 Edge

| # | Langkah | Expected |
|---|---------|----------|
| 14 | Deep link `keutrack://transaction/{id}` | Prefill sama tap History |
| 15 | Deep link id tidak ada | Error + kembali |
| 16 | Offline edit/delete | Sukses lokal; badge PENDING pada row hasil edit |
| 17 | Rotation di form edit | Prefill/draft tidak hilang (`SavedStateHandle` + form state) |

### 22.5 Regresi lain

| # | Langkah | Expected |
|---|---------|----------|
| 18 | History chip + scope Personal/Family | Totals masih scoped (Phase 15); edit tx personal tidak mengubah totals Family |
| 19 | Dashboard saldo + recent | Angka selaras setelah edit/delete (tanpa force-kill) |
| 20 | Family invite / Settings siklus / Auth | Tidak berubah |

### 22.6 Cloud (bukan AC 16 — catat saja)

| # | Langkah | Catatan |
|---|---------|---------|
| 21 | Edit tx yang sudah SYNCED, buka device/akun lain | **Boleh** masih angka lama sampai 16d |
| 22 | Delete tx SYNCED, pull device lain | **Boleh** muncul lagi sampai 16d |

---

## Appendix — Sketsa use case

```kotlin
sealed class TransactionWriteResult {
    data object Success : TransactionWriteResult()

    sealed class Error : TransactionWriteResult() {
        data object MissingId : Error()
        data object InvalidAmount : Error()
        data object MissingWallet : Error()
        data object MissingCategory : Error()
        data object NotFound : Error()
        data class Unknown(val cause: Throwable) : Error()
    }
}

class UpdateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(transaction: Transaction): TransactionWriteResult {
        if (transaction.id.isBlank()) return TransactionWriteResult.Error.MissingId
        if (transaction.amount <= 0) return TransactionWriteResult.Error.InvalidAmount
        if (transaction.walletId.isBlank()) return TransactionWriteResult.Error.MissingWallet
        if (transaction.categoryId.isBlank()) return TransactionWriteResult.Error.MissingCategory
        return try {
            transactionRepository.getTransactionById(transaction.id)
                ?: return TransactionWriteResult.Error.NotFound
            transactionRepository.updateTransaction(transaction)
            TransactionWriteResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TransactionWriteResult.Error.Unknown(e)
        }
    }
}
```

`DeleteTransactionUseCase.invoke(id)`: blank → `Error.MissingId`; else `deleteTransaction(id)`.  
`AddTransactionUseCase` validasi amount/wallet/category yang sama; tidak cek id / not-found.

---

## Appendix — Follow-up 16d (jangan kerjakan di 16)

Agar tidak dilupakan saat sync dijamah:

1. Update remote: jangan `return` saat dokumen exists — tulis field baru + increment **selisih** (atau set absolute wallet dari Room, lebih aman long-term).
2. Delete remote: outbox / tombstone **sebelum** hapus row, atau `pending_deletes` table; `getPending()` hari ini tidak melihat id yang sudah dihapus.
3. Reverse `FieldValue.increment` untuk wallet/budget saat delete/update.
4. Konflik pull Phase 6C/10 vs tombstone.

16a **sengaja** tidak menambah schema supaya slice pertama tetap kecil dan testable.
