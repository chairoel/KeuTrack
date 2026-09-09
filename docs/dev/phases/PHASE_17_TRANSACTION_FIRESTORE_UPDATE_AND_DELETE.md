# Phase 17 — Firestore Update & Delete Transaksi (ex-16d)

> **Modul target:** `:core:data` (outbox + Strategy A harden) → `docs/database/firestore-rules.md` (ACL write family) · domain **additive tipis** (komentar `hasPendingSync` saja)  
> **Estimasi:** ~2–2.5 hari · **17a** ~0.4 hari (outbox lokal) · **17b** ~0.8–1 hari (update remote) · **17c** ~0.6–0.8 hari (delete remote + pull) · **17d** ~0.2 hari (rules)  
> **Prasyarat:** Phase 16a–c ✅ (edit/delete lokal benar) · Phase 2 ✅ (Strategy A skip-if-exists) · Phase 6C ✅ (pull family) · Phase 10 ✅ (pull personal) · Phase 11 ✅ (`findBudgetForExpense`) · Phase 14 ✅ (`PeriodBounds.periodKey`)  
> **Status:** **Not started** — ini spesifikasi lengkap follow-up **16d** di [`PHASE_16_TRANSACTION_EDIT_AND_DELETE.md`](./PHASE_16_TRANSACTION_EDIT_AND_DELETE.md).  
> **Hasil akhir:** Edit/hapus yang sudah benar di Room **ikut benar di Firestore**. Device/akun lain melihat field baru, saldo wallet, dan budget `spent` yang terkoreksi. Hapus tidak “hidup lagi” saat pull Phase 6C/10. UI History / New Entry **tidak** berubah.  
> **Asal-usul 16d:** (1) update remote jangan skip-if-exists — tulis field + increment selisih; (2) delete remote butuh outbox **sebelum** row hilang; (3) reverse `FieldValue.increment` wallet/budget; (4) pull vs tombstone/outbox.

---

## Daftar Isi

