# Phase 6c — Shared Family Data Sync (History / Insights lintas anggota)

> **Modul target:** `:core:data` (+ domain **additive** tipis) · konsumsi di `:features:family` · rules di `docs/database/firestore-rules.md` + Firebase Console  
> **Estimasi:** ~2–3.5 hari  
> **Prasyarat:** Phase 6a ✅ · Phase 6b ✅ (create/join `FamilyGroup` + `User.familyId` + Insights lokal) · rules `family_groups` (termasuk join `arrayUnion`) sudah publish  
> **Status baseline:** Membership jalan; **History/Insights family tidak shared** — tiap anggota punya wallet UUID sendiri; sync **push-only**; Firestore rules wallet/tx **owner-only** + `list: false`  
> **Hasil akhir:** Anggota keluarga melihat **satu dompet keluarga kanonis** dan **transaksi bersama** di Family History Log / Breakdown (offline-first: pull → Room → UI)

---

## Daftar Isi

1. [Konteks & Tujuan](#1-konteks--tujuan)
2. [Inventory — Apa yang Sudah Ada](#2-inventory--apa-yang-sudah-ada)
3. [Root Cause (Kenapa User B Tidak Lihat History A)](#3-root-cause-kenapa-user-b-tidak-lihat-history-a)
4. [Keputusan Desain](#4-keputusan-desain)
5. [Scope — Apa yang Dikerjakan](#5-scope--apa-yang-dikerjakan)
6. [Scope — Apa yang TIDAK Dikerjakan](#6-scope--apa-yang-tidak-dikerjakan)
7. [Prasyarat (Definition of Ready)](#7-prasyarat-definition-of-ready)
8. [File Referensi (Read-Only)](#8-file-referensi-read-only)
9. [File yang TIDAK BOLEH Diubah](#9-file-yang-tidak-boleh-diubah)
10. [File yang BOLEH Diubah / Dibuat](#10-file-yang-boleh-diubah--dibuat)
11. [Struktur File Target](#11-struktur-file-target)
12. [Desain Solusi](#12-desain-solusi)
13. [Firestore Security Rules (Wajib)](#13-firestore-security-rules-wajib)
14. [Task Breakdown Detail](#14-task-breakdown-detail)
15. [Acceptance Criteria](#15-acceptance-criteria)
16. [Catatan Arsitektur & Konvensi](#16-catatan-arsitektur--konvensi)
17. [Dependency Graph](#17-dependency-graph)
18. [Risiko & Mitigasi](#18-risiko--mitigasi)
19. [Urutan Pengerjaan yang Disarankan](#19-urutan-pengerjaan-yang-disarankan)
20. [Relasi ke Phase Lain](#20-relasi-ke-phase-lain)

---

## 1. Konteks & Tujuan

Setelah Phase 6b:

| Area | Status |
|------|--------|
| Create / join `FamilyGroup` + invite code | ✅ |
| `User.familyId` / `familyRole` via `updateFamilyMembership` | ✅ |
| Family Insights dari Room (IDR) | ✅ lokal per device |
| Family History Log lintas anggota | ❌ kosong di device anggota lain |
| Satu shared family wallet | ❌ tiap join create UUID baru |
| Pull sync wallet/tx by `familyId` | ❌ belum ada |
| Rules read shared wallet/tx | ❌ owner-only |

**Bug produk yang dilaporkan:** User A menulis transaksi di wallet Family → User B join keluarga yang sama → Family History Log di device B **tidak** menampilkan transaksi A (`PERMISSION_DENIED` join sudah diperbaiki; ini gap data sharing).

**Tujuan Phase 6c:**
1. **Canonical family wallet** — satu `wallet.id` per `familyId` (dibuat owner; joiner **link/fetch**, bukan mint UUID baru)
2. **Pull sync** — unduh wallet + transaksi family ke Room setelah create/join dan saat screen family terbuka
3. **Rules** — anggota `family_groups` boleh **read/list** wallet & transaksi dengan `familyId` yang sama
4. **Insights** — filter history/breakdown tetap via shared `walletId` (atau additive `familyId` jika perlu)

**Bukan tujuan Phase 6c:**
- Phase 7 Settings currency / Google Sheets
- Conflict resolution multi-writer kompleks (last-write-wins / CRDT)
- Shared budget CRUD UI penuh
- QR invite
- Auth/splash changes

---

## 2. Inventory — Apa yang Sudah Ada

### Membership (6b)

| Item | Lokasi |
|------|--------|
| `FamilyGroup`, `FamilyRole`, `FamilyRepository` | `:core:domain` |
| `CreateFamilyGroupUseCase` / `JoinFamilyGroupUseCase` | `:core:domain` — keduanya `ensure*FamilyWallet` dengan `UUID.randomUUID()` |
| `FamilyGroupFirestoreDataSource` | `:core:data` |
| `UserRepository.updateFamilyMembership` | additive |

### Insights (6a)

| Item | Perilaku |
|------|----------|
| `FamilyViewModel` | `resolveFamilyWallet` → `GetTransactionsUseCase(walletId = …)` |
| `FamilyUiMapper` | Breakdown dari txs family wallet lokal |

### Sync (Phase 2)

| Item | Perilaku |
|------|----------|
| `SyncRepositoryImpl` | Push `PENDING` wallets/budgets/transactions saja |
| Wallet / Transaction Firestore DS | `upsert*` / delete — **tidak ada** query by `familyId` |

### Rules

| Path | Akses hari ini |
|------|----------------|
| `/wallets` | owner only; `list: false` |
| `/transactions` | owner only; `list: false` |
| `/family_groups` | members + join append `memberIds` |

---

## 3. Root Cause (Kenapa User B Tidak Lihat History A)

```
User A create family F
  → Wallet W_A (id baru, familyId=F, ownerId=A)
  → Tx T1 (walletId=W_A, userId=A) → push ke Firestore

User B join F
  → Wallet W_B (id BARU, familyId=F, ownerId=B)   ← bukan W_A
  → Room B hanya punya W_B (+ txs B)
  → Insights filter walletId=W_B → tidak ada T1

Plus:
  Sync = push-only (B tidak pernah pull T1)
  Rules = B tidak boleh get/list dokumen tx milik A
```

Join KDoc di `JoinFamilyGroupUseCase` sudah mengakui gap ini sebagai “multi-device sync later”.

---

## 4. Keputusan Desain

### 4.1 Satu wallet kanonis per family

| Opsi | Keputusan |
|------|-----------|
| A. Satu `wallet.id` shared (owner create; joiner fetch by `familyId`) | ✅ **Dipilih** |
| B. Banyak wallet per anggota + aggregate by `familyId` di query | ❌ Lebih rumit untuk balance/side-effects sync batch |

**Kontrak:**
- Create family → create wallet `type=FAMILY`, `familyId=F`, `id` stabil, sync ke Firestore
- Join family → **jangan** `UUID.randomUUID()` jika remote wallet untuk `F` sudah ada → `getWalletsByFamilyId(F)` → upsert ke Room dengan **id yang sama**
- Legacy: jika user B sudah punya W_B orphan (familyId=F tapi id ≠ W_A), migrasi ringan: prefer wallet remote kanonis; opsional soft-hide/delete lokal orphan (dokumentasikan)

### 4.2 Pull sync scope (MVP)

Pull **hanya** data family yang dibutuhkan Insights:

1. Wallet(s) `where familyId == F` (harap 1 dokumen)
2. Transactions `where familyId == F` (atau `where walletId == W_shared`) orderBy date desc, limit N (mis. 200)

Tidak wajib pull semua personal wallets anggota lain.

### 4.3 Filter Insights

- Prefer tetap `walletId = canonicalFamilyWalletId` setelah pull
- Additive (opsional): `GetTransactionsUseCase.Params.familyId` + DAO `WHERE familyId = :familyId` — hanya jika multi-wallet per family terpaksa; default hindari breaking change

### 4.4 Offline-first tetap berlaku

```
Pull remote → merge Room (upsert by id)
UI baca Room saja
Write lokal → PENDING → push (existing SyncWorker)
```

Conflict: **last remote write wins** pada pull upsert; cukup untuk MVP 2 anggota.

---

## 5. Scope — Apa yang Dikerjakan

| # | Item |
|---|------|
| 1 | Firestore DS: `getWalletsByFamilyId`, `getTransactionsByFamilyId` (atau by `walletId`) |
| 2 | Local: `WalletDao` / `TransactionDao` helper upsert batch; opsional `observeByFamilyId` |
| 3 | `FamilyRepository` atau use case baru: `SyncFamilyDataUseCase` / `EnsureSharedFamilyWalletUseCase` |
| 4 | Ubah `CreateFamilyGroupUseCase` — tetap create wallet, pastikan push segera / enqueue sync |
| 5 | Ubah `JoinFamilyGroupUseCase` — fetch+upsert shared wallet; **hapus** mint UUID bila remote ada |
| 6 | `FamilyViewModel` / splash-after-join: panggil pull family data (IO + `CancellationException` pattern) |
| 7 | Update `docs/database/firestore-rules.md` + publish rules shared read/list |
| 8 | Composite indexes Firestore (`familyId`, `date`) jika query butuh |
| 9 | QA 2 akun: A tulis → B join/pull → B lihat History; B tulis → A pull/refresh → A lihat |

---

## 6. Scope — Apa yang TIDAK Dikerjakan

| Item | Alasan | Phase |
|------|--------|-------|
| Realtime listener penuh multi-device (selain pull on open) | Over-scope; pull on Family tab cukup MVP | Future |
| Shared edit/delete ACL rumit | Owner-write / member-write sama dulu | Future |
| Merge balance konflik batch Firestore | Risky; keep existing side-effect writer | — |
| Settings currency / Sheets | Phase 7 | Phase 7 |
| Auth / splash / build-plugin | Protected | — |
| Breaking rename entity Phase 1 | Freeze | — |
| Unit test penuh | Phase 9 | — |

---

## 7. Prasyarat (Definition of Ready)

1. User A bisa create family + dapat invite code  
2. User B bisa join (rules join `memberIds` sudah OK)  
3. User A bisa catat transaksi di wallet Family; muncul di History A  
4. Sync push transaksi A ke Firestore terlihat di Console (`transactions` + `familyId`)  
5. Build hijau: `./gradlew assembleDevDebug`

```bash
./gradlew :core:domain:compileDevDebugKotlin
./gradlew :core:data:compileDevDebugKotlin
./gradlew :features:family:compileDevDebugKotlin
./gradlew assembleDevDebug
```

---

## 8. File Referensi (Read-Only)

| File | Pelajari |
|------|----------|
| `docs/dev/phases/PHASE_6_FAMILY_INSIGHTS_AND_MEMBERSHIP.md` | 6a/6b kontrak; filter wallet |
| `docs/dev/phases/PHASE_2_ROOM_AND_REPOSITORY_IMPLEMENTATION.md` | Offline-first; sync push |
| `docs/database/firestore-rules.md` | Rules baseline + gap shared access |
| `CreateFamilyGroupUseCase.kt` / `JoinFamilyGroupUseCase.kt` | Wallet ensure saat ini |
| `FamilyViewModel.kt` / `FamilyUiMapper.kt` | Resolve wallet + history |
| `SyncRepositoryImpl.kt` | Push-only surface |
| `WalletFirestoreDataSource.kt` / `TransactionFirestoreDataSource.kt` | Pola upsert |
| `FamilyGroupFirestoreDataSource.kt` | Pola query Firestore |
| `.cursor/rules/keutrack-data-layer.mdc` | CancellationException, offline-first |
| `.cursor/skills/keutrack-dev/SKILL.md` | Protected files |

---

## 9. File yang TIDAK BOLEH Diubah

| Area | Alasan |
|------|--------|
| `features/auth/**`, `features/splashscreen/**` | Protected |
| `build-plugin/**`, root Gradle, `local.properties` | Stable |
| Auth sign-in / `completeSignIn` / rollback | Jangan sentuh |
| Breaking change signature UseCase finansial existing | Hanya **additive** param default |
| Theme hex / DS rename | Consume only |
| Phase 7 Settings currency/Sheets | Out of scope |

**Hati-hati:**
- `UserRepository` — hanya pakai API membership yang sudah ada; jangan ubah auth
- Sync batch transaction side-effects — jangan pecah invariant balance kecuali perlu set `familyId` konsisten

---

## 10. File yang BOLEH Diubah / Dibuat

### Domain (additive)

| File | Peran |
|------|-------|
| `WalletRepository` | + `suspend fun getWalletsByFamilyId` / observe — atau lewat sync use case saja |
| `SyncRepository` atau use case baru | `syncFamilyData(familyId: String)` |
| `JoinFamilyGroupUseCase` / `CreateFamilyGroupUseCase` | REWRITE wallet ensure |
| `GetTransactionsUseCase.Params` | OPSIONAL `familyId: String? = null` |

### Data

| File | Peran |
|------|-------|
| `WalletFirestoreDataSource` | + query by `familyId` |
| `TransactionFirestoreDataSource` | + query by `familyId` / `walletId` |
| `WalletLocalDataSource` / DAO | upsert batch; opsional query by familyId |
| `TransactionLocalDataSource` / DAO | upsert batch dari pull |
| `SyncRepositoryImpl` atau `FamilyDataSync*` | orchestrate pull |
| Mapper | reuse existing |

### Feature family

| File | Peran |
|------|-------|
| `FamilyViewModel` | panggil sync family on render / after membership |
| UI | loading/error tipis jika pull gagal (jangan blank diam-diam) |

### Docs

| File | Peran |
|------|-------|
| `docs/database/firestore-rules.md` | Shared read/list rules + index notes |
| Doc ini | Source of truth eksekusi 6c |

---

## 11. Struktur File Target

```
core/domain/.../
├── usecase/SyncFamilyDataUseCase.kt          ← BARU (nama final bebas)
├── usecase/JoinFamilyGroupUseCase.kt         ← UPDATE wallet ensure
└── usecase/CreateFamilyGroupUseCase.kt       ← UPDATE (pastikan wallet push)

core/data/.../
├── datasource/firestore/WalletFirestoreDataSource.kt      ← +query
├── datasource/firestore/TransactionFirestoreDataSource.kt ← +query
├── repository/SyncRepositoryImpl.kt          ← atau FamilyDataSync helper
└── db/dao/WalletDao.kt / TransactionDao.kt   ← upsert / filter additive

features/family/.../FamilyViewModel.kt        ← trigger pull

docs/database/firestore-rules.md              ← UPDATE publish block
```

---

## 12. Desain Solusi

### 12.1 `SyncFamilyDataUseCase` (usulan)

```kotlin
class SyncFamilyDataUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val familyRepository: FamilyRepository,
    private val walletRepository: WalletRepository,
    // data-facing ports atau SyncRepository additive
) {
    /**
     * Pull shared wallet + transactions for current user's familyId into Room.
     * No-op if user.familyId is null.
     */
    suspend operator fun invoke(): Result<Unit>
}
```

Alur:

1. Baca `user.familyId`  
2. Remote: wallets by `familyId` → upsert Room (`syncStatus = SYNCED`)  
3. Pilih canonical wallet: prefer `type=FAMILY` + `familyId` match; jika >1, ambil tertua / owner’s  
4. Remote: transactions by `familyId` (limit N) → upsert Room  
5. Return success; UI Flow Room otomatis refresh  

### 12.2 Join wallet ensure (ganti perilaku)

```kotlin
// Pseudocode
val remoteWallets = walletRemote.getByFamilyId(family.id)
if (remoteWallets.isNotEmpty()) {
    remoteWallets.forEach { local.upsert(it.copy(syncStatus = SYNCED)) }
} else {
    // Edge: owner belum sync wallet — create sekali + enqueue push
    createCanonicalFamilyWallet(...)
}
```

### 12.3 Create wallet

Tetap create lokal + `enqueueSync`. Opsional: setelah create, `syncFamilyData()` no-op untuk owner (data sudah lokal).

### 12.4 Kapan pull dipanggil

| Trigger | Wajib? |
|---------|--------|
| Setelah join sukses | ✅ |
| `FamilyViewModel.onScreenRendered()` / `LaunchedEffect` | ✅ |
| Setelah create | Opsional |
| Pull periodik / snapshot listener | ❌ Future |

---

## 13. Firestore Security Rules (Wajib)

Tanpa ini, pull tetap `PERMISSION_DENIED`.

### Helper (usulan)

```javascript
function isFamilyMember(familyId) {
  return signedIn()
    && familyId is string
    && familyId.size() > 0
    && request.auth.uid in
      get(/databases/$(database)/documents/family_groups/$(familyId)).data.memberIds;
}
```

### Wallets (additive pada match existing)

```javascript
match /wallets/{walletId} {
  allow create: if signedIn()
    && request.resource.data.ownerId == request.auth.uid;

  allow get: if signedIn() && (
    resource == null
    || resource.data.ownerId == request.auth.uid
    || isFamilyMember(resource.data.familyId)
  );

  // Pull by familyId needs list/query
  allow list: if signedIn();  // MVP — tighten later with query constraints if possible

  allow update, delete: if signedIn()
    && resource.data.ownerId == request.auth.uid;
}
```

> **Catatan keamanan:** `allow list: if signedIn()` longgar untuk MVP. Idealnya batasi dengan rules yang cocok dengan query `where familyId == X` + membership check. Dokumentasikan follow-up harden.

### Transactions (serupa)

```javascript
match /transactions/{txId} {
  allow create: if signedIn()
    && request.resource.data.userId == request.auth.uid;

  allow get: if signedIn() && (
    resource == null
    || resource.data.userId == request.auth.uid
    || isFamilyMember(resource.data.familyId)
  );

  allow list: if signedIn(); // MVP — harden later

  allow update, delete: if signedIn()
    && resource.data.userId == request.auth.uid;
}
```

### Index

Console → Indexes (jika diminta runtime):

- `transactions`: `familyId` Asc + `date` Desc  
- `wallets`: `familyId` Asc + `type` Asc (opsional)

### Publish

Update **Rules to publish** di `docs/database/firestore-rules.md` lalu **Publish** di Firebase Console sebelum QA 2-device.

---

## 14. Task Breakdown Detail

### Task 0: Repro baseline

1. Dua akun (atau 2 emulator)  
2. A create + catat 1 tx Family → sync  
3. B join → History B kosong → screenshot/log sebagai baseline  

### Task 1: Firestore query DS

1. `WalletFirestoreDataSource.getByFamilyId(familyId)`  
2. `TransactionFirestoreDataSource.getByFamilyId(familyId, limit)`  
3. Map Timestamp → domain (reuse mapper patterns)  

### Task 2: Room upsert dari pull

1. Batch upsert wallet/tx tanpa menandai PENDING  
2. Jangan overwrite PENDING lokal dengan remote lebih lama jika conflict sederhana (MVP: remote wins kecuali local PENDING untuk id yang sama — dokumentasikan)  

### Task 3: `SyncFamilyDataUseCase` + Hilt

1. Implement + bind dependencies  
2. Unit-less compile `:core:domain` / `:core:data`  

### Task 4: Fix Create/Join wallet

1. Join: fetch remote first  
2. Create: single canonical wallet; enqueue sync  
3. Hapus / guard path mint UUID ganda untuk `familyId` yang sama  

### Task 5: Wire FamilyViewModel

1. `onScreenRendered()` → `syncFamilyData()` di `dispatcher.io`  
2. Setelah join success message, trigger sync  
3. Error → snackbar non-fatal  

### Task 6: Rules + indexes + docs

1. Update `firestore-rules.md` full publish block  
2. Publish Console  
3. Create indexes jika error  

### Task 7: QA

| Kasus | Expected |
|-------|----------|
| B join setelah A punya txs | B History menampilkan txs A (attribution `addedByName`) |
| B catat tx | Setelah A buka Family tab (pull), A melihat tx B |
| Offline B | Room B tetap tampil data terakhir yang sudah di-pull |
| Personal wallet | Tidak bocor ke anggota lain |
| Auth login/out | Tidak regresi |

---

## 15. Acceptance Criteria

- [ ] Join **tidak** membuat wallet UUID baru jika wallet `familyId` sudah ada di Firestore  
- [ ] Create & join berbagi **wallet.id** yang sama di Room kedua device (setelah sync/pull)  
- [ ] Pull family data mengisi Room dengan transaksi anggota lain  
- [ ] Family History Log di device B menampilkan transaksi A (IDR, attribution)  
- [ ] Breakdown/insight family memakai data shared (bukan hanya txs lokal B)  
- [ ] Rules + docs ter-update; create/join/pull tidak `PERMISSION_DENIED` untuk member  
- [ ] Personal txs/wallets tetap private  
- [ ] `assembleDevDebug` hijau  
- [ ] Diff protected: `features/auth`, `splashscreen`, `build-plugin` kosong  

```bash
./gradlew assembleDevDebug
git diff --stat -- features/auth features/splashscreen build-plugin
```

---

## 16. Catatan Arsitektur & Konvensi

| Aturan | Phase 6c |
|--------|----------|
| Offline-first | UI tetap dari Room; pull adalah hydrate |
| Feature ↛ feature | Sync di domain/data; family hanya panggil use case |
| Money `Long` | Tidak berubah |
| `CancellationException` | Re-throw sebelum catch generik |
| Additive APIs | Prefer; jangan breaking `GetTransactionsUseCase` kecuali default null |
| Protected auth | Jangan campur pull ke `completeSignIn` |

---

## 17. Dependency Graph

```
Join / FamilyScreen open
        │
        ▼
SyncFamilyDataUseCase
        ├── UserRepository.getCurrentUser (familyId)
        ├── WalletFirestoreDataSource.getByFamilyId
        ├── TransactionFirestoreDataSource.getByFamilyId
        └── Room upsert
                │
                ▼
FamilyViewModel combine Flows (existing)
        └── GetTransactionsUseCase(walletId = canonical)
                │
                ▼
        Family History / Breakdown UI
```

Rules: `isFamilyMember(familyId)` harus true agar get/list sukses.

---

## 18. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| `allow list: if signedIn()` terlalu longgar | Data leak antar user | MVP + harden query-scoped rules segera setelah hijau; batasi field di docs |
| Owner belum push wallet saat B join | B tidak dapat canonical | Fallback create sekali **atau** retry pull + snackbar “tunggu sync owner” |
| Orphan W_B dari 6b testing | Duplikat wallet lokal | Prefer remote id; dokumentasikan hapus orphan manual / migration kecil |
| Index missing | Query gagal | Link create index dari error log |
| Pull menimpa PENDING lokal | Kehilangan write offline | Skip overwrite id yang masih PENDING |
| Balance side-effect multi-writer | Race | Tetap single-writer semantics existing; 6c fokusokus read-share dulu |
| `get()` family_groups di rules mahal | Latency rules | Acceptable MVP; cache membership di custom claims = future |

---

## 19. Urutan Pengerjaan yang Disarankan

```
Step 1: Rules draft + publish (shared get/list) + indexes
Step 2: Firestore DS query wallet/tx by familyId
Step 3: Room upsert pull path
Step 4: SyncFamilyDataUseCase
Step 5: Rewrite Join (dan Create) wallet ensure → canonical id
Step 6: FamilyViewModel trigger pull
Step 7: QA 2 akun History shared
Step 8: Update firestore-rules.md “Rules to publish” agar match Console
Step 9: assembleDevDebug + protected diff check
```

---

## 20. Relasi ke Phase Lain

| Phase | Relasi |
|-------|--------|
| **6a** | Insights UI — dikonsumsi; filter tetap |
| **6b** | Membership — prasyarat; wallet ensure diganti di 6c |
| **7** | Settings persistence — tidak diblokir 6c; family ID copy sudah ada |
| **2** | Sync push tetap; 6c menambah **pull family slice** |
| **9** | Tests untuk SyncFamilyData / rules — belakangan |

---

## Estimasi Effort

| Bucket | Porsi |
|--------|-------|
| Rules + indexes + docs | ~15% |
| Firestore query DS + Room upsert | ~30% |
| SyncFamilyData + Create/Join wallet fix | ~30% |
| FamilyViewModel wiring + QA 2-device | ~25% |

---

## Ringkasan File Policy

| Kategori | Policy |
|----------|--------|
| `:core:data` Firestore DS + sync pull | ✅ Primary |
| Create/Join use cases wallet ensure | ✅ UPDATE |
| `FamilyViewModel` trigger | ✅ |
| `firestore-rules.md` + Console publish | ✅ Wajib |
| Domain UseCase sync baru | ✅ Additive |
| Auth / splash / build-plugin | ❌ |
| Breaking finansial signatures | ❌ |
| Phase 7 Settings currency | ❌ |

---

*Dokumen ini adalah plan resolusi untuk shared Family History/Insights lintas anggota setelah Phase 6b. Kerjakan 6c sampai dua akun melihat transaksi family yang sama di Room, lalu lanjut Phase 7 (Settings persistence) bila siap.*
