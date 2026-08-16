# Phase 10 — Personal Wallet Restore Sync (Reinstall / Device Baru)

> **Modul target:** `:core:data` (+ domain **additive** tipis) · konsumsi di `:features:dashboard` · rules/index notes di `docs/database/firestore-rules.md`  
> **Estimasi:** ~2–3 hari  
> **Prasyarat:** Phase 2 ✅ (push sync) · Phase 4 ✅ (Dashboard real data) · Phase 5 ✅ (transaction flow) · Phase 6c ✅ (pola pull family)  
> **Status:** 10a + 10b implemented (`feat/personal-wallet-restore`)  
> **Status baseline:** Personal wallet **push-only**; `ensureDefaultPersonalWallet()` mint UUID baru saat Room kosong; **tidak ada** pull `ownerId` / transaksi personal  
> **Hasil akhir:** Setelah install ulang + login akun yang sama, Dashboard menampilkan **wallet personal kanonis yang sama** (id + saldo + transaksi) dari Firestore — tanpa membuat dompet kosong baru

---

## Daftar Isi

1. [Konteks & Tujuan](#1-konteks--tujuan)
2. [Inventory — Apa yang Sudah Ada](#2-inventory--apa-yang-sudah-ada)
3. [Root Cause (Kenapa Reinstall Kehilangan Personal Wallet)](#3-root-cause-kenapa-reinstall-kehilangan-personal-wallet)
4. [Keputusan Desain](#4-keputusan-desain)
5. [Scope — Apa yang Dikerjakan](#5-scope--apa-yang-dikerjakan)
6. [Scope — Apa yang TIDAK Dikerjakan](#6-scope--apa-yang-tidak-dikerjakan)
7. [Prasyarat (Definition of Ready)](#7-prasyarat-definition-of-ready)
8. [File Referensi (Read-Only)](#8-file-referensi-read-only)
9. [File yang TIDAK BOLEH Diubah](#9-file-yang-tidak-boleh-diubah)
10. [File yang BOLEH Diubah / Dibuat](#10-file-yang-boleh-diubah--dibuat)
11. [Struktur File Target](#11-struktur-file-target)
12. [Desain Solusi](#12-desain-solusi)
13. [Firestore Security Rules & Indexes](#13-firestore-security-rules--indexes)
14. [Task Breakdown Detail](#14-task-breakdown-detail)
15. [Acceptance Criteria](#15-acceptance-criteria)
16. [Catatan Arsitektur & Konvensi](#16-catatan-arsitektur--konvensi)
17. [Dependency Graph](#17-dependency-graph)
18. [Risiko & Mitigasi](#18-risiko--mitigasi)
19. [Urutan Pengerjaan yang Disarankan](#19-urutan-pengerjaan-yang-disarankan)
20. [Relasi ke Phase Lain](#20-relasi-ke-phase-lain)

---

## 1. Konteks & Tujuan

Setelah Phase 6c, family wallet **sudah** punya pull (`syncFamilyData` + `getByFamilyId`). Personal wallet **sengaja ditunda** (PHASE_6C §4.2: “Tidak wajib pull semua personal wallets”).

Dampak produk yang sekarang terasa:

| Area | Status |
|------|--------|
| Push wallet/tx personal (`SyncWorker` / `syncAll`) | ✅ |
| Pull family wallet + tx by `familyId` | ✅ Phase 6c |
| `syncUserProfile` di splash (nama, currency, `familyId`) | ✅ |
| Pull personal wallet by `ownerId` | ❌ belum ada |
| Pull transaksi personal by `userId` / `walletId` | ❌ belum ada |
| Seed default personal saat Room kosong | ✅ — **tapi mint UUID baru** |
| Restore personal setelah reinstall / clear data | ❌ saldo 0, history hilang |

**Bug produk:** User catat transaksi personal → uninstall / clear data → install ulang → login akun yang sama → card Personal Wallet Rp 0, history personal kosong. Data lama tetap di Firestore, tidak pernah di-hydrate ke Room.

**Tujuan Phase 10:**
1. **Pull personal slice** — unduh wallet `type=PERSONAL` milik `ownerId == uid` + transaksi terkait ke Room
2. **Pull-before-seed** — `ensureDefaultPersonalWallet()` **tidak** mint UUID baru jika remote sudah punya personal wallet
3. **Canonical personal wallet** — satu id stabil per user (pilih tertua jika ada orphan dari reinstall sebelumnya)
4. **Dashboard hydrate** — saldo, recent tx, dan monthly income/expense personal kembali setelah restore
5. **Offline-first tetap** — UI baca Room; pull hanya hydrate; write lokal → PENDING → push existing

**Bukan tujuan Phase 10:**
- Realtime listener / multi-device live sync personal
- Hapus otomatis orphan wallet di Firestore (dokumentasikan saja)
- Pull budget personal (rules `list: false`)
- Auth / splash / build-plugin changes
- Phase 9 test suite menyeluruh di luar path restore ini

---

## 2. Inventory — Apa yang Sudah Ada

### Sync (Phase 2 + 6c)

| Item | Perilaku |
|------|----------|
| `SyncRepository.syncAll()` | Push `PENDING` wallets / budgets / transactions saja |
| `SyncRepository.syncFamilyData(familyId)` | Pull wallet + tx **family** → Room; skip overwrite jika lokal PENDING; pilih canonical tertua |
| `SyncFamilyDataUseCase` | Resolve `user.familyId` → `syncFamilyData` |
| `RetryPendingSyncUseCase` | Dashboard: enqueue push **hanya** jika ada PENDING/FAILED |
| `SyncWorker` | Memanggil `syncAll()` — **bukan** pull |

### Wallet

| Item | Perilaku |
|------|----------|
| `WalletRepositoryImpl.ensureDefaultPersonalWallet()` | Jika `local.getPersonal() == null` → mint `UUID` + `balance = 0` + `PENDING` + `enqueueSync` |
| `observeWallets()` / `observeWalletsByType()` | `.onStart { ensureDefaultPersonalWallet() }` — jalan **sebelum** UI sempat pull |
| `WalletFirestoreDataSource` | `upsertWallet`, `setBalance`, `getByFamilyId`, `deleteWallet` — **tidak ada** `getByOwnerId` |
| `WalletDao.getPersonal()` | `WHERE type = 'personal' LIMIT 1` — tanpa `ORDER BY` (non-deterministik jika >1) |

### Transaction / summary

| Item | Perilaku |
|------|----------|
| `TransactionFirestoreDataSource.getByFamilyId` | Pull family saja |
| `CategorySummaryFirestoreDataSource` | **Push-only** (`upsertSummary`) |
| `GetMonthlySummaryUseCase` | Baca `CategorySummary` di Room (bukan hitung ulang dari tx di UI) |
| `GetWalletSummaryUseCase` | `personal.firstOrNull()` — berbahaya jika ada 2 personal wallet |

### Feature trigger

| Item | Perilaku |
|------|----------|
| Splash | `syncUserProfile()` saja — **protected, jangan diubah** |
| `DashboardViewModel.onScreenRendered()` | `retryPendingSync()` saja (push) |
| `FamilyViewModel` | `syncFamilyData()` on render — pola yang ditiru |

### Rules

| Path | List hari ini | Cukup untuk Phase 10? |
|------|---------------|------------------------|
| `/wallets` | `allow list: if signedIn()` | ✅ query `ownerId == uid` lolos MVP |
| `/transactions` | `allow list: if signedIn()` | ✅ query `userId == uid` lolos MVP |
| `/budgets` | `list: false` | ❌ pull budget out of scope |
| `/users/{uid}/category_summaries` | owner read | ✅ bisa pull; alternatif: rebuild dari tx |

---

## 3. Root Cause (Kenapa Reinstall Kehilangan Personal Wallet)

```
Device lama
  → Wallet W_OLD (id stabil, ownerId=U, type=PERSONAL, balance=X)
  → Tx T1, T2 (walletId=W_OLD, userId=U) → push Firestore ✅

Install ulang / clear data
  → Room kosong
  → Dashboard combine(GetWalletSummary) → observeWallets()
  → onStart { ensureDefaultPersonalWallet() }
  → local.getPersonal() == null
  → mint W_NEW (UUID baru, balance=0, PENDING)
  → enqueueSync → push W_NEW ke Firestore   ← orphan kedua

Yang tidak pernah terjadi:
  → getByOwnerId(U) / getByWalletId(W_OLD)
  → hydrate T1, T2 ke Room

UI baca Room → kartu personal Rp 0
W_OLD + T1/T2 tetap di cloud, tidak terpakai
```

Dua keputusan yang bertemu:

1. **Sync personal = push-only** — tidak ada analog `syncFamilyData` untuk `ownerId`
2. **Seed lebih dulu daripada restore** — `ensureDefault` menganggap “Room kosong = user baru”

Family tidak kena bug yang sama karena joiner **fetch remote dulu**, bukan mint UUID.

---

## 4. Keputusan Desain

### 4.1 Satu personal wallet kanonis per user

| Opsi | Keputusan |
|------|-----------|
| A. Satu `wallet.id` personal per `ownerId` (pilih tertua jika >1) | ✅ **Dipilih** — cermin canonical family di 6c |
| B. Banyak personal wallet + aggregate di UI | ❌ `GetWalletSummaryUseCase` / `getPersonal()` sudah asumsi 1 |

**Kontrak:**
- User baru (remote kosong) → tetap seed `Dompet Utama` + push (perilaku hari ini)
- User lama (remote ada PERSONAL) → **upsert id remote yang sama**; **jangan** mint UUID
- Legacy orphan (W_NEW dari reinstall sebelumnya): prefer wallet remote **tertua** (`minBy createdAt`); drop extra **lokal**; **jangan** auto-delete orphan remote di Phase 10

### 4.2 Pull-before-seed (invariant wajib)

```
ensureDefaultPersonalWallet():
  1. local.getPersonal() != null AND bukan orphan kosong? → return
  2. syncPersonalData(uid)          // pull remote → Room
  3. local.getPersonal() != null → return   // restored
  4. mint default + enqueue push            // truly new user
```

Tanpa langkah 2, `onStart` di `observeWallets()` **selalu** menang race terhadap `DashboardViewModel.onScreenRendered()`.

### 4.3 Pull sync scope (MVP)

Pull **hanya** data personal milik user yang login:

1. Wallets `where ownerId == uid` — filter `type == PERSONAL` di client (equality-only, tanpa composite index)
2. Transactions `where userId == uid` (limit N, mis. 200) — filter client: `walletId` ∈ personalIds **atau** `familyId == null`
3. Category summaries: **rebuild dari transaksi yang di-pull** (jangan andalkan remote balance / summary sebagai sumber kebenaran)

Tidak pull:
- Personal wallet milik user lain
- Family wallet (sudah di 6c)
- Budgets (rules `list: false`)

### 4.4 Recompute balance (sama 6c)

```
computedBalance = sum(walletDelta(tx)) untuk tx.walletId == canonical.id
upsert wallet.copy(balance = computedBalance, syncStatus = SYNCED)
jika remote.balance != computed → best-effort walletRemote.setBalance
```

Jangan tulis `upsertWallet` dengan balance lokal yang sudah di-increment — side-effect increment tetap milik transaction sync.

### 4.5 Conflict

| Kondisi | Perilaku |
|---------|----------|
| Lokal PENDING untuk id yang sama | **Skip overwrite** (tulisan offline menang) |
| Lokal personal id ≠ canonical, 0 tx, balance 0, PENDING | Hapus lokal orphan; pakai canonical |
| Lokal personal id ≠ canonical, ada PENDING tx | Jangan hapus; skip replace (dokumentasikan; jarang setelah reinstall) |
| Remote wins selain PENDING | Cukup MVP |

### 4.6 Offline-first tetap berlaku

```
Pull remote → merge Room (upsert by id)
UI baca Room saja
Write lokal → PENDING → push (existing SyncWorker)
```

### 4.7 Kapan pull dipanggil

| Trigger | Wajib? | Alasan |
|---------|--------|--------|
| `ensureDefaultPersonalWallet()` sebelum seed | ✅ | Menutup race `observeWallets().onStart` |
| `DashboardViewModel.onScreenRendered()` | ✅ | Refresh + restore orphan dari sesi reinstall sebelumnya |
| Splash / `completeSignIn` | ❌ | Protected; jangan campur finance ke auth |
| Snapshot listener / pull periodik | ❌ | Future |

---

## 5. Scope — Apa yang Dikerjakan

### A. Phase 10a — Restore personal (wajib)

| # | Item |
|---|------|
| 1 | `WalletFirestoreDataSource.getByOwnerId(ownerId)` |
| 2 | `TransactionFirestoreDataSource.getByUserId(userId, limit)` (equality-only + sort/limit client) |
| 3 | `SyncRepository.syncPersonalData(userId: String)` — pull + canonical + recompute + upsert Room |
| 4 | `SyncPersonalDataUseCase` — resolve uid dari session; no-op jika belum login |
| 5 | `WalletRepositoryImpl.ensureDefaultPersonalWallet()` — **pull-before-seed** |
| 6 | Canonical: `minBy createdAt`; drop extra personal **lokal** |
| 7 | Rebuild `CategorySummary` bulan yang muncul di tx personal yang di-pull |
| 8 | `DashboardViewModel.onScreenRendered()` — panggil `SyncPersonalDataUseCase` + tetap `retryPendingSync` |
| 9 | Unit test: `SyncRepositoryImpl`, `WalletRepositoryImpl`, `SyncPersonalDataUseCase` |
| 10 | Notes index/rules di `firestore-rules.md` (query `ownerId` / `userId`) |

### B. Phase 10b — Polish (setelah 10a hijau)

| # | Item |
|---|------|
| 11 | `WalletDao.getPersonal()` → `ORDER BY createdAtEpochMs ASC LIMIT 1` (deterministik) |
| 12 | UX: error snackbar non-fatal jika pull gagal (“Gagal memuat dompet. Coba buka ulang Dashboard.”) |
| 13 | Opsional: `getByWalletId` sebagai alternatif query tx jika `userId` terlalu lebar |
| 14 | Opsional: dokumentasi cleanup orphan remote di Console (manual) |

---

## 6. Scope — Apa yang TIDAK Dikerjakan

| Item | Alasan | Ditunda ke |
|------|--------|------------|
| Realtime listener personal | Over-scope; pull on open cukup | Future |
| Auto-delete orphan wallet di Firestore | Risiko hapus data; user mungkin punya 2 docs dari reinstall | Future / script Console |
| Pull budgets | `allow list: if false` | Future + rules |
| Pull category summaries remote sebagai SoT | Rebuild dari tx lebih konsisten dengan 6c | — |
| Multi personal wallet UI | Produk 1 dompet utama | Future wallet mgmt |
| Auth / splash / `syncUserProfile` | Protected | — |
| Ubah `completeSignIn` / rollback | Jangan campur finance | — |
| Family pull / rules membership | Sudah 6c; jangan regress | — |
| Edit/delete transaction UI | Bukan restore | Future |
| Phase 9 test suite penuh (semua UseCase/VM) | Hanya regen path restore | Phase 9 |

---

## 7. Prasyarat (Definition of Ready)

1. User bisa login (email / Google) dan session persist  
2. User bisa catat transaksi **personal**; muncul di Dashboard + History  
3. Sync push terlihat di Console: `wallets/{id}` (`type=personal`, `ownerId`) + `transactions` (`userId`, `walletId`)  
4. Family pull 6c tetap hijau (jangan pecah)  
5. Build hijau sebelum mulai:

```bash
./gradlew :core:domain:compileDevDebugKotlin
./gradlew :core:data:compileDevDebugKotlin
./gradlew :features:dashboard:compileDevDebugKotlin
./gradlew :core:data:testDevDebugUnitTest
./gradlew assembleDevDebug
```

- [ ] Branch kerja: `feat/personal-wallet-restore` (atau setara)
- [ ] Baca dokumen ini + PHASE_6C §4 / §12 (pola pull) sebelum coding
- [ ] Repro baseline: reinstall → personal Rp 0 (screenshot / log)

---

## 8. File Referensi (Read-Only)

| File | Pelajari |
|------|----------|
| `docs/dev/phases/PHASE_6C_SHARED_FAMILY_DATA_SYNC.md` | Pola pull, canonical, skip PENDING, recompute balance |
| `docs/dev/phases/PHASE_2_ROOM_AND_REPOSITORY_IMPLEMENTATION.md` | Offline-first; seed default wallet |
| `docs/database/firestore-rules.md` | `list` wallets/tx sudah signed-in; budgets `list: false` |
| `SyncRepository.kt` / `SyncRepositoryImpl.kt` | Surface push + `syncFamilyData` |
| `SyncFamilyDataUseCase.kt` | Pola use case pull |
| `WalletRepositoryImpl.kt` | `ensureDefaultPersonalWallet` — titik race |
| `WalletFirestoreDataSource.kt` / `TransactionFirestoreDataSource.kt` | Pola `getByFamilyId` (ditiru untuk owner/user) |
| `DashboardViewModel.kt` | `onScreenRendered` + `combine` Flows |
| `FamilyViewModel.kt` | `pullFamilyData` — pola trigger + error non-fatal |
| `.cursor/rules/keutrack-data-layer.mdc` | `CancellationException`, offline-first |
| `.cursor/skills/keutrack-dev/SKILL.md` | Protected files |

---

## 9. File yang TIDAK BOLEH Diubah

| Area | Alasan |
|------|--------|
| `features/auth/**`, `features/splashscreen/**` | Protected |
| `build-plugin/**`, root Gradle, `local.properties` | Stable |
| Auth sign-in / `completeSignIn` / rollback / `UserRepository` write path | Jangan campur restore ke auth |
| `User.kt` / `AuthResult` / `TokenResult` | Protected domain |
| Breaking change signature UseCase finansial existing | Hanya **additive** |
| Theme hex / DS rename | Consume only |
| `syncFamilyData` behavior (kecuali extract helper bersama, tanpa ubah kontrak) | Jangan regress 6c |

**Hati-hati:**
- `upsertWallet` **jangan** menulis balance yang sudah di-increment lokal (double-count)
- `GetWalletSummaryUseCase` — jangan ubah semantik kecuali perlu filter canonical; prefer bersihkan Room jadi 1 personal
- Jangan inject Firestore ke `:features:dashboard` — hanya UseCase

---

## 10. File yang BOLEH Diubah / Dibuat

### Domain (additive)

| File | Peran |
|------|-------|
| `SyncRepository` | + `suspend fun syncPersonalData(userId: String)` |
| `SyncPersonalDataUseCase.kt` | **BARU** — mirror `SyncFamilyDataUseCase` |
| `FakeRepositories` (test) | Implement method baru |

### Data

| File | Peran |
|------|-------|
| `WalletFirestoreDataSource` | + `getByOwnerId(ownerId)` |
| `TransactionFirestoreDataSource` | + `getByUserId(userId, limit)` |
| `SyncRepositoryImpl` | Orchestrate pull personal (boleh extract helper bersama family) |
| `WalletRepositoryImpl` | Pull-before-seed; inject `SyncRepository` **atau** restorer tipis |
| `WalletDao` / `WalletLocalDataSource` | 10b: `ORDER BY createdAt` pada `getPersonal` |
| `CategorySummaryLocalDataSource` | Reuse `upsert` untuk rebuild |
| Mapper | Reuse existing |

> **Siklus DI:** `SyncRepositoryImpl` memakai `WalletLocalDataSource`, **bukan** `WalletRepository`. `WalletRepositoryImpl` boleh depend `SyncRepository`. Jika Hilt cycle muncul, extract `PersonalDataRestorer` di `:core:data`.

### Feature dashboard

| File | Peran |
|------|-------|
| `DashboardViewModel` | `onScreenRendered` → `syncPersonalData()` + `retryPendingSync()` |
| `DashboardViewModelTest` | Verify pull dipanggil |
| UI | Opsional snackbar non-fatal (10b); jangan blank diam-diam |

### Docs / tests

| File | Peran |
|------|-------|
| `docs/database/firestore-rules.md` | Catatan query `ownerId` / `userId`; index opsional |
| `SyncRepositoryImplTest` | Kasus restore / skip PENDING / canonical |
| `WalletRepositoryImplTest` | Tidak mint jika pull mengisi Room |
| `SyncPersonalDataUseCaseTest` | No-op tanpa uid; delegasi repo |
| Doc ini | Source of truth eksekusi Phase 10 |

---

## 11. Struktur File Target

```
core/domain/.../
├── repository/SyncRepository.kt                 ← + syncPersonalData
└── usecase/SyncPersonalDataUseCase.kt           ← BARU

core/domain/src/test/.../
├── usecase/SyncPersonalDataUseCaseTest.kt       ← BARU
└── usecase/FakeRepositories.kt                  ← + stub

core/data/.../
├── datasource/firestore/WalletFirestoreDataSource.kt       ← +getByOwnerId
├── datasource/firestore/TransactionFirestoreDataSource.kt  ← +getByUserId
├── repository/SyncRepositoryImpl.kt             ← +syncPersonalData
├── repository/WalletRepositoryImpl.kt           ← pull-before-seed
└── db/dao/WalletDao.kt                          ← 10b ORDER BY

core/data/src/test/.../
├── repository/SyncRepositoryImplTest.kt         ← + kasus personal
└── repository/WalletRepositoryImplTest.kt       ← + tidak mint jika remote ada

features/dashboard/.../
├── presentation/DashboardViewModel.kt           ← trigger pull
└── (test) DashboardViewModelTest.kt             ← verify syncPersonal

docs/database/firestore-rules.md                 ← notes query personal
docs/dev/phases/PHASE_10_PERSONAL_WALLET_RESTORE_SYNC.md  ← dokumen ini
```

---

## 12. Desain Solusi

### 12.1 `SyncPersonalDataUseCase` (usulan)

```kotlin
class SyncPersonalDataUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val syncRepository: SyncRepository,
) {
    /**
     * Pull personal wallet + transactions for the signed-in user into Room.
     * No-op if there is no current user.
     */
    suspend operator fun invoke(userId: String? = null): Result<Unit>
}
```

Alur use case:

1. Resolve `userId` dari argumen atau `userRepository.getCurrentUser().first()?.id`  
2. Blank / null → `Result.success(Unit)` (no-op)  
3. `syncRepository.syncPersonalData(uid)`  
4. `CancellationException` re-throw; error lain → `Result.failure`

### 12.2 `syncPersonalData` (usulan)

```kotlin
// Pseudocode — SyncRepositoryImpl
override suspend fun syncPersonalData(userId: String) {
    if (userId.isBlank()) return

    val remoteWallets = walletRemote.getByOwnerId(userId)
        .filter { it.type == WalletType.PERSONAL }
    if (remoteWallets.isEmpty()) return

    val canonical = remoteWallets.minBy { it.createdAt }

    val remoteTxs = transactionRemote.getByUserId(userId, limit = PERSONAL_TX_PULL_LIMIT)
        .filter { it.walletId == canonical.id }

    val pendingWalletIds = transactionLocal.getPending().map { it.walletId }.toSet()
    val existing = walletLocal.getById(canonical.id)
    if (existing?.syncStatus != PENDING.name && canonical.id !in pendingWalletIds) {
        val computed = remoteTxs.sumOf { walletDeltaFor(it) }
        walletLocal.upsert(
            walletMapper.toEntity(
                canonical.copy(balance = computed, syncStatus = SYNCED),
            ),
        )
        if (canonical.balance != computed) {
            runCatching { walletRemote.setBalance(canonical.id, computed) }
        }
    }

    // Drop extra local PERSONAL yang bukan canonical (kecuali punya PENDING tx)
    walletLocal /* observe/get all personal */
        .filter { it.id != canonical.id && it.id !in pendingWalletIds }
        .forEach { walletLocal.delete(it.id) }

    remoteTxs.forEach { tx ->
        val existingTx = transactionLocal.getById(tx.id)
        if (existingTx?.syncStatus == PENDING.name) return@forEach
        transactionLocal.upsert(
            transactionMapper.toEntity(tx.copy(syncStatus = SYNCED)),
        )
    }

    rebuildPersonalSummaries(userId, remoteTxs)
}
```

Reuse konstanta pola 6c: `PERSONAL_TX_PULL_LIMIT = 200`.

### 12.3 Rebuild category summary

Dashboard income/expense **bukan** dari list transaksi — dari `CategorySummary` Room. Setelah reinstall, summary kosong meskipun tx sudah di-pull.

**Wajib 10a:** setelah upsert tx, rebuild summary per `yyyy-MM` dari **seluruh** tx personal kanonis yang ada di Room (bukan hanya batch remote), lalu `summaryLocal.upsert`.

Jangan `summaryRemote.upsertSummary` saat pull — itu push; remote sudah punya angka. Rebuild lokal cukup agar UI hidup. Push summary tetap lewat `syncPendingTransactions` seperti sekarang.

### 12.4 Pull-before-seed di `WalletRepositoryImpl`

```kotlin
private suspend fun ensureDefaultPersonalWallet(): Wallet? =
    ensureWalletMutex.withLock {
        local.getPersonal()?.let { return@withLock mapper.toDomain(it) }

        val uid = authNetworkDataSource.getCurrentUser()?.uid ?: return@withLock null

        syncRepository.syncPersonalData(uid)

        local.getPersonal()?.let { return@withLock mapper.toDomain(it) }

        val wallet = Wallet(
            id = UUID.randomUUID().toString(),
            ownerId = uid,
            name = "Dompet Utama",
            type = WalletType.PERSONAL,
            balance = 0L,
            currency = "IDR",
            syncStatus = SyncStatus.PENDING,
            createdAt = Instant.now(),
        )
        local.upsert(mapper.toEntity(wallet))
        syncScheduler.enqueueSync()
        wallet
    }
```

Mutex yang sudah ada **harus** mencakup pull agar dua collector `observeWallets` tidak mint ganda.

### 12.5 Reconcile orphan W_NEW (reinstall yang sudah sempat seed)

Jika user sudah sempat buka app setelah reinstall **sebelum** Phase 10 (Room sudah punya W_NEW kosong):

`onScreenRendered` → `syncPersonalData`:

- Remote: `[W_OLD (lama, ada tx), W_NEW (baru, 0)]`
- Canonical = `W_OLD`
- Lokal W_NEW: 0 tx + PENDING → **delete lokal** (jangan push ulang sebagai “sumber kebenaran”)
- Upsert W_OLD + txs → UI pindah ke saldo lama

Jangan hapus `W_NEW` di Firestore di Phase 10 (aman; hanya tidak dipakai).

### 12.6 Query Firestore (equality-only)

Ikuti 6c: **jangan** `orderBy` remote di MVP (hindari composite index).

| Method | Query | Post-process |
|--------|-------|--------------|
| `getByOwnerId` | `wallets.whereEqualTo("ownerId", uid)` | filter `type == personal` |
| `getByUserId` | `transactions.whereEqualTo("userId", uid)` | sort `date` desc, `take(limit)`, filter `walletId` |

Mapper: reuse `fromSnapshot` yang sudah ada.

### 12.7 Dashboard wiring

```kotlin
fun onScreenRendered() {
    viewModelScope.launch(dispatcher.io) {
        try {
            syncPersonalData() // Result; failure → optional errorMessage
            retryPendingSync()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { /* best-effort */ }
    }
}
```

Urutan: **pull dulu**, baru retry push — supaya W_NEW kosong tidak sempat menang sebelum restore (pada device yang sudah ter-seed).

`combine` Flows tetap baca Room; setelah upsert, UI refresh otomatis.

---

## 13. Firestore Security Rules & Indexes

### 13.1 Rules — kemungkinan besar **tidak perlu publish baru**

MVP 6c sudah:

```javascript
allow list: if signedIn();  // wallets + transactions
```

Query `where ownerId == uid` / `where userId == uid` lolos list.

**Harden (opsional, bukan blocker 10a):**

```javascript
// Ideal follow-up — query-scoped
allow list: if signedIn() && (
  resource.data.ownerId == request.auth.uid ||
  isFamilyMember(resource.data.familyId)
);
```

Firestore rules `list` + `resource.data` pada query **tidak selalu** men-constrain field yang di-query. Jangan anggap harden ini trivial — dokumentasikan sebagai follow-up keamanan (sama seperti catatan 6c).

**Jangan** longgarkan `/budgets` list di Phase 10.

### 13.2 Indexes

**MVP:** equality-only → **tidak wajib** composite index.

Opsional nanti (server `orderBy("date")` + `limit`):

| Collection | Fields | Kapan |
|------------|--------|-------|
| `wallets` | `ownerId` Asc + `type` Asc | Jika query dua field di server |
| `transactions` | `userId` Asc + `date` Desc | Jika restore remote orderBy |
| `transactions` | `walletId` Asc + `date` Desc | Jika ganti ke `getByWalletId` |

Jika `FAILED_PRECONDITION: The query requires an index` — buka link di error (project `keutrack-dev`), create, tunggu **Enabled**.

### 13.3 Docs

Update `docs/database/firestore-rules.md`:

- Section “Phase 10 notes”: pull personal by `ownerId` / `userId`
- Indexes table: baris opsional di atas
- **Tidak** wajib mengubah blok “Rules to publish” kecuali harden jadi in-scope

---

## 14. Task Breakdown Detail

### Task 0: Repro baseline

1. Akun yang sudah punya personal wallet + ≥2 transaksi (income + expense)  
2. Pastikan docs ada di Firestore Console  
3. Uninstall / clear data → install → login  
4. Catat: personal Rp 0, history kosong, wallet id **baru** di Room vs Console  
5. Simpan wallet id lama (`W_OLD`) untuk QA sesudahnya  

### Task 1: Firestore query DS

1. `WalletFirestoreDataSource.getByOwnerId(ownerId: String): List<Wallet>`  
2. `TransactionFirestoreDataSource.getByUserId(userId: String, limit: Int): List<Transaction>`  
3. Reuse `fromSnapshot`; sort + limit di client (tx)  
4. Compile `:core:data`  

### Task 2: `syncPersonalData` + tests

1. Implement di `SyncRepositoryImpl` (lihat §12.2)  
2. Additive di `SyncRepository` + KDoc  
3. Tests (`SyncRepositoryImplTest`):  
   - upsert wallet + tx remote ke lokal  
   - skip overwrite jika lokal PENDING  
   - pilih wallet tertua sebagai canonical  
   - delete extra personal lokal tanpa PENDING tx  
   - recompute balance dari tx  
   - no-op `userId` blank  

### Task 3: Rebuild category summary

1. Helper privat: group tx personal by `yyyy-MM` → `CategorySummary` → `summaryLocal.upsert`  
2. Test: setelah pull, `summaryLocal.upsert` terpanggil untuk period yang ada  
3. Pastikan `GetMonthlySummaryUseCase` (Dashboard income/expense) dapat angka non-zero  

### Task 4: `SyncPersonalDataUseCase` + Hilt

1. File baru; pola identik `SyncFamilyDataUseCase`  
2. `FakeRepositories` + `SyncPersonalDataUseCaseTest`  
3. No-op tanpa user; failure → `Result.failure`; `CancellationException` propagate  

### Task 5: Pull-before-seed

1. `WalletRepositoryImpl` inject `SyncRepository` (atau restorer)  
2. `ensureDefaultPersonalWallet` sesuai §12.4  
3. Tests:  
   - remote ada → **tidak** `upsert` wallet baru / **tidak** `enqueueSync` seed  
   - remote kosong → tetap seed `Dompet Utama`  
   - sudah ada lokal → tidak pull berulang di path `getPersonal()` early-return (pull tetap dari Dashboard)  
4. Update constructor test existing (`WalletRepositoryImplTest`)  

### Task 6: Wire Dashboard

1. Inject `SyncPersonalDataUseCase`  
2. `onScreenRendered`: pull lalu `retryPendingSync`  
3. `DashboardViewModelTest`: verify `syncPersonalData()` dipanggil  
4. 10b: map failure ke `errorMessage` non-fatal (jangan blokir UI Room)  

### Task 7: DAO polish (10b)

1. `getPersonal()` `ORDER BY createdAtEpochMs ASC LIMIT 1`  
2. Test DAO atau repo: dua personal → yang tertua  

### Task 8: Docs + QA

1. Update `firestore-rules.md` notes Phase 10  
2. QA tabel §14.9  
3. `assembleDevDebug` + unit test module terkait  
4. Protected diff kosong  

### Task 9: QA manual

| Kasus | Expected |
|-------|----------|
| Reinstall + login user lama (punya tx personal) | Saldo = jumlah delta tx; history personal muncul; **wallet id = W_OLD** |
| User baru (Firestore tanpa personal) | Tetap seed `Dompet Utama` + push |
| Reinstall **setelah** sempat buka app (sudah ada W_NEW kosong) | Dashboard refresh → pindah ke W_OLD; saldo lama kembali |
| Offline setelah restore | Room tetap tampil data terakhir |
| Family wallet / History family | Tidak regress; tidak tertimpa pull personal |
| Personal milik user A | Tidak muncul di device user B |
| Auth login/out | Tidak regresi |
| `observeWallets` dua collector bersamaan | Hanya 1 seed (mutex) |
| PENDING tx lokal (bukan reinstall) | Tidak di-overwrite pull |

---

## 15. Acceptance Criteria

- [ ] Reinstall + login akun lama → personal wallet **id sama** dengan dokumen Firestore tertua  
- [ ] Saldo personal = recompute dari transaksi yang di-pull (bukan stuck 0)  
- [ ] Transaction History personal menampilkan tx lama (IDR, kategori)  
- [ ] Dashboard income/expense bulan berjalan terisi ulang (summary rebuild)  
- [ ] User baru tanpa dokumen remote tetap mendapat seed default  
- [ ] `ensureDefaultPersonalWallet` **tidak** mint UUID jika remote PERSONAL ada  
- [ ] Pull tidak menimpa baris lokal `PENDING`  
- [ ] Extra personal lokal (orphan W_NEW kosong) dibuang; UI hanya 1 personal  
- [ ] Family pull 6c tidak regress  
- [ ] Personal user lain tidak bocor  
- [ ] Diff protected kosong: `features/auth`, `features/splashscreen`, `build-plugin`  
- [ ] Unit test path restore hijau  
- [ ] `assembleDevDebug` hijau  

```bash
./gradlew :core:domain:testDevDebugUnitTest
./gradlew :core:data:testDevDebugUnitTest
./gradlew :features:dashboard:testDevDebugUnitTest
./gradlew assembleDevDebug
git diff --stat -- features/auth features/splashscreen build-plugin
```

---

## 16. Catatan Arsitektur & Konvensi

| Aturan | Phase 10 |
|--------|----------|
| Offline-first | UI tetap dari Room; pull adalah hydrate |
| Feature ↛ feature | Dashboard hanya panggil UseCase; tidak ke Family |
| Feature ↛ Firestore / Room | Hanya domain UseCase |
| Money `Long` | Tidak berubah |
| `CancellationException` | Re-throw sebelum catch generik |
| Additive APIs | `syncPersonalData`; jangan breaking `syncFamilyData` / `GetTransactionsUseCase` |
| Protected auth | Jangan campur pull ke splash / `completeSignIn` |
| Canonical 1 personal | Mirror 6c 1 family wallet |
| `upsertWallet` | Metadata only; balance via increment / `setBalance` setelah recompute |

---

## 17. Dependency Graph

```
Dashboard open / observeWallets.onStart
        │
        ├─ ensureDefaultPersonalWallet
        │       │
        │       ├─ local.getPersonal()? → pakai lokal
        │       └─ SyncRepository.syncPersonalData(uid)
        │               ├── WalletFirestoreDataSource.getByOwnerId
        │               ├── TransactionFirestoreDataSource.getByUserId
        │               ├── Room upsert wallet + tx (skip PENDING)
        │               ├── drop extra local PERSONAL
        │               └── rebuild CategorySummary
        │       └─ masih kosong? → seed Dompet Utama + enqueue push
        │
        └─ onScreenRendered
                ├── SyncPersonalDataUseCase   ← refresh / orphan reconcile
                └── RetryPendingSyncUseCase   ← push PENDING

Room berubah
        │
        ▼
GetWalletSummary / GetTransactions / GetMonthlySummary
        │
        ▼
Dashboard personal card + recent tx + income/expense
```

---

## 18. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| Seed menang race terhadap pull | Tetap mint W_NEW | Pull **di dalam** `ensureDefault` + mutex; Dashboard pull lagi untuk orphan |
| Orphan remote menumpuk | Banyak wallet PERSONAL di Console | Canonical = tertua; jangan auto-delete remote; docs cleanup manual |
| `getPersonal() LIMIT 1` tanpa order | UI ambil W_NEW | 10b `ORDER BY createdAt ASC`; + delete extra lokal |
| Pull menimpa PENDING | Kehilangan write offline | Skip id PENDING (sama 6c) |
| `userId` query menarik tx family user | Duplikat / salah wallet | Filter `walletId == canonical`; family tetap 6c |
| Summary tidak di-rebuild | Saldo wallet OK, income/expense 0 | Task 3 wajib 10a |
| `WalletRepository` ↔ `SyncRepository` cycle | Hilt gagal | Depend local DS di sync; atau extract `PersonalDataRestorer` |
| Index missing | Query gagal | Equality-only dulu; create index dari link error |
| `allow list: if signedIn()` longgar | Leak antar user (sudah ada sejak 6c) | Bukan diperlebar; harden = follow-up |
| Offline pertama kali reinstall | Tidak bisa pull; seed kosong | Terima: seed default; pull saat online + `onScreenRendered` |
| `setBalance` PERMISSION | Remote drift | Local sudah benar; remote catch-up next pull (sama 6c) |
| Limit 200 tx | History lama terpotong | Cukup MVP; dokumentasikan pagination future |

---

## 19. Urutan Pengerjaan yang Disarankan

```
Step 1:  Repro baseline (Task 0) + catat W_OLD
Step 2:  Firestore DS getByOwnerId + getByUserId
Step 3:  syncPersonalData + unit tests (canonical, skip PENDING, recompute)
Step 4:  Rebuild CategorySummary dari tx yang di-pull
Step 5:  SyncPersonalDataUseCase + tests
Step 6:  Pull-before-seed di WalletRepositoryImpl + tests
Step 7:  DashboardViewModel.onScreenRendered wiring + VM test
Step 8:  getPersonal ORDER BY (10b) + snackbar opsional
Step 9:  firestore-rules.md notes
Step 10: QA reinstall + family regression + assembleDevDebug + protected diff
```

Jangan kerjakan Step 6 sebelum Step 3 hijau — seed tanpa pull yang benar akan terus men-generate orphan saat development.

---

## 20. Relasi ke Phase Lain

| Phase | Relasi |
|-------|--------|
| **2** | Push sync tetap; Phase 10 menambah **pull personal slice** |
| **4** | Dashboard konsumsi Room — otomatis benar setelah hydrate |
| **5** | History personal filter `walletId` — butuh id kanonis yang sama |
| **6c** | Pola yang ditiru (`syncFamilyData`, canonical, skip PENDING, recompute) |
| **6c §4.2** | “Tidak wajib pull personal” — **ditutup di Phase 10** |
| **7** | Settings connected wallets baca `GetWalletSummary` — ikut ter-restore |
| **8** | Nav tidak berubah |
| **9** | Test suite luas tetap Phase 9; Phase 10 hanya test path restore |
| **Auth / splash** | Tetap protected; restore **bukan** tanggung jawab splash |

---

## Estimasi Effort

| Bucket | Porsi |
|--------|-------|
| Firestore DS query owner/user | ~15% |
| `syncPersonalData` + summary rebuild + tests | ~35% |
| Pull-before-seed + WalletRepository tests | ~20% |
| Dashboard wiring + VM test | ~15% |
| QA reinstall + docs rules + regression family | ~15% |

---

## Ringkasan File Policy

| Kategori | Policy |
|----------|--------|
| `:core:data` Firestore DS + `syncPersonalData` | ✅ Primary |
| `WalletRepositoryImpl` pull-before-seed | ✅ UPDATE |
| `SyncPersonalDataUseCase` additive | ✅ |
| `DashboardViewModel` trigger | ✅ |
| Unit tests restore path | ✅ |
| `firestore-rules.md` notes | ✅ |
| Auth / splash / build-plugin | ❌ |
| Breaking finansial signatures | ❌ |
| Auto-delete orphan remote | ❌ |
| Budget pull / rules list | ❌ |

---

*Dokumen ini adalah plan resolusi untuk personal wallet + transaksi yang hilang setelah install ulang. Kerjakan 10a sampai reinstall menampilkan wallet id dan saldo yang sama dengan Firestore, lalu 10b bila sempat. Phase 9 (test suite luas) tetap terpisah.*