1. [Konteks & Tujuan](#1-konteks--tujuan)
2. [Inventory — Apa yang Sudah Ada](#2-inventory--apa-yang-sudah-ada)
3. [Root Cause (Kenapa Cloud Stale)](#3-root-cause-kenapa-cloud-stale)
4. [Keputusan Desain](#4-keputusan-desain)
5. [Scope — Apa yang Dikerjakan](#5-scope--apa-yang-dikerjakan)
6. [Scope — Apa yang TIDAK Dikerjakan](#6-scope--apa-yang-tidak-dikerjakan)
7. [Prasyarat (Definition of Ready)](#7-prasyarat-definition-of-ready)
8. [File Referensi (Read-Only)](#8-file-referensi-read-only)
9. [File yang TIDAK BOLEH Diubah](#9-file-yang-tidak-boleh-diubah)
10. [File yang Diubah / Dibuat](#10-file-yang-diubah--dibuat)
11. [Struktur File Target](#11-struktur-file-target)
12. [Desain Outbox Lokal (17a)](#12-desain-outbox-lokal-17a)
13. [Desain Update Remote (17b)](#13-desain-update-remote-17b)
14. [Desain Delete Remote + Pull (17c)](#14-desain-delete-remote--pull-17c)
15. [Firestore Security Rules (17d)](#15-firestore-security-rules-17d)
16. [Task Breakdown Detail](#16-task-breakdown-detail)
17. [Acceptance Criteria](#17-acceptance-criteria)
18. [Catatan Arsitektur & Konvensi](#18-catatan-arsitektur--konvensi)
19. [Dependency Graph](#19-dependency-graph)
20. [Risiko & Mitigasi](#20-risiko--mitigasi)
21. [Urutan Pengerjaan yang Disarankan](#21-urutan-pengerjaan-yang-disarankan)
22. [Relasi ke Phase Lain](#22-relasi-ke-phase-lain)
23. [Rencana Commit](#23-rencana-commit)
24. [Manual Test Plan](#24-manual-test-plan)

---

## 1. Konteks & Tujuan

Phase 16 menutup edit/hapus **di Room**. `SyncScheduler.enqueueSync()` tetap dipanggil, tetapi jalur Firestore masih Strategy A MVP: **create-only**.

**Sesudah Phase 16 (sebelum 17):**

| Permukaan | Perilaku |
|-----------|----------|
| History tap → edit/hapus | Lokal benar: wallet, budget `spent`, summary, totals |
| `updateTransaction` lokal | Reverse + apply + upsert `PENDING` + enqueue |
| `deleteTransaction` lokal | Reverse + **hapus row** + enqueue |
| `getPending()` | Hanya row `PENDING` / `FAILED` yang **masih ada** |
| `upsertTransactionWithSideEffects` | Jika dokumen Firestore **sudah ada** → `return` (tidak tulis field, tidak increment) |
| `TransactionFirestoreDataSource.deleteTransaction` | `document.delete()` saja; **tidak** dipanggil dari sync setelah hapus lokal |
| Pull 6C / 10 | Upsert semua remote tx; **tidak** menghapus row lokal yang hilang di remote |

Dampak produk (Phase 16 §22.6, sengaja bukan AC):

1. Edit tx yang sudah `SYNCED` → device lain / reinstall tetap lihat **angka lama**.
2. Hapus tx yang sudah `SYNCED` → dokumen Firestore tetap ada → pull **menghidupkan lagi** row di History anggota keluarga / device baru.
3. User mengira badge sync / enqueue = cloud sudah ikut. Tidak.

**Tujuan 17a — Outbox delete (wajib dulu):**

1. Sebelum row transaksi dihapus dari Room, tulis baris outbox `pending_transaction_deletes` dalam **satu** `db.withTransaction`.
2. `getPending()` / History / totals **tidak** perlu melihat tombstone di tabel `transactions`.
3. `hasPendingSync()` true jika outbox tidak kosong (meski tidak ada row `PENDING`).
4. Tes: delete menulis outbox + menghapus row; crash-safety = tidak ada delete tanpa outbox.

**Tujuan 17b — Update remote:**

5. Jangan `return` saat dokumen exists. Tulis field baru. Increment wallet/budget = **selisih snapshot remote vs lokal** (idempotent).
6. Create path (dokumen belum ada) tetap increment penuh seperti hari ini.
7. `monthKey` sync memakai `PeriodBounds.periodKey` + `cycleStartDay` (sama Phase 16a / 14), bukan `yyyy-MM` kalender.
8. Tes: amount, tipe, kategori, wallet, period, note-only, retry (increment 0).

**Tujuan 17c — Delete remote + pull:**

9. `syncPendingTransactions` memproses outbox **lebih dulu**: reverse increment dari snapshot + hapus dokumen; lalu hapus baris outbox.
10. Delete idempotent: dokumen sudah hilang → ack outbox, jangan increment.
11. Pull 6C / 10: **jangan** upsert id yang ada di outbox; **orphan sweep** row lokal `SYNCED` yang masuk jendela pull tetapi id-nya tidak ada di remote.

**Tujuan 17d — Rules:**

12. Anggota family boleh `update` / `delete` dokumen transaksi `familyId` yang sama (cermin Phase 16 P11).
13. Anggota family boleh `update` budget **hanya** field `spent` (cermin wallet `balance`).

**Bukan tujuan Phase 17:**

- UI baru, swipe-to-delete, tap Dashboard recent
- Cloud Function / Strategy B (recompute server-side)
- Tombstone `isDeleted` di dokumen Firestore
- Realtime listener
- ACL “hanya penulis” (itu dibatalkan Phase 16 P11 untuk family)
- Pagination History / filter baru

---

## 2. Inventory — Apa yang Sudah Ada

### Lokal (Phase 16a)

| Item | Lokasi | Status vs Phase 17 |
|------|--------|-------------------|
| `applyUpdatedTransactionAtomically` | `TransactionLocalDataSource` | Tetap; 17 **tidak** mengubah rumus lokal |
| `applyDeletedTransactionAtomically` | sama | **Harden** — sisip outbox sebelum `deleteById` |
| `TransactionRepositoryImpl.update/delete` | data | Update: enqueue cukup. Delete: outbox lewat atomic helper |
| Signed-delta wallet / budget / summary | `TransactionRepositoryImpl` | Tetap sumber kebenaran lokal |
| `monthKey` = `PeriodBounds.periodKey` | `TransactionRepositoryImpl` | Sync **belum** memakai ini |
| Tes `TransactionRepositoryImplTest` | `core/data/src/test` | Cover side-effect lokal; 17 menambah assert outbox |

### Sync / Firestore (Phase 2 + 6C + 10)

| Item | Lokasi | Status vs Phase 17 |
|------|--------|-------------------|
| `upsertTransactionWithSideEffects` | `TransactionFirestoreDataSource` | **Harden** — hilangkan skip-if-exists |
| `deleteTransaction(id)` | sama | **Harden** — reverse increment + delete dalam `runTransaction` |
| `walletDeltaFor` | sama | Reuse; dipakai juga untuk snapshot lama |
| `syncPendingTransactions` | `SyncRepositoryImpl` | **Harden** — deletes dulu, lalu upsert; `monthKey` siklus |
| `hasPendingSync` | sama | **Harden** — `\|\| outbox.isNotEmpty()` |
| `syncFamilyData` / `syncPersonalData` | sama | **Harden** — skip outbox id + orphan sweep |
| `setBalance` setelah recompute pull | sama | Safety net; **bukan** strategi primer update/delete |
| `SyncRepository` interface | domain | Signature **tidak** berubah |
| Tes `SyncRepositoryImplTest` | data test | Pola mockk; 17 menambah kasus update/delete/pull |
| `AppDatabase` version | `1`, `exportSchema = false` | **Bump** ke `2` + `MIGRATION_1_2` |
| `fallbackToDestructiveMigration` | `DatabaseModule` | Tetap last-resort; **jangan** andalkan wipe untuk 17 |

### Rules

| Item | Status vs Phase 17 |
|------|-------------------|
| Tx `update` / `delete` | Owner `userId` saja — anggota family **gagal** sync edit/hapus tx orang lain |
| Wallet `update` | Owner penuh; member hanya `balance` — cukup untuk increment |
| Budget `update` | Owner `userId` saja — increment `spent` oleh member **gagal** (sudah berisiko di create) |

### Feature

| Item | Status |
|------|--------|
| New Entry edit/delete UI | **Tidak** disentuh |
| History tap | **Tidak** disentuh |
| Badge `PENDING` | Tetap; delete tidak punya row jadi badge tidak relevan |

---

## 3. Root Cause (Kenapa Cloud Stale)

### 3.1 Update: skip-if-exists

```43:46:core/data/src/main/kotlin/com/mascill/keutrack/core/data/datasource/firestore/TransactionFirestoreDataSource.kt
            val existing = fsTxn.get(txnRef)
            if (existing.exists()) {
                return@runTransaction
            }
```

Idempotency create (benar untuk retry add). Fatal untuk edit: id sama → remote field + increment **beku** di nilai pertama.

Setelah `runTransaction` return sukses, `syncPendingTransactions` tetap `updateSyncStatus(SYNCED)`. Lokal kelihatan bersih; cloud salah.

### 3.2 Delete: row hilang sebelum outbox

```140:146:core/data/src/main/kotlin/com/mascill/keutrack/core/data/datasource/local/TransactionLocalDataSourceImpl.kt
            applyWalletDelta(walletId, walletDelta)
            applyBudgetDelta(budgetId, budgetDelta)
            if (summaryUpsert != null) {
                categorySummaryDao.upsert(summaryUpsert)
            }
            transactionDao.deleteById(id)
```

`getPending()`:

```70:71:core/data/src/main/kotlin/com/mascill/keutrack/core/data/db/dao/TransactionDao.kt
    @Query("SELECT * FROM transactions WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getPending(): List<TransactionEntity>
```

Enqueue tidak ada yang dikonsumsi. `deleteTransaction` Firestore tidak pernah dipanggil dari `SyncRepositoryImpl`.

### 3.3 Pull menghidupkan mayat

`syncFamilyData` / `syncPersonalData` upsert setiap remote tx kecuali lokal `PENDING`. Tidak ada “remote tidak punya id ini → hapus lokal”. Limit pull 200: tidak boleh menganggap semua id di luar hasil query sudah dihapus.

### 3.4 `monthKey` sync vs write lokal

`TransactionRepositoryImpl.monthKey` memakai payday cycle. `SyncRepositoryImpl.syncPendingTransactions` memakai `DateTimeFormatter.ofPattern("yyyy-MM")`. Jika `cycleStartDay != 1`, budget remote bisa kena dokumen bulan yang salah — edit/hapus akan meng-increment budget yang tidak pernah di-increment saat create. Phase 17 **wajib** menyelaraskan.

---

## 4. Keputusan Desain

| # | Keputusan | Pilihan | Alasan |
|---|-----------|---------|--------|
| P1 | Jejak delete lokal | **Tabel outbox** `pending_transaction_deletes`, **bukan** kolom `isDeleted` di `transactions` | History / `observeSumsByType` / recent **tidak** berubah; menghindari regresi Phase 13–15 |
| P2 | Jejak delete remote | **Hard-delete** dokumen Firestore, **bukan** tombstone `isDeleted: true` | Query pull existing tidak perlu filter baru; storage tidak menumpuk mayat |
| P3 | Kapan outbox ditulis | **Satu** `withTransaction` bersama reverse + `deleteById` | Crash di tengah tidak boleh “row hilang tanpa outbox” |
| P4 | Strategi increment update | **Snapshot-diff** di dalam `runTransaction`: `Δ = effect(local) − effect(remoteSnapshot)` | Idempotent: retry setelah sukses → snapshot sudah = lokal → `Δ = 0`. Multi-writer lebih aman daripada `setBalance` dari Room satu device |
| P5 | `set` absolute wallet dari Room | **Bukan** strategi primer | Race dengan increment anggota lain; Phase 6C/10 `setBalance` dari **sum remote tx** tetap safety net pull |
| P6 | Category summary remote | Tetap **set absolute** dari Room (1 atau 2 period) | Sudah pola create; kurang contended (per `userId`) |
| P7 | Urutan push | **Outbox delete dulu**, baru upsert `getPending()` | Hindari upsert lalu delete id yang sama dalam satu `syncAll` |
| P8 | Create tidak regresi | Dokumen belum ada → increment **penuh** nilai baru (seperti hari ini) | Edit-before-first-sync: remote belum ada, cukup apply baru |
| P9 | Delete belum pernah di remote | `get()` missing → hapus outbox, **jangan** increment | Create PENDING lalu hapus sebelum push |
| P10 | Pull vs outbox | Jangan upsert id yang ada di outbox; jika row sempat ter-hydrate, hapus lagi tanpa enqueue | Device yang sama: pull jangan mengalahkan delete yang belum ter-push |
| P11 | Pull vs device lain | **Orphan sweep**: lokal `SYNCED` yang tanggalnya ≥ tx remote tertua di hasil pull, id tidak ada di remote → reverse lokal **tanpa** outbox | Limit 200: jangan sweep tx lebih tua dari jendela. `PENDING` lokal jangan di-sweep |
| P12 | Interface `SyncRepository` | **Tidak** pecah method baru | Lipat ke `syncPendingTransactions` + `hasPendingSync` + pull existing |
| P13 | Schema Room | Version **2** + `MIGRATION_1_2` `CREATE TABLE` | Jangan andalkan `fallbackToDestructiveMigration` (itu wipe data user) |
| P14 | UI / use case tulis | **Tidak** berubah | 16b/16c sudah cukup; ini phase data |
| P15 | `monthKey` sync | Sama dengan `TransactionRepositoryImpl` (`PeriodBounds` + `cycleStartDay`) | Budget reverse/apply harus kena dokumen yang sama |
| P16 | Budget id di snapshot | Pre-read + `findBudgetForExpense` **sebelum** `runTransaction`; jika snapshot berubah vs pre-read, abort agar retry | `runTransaction` sinkron — tidak bisa query Room di dalamnya. Dokumen tx **tidak** punya `budgetId` hari ini |
| P17 | Rules family tx | Anggota `familyId` boleh update/delete tx shared | Phase 16 P11: siapa yang lihat row boleh edit; tanpa ini sync member → `PERMISSION_DENIED` |
| P18 | Rules family budget | Member boleh update **hanya** `spent` | Cermin wallet `balance`; perlu untuk create **dan** reverse edit/hapus |
| P19 | Konflik dua device edit tx yang sama | Last-write-wins pada field dokumen; increment dari snapshot saat transaksi | Tidak ada CRDT / revision vector di 17 |
| P20 | `updatedAt` di dokumen tx | **Tidak** wajib | Snapshot-diff sudah idempotent; jangan tambah field kalau tidak dipakai |
| P21 | Merge PR | 17a boleh merge sendiri (outbox tanpa consumer remote masih lebih aman daripada sekarang). 17b+17c+17d boleh satu PR. **Jangan** ship 17b tanpa tes retry | Rules (17d) wajib sebelum QA 2 akun |

---

## 5. Scope — Apa yang Dikerjakan

### 17a — Outbox lokal

1. Entity + DAO + register `AppDatabase` v2 + `MIGRATION_1_2`.
2. `applyDeletedTransactionAtomically` menulis outbox lalu hapus row.
3. `hasPendingSync` melihat outbox.
4. Tes repo: delete → outbox ada, row hilang; delete missing id → tidak tulis outbox.

### 17b — Update remote

5. `upsertTransactionWithSideEffects` : exists → `set`/`update` field + increment snapshot-diff (wallet, termasuk pindah wallet; budget, termasuk pindah budget).
6. Not exists → perilaku create (increment penuh).
7. Summary: set absolute 1–2 period dari Room.
8. `syncPendingTransactions` pakai `monthKey` siklus + lookup budget old/new dari pre-read.
9. Tes sync: kasus §13.2; create existing tes **jangan** pecah makna (sekarang harus **menulis** field, increment 0 jika snapshot = lokal).

### 17c — Delete remote + pull

10. API Firestore delete-with-reverse dalam `runTransaction`.
11. `syncPendingTransactions` drain outbox dulu.
12. Pull skip outbox ids + orphan sweep (family + personal).
13. Tes: delete remote; missing doc; pull tidak menghidupkan id outbox; sweep `SYNCED` di jendela; jangan sweep `PENDING` / lebih tua dari jendela.

### 17d — Rules + catatan

14. Update `docs/database/firestore-rules.md` + publish rules di Firebase Console (`keutrack-dev`).
15. Catatan: member increment `spent`; member update/delete tx `familyId`.

---

## 6. Scope — Apa yang TIDAK Dikerjakan

| Item | Alasan |
|------|--------|
| Kolom `isDeleted` / tombstone Firestore | P1 / P2 |
| Cloud Function recompute balance | Strategy B; Phase 2 out of scope |
| UI edit/delete / Dashboard recent tap | Sudah 16c / P16 Phase 16 |
| Ubah signature `TransactionRepository` / use case | P14 |
| Realtime snapshot listener | Phase 6C/10 tetap pull on open |
| Pagination / filter History | Phase 13–15 |
| ACL penulis-only | Bertentangan Phase 16 P11 |
| Auth / splash / `UserRepository` | Protected |
| Google Sheets export | Future |
| Hapus otomatis orphan wallet Firestore | Phase 10 tetap di luar |
| i18n `strings.xml` | Tidak ada copy baru |

---

## 7. Prasyarat (Definition of Ready)

- [x] Phase 16a: `applyUpdated` / `applyDeleted` + tes reverse/apply hijau
- [x] Phase 16b: `UpdateTransactionUseCase` / `DeleteTransactionUseCase` / `TransactionWriteResult`
- [x] Phase 16c: History tap + form edit/hapus (enqueue sudah dipanggil)
- [x] Phase 2: `upsertTransactionWithSideEffects` + `SyncWorker` / `syncAll`
- [x] Phase 6C: `syncFamilyData` skip lokal `PENDING` + recompute wallet
- [x] Phase 10: `syncPersonalData` pola yang sama
- [x] Phase 11: `findBudgetForExpense`
- [x] Phase 14: `PeriodPreferencesRepository` + `PeriodBounds.periodKey`
- [x] Rules wallet member boleh increment `balance` (sudah)

---

## 8. File Referensi (Read-Only)

| File | Kenapa |
|------|--------|
| [`PHASE_16_TRANSACTION_EDIT_AND_DELETE.md`](./PHASE_16_TRANSACTION_EDIT_AND_DELETE.md) Appendix 16d + §11 rumus delta | Kontrak produk; rumus lokal yang 17 **cerminkan** di remote |
| `TransactionFirestoreDataSource.upsertTransactionWithSideEffects` | Skip-if-exists yang diganti |
| `SyncRepositoryImpl.syncPendingTransactions` | Caller increment hari ini (salah untuk update) |
| `SyncRepositoryImpl.syncFamilyData` / `syncPersonalData` | Pola skip PENDING + tempat orphan sweep |
| `TransactionRepositoryImpl` signed-delta + `monthKey` | Rumus yang harus dipakai sync |
| `TransactionLocalDataSourceImpl.applyDeletedTransactionAtomically` | Titik sisip outbox |
| [`PHASE_2_ROOM_AND_REPOSITORY_IMPLEMENTATION.md`](./PHASE_2_ROOM_AND_REPOSITORY_IMPLEMENTATION.md) § Strategi A | Konteks increment + idempotency create |
| [`PHASE_6C_SHARED_FAMILY_DATA_SYNC.md`](./PHASE_6C_SHARED_FAMILY_DATA_SYNC.md) | Pull family |
| [`PHASE_10_PERSONAL_WALLET_RESTORE_SYNC.md`](./PHASE_10_PERSONAL_WALLET_RESTORE_SYNC.md) | Pull personal; jangan `upsertWallet` balance yang sudah di-increment |
| `docs/database/firestore-rules.md` | Owner-only update/delete tx; member wallet `balance` |
| `SyncRepositoryImplTest` | Pola tes mockk yang diikuti |

---

## 9. File yang TIDAK BOLEH Diubah

- `features/auth/**`, `features/splashscreen/**`
- Domain user/auth + `UserRepository` / `UserRepositoryImpl`
- `build-plugin/**`, `settings.gradle.kts`, `gradle.properties`, `local.properties`
- `features/transaction/**` UI / ViewModel / History (kecuali terpaksa tes kompilasi — **jangan**)
- `features/dashboard/**`, `features/family/**`, `features/settings/**`
- Signature `TransactionRepository` / `Add` / `Update` / `Delete` use case
- Schema kolom tabel `transactions` / `wallets` / `budgets` / `category_summaries` (hanya **tabel baru**)
- Settings siklus UI
- Family invite / QR / budget authoring UI
- `GetPeriodTotalsUseCase` / DAO `observeSumsByType` (Phase 15)

Boleh sentuh `docs/database/firestore-rules.md` **hanya** klausa update/delete transaksi + update budget `spent` (17d).

---

## 10. File yang Diubah / Dibuat

### 17a — outbox

| Path | Aksi |
|------|------|
| `core/data/.../db/entity/PendingTransactionDeleteEntity.kt` | **Baru** |
| `core/data/.../db/dao/PendingTransactionDeleteDao.kt` | **Baru** |
| `core/data/.../db/AppDatabase.kt` | Register entity; `version = 2` |
| `core/data/.../db/Migrations.kt` (atau di `AppDatabase`) | `MIGRATION_1_2` |
| `core/data/.../di/DatabaseModule.kt` | `.addMigrations(MIGRATION_1_2)` + provide DAO |
| `TransactionLocalDataSource.kt` + Impl | `getPendingDeletes` / `removePendingDelete`; atomic delete menulis outbox |
| `TransactionRepositoryImpl.deleteTransaction` | Pastikan snapshot outbox lengkap; tetap `enqueueSync()` |
| `TransactionRepositoryImplTest` | Assert outbox |

### 17b / 17c — sync

| Path | Aksi |
|------|------|
| `TransactionFirestoreDataSource.kt` | Update snapshot-diff; delete + reverse |
| `SyncRepositoryImpl.kt` | Drain outbox; `monthKey` siklus; pull skip + sweep |
| `SyncRepository.kt` | Komentar `hasPendingSync` saja (outbox) |
| `SyncRepositoryImplTest` | Update / delete / pull |

`PeriodPreferencesRepository` di-inject ke `SyncRepositoryImpl` (sudah ada di `TransactionRepositoryImpl`).

### 17d

| Path | Aksi |
|------|------|
| `docs/database/firestore-rules.md` | Family tx update/delete; budget `spent`-only |
| Firebase Console | Publish rules `keutrack-dev` |

Interface `SyncRepository` method **tidak** bertambah.

---

## 11. Struktur File Target

```
core/data/.../db/
├── AppDatabase.kt                         ← v2 + pending deletes
├── Migrations.kt                          ← MIGRATION_1_2
├── entity/PendingTransactionDeleteEntity.kt
└── dao/PendingTransactionDeleteDao.kt

core/data/.../datasource/local/
├── TransactionLocalDataSource.kt          ← getPendingDeletes / removePendingDelete
└── TransactionLocalDataSourceImpl.kt      ← outbox di applyDeleted

core/data/.../datasource/firestore/
└── TransactionFirestoreDataSource.kt      ← update diff + delete reverse

core/data/.../repository/
├── TransactionRepositoryImpl.kt           ← snapshot outbox (jika perlu di helper)
└── SyncRepositoryImpl.kt                  ← deletes → upserts; pull sweep

core/data/.../di/DatabaseModule.kt         ← migration + DAO

docs/database/firestore-rules.md           ← 17d
```

Tidak ada route, screen, atau use case baru.

---

## 12. Desain Outbox Lokal (17a)

### 12.1 Kenapa bukan `isDeleted` di `transactions`

Semua query History / recent / `SUM` hari ini:

```sql
SELECT * FROM transactions WHERE …  -- tanpa filter deleted
```

Kolom tombstone memaksa sentuh `TransactionDao.observeFiltered`, `observeRecent`, `observeSumsByType`, plus mapper/domain. Risiko regresi Phase 15 lebih besar daripada satu tabel kecil.

Outbox **tidak** muncul di UI. Row transaksi tetap hilang segera (perilaku 16c).

### 12.2 Entity (usulan)

```kotlin
@Entity(tableName = "pending_transaction_deletes")
data class PendingTransactionDeleteEntity(
    @PrimaryKey val id: String,          // transaction id
    val walletId: String,
    val userId: String,
    val familyId: String?,
    val type: String,
    val amount: Long,
    val categoryId: String,
    val dateEpochMs: Long,
    val queuedAtEpochMs: Long,
    val syncStatus: String,             // PENDING | FAILED
)
```

Field cukup untuk `monthKey` + `findBudgetForExpense` + `walletDeltaFor` jika pre-read remote gagal. Sumber increment yang **diutamakan** tetap snapshot Firestore saat sync (P4 / P16).

Jangan simpan `budgetId` sebagai kebenaran mutlak — budget bisa diganti / dihapus; lookup saat sync lebih aman.

### 12.3 DAO

```kotlin
@Dao
interface PendingTransactionDeleteDao {
    @Query("SELECT * FROM pending_transaction_deletes WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getPending(): List<PendingTransactionDeleteEntity>

    @Query("SELECT id FROM pending_transaction_deletes")
    suspend fun getAllIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingTransactionDeleteEntity)

    @Query("DELETE FROM pending_transaction_deletes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE pending_transaction_deletes SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
```

### 12.4 Atomic delete (kontrak)

Nama parameter boleh disesuaikan; yang wajib: **outbox + reverse + hapus row** satu `withTransaction`.

```kotlin
suspend fun applyDeletedTransactionAtomically(
    id: String,
    walletId: String,
    walletDelta: Long,
    budgetId: String?,
    budgetDelta: Long,
    summaryUpsert: CategorySummaryEntity?,
    pendingDelete: PendingTransactionDeleteEntity, // NEW
)
```

Urutan di dalam transaksi Room:

1. `pendingDeleteDao.upsert(pendingDelete)`
2. reverse wallet / budget / summary (seperti sekarang)
3. `transactionDao.deleteById(id)`

Jangan upsert outbox **setelah** `withTransaction` selesai.

### 12.5 Kapan selalu tulis outbox

**Selalu**, termasuk lokal masih `PENDING`. Alasan: `PENDING` setelah edit tx yang pernah `SYNCED` berarti remote **masih punya** dokumen versi lama. Skip outbox = cloud yatim.

Delete id yang `getById` null: tetap no-op (tidak outbox, tidak enqueue) — sama 16a.

### 12.6 `hasPendingSync`

```kotlin
walletLocal.getPending().isNotEmpty() ||
    budgetLocal.getPending().isNotEmpty() ||
    transactionLocal.getPending().isNotEmpty() ||
    transactionLocal.getPendingDeletes().isNotEmpty()
```

Tanpa ini, hapus-only (tidak ada row `PENDING`) tidak pernah di-retry dari Dashboard.

### 12.7 Migrasi

```sql
CREATE TABLE IF NOT EXISTS pending_transaction_deletes (
    id TEXT NOT NULL,
    walletId TEXT NOT NULL,
    userId TEXT NOT NULL,
    familyId TEXT,
    type TEXT NOT NULL,
    amount INTEGER NOT NULL,
    categoryId TEXT NOT NULL,
    dateEpochMs INTEGER NOT NULL,
    queuedAtEpochMs INTEGER NOT NULL,
    syncStatus TEXT NOT NULL,
    PRIMARY KEY(id)
)
```

`AppDatabase.version = 2`. `DatabaseModule`:

```kotlin
.addMigrations(MIGRATION_1_2)
.fallbackToDestructiveMigration(dropAllTables = true) // last-resort saja
```

### 12.8 Yang dilarang di 17a

- Memanggil Firestore
- Mengubah query History
- Menghapus `enqueueSync()` di `deleteTransaction`

---

## 13. Desain Update Remote (17b)

Semua angka uang `Long`. Jangan `Double`.

### 13.1 Rumus snapshot-diff

Pakai rumus yang sama dengan 16a §11.1, sumber “lama” = **dokumen Firestore saat `get()`**, sumber “baru” = transaksi lokal `PENDING`.

```
walletDelta(tx) =
    INCOME  → +amount
    EXPENSE → -amount

budgetDelta(tx) =
    EXPENSE + budget ketemu → +amount
    selain itu              → 0
```

**Create** (snapshot tidak ada):

```
walletInc[newWallet] = walletDelta(local)
budgetInc[newBudget] = budgetDelta(local)
set tx fields
set summary period baru (absolute Room)
```

**Update** (snapshot ada):

```
walletInc[oldWallet] += −walletDelta(remote)
walletInc[newWallet] += +walletDelta(local)
budgetInc[oldBudget] += −budgetDelta(remote)   // lookup dari field snapshot
budgetInc[newBudget] += +budgetDelta(local)
set tx fields = local
set summary period lama dan/atau baru (absolute Room)
```

Jika `oldWallet == newWallet`, satu `FieldValue.increment(net)`. Sama untuk budget.

**Note-only / retry setelah sukses:** `walletDelta(local) == walletDelta(remote)` dan wallet/kategori/tipe/tanggal sama → semua increment 0; hanya `set` field (idempotent).

**Jangan** hitung selisih di `SyncRepositoryImpl` dari memori “tx lama lokal” — row lama sudah tertimpa. Remote snapshot adalah last-synced state.

### 13.2 Kasus wajib (cermin 16a §11.2)

| # | Perubahan | Remote wallet | Remote budget | Remote tx doc | Summary |
|---|-----------|---------------|---------------|---------------|---------|
| 1 | Amount expense 100 → 150 | increment −50 (satu wallet) | `spent` +50 | field amount 150 | set period yang sama |
| 2 | Expense ↔ Income, amount sama | increment +2× amount | reverse spent lama; income 0 | `type` baru | set |
| 3 | Kategori A → B | 0 jika amount/tipe/wallet sama | reverse A, apply B | `categoryId` | set |
| 4 | Wallet personal → family | increment wallet lama −oldEffect; baru +newEffect | lookup budget memakai `familyId` **baru** | `walletId` + `familyId` | set (tetap per userId) |
| 5 | Tanggal pindah `monthKey` | 0 (kecuali #4) | budget period lama vs baru | `date` | **dua** dokumen summary |
| 6 | Note-only | 0 | 0 | `note` | set (boleh sama) |
| 7 | Retry setelah sukses, lokal masih `PENDING` (crash sebelum `SYNCED`) | 0 | 0 | set ulang field sama | set |
| 8 | Create (doc missing) | increment penuh | increment penuh | `set` create | set |
| 9 | Edit sebelum first sync (doc missing) | increment **nilai baru saja** | sama | `set` create | set |

### 13.3 API Firestore (usulan)

`runTransaction` **sinkron** — semua budget id diselesaikan **sebelum** callback.

```kotlin
suspend fun upsertTransactionWithSideEffects(
    transaction: Transaction,
    oldWalletId: String?,          // null = create (doc expected missing)
    oldWalletDelta: Long,          // −walletDelta(remote) atau 0
    newWalletDelta: Long,
    oldBudgetId: String?,
    oldBudgetDelta: Long,
    newBudgetId: String?,
    newBudgetDelta: Long,
    summaries: List<CategorySummary>, // 1 atau 2
)
```

Di dalam transaksi:

1. `existing = get(txnRef)`
2. Jika `existing.exists()` **dan** field identitas (amount, type, walletId, categoryId, familyId, date) **berbeda** dari yang dipakai menghitung `old*Delta` → **throw** khusus (`SideEffectSnapshotMismatch`) agar worker retry dengan pre-read baru (P16).
3. Jika `!existing.exists()`: **abaikan** old-delta (treat as create); apply `newWalletDelta` / `newBudgetDelta` saja. Ini meng-cover kasus 8–9 meski caller mengira update.
4. `set` field transaksi (map existing + `id` / `createdAt` dipertahankan dari lokal; `createdAt` **jangan** diganti `now`).
5. Increment wallet/budget sesuai net map.
6. `set` tiap summary.

Panggil `summaryRemote.upsertSummary` di luar transaksi **boleh tetap** seperti sekarang **atau** pindah ke dalam `runTransaction` (lebih atomic). **Pilih:** masukkan `set` summary ke `runTransaction` yang sama supaya retry tidak dobel increment tanpa update summary. Hari ini summary di-set dua kali (dalam upsert + `summaryRemote.upsertSummary`). 17: **satu kali** di dalam transaksi; hapus call `summaryRemote.upsertSummary` yang redundan di loop yang sama.

### 13.4 Pre-read di `SyncRepositoryImpl`

```
for each pending tx:
  remote = transactionRemote.getById(id)   // method baru, get() satu doc
  oldMonth = remote?.let { monthKey(it) }
  newMonth = monthKey(local)
  oldBudget = remote?.let { budgetMatch(it, oldMonth) }
  newBudget = budgetMatch(local, newMonth)
  oldWalletDelta = remote?.let { -walletDeltaFor(it) } ?: 0
  newWalletDelta = walletDeltaFor(local)
  summaries = buildSummaryUpserts dari Room (reuse helper 16a jika diekstrak,
              atau baca summaryLocal getByPeriod untuk newMonth + oldMonth)
  upsertTransactionWithSideEffects(...)
  mark SYNCED
```

Tambah `TransactionFirestoreDataSource.getById(id): Transaction?`. Jangan pakai `getByUserId` full scan.

`monthKey` **wajib** inject `PeriodPreferencesRepository` (P15).

Ekstrak `walletDeltaFor` / `budgetMatch` / `monthKey` supaya tidak diduplikasi mentah-mentah — boleh private di sync dulu; jangan pindah ke domain kecuali perlu tes murni.

### 13.5 Field dokumen transaksi

Tetap field existing (`id`, `walletId`, `userId`, `familyId`, `type`, `amount`, `categoryId`, `note`, `date`, `addedByName`, `createdAt`). **Jangan** tulis `syncStatus` ke Firestore (lokal-only).

`createdAt` / `userId` / `addedByName` / `id` tidak berubah saat edit (Phase 16 P6).

### 13.6 Yang dilarang di 17b

- `setBalance` wallet dari saldo Room sebagai pengganti increment
- Increment dari nilai lokal saja tanpa mengurangi snapshot (double-apply saat retry)
- Memakai `yyyy-MM` kalender untuk budget match

---

## 14. Desain Delete Remote + Pull (17c)

### 14.1 Push delete

```
syncPendingTransactions:
  1. for each outbox pending/failed:
       try deleteTransactionWithReverse(outbox)
       remove outbox row
     catch → mark outbox FAILED; hasFailure
  2. for each transactionLocal.getPending():
       (17b upsert)
```

Jika id ada di outbox **dan** masih ada row `PENDING` (seharusnya tidak, row sudah dihapus): delete menang (P7). Guard: skip upsert jika `id in outboxIds`.

### 14.2 `deleteTransactionWithReverse`

```kotlin
suspend fun deleteTransactionWithReverse(
    transactionId: String,
    expected: Transaction?,          // dari outbox, untuk mismatch check
    budgetId: String?,
    budgetDelta: Long,               // biasanya −amount jika expense
    walletDelta: Long,               // −walletDelta(old)
    summaries: List<CategorySummary>,
)
```

Di dalam `runTransaction`:

1. `existing = get(txnRef)`
2. Jika **tidak** exists → return (P9). Caller hapus outbox.
3. Hitung reverse dari **snapshot** (bukan semata outbox): `walletInc = −walletDeltaFor(snapshot)`. Budget: jika snapshot match `expected` identity, pakai `budgetId`/`budgetDelta` precomputed; else throw mismatch → retry.
4. Increment wallet/budget.
5. `delete(txnRef)`.
6. `set` summary absolute.

Idempotensi: sukses sekali → doc hilang → retry no-op increment.

API lama `deleteTransaction(id)` yang hanya `delete()` boleh didelegasikan ke method baru dengan delta 0 **atau** dihapus jika tidak ada caller lain. Grep dulu; jangan biarkan jalur “hapus doc tanpa reverse” hidup.

### 14.3 Pull: skip outbox

Di `syncFamilyData` / `syncPersonalData`, sebelum `transactionLocal.upsert`:

```
if (transaction.id in pendingDeleteIds) return@forEach
```

Jika pull sempat menulis row sebelum outbox dicek (jangan): urutan harus **baca outbox dulu**.

### 14.4 Orphan sweep (device lain / reinstall tidak relevan)

Reinstall: Room kosong, outbox kosong, remote sudah hard-delete → tidak ada yang dihidupkan. Sweep untuk **device yang masih punya row `SYNCED`**.

```
remoteIds = pulled.map { it.id }.toSet()
oldestPulled = pulled.minByOrNull { it.date }?.date

localCandidates = transaksi lokal scope (familyId / personal canonical wallet)
    .filter { syncStatus == SYNCED }
    .filter { it.id !in remoteIds }
    .filter { oldestPulled == null || !it.date.isBefore(oldestPulled) }

for (orphan in localCandidates):
    applyDeletedTransactionAtomically(…, pendingDelete = JANGAN)
    // helper baru: reverse + delete row TANPA outbox
```

Tanpa outbox: remote sudah tidak punya dokumen; menulis outbox akan mencoba delete no-op lalu reverse remote **kedua kali** jika ada race — lebih aman local-only reverse.

Wallet: Phase 6C/10 tetap recompute `sum(walletDelta)` dari **remote** txs lalu `setBalance` jika beda. Urutan yang disarankan:

1. Pull wallets (skip PENDING seperti sekarang)
2. Pull txs → upsert (skip outbox + skip lokal PENDING)
3. **Orphan sweep** (local-only reverse)
4. Recompute wallet dari remote txs + `setBalance` (existing)
5. Hydrate budgets (existing)

Budget `spent` di device B: setelah A sync delete, remote `spent` sudah turun; hydrate menimpa lokal B jika B tidak `PENDING`/`FAILED`. Sweep + hydrate = safety net ganda. Jangan sweep jika pull **throw**; empty list sukses = otoritatif.

`oldestPulled == null` (pull sukses, 0 tx): sweep semua `SYNCED` di scope itu. `PENDING` lokal tetap.

Jangan sweep id yang ada di outbox — biarkan push delete yang membersihkan remote; lokal row seharusnya sudah tidak ada.

### 14.5 Helper local-only reverse

Tambah overload atau flag di `applyDeletedTransactionAtomically` (`pendingDelete: PendingTransactionDeleteEntity? = null`). Sweep memanggil tanpa outbox.

Jangan panggil `TransactionRepository.deleteTransaction` dari pull — itu akan enqueue + outbox.

### 14.6 Category summary saat sweep

Family pull **tidak** rebuild summary hari ini. Sweep harus reverse summary (helper 16a) supaya insight penulis di device B tidak stale. Personal pull sudah `rebuildPersonalSummaries` dari txs tersisa — sweep **sebelum** rebuild, atau rebuild menghitung ulang dari row yang sudah bersih.

**Pilih:** personal: sweep dulu, lalu `rebuildPersonalSummaries` existing. Family: sweep memakai reverse summary 16a (tidak ada rebuild family).

---

## 15. Firestore Security Rules (17d)

Publish ke Console **sebelum** QA 2 akun. Tanpa ini, 17b/17c hijau di unit test tetapi SyncWorker `PERMISSION_DENIED` di device member.

### 15.1 Transaksi

Hari ini:

```
allow update, delete: if signedIn()
  && resource.data.userId == request.auth.uid;
```

Target:

```
allow update, delete: if signedIn() && (
  resource.data.userId == request.auth.uid
  || isFamilyMember(resource.data.familyId)
);
```

Personal (`familyId` kosong / missing): tetap owner-only (`isFamilyMember` harus false untuk non-string / empty — samakan helper existing).

Create tetap `request.resource.data.userId == request.auth.uid` (jangan biarkan member memalsukan `userId`).

### 15.2 Budget `spent`

Cermin wallet:

```
allow update: if signedIn() && (
  resource.data.userId == request.auth.uid
  || (
    isFamilyMember(resource.data.familyId)
    && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['spent'])
  )
);
```

Perlu untuk create expense member **dan** reverse edit/hapus. Dokumentasikan di `firestore-rules.md` (bagian “How create / get / update is checked” + catatan SyncWorker).

### 15.3 Wallet

Tidak berubah (member sudah boleh `balance`).

### 15.4 Summary

`/users/{uid}/category_summaries/{period}` tetap owner-only. Sync **penulis** yang men-set summary miliknya. Device member tidak men-set summary user lain.

---

## 16. Task Breakdown Detail

Kerjakan **17a → 17b → 17c → 17d**. 17d boleh disiapkan paralel (docs) tetapi **publish** sebelum QA 2 akun.

### 17a — Task 1: Schema + atomic outbox

- Entity, DAO, v2, `MIGRATION_1_2`, Hilt provide.
- `applyDeleted` tulis outbox.
- `getPendingDeletes` / `removePendingDelete` / `updateDeleteSyncStatus`.
- `hasPendingSync` + tes existing `hasPendingSync is false` **update** (stub outbox kosong).
- Verify: `./gradlew :core:data:compileDebugKotlin`

### 17a — Task 2: Tes delete lokal

Di `TransactionRepositoryImplTest` (pola mock local):

1. Delete expense `SYNCED` → `applyDeleted` dipanggil dengan `pendingDelete.id` sama; row tidak di-`getPending`.
2. Delete missing id → tidak upsert outbox, tidak `enqueueSync`.
3. Update **tidak** menulis outbox (regresi).

Verify: `./gradlew :core:data:testDevDebugUnitTest --tests "*TransactionRepositoryImpl*"`

### 17b — Task 3: Firestore update + monthKey

- `getById` remote.
- Ganti skip-if-exists.
- Inject `PeriodPreferencesRepository` ke `SyncRepositoryImpl`.
- Pre-read + pass old/new deltas.
- Summary sekali di dalam transaksi.
- Verify compile.

### 17b — Task 4: Tes update sync

Minimal di `SyncRepositoryImplTest` dan/atau tes murni helper jika diekstrak:

1. Pending edit amount → `upsert` dipanggil dengan `oldWalletDelta` / `newWalletDelta` benar.
2. Doc missing → treat create (`old*` diabaikan di DS; verifikasi lewat arg atau tes DS jika ada).
3. Note-only → old/new wallet delta net 0.
4. `hasPendingSync` tidak berubah maknanya untuk upsert.
5. Tes create existing **jangan** mengharapkan skip diam-diam.

Kalau DS sulit di-unit-test tanpa Firebase, tes impl dengan mock `upsertTransactionWithSideEffects` + verify parameter (sama pola 16a).

Verify: `./gradlew :core:data:testDevDebugUnitTest --tests "*SyncRepositoryImpl*"`

### 17c — Task 5: Delete remote + drain outbox

- `deleteTransactionWithReverse`.
- Loop outbox sebelum upsert.
- Outbox sukses → `deleteById`; gagal → `FAILED` + throw batch (pola existing).
- Tes: outbox + remote delete dipanggil; missing remote → outbox tetap dihapus; upsert tidak jalan untuk id outbox.

### 17c — Task 6: Pull skip + sweep

- `syncFamilyData` + `syncPersonalData`.
- Tes: remote tx id = outbox → tidak `upsert` lokal.
- Tes: lokal `SYNCED` di jendela, tidak ada di remote → local-only reverse (verify `applyDeleted` tanpa outbox **atau** helper sweep).
- Tes: lokal `PENDING` create tidak di-sweep.
- Tes: lokal `SYNCED` lebih tua dari `oldestPulled` tidak di-sweep.
- Tes existing recompute wallet **jangan** pecah.

### 17d — Task 7: Rules

- Edit markdown + publish Console.
- QA 2 akun tidak boleh dijalankan sebelum ini.

Verify akhir: `./gradlew :core:data:testDevDebugUnitTest assembleDevDebug`

---

## 17. Acceptance Criteria

### Harus terpenuhi

- [ ] Delete lokal menulis outbox dalam transaksi yang sama dengan hapus row
- [ ] `hasPendingSync()` true jika hanya outbox yang terisi
- [ ] Update tx `SYNCED`: dokumen Firestore field ikut berubah; wallet increment = selisih, **bukan** amount penuh
- [ ] Retry update setelah sukses (atau snapshot sudah = lokal) **tidak** mendobel saldo remote
- [ ] Create path: dokumen baru tetap increment penuh; FAB create tidak regresi
- [ ] Hapus tx `SYNCED`: dokumen Firestore hilang; wallet/budget remote terkoreksi (reverse)
- [ ] Hapus tx yang belum pernah ada di remote: outbox ter-ack, increment remote 0
- [ ] Device B pull family: tx yang A hapus (sudah sync) **tidak** muncul lagi; saldo/spent selaras
- [ ] Device yang sama pull sebelum delete sempat push: id outbox **tidak** di-upsert kembali
- [ ] Orphan sweep tidak menghapus `PENDING` lokal / tx di luar jendela 200
- [ ] `monthKey` sync = siklus payday (kasus `cycleStartDay` 25)
- [ ] `MIGRATION_1_2` ada; Room v2
- [ ] Auth / splash / Settings / Family invite UI tidak disentuh
- [ ] Tes 17a–17c hijau

### Sengaja belum

- [ ] Dua orang mengedit tx yang sama dalam detik yang sama (last-write-wins)
- [ ] Tombstone Firestore / undo delete
- [ ] Tap recent Dashboard
- [ ] Swipe-to-delete
- [ ] Cloud Function recompute

---

## 18. Catatan Arsitektur & Konvensi

- Feature → UseCase → Repository. Sync **hanya** dari `SyncRepository` / Worker — jangan panggil Firestore dari `TransactionRepositoryImpl`.
- Offline-first: UI tetap baca Room; 17 hanya memperbaiki **push** dan **pull reconcile**.
- `CancellationException` selalu di-rethrow sebelum `catch (e: Exception)`.
- Uang `Long`; tanggal domain `Instant`.
- Kotlin only. Jangan sentuh file protected skill (`User*`, auth, splash, build-plugin, gradle root).
- Jangan inject `TransactionRepository` ke tempat baru.
- `CommonDispatcher` tidak relevan di repository sync (sudah suspend + WorkManager).

---

## 19. Dependency Graph

```
User hapus (16c)
  → DeleteTransactionUseCase
      → TransactionRepositoryImpl.deleteTransaction
          → applyDeleted (Room: outbox + reverse + delete row)
          → SyncScheduler.enqueueSync()

User edit (16c)
  → UpdateTransactionUseCase
      → updateTransaction (Room reverse/apply, row PENDING)
      → enqueueSync()

SyncWorker / syncAll
  → syncPendingWallets
  → syncPendingBudgets
  → syncPendingTransactions
        ├─ drain pending_transaction_deletes
        │     → deleteTransactionWithReverse (snapshot + increment + delete doc)
        └─ getPending() upserts
              → getById remote + snapshot-diff upsert

syncFamilyData / syncPersonalData
  → skip upsert id ∈ outbox
  → orphan sweep SYNCED in window (local reverse, no outbox)
  → recompute wallet from remote txs (existing)
```

---

## 20. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| Skip-if-exists tertinggal | Cloud beku | P4; tes #1 + #7 |
| Increment dari lokal tanpa kurangi snapshot | Saldo remote dobel saat retry | P4; tes retry |
| Outbox setelah `withTransaction` | Row hilang, cloud yatim | P3 |
| `isDeleted` di tabel transactions | Totals History salah | P1 |
| Sweep semua id yang tidak ada di pull 200 | Tx lama terhapus lokal | P11 jendela `oldestPulled` |
| Sweep saat pull error / empty exception | Data lokal lenyap | Jangan sweep jika `getByFamilyId` throw |
| `setBalance` dari Room device A | Menimpa increment device B | P5 |
| `monthKey` kalender | Budget salah bulan | P15 + tes cycle 25 |
| Rules owner-only | Member edit/hapus `FAILED` selamanya | P17 / P18 + Task 7 |
| Pre-read vs snapshot drift | Delta salah | P16 throw mismatch → retry |
| `hasPendingSync` lupa outbox | Hapus tidak pernah di-retry | Task 1 |
| Destructive migration tanpa `MIGRATION_1_2` | User kehilangan Room | P13 |
| Upsert lalu delete id yang sama | Flash remote + increment sia-sia | P7 |
| Summary di-set dua kali + increment retry | Agregat aneh | Satu `set` di `runTransaction` |
| Family ACL diam-diam diketatkan | Bertentangan 16 P11 | P17 |
| Tes create mengharapkan skip | False red / false green | Task 4 #5 |

---

## 21. Urutan Pengerjaan yang Disarankan

1. Task 1–2 (17a) — **berhenti jika tes repo merah.**
2. Task 3–4 (17b) update remote + `monthKey`.
3. Task 5–6 (17c) outbox drain + pull.
4. Task 7 (17d) publish rules.
5. `assembleDevDebug` + QA §24 (2 device / 2 akun family).
6. Baru anggap Phase 16 §22.6 tertutup.

Jangan merge 17c ke `main` tanpa 17a. 17b boleh satu commit dengan 17c. Rules boleh commit `[DOCS]` terpisah tetapi **harus** live sebelum QA.

---

## 22. Relasi ke Phase Lain

| Phase | Relasi |
|-------|--------|
| **16** | 17 = 16d. Lokal sudah benar; 17 menutup cloud. Jangan rollback 16a reverse/apply |
| **2** | Strategy A di-extend: skip-if-exists hanya implisit via `Δ = 0`, bukan `return` |
| **6C** | Pull family wajib skip outbox + sweep; recompute wallet tetap |
| **10** | Pull personal sama; restore setelah hapus-sync tidak boleh menghidupkan tx |
| **11** | `findBudgetForExpense` dipakai pre-read old/new; rules `spent` |
| **14** | `monthKey` siklus wajib di sync |
| **15** | Query History tidak berubah (alasan P1) |
| **5 / 12** | Form create/edit tidak berubah |
| **9** | Tes UI cloud tidak wajib; 17 wajib tes repo + sync |

---

## 23. Rencana Commit

Ikuti tag repo. Branch usulan: `feat/transaction-firestore-update-delete`.

```
[FEAT] Queue transaction deletes in a Room outbox
[FEAT] Sync transaction updates with snapshot-diff increments
[FEAT] Sync transaction deletes and reconcile family pull
[DOCS] Allow family members to update shared transactions
[DOCS] Add Phase 17 Firestore update and delete plan
```

Rules Console publish **bukan** git commit — catat di PR bahwa rules sudah di-deploy ke `keutrack-dev`.

---

## 24. Manual Test Plan

Pakai **dua akun** satu family jika memungkinkan (A penulis, B anggota). Catat saldo Firestore + Room **sebelum** tiap langkah. Online kecuali baris yang disebut offline.

### 24.1 Create tidak regresi

| # | Langkah | Expected |
|---|---------|----------|
| 1 | FAB expense → save → tunggu SYNCED | Dokumen Firestore baru; wallet remote turun amount penuh (bukan 0); budget `spent` naik |
| 2 | Airplane → create → online | Satu increment, bukan dua (retry idempotent) |

### 24.2 Update cloud

| # | Langkah | Expected |
|---|---------|----------|
| 3 | Tx SYNCED, ubah note → simpan | Field `note` remote berubah; `balance` / `spent` **tidak** bergerak |
| 4 | Expense 100rb → 150rb | Remote amount 150rb; wallet −50rb lagi; `spent` +50rb |
| 5 | Expense → income amount sama | Wallet +2×; `spent` turun; `type` income |
| 6 | Ganti kategori (kedua budget ada) | Budget A `spent` turun, B naik |
| 7 | Personal → family wallet | Saldo personal remote balik; family terserap; `familyId` terisi |
| 8 | Ganti tanggal menyeberang siklus (`cycleStartDay` 25) | Budget/summary period lama vs baru di Firestore |
| 9 | Device B buka Family / History (setelah A SYNCED) | Angka + field baru, bukan stale Phase 16 |

### 24.3 Delete cloud

| # | Langkah | Expected |
|---|---------|----------|
| 10 | Hapus tx SYNCED, online | Row lokal hilang; dokumen Firestore hilang; wallet/spent remote reverse |
| 11 | Airplane → hapus → (opsional pull di device sama) → online | Row tetap hilang; setelah online, remote ikut hilang; pull tidak menghidupkan |
| 12 | Create PENDING (belum sync) → hapus | Tidak ada dokumen remote; wallet remote tidak berubah |
| 13 | Device B pull setelah A delete SYNCED | Tx tidak muncul; saldo B = recompute remote |

### 24.4 Rules / member

| # | Langkah | Expected |
|---|---------|----------|
| 14 | B edit tx A (family) | Sync sukses, bukan `PERMISSION_DENIED` |
| 15 | B hapus tx A (family) | Sama; A pull → row hilang |
| 16 | B edit tx **personal** A | Tidak bisa (tidak terlihat / rules owner-only) |

### 24.5 Regresi lain

| # | Langkah | Expected |
|---|---------|----------|
| 17 | History totals + chip periode | Tetap benar (query tidak diubah) |
| 18 | Reinstall + login A setelah delete sync | Phase 10 restore **tanpa** tx yang sudah dihapus |
| 19 | Family invite / Settings siklus / Auth | Tidak berubah |
| 20 | Dashboard saldo setelah edit/delete + sync | Selaras Room vs Firestore (boleh beda sesaat sebelum worker selesai) |

---

## Appendix — Sketsa snapshot-diff (ide, bukan copy-paste wajib)

```kotlin
internal fun walletDeltaFor(type: TransactionType, amount: Long): Long =
    when (type) {
        TransactionType.INCOME -> amount
        TransactionType.EXPENSE -> -amount
    }

// net increment per wallet id (boleh 1 atau 2 kunci)
internal fun walletIncrements(old: Transaction?, new: Transaction): Map<String, Long> {
    val inc = mutableMapOf<String, Long>()
    if (old != null) {
        inc[old.walletId] = (inc[old.walletId] ?: 0L) - walletDeltaFor(old.type, old.amount)
    }
    inc[new.walletId] = (inc[new.walletId] ?: 0L) + walletDeltaFor(new.type, new.amount)
    return inc.filterValues { it != 0L }
}
```

`runTransaction` menerapkan map itu via `FieldValue.increment`. Retry aman jika `old` dibaca ulang dari snapshot dan lolos mismatch check.

---

## Appendix — Pemetaan 16d → tugas 17

| Butir 16d (Phase 16 appendix) | Di mana di 17 |
|-------------------------------|---------------|
| 1. Update remote: jangan return saat exists; tulis field + increment selisih (atau set absolute wallet) | §13, P4–P5, Task 3–4. **Pilih increment selisih**, bukan set absolute |
| 2. Delete remote: outbox/tombstone sebelum hapus row / tabel `pending_deletes` | §12, P1–P3, Task 1–2. **Pilih tabel outbox** |
| 3. Reverse `FieldValue.increment` wallet/budget | §13 + §14.2, Task 3 + 5 |
| 4. Konflik pull 6C/10 vs tombstone | §14.3–14.4, P10–P11, Task 6 |

16a sengaja tidak menambah schema. 17a menambah **hanya** tabel outbox supaya slice sync tetap testable tanpa menyentuh query History.
