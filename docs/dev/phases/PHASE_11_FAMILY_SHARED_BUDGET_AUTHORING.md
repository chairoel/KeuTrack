# Phase 11 — Family Shared Budget Authoring (Limit per Kategori)

> **Modul target:** `:features:family` (+ domain/data **additive** untuk upsert/pull budget; rules di `docs/database/firestore-rules.md`)
> **Estimasi:** ~3–4.5 hari total · **11a** ~2–2.5 hari · **11b** ~1–2 hari
> **Prasyarat:** Phase 1–2 ✅ (Budget entity + repo + push sync) · Phase 5 ✅ (kategori + transaksi family) · Phase 6a/6b ✅ (Insights + membership) · Phase 6c ✅ (canonical family wallet + tx pull)
> **Status kode (siap push):** 11a+11b di branch `feat/family-budget-authoring`. Owner bisa set/ubah/hapus limit dari tab Family; Shared Budgets = spent/limit + 4 tone awareness. Pull masuk `syncFamilyData`. Rules 11b ada di `firestore-rules.md` — **belum close phase** sampai Publish + index Enabled + QA §18.
> **Sisa setelah push:** Task 6 QA 1 device · Publish rules + index · QA 2 akun §18.2 · Task 9 tes/assemble.

---

## Daftar Isi

1. [Konteks & Tujuan](#1-konteks--tujuan)
2. [Inventory — Apa yang Sudah Ada](#2-inventory--apa-yang-sudah-ada)
3. [Root Cause (Kenapa Limit Tidak Bisa Diatur)](#3-root-cause-kenapa-limit-tidak-bisa-diatur)
4. [Keputusan Produk](#4-keputusan-produk)
5. [Scope — Apa yang Dikerjakan](#5-scope--apa-yang-dikerjakan)
6. [Scope — Apa yang TIDAK Dikerjakan](#6-scope--apa-yang-tidak-dikerjakan)
7. [Prasyarat (Definition of Ready)](#7-prasyarat-definition-of-ready)
8. [File Referensi (Read-Only)](#8-file-referensi-read-only)
9. [File yang TIDAK BOLEH Diubah](#9-file-yang-tidak-boleh-diubah)
10. [File yang BOLEH Diubah / Dibuat](#10-file-yang-boleh-diubah--dibuat)
11. [Struktur File Target](#11-struktur-file-target)
12. [Desain UX](#12-desain-ux)
13. [Desain Domain & Use Case](#13-desain-domain--use-case)
14. [Desain Data: Matching, Spent Seed, Uniqueness](#14-desain-data-matching-spent-seed-uniqueness)
15. [Firestore Security Rules & Pull (11b)](#15-firestore-security-rules--pull-11b)
16. [Pemetaan UI → State / Use Case](#16-pemetaan-ui--state--use-case)
17. [Task Breakdown Detail](#17-task-breakdown-detail)
18. [Acceptance Criteria](#18-acceptance-criteria)
19. [Catatan Arsitektur & Konvensi](#19-catatan-arsitektur--konvensi)
20. [Dependency Graph](#20-dependency-graph)
21. [Risiko & Mitigasi](#21-risiko--mitigasi)
22. [Urutan Pengerjaan yang Disarankan](#22-urutan-pengerjaan-yang-disarankan)
23. [Relasi ke Phase Lain](#23-relasi-ke-phase-lain)
24. [Rencana Commit](#24-rencana-commit)

---

## 1. Konteks & Tujuan

Setelah Phase 6c, keluarga punya **wallet bersama** dan **transaksi shared**. Tab Family memisahkan:

| Kartu | Data hari ini |
|-------|----------------|
| Family Breakdown | Pengeluaran bulan ini **per anggota** (`userId` / `addedByName`) |
| Shared Budgets | Pengeluaran bulan ini **per kategori** |
| History | Transaksi family (`addedByName`) |

Shared Budgets **terlihat seperti budget** (progress bar, `spent / cap`), tetapi cap-nya **bukan limit** — itu total pengeluaran keluarga, karena tidak ada baris `Budget` di Room.

Create family (`CreateFamilyGroupUseCase`) hanya:

1. `FamilyGroup` + invite code
2. `User.familyId` / `familyRole = owner`
3. Wallet `WalletType.FAMILY`

Tidak ada langkah “set target Household Rp 2.000.000”. Dialog membership hanya nama / kode. Ini **by design Phase 6** (CRUD budget ditunda).

Assessment (`docs/dev/Project_Assessment_Current.md`) menandai **Budget authoring UI** sebagai sisa kerja. Phase 6 §5: *“Edit budget targets dari Adjust Targets penuh — CRUD budget UI terpisah / later.”* Phase 6c §1: *“Shared budget CRUD UI penuh”* di luar scope.

**Tujuan Phase 11:**

1. **11a — Authoring:** Owner set/ubah/hapus limit per kategori untuk **bulan berjalan** dari tab Family (sheet, bukan dialog create-family).
2. **11a — Progress jujur:** Jika `Budget` ada → Shared Budgets = `spent / limit` + footnote, dengan **warna bar hijau / kuning / oranye / merah** (merah mulai **> 90%** sampai melewati limit). Jika tidak ada limit → tetap porsi dari total + warna kategori (perilaku sekarang).
3. **11a — Matching benar:** Transaksi family meng-increment budget **family** (bukan budget personal kategori yang sama).
4. **11b — Shared sungguhan:** Anggota lain pull budget `familyId` ke Room; rules mengizinkan read membership, bukan hanya `userId` creator.

**Bukan tujuan Phase 11:**

- Seed budget default saat create/join family
- Budget personal (dompet sendiri) di Dashboard
- Copy-forward limit ke bulan berikutnya otomatis
- Multi-admin ACL / approval
- Realtime listener budget
- Redesign visual Atelier / auth / splash / build-plugin

---

## 2. Inventory — Apa yang Sudah Ada

### Feature family

| File | Peran vs Phase 11 |
|------|-------------------|
| `FamilyScreen.kt` | Sheet Atur Target + CTA owner; membership dialog tetap tanpa field limit ✅ |
| `FamilyViewModel.kt` | Upsert/delete + sheet events; snackbar `budgetMessage` ✅ |
| `FamilyUiMapper.kt` | Overlay limit + 4 tone awareness; `canEditBudgets` owner+wallet ✅ |
| `FamilyUIState.kt` | `canEditBudgets`, `budgetSheet`, `isBudgetSaving`, `budgetMessage` ✅ |
| `FamilyBudgetRowUi` | `categoryId` + `hasLimit` + tone Success/Watch/Critical/Error/Neutral ✅ |
| `FamilySharedBudgetsCard.kt` | Tap row + header Atur jika `canEditBudgets` ✅ |
| `FamilySavingTogetherCard.kt` | CTA “Atur Target” hanya untuk owner ✅ |
| `FamilyMembershipDialog.kt` | Create/join nama/kode — **jangan** tambah form limit di sini |

### Domain / data

| Item | Status |
|------|--------|
| `Budget` (`limit`, `spent`, `categoryId`, `month`, `familyId`, `walletId`) | ✅ |
| `BudgetRepository.createBudget` / `updateBudget` / `deleteBudget` | ✅ Room + enqueue push |
| `GetBudgetProgressUseCase(month)` | ✅ observe Room by month |
| `CreateBudgetUseCase` / `UpsertFamilyBudgetUseCase` | ✅ `UpsertFamilyBudgetUseCase` + `DeleteFamilyBudgetUseCase` |
| `BudgetDao.getByMonthCategoryAndFamily` / `Personal` | ✅ matching family vs personal |
| `TransactionRepositoryImpl.addTransaction` | ✅ increment budget scoped family/personal |
| Push `syncPendingBudgets()` | ✅ |
| Pull budget by `familyId` | ✅ `syncFamilyData` hydrate current + prior month; skip PENDING/FAILED |
| Firestore `/budgets` get/list | ✅ docs: membership get + signed-in list; **Publish** + index masih manual |

### Design system

| API | Pakai untuk |
|-----|-------------|
| `KeuTrackModalBottomSheet` | Sheet “Atur Target” |
| `KeuTrackTextField` | Input limit IDR |
| `KeuTrackButton` | Simpan / hapus |
| `CurrencyFormat.formatIdr` | Preview + labels |
| `KeuTrackProgressBar` | Shared Budgets: `hasLimit` → Success/Warning/Caution/Danger; else `fillColor` kategori |
| `KeuTrackProgressTone` | Additive `Warning` (`w300`) + `Caution` (`w500`) ✅ |

### Kategori

`GetCategoriesUseCase(CategoryType.EXPENSE)` sudah dipakai New Entry. Family sheet **reuse** daftar expense category — jangan hardcode Household/Education.

---

## 3. Root Cause (Kenapa Limit Tidak Bisa Diatur)

Tiga gap terpisah:

```
Create/Join family
  → FamilyGroup + FAMILY wallet
  → TIDAK menulis /budgets

Tab Family “Adjust Targets”
  → onAdjustTargetsClick = {}
  → tidak ada sheet, tidak ada use case

Shared Budgets mapper
  → rows dari transaksi per kategori
  → limit hanya overlay jika Budget ada di Room
  → Room kosong → cap = monthly total (bukan target)
```

Plus gap **sharing** (meski authoring sudah ada di device owner):

```
Owner createBudget → Room + PENDING → push Firestore (userId = owner)
Anggota B buka Family
  → syncFamilyData pull wallet + tx saja
  → observeBudgets(month) di device B = []
  → B tidak melihat limit
  → rules list: false memblokir query familyId
```

---

## 4. Keputusan Produk

| # | Keputusan | Alasan |
|---|-----------|--------|
| P1 | **Jangan** taruh setup limit di dialog Buat/Gabung Keluarga | Dialog harus tetap 1 field. Limit diatur setelah wallet ada. |
| P2 | Authoring di **tab Family** setelah `familyId` terisi | Konteks visual sudah Shared Budgets + Adjust Targets. |
| P3 | Satu budget = **satu kategori + satu bulan + satu familyId** | Selaras `Budget.month` (`yyyy-MM`) + `categoryId`. |
| P4 | Hanya **bulan berjalan** di MVP | Tidak copy-forward; bulan baru = set ulang (atau tanpa limit = porsi total). |
| P5 | Write: **owner only** (`familyRole == owner`) | MVP; anggota read-only. |
| P6 | Tidak seed kategori/limit default | Tiap keluarga beda; hindari data sampah. |
| P7 | Create budget **seed `spent`** dari expense family bulan ini untuk kategori itu | Kalau sudah ada txs, progress tidak mulai dari Rp 0. |
| P8 | Update limit **tidak** reset `spent` | Hanya `limit` + `PENDING`. |
| P9 | Tanpa budget → UI tetap porsi total (perilaku sekarang) | Jangan kosongkan kartu hanya karena belum ada limit. |
| P10 | Amount `Long` rupiah, tanpa desimal | Konvensi finansial KeuTrack. |
| P11 | 11a boleh ship tanpa 11b, tapi **dokumentasikan** “limit hanya di device owner” sampai 11b hijau | Jangan klaim “shared” sebelum pull + rules. |
| P12 | Matching transaksi→budget harus **scoped family/wallet** | Cegah expense family menaikkan budget personal (bug `getByMonthAndCategory`). |
| P13 | Progress bar **setelah ada limit** = hijau → kuning → oranye → **merah dari >90% s.d. over**, bukan warna kategori | Rampa panas intuitif (bukan biru-sebelum-hijau). Merah lebih awal supaya user sadar sebelum tembus. Warna kategori tetap di picker / optional dot judul. Tanpa limit, bar boleh tetap warna kategori. |

### Mengapa bukan keypad New Entry?

Set budget jarang. Sheet dengan `KeuTrackTextField` digit-only + preview `CurrencyFormat` cukup. Jangan copy seluruh keypad New Entry (over-scope).

---

## 5. Scope — Apa yang Dikerjakan

### A. Phase 11a — Authoring di device owner (wajib dulu)

| # | Item |
|---|------|
| 1 | `UpsertFamilyBudgetUseCase` — validasi + create-or-update by (`familyId`, `categoryId`, `month`) |
| 2 | `DeleteFamilyBudgetUseCase` — owner hapus target kategori bulan ini |
| 3 | Perbaiki lookup budget: `getByMonthCategoryAndFamily` / wallet (bukan `LIMIT 1` global) |
| 4 | Seed `spent` saat create dari sum expense family kategori+bulan |
| 5 | `FamilyUIState`: `canEditBudgets`, `budgetSheet`, snackbar error upsert |
| 6 | `FamilyBudgetRowUi.categoryId` + `hasLimit` |
| 7 | Sheet `FamilyBudgetTargetSheet` via `KeuTrackModalBottomSheet` |
| 8 | Wire **Adjust Targets** → buka sheet (kategori picker + limit) |
| 9 | Wire **tap baris** Shared Budgets → sheet prefill kategori itu |
| 10 | CTA “Atur target” di header/empty Shared Budgets jika `canEditBudgets` |
| 11 | Mapper: overlay limit family-only (abaikan budget `familyId` null / beda family); bar `hasLimit` → 4 tone §12.3.1, else `barColorHex` |
| 12 | Preview + unit test mapper/use case/ViewModel upsert **+ tone vs threshold 0.60 / 0.75 / 0.90 / over** |
| 13 | Manual QA: set limit → progress spent/limit; transaksi baru → spent naik; warna bar awareness bukan kategori |

### B. Phase 11b — Pull + rules agar anggota melihat limit (wajib untuk “shared”)

| # | Item |
|---|------|
| 14 | `BudgetFirestoreDataSource.getByFamilyId(familyId, month?)` |
| 15 | `SyncRepository.syncFamilyData` **additive**: pull budgets family → Room (skip overwrite jika lokal PENDING) |
| 16 | Rules: `get`/`list` budget jika `userId == uid` **atau** `isFamilyMember(familyId)` |
| 17 | `create`: `userId == uid`; jika `familyId` set, creator harus anggota (lebih ketat: owner — jika bisa dicek tanpa extra get mahal, dokumentasikan) |
| 18 | Composite index Firestore `familyId` + `month` |
| 19 | QA 2 akun: owner set limit → anggota buka tab Family → lihat cap yang sama |

---

## 6. Scope — Apa yang TIDAK Dikerjakan

| Item | Alasan | Phase |
|------|--------|-------|
| Form limit di `FamilyMembershipDialog` / create family | P1 | — |
| Seed budget on `CreateFamilyGroupUseCase` / join | P6 | — |
| Budget personal di Dashboard | Produk terpisah | Future |
| Auto copy limit bulan lalu → bulan ini | P4 | Future |
| Weekly period UI (`BudgetPeriod.WEEKLY`) | Entity ada; UI monthly saja | Future |
| Multi-currency / desimal | IDR `Long` | — |
| Edit/delete transaksi (spent reverse) | Masih gap Phase 5 | Future |
| QR invite, Sheets, auth/splash | Protected / phase lain | — |
| Realtime snapshot listener budget | Pull on Family tab cukup | Future |
| Anggota boleh tulis limit | P5 | Future |
| Breaking change `Budget` fields | Freeze entity | — |
| `allow list: if signedIn()` longgar untuk semua budget | Terlalu lebar; pakai membership | — |

---

## 7. Prasyarat (Definition of Ready)

1. User bisa create/join family; tab Family menampilkan Insights (bukan hanya banner join).
2. Ada **minimal 1 transaksi expense** di dompet family, **≥ 2 kategori** (untuk verifikasi baris vs donut per-user).
3. Shared Budgets sudah menampilkan baris kategori (tanpa limit) dari transaksi.
4. Kategori expense ter-seed / ter-observe lewat `GetCategoriesUseCase`.
5. Push sync budget existing (`syncPendingBudgets`) tidak error di device owner (boleh 0 row).
6. Build hijau:

```bash
./gradlew :core:domain:compileDevDebugKotlin
./gradlew :core:data:compileDevDebugKotlin
./gradlew :features:family:compileDevDebugKotlin
./gradlew assembleDevDebug
```

- [ ] Branch kerja: `feat/family-budget-authoring` (pola repo: `feat/` + kebab-case fitur, tanpa nomor phase)
- [ ] Baca dokumen ini + §24 sebelum coding
- [ ] Jangan mulai Task 3–5 jika Task 1 belum hijau

---

## 8. File Referensi (Read-Only)

| File | Pelajari |
|------|----------|
| `core/domain/.../model/Budget.kt` | `limit`, `spent`, `month`, `familyId`, `walletId` |
| `core/domain/.../repository/BudgetRepository.kt` | CRUD existing |
| `core/data/.../repository/BudgetRepositoryImpl.kt` | create → spent 0 + PENDING (11a **override seed spent di use case sebelum create**) |
| `core/data/.../db/dao/BudgetDao.kt` | Query bulan; perlu query family-scoped |
| `core/data/.../repository/TransactionRepositoryImpl.kt` | `getByMonthAndCategory` saat add tx |
| `core/data/.../repository/SyncRepositoryImpl.kt` | Pola skip PENDING di `syncFamilyData` |
| `features/family/.../model/FamilyUiMapper.kt` | Overlay limit vs porsi total |
| `features/family/.../membership/FamilyMembershipDialog.kt` | Pola dialog; **jangan** dicampur form budget |
| `core/designsystem/.../KeuTrackModalBottomSheet.kt` | Chrome sheet |
| `docs/dev/phases/PHASE_6_FAMILY_INSIGHTS_AND_MEMBERSHIP.md` | Stub Adjust Targets |
| `docs/dev/phases/PHASE_6C_SHARED_FAMILY_DATA_SYNC.md` | Pull family; budget CRUD out of scope |
| `docs/database/firestore-rules.md` | 11b: get membership + list signed-in; index `familyId`+`month` |
| `docs/dev/Project_Assessment_Current.md` | “Budget authoring UI” remaining |

---

## 9. File yang TIDAK BOLEH Diubah

Sesuai skill + freeze auth:

- `core/domain/.../model/User.kt`, `AuthResult.kt`, `TokenResult.kt`
- `core/domain/.../repository/UserRepository.kt`
- `core/data/.../repository/UserRepositoryImpl.kt`
- Semua `features/auth/`, `features/splashscreen/`
- `build-plugin/`, `settings.gradle.kts`, `gradle.properties`, `local.properties`
- `CreateFamilyGroupUseCase` / `JoinFamilyGroupUseCase` — **jangan** seed budget
- Signature breaking `AddTransactionUseCase` / `Budget` data class (additive DAO/query OK)

`Budget.createBudget` di repo yang memaksa `spent = 0` boleh tetap; **use case** mengirim `spent` hasil seed, lalu repo harus **menghormati spent yang dikirim** atau use case panggil API yang tidak men-zero-kan. Jika repo selalu `spent = 0`, **perbaiki secara sadar** di 11a Task 3 (lihat §14) — ini perubahan perilaku create, dokumentasikan di PR.

---

## 10. File yang BOLEH Diubah / Dibuat

### Domain (additive)

| File | Aksi |
|------|------|
| `core/domain/.../usecase/UpsertFamilyBudgetUseCase.kt` | BARU |
| `core/domain/.../usecase/DeleteFamilyBudgetUseCase.kt` | BARU |
| `core/domain/.../repository/BudgetRepository.kt` | Additive: `getByMonthCategoryFamily(...)` jika belum cukup DAO |
| `core/domain/.../repository/SyncRepository.kt` | Tidak wajib API baru jika pull masuk `syncFamilyData` |

### Data

| File | Aksi |
|------|------|
| `BudgetDao.kt` / `BudgetLocalDataSource(+Impl)` | Query family-scoped; unique-ish lookup |
| `BudgetRepositoryImpl.kt` | Implement lookup; **jangan** clobber spent on update; putuskan spent-on-create |
| `TransactionRepositoryImpl.kt` | Match budget family vs personal |
| `BudgetFirestoreDataSource.kt` | 11b: query `familyId` (+ `month`) |
| `SyncRepositoryImpl.kt` | 11b: hydrate budgets di `syncFamilyData` |
| `docs/database/firestore-rules.md` | 11b: get/list membership |

### Feature family

| File | Aksi |
|------|------|
| `FamilyUIState.kt` | `canEditBudgets`, sheet, saving flag, message |
| `FamilyInsightsMockContent.kt` / `FamilyBudgetRowUi` | `categoryId`, `hasLimit` |
| `FamilyUiMapper.kt` | Filter budget by `user.familyId`; map sheet helpers |
| `FamilyViewModel.kt` | Open/close sheet; upsert/delete |
| `FamilyScreen.kt` | Wire CTA + sheet |
| `FamilySharedBudgetsCard.kt` | Tap row; header action |
| `budget/FamilyBudgetTargetSheet.kt` | BARU |
| Tes: `FamilyUiMapperTest`, `FamilyViewModelTest`, use case tests | UPDATE / BARU |

---

## 11. Struktur File Target

```
core/domain/.../usecase/
├── UpsertFamilyBudgetUseCase.kt          ← BARU
└── DeleteFamilyBudgetUseCase.kt          ← BARU

core/data/.../
├── db/dao/BudgetDao.kt                   ← UPDATE query
├── datasource/local/BudgetLocalDataSource*.kt
├── datasource/firestore/BudgetFirestoreDataSource.kt  ← 11b query
└── repository/
    ├── BudgetRepositoryImpl.kt
    ├── TransactionRepositoryImpl.kt      ← match scoped
    └── SyncRepositoryImpl.kt             ← 11b pull

features/family/.../presentation/
├── FamilyScreen.kt                       ← sheet + callbacks
├── FamilyViewModel.kt
├── model/FamilyUIState.kt
├── model/FamilyUiMapper.kt
├── model/FamilyInsightsMockContent.kt    ← FamilyBudgetRowUi fields
├── components/FamilySharedBudgetsCard.kt
└── budget/
    └── FamilyBudgetTargetSheet.kt        ← BARU
```

Jangan buat feature module baru. Jangan depend `:features:transaction`.

---

## 12. Desain UX

### 12.1 Entry points

| Entry | Siapa | Perilaku |
|-------|--------|----------|
| Saving Together → **Adjust Targets** | Owner | Buka sheet; kategori **picker** (expense) + limit. Prefill jika kategori sudah punya budget bulan ini. |
| Header Shared Budgets → **Atur** | Owner | Sama seperti Adjust Targets. |
| Tap baris kategori | Owner | Sheet; kategori **terkunci**; limit prefill jika `hasLimit`. |
| Tap baris / CTA | Member / belum join | Tidak tampil aksi tulis; tap no-op. |

Non-owner: Shared Budgets read-only. Adjust Targets **disembunyikan** atau no-op + snackbar “Hanya pemilik keluarga yang bisa mengatur target” — pilih **sembunyikan** agar lebih jujur (P5).

User tanpa `familyId`: sheet tidak dibuka; banner join tetap seperti sekarang.

### 12.2 Sheet “Atur Target”

```
Atur Target
Bulan: Agustus 2026          ← read-only, YearMonth.now()

Kategori                    ← dropdown/list expense categories
  [Makanan            ▼]

Limit (Rp)                  ← digits only
  2.000.000
  Preview: Rp 2.000.000

[ Simpan target ]

[ Hapus target ]            ← hanya jika budget existing; text/error tone
```

- Simpan disabled jika limit ≤ 0, kategori kosong, atau `isSaving`.
- Sukses → tutup sheet; Shared Budgets recompose dari Flow Room (tanpa navigasi).
- Gagal validasi → error di sheet (`errorMessage`).
- Gagal repo → snackbar di Family screen.

### 12.3 Visual Shared Budgets setelah limit ada

| Kondisi | `spentLabel / capLabel` | Progress | Warna bar | Footnote |
|---------|-------------------------|----------|-----------|----------|
| Ada limit | spent / **limit** | spent÷limit | **4 awareness** hijau/kuning/oranye/merah (§12.3.1) | on track / perhatian / hampir habis / over |
| Tidak ada limit | spent / monthly total | spent÷total | **Warna kategori** (`Category.color` / `barColorHex`) | `X% dari pengeluaran keluarga` |

Jangan campur skala: baris dengan limit **tidak** memakai monthly total sebagai cap.

**Jangan** mewarnai bar limit dengan warna kategori (Belanja oranye, Makanan coral, …). Setelah target di-set, satu-satunya job bar adalah **awareness sisa limit** — 4 status (hijau / kuning / oranye / merah), bukan 8 palet kategori.

Optional (jangan over-scope): titik 8.dp di kiri judul tetap `Category.color` agar identitas kategori tidak hilang.

### 12.3.1 Empat warna awareness (wajib jika `hasLimit`)

Pakai **token design system yang sudah ada** — jangan hex ad-hoc di feature. Implementasi: `KeuTrackProgressBar(tone = …)` / `fillColor` dari token; **abaikan** `barColorHex` jika `hasLimit`.

Ambang `p = spent / limit`. Visual bar di-clamp `0f..1f`; over tetap isi penuh + `isOverLimit`.

Palet: **hijau → kuning → oranye → merah** (rampa panas). **Jangan** selipkan biru sebelum hijau — hijau = aman secara universal. Merah mulai **lebih dari 90%** dan **tetap merah** setelah melewati limit.

| Status | Ambang | Tone UI | Token isi bar | Hex referensi (light) | Footnote + warna teks |
|--------|--------|---------|---------------|----------------------|------------------------|
| **Aman** | `p ≤ 0.60` | `Success` | `success.s500` | `#00B844` (Green) | “On track — sisa Rp …” · teks `success.s500` |
| **Perhatian** | `0.60 < p ≤ 0.75` | `Watch` (**baru**) | `warning.w300` | `#FFA544` (YellowOrange) | “Perhatian — sisa Rp …” · teks `warning.w700` (`#E07400` Fulvous) |
| **Hampir habis** | `0.75 < p ≤ 0.90` | `Critical` (**baru**) | `warning.w500` | `#FA8B15` (Beer) | “Mendekati limit (X% tersisa)” · teks `warning.w700` |
| **Bahaya** | `p > 0.90` **termasuk** `spent > limit` | `Error` / Danger | `semantic.error` | light `#EF4444` (CarminePink) · dark `#FB7185` | `p ≤ 1` → “Limit hampir habis (X% tersisa)” · over → “Melebihi limit Rp …” · teks `semantic.error` |

`p > 0.90` = 90,1% ke atas. Tepat 90% masih oranye; 91%+ sudah merah. Rounding UI mengikuti float mentah (`spent * 10 > limit * 9`).

Dark theme: bind token (`successColors`, `warningColors`, `semantic.error`), jangan hardcode hex.

Mengapa 4 warna ini:

1. Hijau = aman (zona nyaman, ≤60%).
2. Kuning = mulai ketat, belum panik.
3. Oranye = limit hampir habis (75–90%) — peringatan sebelum merah.
4. Merah dari >90% s.d. over = satu zona bahaya; user sadar *sebelum* tembus, over tidak butuh warna ke-5.

**Jangan** pakai `semantic.primary` / navy / Azure sebagai status limit — itu brand, bukan heat map.

```kotlin
enum class FamilyBudgetBarTone {
    Success,   // hijau  p ≤ 0.60
    Watch,     // kuning 0.60 < p ≤ 0.75
    Critical,  // oranye 0.75 < p ≤ 0.90
    Error,     // merah  p > 0.90 || over
    Neutral,   // tidak ada limit — bar pakai barColorHex
}
```

`KeuTrackProgressTone` hari ini `Primary | Success | Danger`. Additive:

```kotlin
enum class KeuTrackProgressTone {
    Primary,
    Success,   // #00B844
    Warning,   // kuning w300 #FFA544
    Caution,   // oranye w500 #FA8B15  (atau Warning + fillColor w500)
    Danger,    // merah dari p > 0.90 dan over
}
```

Jika DS tetap tanpa `Caution`: `Warning` = kuning (`w300`), Critical memakai `fillColor = warning.w500`. **Wajib** oranye ≠ merah.

Konstanta (ganti `BUDGET_WARN_THRESHOLD = 0.85f`):

```kotlin
private const val BUDGET_SUCCESS_MAX = 0.60f   // ≤ 60% hijau
private const val BUDGET_WATCH_MAX = 0.75f     // ≤ 75% kuning; di atasnya oranye s.d. 90%
private const val BUDGET_RED_AFTER = 0.90f     // > 90% merah
```

```kotlin
fun budgetTone(progress: Float, isOverBudget: Boolean): FamilyBudgetBarTone =
    when {
        isOverBudget || progress > BUDGET_RED_AFTER -> FamilyBudgetBarTone.Error
        progress <= BUDGET_SUCCESS_MAX -> FamilyBudgetBarTone.Success
        progress <= BUDGET_WATCH_MAX -> FamilyBudgetBarTone.Watch
        else -> FamilyBudgetBarTone.Critical
    }
```

### 12.4 Empty states

| Kondisi | Copy |
|---------|------|
| Ada family, 0 expense, 0 budget | “Belum ada pengeluaran per kategori. Catat transaksi, atau atur target dulu.” + CTA Atur (owner) |
| Ada expense, 0 budget | Baris kategori tanpa limit + CTA Atur |
| Ada budget, 0 expense kategori itu | Tetap tampilkan baris budget (spent 0 / limit) — union keys mapper **sudah** support budget-only |

### 12.5 Copy CTA

- Tombol insight: tetap **Adjust Targets** (existing) atau ganti **Atur Target** (ID). Disarankan **Atur Target** agar konsisten IDR/UI Indonesia.
- Jangan ubah alur membership copy.

---

## 13. Desain Domain & Use Case

### 13.1 `UpsertFamilyBudgetUseCase`

```kotlin
class UpsertFamilyBudgetUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val budgetRepository: BudgetRepository,
    private val walletRepository: WalletRepository, // atau GetWalletSummary
    private val transactionRepository: TransactionRepository,
) {
    data class Params(
        val categoryId: String,
        val limit: Long,
        val month: String, // "yyyy-MM", default current di VM
    )

    suspend operator fun invoke(params: Params): Result<Budget>
}
```

**Validasi (urutan):**

1. `limit > 0` — else `"Limit harus lebih dari 0"`
2. `categoryId.isNotBlank()`
3. Current user non-null
4. `user.familyId` non-blank — else `"Buat atau gabung keluarga dulu"`
5. `FamilyRole.fromValue(user.familyRole) == OWNER` — else `"Hanya pemilik keluarga yang bisa mengatur target"`
6. Family wallet ada (`wallet.familyId == user.familyId`, type FAMILY) — else `"Dompet keluarga belum siap"`
7. `month` format `YYYY-MM`

**Upsert logic:**

```
existing = budgetRepository.find(familyId, categoryId, month)
if (existing != null) {
    update existing.copy(limit = params.limit)  // keep spent, id, createdAt
} else {
    spent = sum EXPENSE txs where familyId && categoryId && month(tx) == month
    create Budget(
        id = UUID,
        userId = user.uid,
        familyId = familyId,
        walletId = familyWallet.id,
        categoryId = params.categoryId,
        limit = params.limit,
        spent = spent,
        period = MONTHLY,
        month = month,
        syncStatus = PENDING,
    )
}
```

`CancellationException` harus di-rethrow.

### 13.2 `DeleteFamilyBudgetUseCase`

- Validasi owner + family sama.
- `deleteBudget(id)` hanya jika `budget.familyId == user.familyId` (jangan hapus budget personal orang lain di device yang sama).
- Setelah hapus, baris kategori kembali ke mode porsi total (jika masih ada expense).

### 13.3 Repo additive (disarankan)

```kotlin
suspend fun findFamilyBudget(
    familyId: String,
    categoryId: String,
    month: String,
): Budget?
```

Jangan pakai `getByMonthAndCategory` lama untuk family upsert.

### 13.4 Tidak perlu use case “GetBudgets”

`GetBudgetProgressUseCase(month)` tetap sumber observe. Filter family di **mapper** (P11 / §16).

---

## 14. Desain Data: Matching, Spent Seed, Uniqueness

### 14.1 Bug matching hari ini

```kotlin
val budget = budgetLocal.getByMonthAndCategory(month, pending.categoryId)
```

DAO:

```sql
SELECT * FROM budgets
WHERE month = :month AND categoryId = :categoryId
LIMIT 1
```

Jika user punya budget **personal** Makanan dan nanti budget **family** Makanan, `LIMIT 1` tidak terdefinisi. Expense family bisa menaikkan spent personal.

**Perbaikan 11a (wajib):**

Saat `addTransaction`:

- Jika `transaction.familyId != null` → cari budget `month + categoryId + familyId == tx.familyId` (atau `walletId == tx.walletId`).
- Else → cari budget `month + categoryId` dengan `familyId IS NULL` (personal).

Tambah DAO:

```sql
SELECT * FROM budgets
WHERE month = :month AND categoryId = :categoryId AND familyId = :familyId
LIMIT 1
```

dan

```sql
SELECT * FROM budgets
WHERE month = :month AND categoryId = :categoryId AND familyId IS NULL
LIMIT 1
```

Index existing `(month, categoryId)` cukup sebagai prefix; boleh tambah index `(month, categoryId, familyId)`.

**Uniqueness produk:** 1 row per `(familyId, categoryId, month)`. Enforce di use case (find-then-update), bukan migration unik Room di MVP (data lama mungkin duplikat). Jika duplikat ketemu: pakai tertua (`min createdAt`), jangan sum limit (itu menggandakan cap).

### 14.2 `createBudget` vs seed spent

`BudgetRepositoryImpl.createBudget` saat ini:

```kotlin
val pending = budget.copy(spent = 0L, syncStatus = PENDING)
```

Ini **bertentangan** dengan P7. Pilih **satu**:

| Opsi | Tindakan |
|------|----------|
| **A (disarankan)** | Repo create **tidak** men-zero-kan `spent` jika caller sudah set (≥ 0). Tetap force `PENDING`. Tes repo yang expect spent 0 harus di-update. |
| B | Repo tetap zero; use case `create` lalu `update` spent — 2 write, rapuh. |

Jangan hitung spent di ViewModel.

### 14.3 Update limit

`updateBudget` sudah `copy(syncStatus = PENDING)` tanpa reset spent. Pertahankan.

Hapus: delete lokal + enqueue sync; Firestore delete hanya jika rules allow (owner `userId`). 11b: anggota tidak delete.

### 14.4 Month key

Samakan dengan transaksi: `YearMonth.from(date.atZone(systemDefault())).toString()` → `"2026-08"`. ViewModel `YearMonth.now().toString()` untuk sheet.

Timezone: `ZoneId.systemDefault()` — sama dengan `FamilyUiMapper`.

---

## 15. Firestore Security Rules & Pull (11b)

### 15.1 Rules di repo (Publish ke Console masih manual)

Blok publish ada di `docs/database/firestore-rules.md`. Ringkas `/budgets`:

- **create:** `userId == uid`; jika `familyId` string, creator harus `isFamilyMember`
- **get:** missing doc, creator, atau `isFamilyMember(familyId)`
- **list:** `signedIn()` (MVP, sama wallet/tx)
- **update/delete:** tetap `userId` creator

Pull anggota **setelah** Publish + index `familyId` + `month` **Enabled**.

### 15.2 Rules target 11b

Pakai helper `isFamilyMember(familyId)` yang sama dengan wallets/transactions.

```
allow get: if signedIn() && (
  resource == null
  || resource.data.userId == request.auth.uid
  || isFamilyMember(resource.data.familyId)
);

allow list: if signedIn();  // MVP query familyId — harden later
```

**Lebih ketat (disarankan jika query constraints rules mengizinkan):** jangan `list: if signedIn()` global. Mirror Phase 6c txs: list signed-in MVP **hanya** jika tim menerima risiko yang sama. Dokumentasikan follow-up: `list` + query `familyId == X` + membership.

`update`/`delete`: tetap **creator `userId`** di MVP (owner yang upsert). Anggota tidak overwrite.

`create`: `userId == uid`. Jika `familyId != null`, idealnya `request.auth.uid in get(/family_groups/$(familyId)).data.memberIds` (mahal; acceptable MVP). Opsional: `familyRole` tidak ada di token — jangan andalkan custom claims di 11.

### 15.3 Query pull

```
budgets
  .whereEqualTo("familyId", familyId)
  .whereEqualTo("month", currentMonthKey)  // boleh tarik 2 bulan (current + prior) jika insights butuh
```

Index: collection `budgets`, fields `familyId` ASC, `month` ASC.

Hydrate: pola `syncFamilyData` — skip overwrite jika lokal `PENDING`/`FAILED`. Map Firestore → entity `SYNCED`.

Jangan pull semua budget user (personal) di 11b — itu Phase 10 follow-up terpisah (`list: false` personal tetap).

### 15.4 Publish checklist

1. Publish rules + buat index (tunggu enabled).
2. Owner set limit online → doc `/budgets/{id}` dengan `familyId`, `month`, `limit`.
3. Device B (anggota) buka Family → Room dapat row → cap = limit.
4. Jika `PERMISSION_DENIED`, cek field `familyId` terisi (bukan null) dan B ada di `memberIds`.

---

## 16. Pemetaan UI → State / Use Case

### 16.1 State tambahan

```kotlin
data class FamilyUIState(
    // existing...
    val canEditBudgets: Boolean = false,
    val budgetSheet: FamilyBudgetSheetState? = null,
    val isBudgetSaving: Boolean = false,
)

data class FamilyBudgetSheetState(
    val categoryId: String?,          // null = user pilih di sheet
    val categoryLocked: Boolean,      // true jika dibuka dari tap row
    val limitInput: String,           // digits
    val existingBudgetId: String?,
    val errorMessage: String?,
)

data class FamilyBudgetRowUi(
    val categoryId: String,
    val hasLimit: Boolean,
    // title, spentLabel, capLabel, progress, footnote, tone, muted
    // barColorHex: hanya dipakai jika !hasLimit (identitas kategori)
    // hasLimit → tone Success/Watch/Critical/Error; abaikan barColorHex untuk isi bar
)
```

`canEditBudgets = user.familyRole == owner && !user.familyId.isNullOrBlank() && hasFamilyWallet`.

### 16.2 Filter budget di mapper

```kotlin
fun filterSharedBudgets(...): List<Budget> =
    budgets.filter { budget ->
        !familyId.isNullOrBlank() && budget.familyId == familyId
    }
```

**Longgarkan filter longgar hari ini** (`budget.familyId.isNotBlank()` tanpa cocokkan family) — itu bisa menampilkan budget keluarga lain di device yang sama. Phase 11 **ketatkan** ke `budget.familyId == user.familyId`.

### 16.3 ViewModel events

| Event | Aksi |
|-------|------|
| `onAdjustTargetsClick()` | Buka sheet `categoryLocked = false` |
| `onBudgetRowClick(categoryId)` | Jika `canEditBudgets` → sheet locked + prefill limit |
| `onSheetCategorySelected` | Update + prefill limit jika existing |
| `onLimitChanged` | Filter non-digit; cap max sama New Entry jika ada (`MAX_AMOUNT_RUPIAH`) |
| `onSaveBudget` | `UpsertFamilyBudgetUseCase` → close sheet / error |
| `onDeleteBudget` | `DeleteFamilyBudgetUseCase` jika `existingBudgetId != null` |
| `onDismissSheet` | `budgetSheet = null` kecuali `isBudgetSaving` |

### 16.4 Routing

Tidak perlu route baru. Sheet di `FamilyScreen` / `FamilyRouting` seperti membership dialog.

---

## 17. Task Breakdown Detail

### Task 0 — Konfirmasi filter mapper (sebelum UI) ✅

Ketatkan `filterSharedBudgets` ke `familyId` user. Tes mapper: budget family lain tidak masuk.

### Task 1 (11a) — DAO + matching transaksi ✅

1. `getByMonthCategoryAndFamily`
2. `getByMonthCategoryPersonal` (`familyId IS NULL`)
3. `TransactionRepositoryImpl.addTransaction` cabang family vs personal
4. Tes: expense family tidak `applySpentDelta` ke budget personal kategori sama

### Task 2 (11a) — Use cases ✅

1. `UpsertFamilyBudgetUseCase` + tes: unauth, not in family, not owner, limit 0, create seeds spent, update keeps spent
2. `DeleteFamilyBudgetUseCase` + tes: member ditolak; owner sukses
3. Repo create menghormati spent (Opsi A) + update tes `BudgetRepositoryImplTest`

### Task 3 (11a) — UI models ✅

1. Extend `FamilyBudgetRowUi` + mock preview (punya `hasLimit true` dan false)
2. `FamilyUIState` sheet fields
3. Mapper `canEditBudgets`
4. Tone awareness: `hasLimit` → 4 status §12.3.1; tes threshold 0.60 / 0.75 / 0.90 / over
5. `KeuTrackProgressBar`: Success / Warning / Caution / Danger — jangan `fillColor` kategori jika `hasLimit`

### Task 4 (11a) — Sheet + card ✅

1. `FamilyBudgetTargetSheet`
2. Shared Budgets tap + header Atur
3. Prefill dari budget existing
4. `@Preview` light/dark sheet

### Task 5 (11a) — ViewModel + Routing ✅

1. Inject use cases
2. Wire `onAdjustTargetsClick`
3. Snackbar reuse `membershipMessage` **atau** field `budgetMessage` terpisah (lebih bersih, jangan campur copy join)
4. Tes ViewModel: owner save → use case invoked; member click tidak buka sheet

### Task 6 (11a) — QA device owner

Checklist §18.1 (manual, bukan commit). Authoring owner di Room sudah bisa dicek di 1 device.

`PERMISSION_DENIED` pada list budget **hanya** jika rules 11b belum di-Publish. Setelah Publish + index Enabled, warning itu harus hilang.

### Task 7 (11b) — Firestore DS + sync ✅

1. Query by `familyId` (+ month)
2. Merge ke `syncFamilyData` (sudah dipanggil `onScreenRendered` / after join)
3. Skip PENDING overwrite
4. Tes repo sync dengan fake DS (pola 6c/10)

### Task 8 (11b) — Rules + index + QA 2 akun ✅ docs di git

1. `firestore-rules.md`: get membership + `list: if signedIn()`; update/delete tetap creator — **land**
2. Index `budgets` `familyId` ASC + `month` ASC (catatan di docs) — **land**
3. **Setelah push:** Publish rules di Console, tunggu index **Enabled**, QA 2 akun §18.2

### Task 9 — Lint/build (setelah push / QA)

```bash
./gradlew :core:domain:testDevDebugUnitTest --tests "*FamilyBudget*"
./gradlew :core:data:testDevDebugUnitTest --tests "*Budget*" --tests "*TransactionRepositoryImpl*"
./gradlew :features:family:testDevDebugUnitTest
./gradlew assembleDevDebug
```

---

## 18. Acceptance Criteria

### 18.1 Phase 11a (owner, 1 device)

- [ ] Dialog buat/gabung keluarga **tidak** berisi field limit
- [ ] Owner di Family: Adjust Targets / Atur / tap row membuka sheet
- [ ] Member: aksi tulis tidak tampil
- [ ] Simpan limit Makanan Rp 1.000.000 → baris Makanan `spent / Rp 1.000.000`, bukan `/ total keluarga`
- [ ] Setelah ada limit: isi bar **bukan** warna kategori melainkan status: ≤60% hijau `#00B844`, 60–75% kuning `#FFA544`, 75–90% oranye `#FA8B15`, **>90% s.d. over** merah `#EF4444` (via token)
- [ ] 91% limit sudah **merah** (footnote hampir habis); 110% tetap **merah** (footnote melebihi)
- [ ] Dua kategori dengan limit, keduanya ≤60% → **sama-sama hijau**, meski Belanja vs Makanan beda di picker
- [ ] Kategori lain tanpa budget tetap porsi total **dan** tetap warna kategori
- [ ] Jika bulan ini sudah ada expense Makanan Rp 400.000 lalu set limit 1.000.000 → spent awal **400.000** bukan 0
- [ ] Ubah limit ke 2.000.000 → spent tetap 400.000
- [ ] Catat expense family Makanan baru → spent budget family naik; budget personal Makanan (jika ada) **tidak** naik
- [ ] Hapus target → baris kembali ke mode tanpa limit
- [ ] Limit 0 / kosong → error, tidak menulis Room
- [ ] Offline: row muncul segera (Room); sync PENDING lalu SYNCED saat online
- [ ] Preview Family + sheet tidak crash
- [ ] `CreateFamilyGroupUseCase` tidak membuat row budget

### 18.2 Phase 11b (2 akun)

- [ ] Owner sync → Firestore `/budgets/{id}` punya `familyId`, `categoryId`, `month`, `limit`, `userId` owner
- [ ] Anggota B buka tab Family (setelah join) → cap sama dengan owner
- [ ] B tidak bisa update/delete budget owner di Console dengan auth B (rules)
- [ ] Reinstall B + login + join (atau masih member) + buka Family → budget ter-hydrate (tidak perlu set ulang)
- [ ] Index error tidak muncul di logcat untuk query `familyId` + `month`

### 18.3 Non-regression

- [ ] Breakdown tetap **per user**
- [ ] History family tetap pull 6c
- [ ] Transaksi personal tidak menulis `familyId` budget
- [ ] Auth / splash / settings membership tidak berubah

---

## 19. Catatan Arsitektur & Konvensi

- Feature **tidak** panggil `BudgetRepository` langsung — lewat use case (validasi P5/P7).
- Feature **tidak** depend feature lain.
- `CommonDispatcher` di ViewModel; `catch (e: CancellationException) { throw e }`.
- Uang: `Long`. Format hanya di mapper/UI.
- `KeuTrackTheme` di Preview; Material 2 di content sheet; chrome `KeuTrackModalBottomSheet` (M3) sudah di DS.
- Satu `FamilyUIState`; sheet nullable, bukan screen state machine terpisah.
- Offline-first: baca Room; tulis lokal + `PENDING` + `SyncScheduler.enqueueSync()`.
- Jangan `Dispatchers.IO` hardcoded.

### Spent ganda?

Mapper **lebih percaya sum transaksi family** untuk `spent` display jika txs tersedia; `Budget.spent` fallback jika tidak ada txs. Setelah 11a matching benar, `Budget.spent` harus ≈ sum txs. Jika drift (tx sebelum budget tanpa recompute selain seed create), display dari **txs** tetap sumber UI — konsisten dengan mapper sekarang. Seed create menutup drift awal.

---

## 20. Dependency Graph

```
FamilyScreen
  └── FamilyViewModel
        ├── GetBudgetProgressUseCase ──▶ BudgetRepository.observeBudgets
        ├── GetTransactionsUseCase     ──▶ family txs (spent display + seed input)
        ├── GetCategoriesUseCase       ──▶ picker
        ├── UpsertFamilyBudgetUseCase  ──▶ User + Budget + Wallet + Tx (read sum)
        ├── DeleteFamilyBudgetUseCase
        └── SyncFamilyDataUseCase      ──▶ 11b budgets pull

addTransaction
  └── BudgetLocal.getByMonthCategoryAndFamily | Personal

SyncWorker
  └── syncPendingBudgets (push existing)
```

Tidak ada tepi `:features:family` → `:features:transaction`.

---

## 21. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| `createBudget` zero spent | Progress bohong setelah txs existing | Opsi A §14.2 + tes seed |
| `getByMonthAndCategory` LIMIT 1 | Spent personal vs family kacau | Task 1 wajib sebelum QA transaksi baru |
| Filter mapper longgar | Budget family lain tampil | Task 0 ketatkan `familyId` |
| 11a tanpa 11b | Produk “shared” palsu di device B | Jangan close phase tanpa 11b; copy UI jujur jika 11b slip |
| `list: if signedIn()` | User lain query semua budgets | Terima sebagai MVP 6c-style **atau** batasi + dokumentasikan harden |
| Duplikat row `(family, cat, month)` | Cap dobel jika di-sum | Find tertua; jangan `sumOf(limit)` untuk family yang sama |
| Owner ganti; budget `userId` lama | Anggota baru owner tidak bisa update (rules userId) | MVP: terima; future migrate `userId` ke owner baru / rules ownerId family |
| Month timezone | Limit “hilang” lewat tengah malam | Samakan `YearMonth` mapper & use case |
| Input limit string | Overflow Long | Digit filter + max cap seperti New Entry |
| Sheet vs membership dialog overlap | Dua modal | `BackHandler` existing; satu modal at a time |
| Bar tetap warna kategori setelah ada limit | User tidak sadar limit hampir habis | P13: `hasLimit` → 4 tone; QA dua kategori aman harus sewarna hijau |
| Merah baru muncul saat over | User terlambat | Merah dari `p > 0.90` sampai over (satu zona bahaya) |

---

## 22. Urutan Pengerjaan yang Disarankan

```
Task 0  Filter mapper familyId ketat              ✅
   ↓
Task 1  DAO + matching addTransaction             ✅  fondasi spent
   ↓
Task 2  Upsert/Delete use case + repo spent       ✅
   ↓
Task 3  UIState / row model                       ✅
   ↓
Task 4  Sheet + tap/CTA                           ✅
   ↓
Task 5  ViewModel wire                            ✅
   ↓
Task 6  QA 11a 1 device                           ⬜ setelah push
   ↓
Task 7  Pull budgets in syncFamilyData            ✅
   ↓
Task 8  Rules + index (docs)                      ✅ git; Publish + QA 2 akun ⬜
   ↓
Task 9  Tes + assembleDevDebug                    ⬜ setelah push
```

**Stop-the-line:** jika Task 1 belum hijau, jangan anggap limit “benar” meski UI sudah simpan angka.

Peta task → commit: §24. Jangan 1 commit raksasa, jangan 1 commit per file.

Estimasi kasar:

| Slice | Waktu |
|-------|--------|
| 11a Task 0–2 (data + domain) | ~0.75–1 hari |
| 11a Task 3–6 (UI + QA) | ~1–1.5 hari |
| 11b Task 7–8 | ~1–1.5 hari |
| Tes/polish | ~0.5 hari |

---

## 23. Relasi ke Phase Lain

| Phase | Hubungan |
|-------|----------|
| 1 | Entity `Budget` sudah ada — jangan redesign |
| 2 | Repo/DAO/push — extend query + spent-on-create |
| 5 | New Entry menulis tx yang menaikkan spent; matching harus family-aware |
| 6 | Insights + stub Adjust Targets — Phase 11 mengisi stub |
| 6c | `syncFamilyData` pull wallet/tx; 11b **additive** budgets |
| 7 | Settings tidak mengatur budget |
| 9 | Tes use case/repo/VM masuk slice ini (jangan tunggu “Phase 9 penuh”) |
| 10 | Personal wallet restore; **jangan** longgarkan pull budget personal di 11 |
| Future | Copy-forward bulan; budget personal Dashboard; member-write; harden `list` rules |

Setelah 11a+11b hijau, tab Family punya arti penuh: **siapa belanja** (breakdown), **berapa vs target kategori** (shared budgets), **apa yang baru terjadi** (history).

---

## 24. Rencana Commit

Branch: **`feat/family-budget-authoring`**. Satu branch untuk 11a+11b (satu PR). Pecah 11b ke `feat/family-budget-pull` hanya jika rules/index butuh PR terpisah.

Format pesan mengikuti `.cursor/rules/keutrack-git-commits.mdc`: `[TAG]` + imperative, < 72 karakter, tanpa titik.

### 24.1 Rekomendasi: 9 commit

| # | Status | Task | Slice | Pesan |
|---|--------|------|-------|--------|
| 1 | ✅ file ini | — | Docs plan | `[DOCS] Add Phase 11 family shared budget authoring plan` |
| 2 | ✅ done | 0 | Filter mapper | `[FIX] Scope shared budget rows to current familyId` |
| 3 | ✅ done | 1 | DAO + matching tx | `[FIX] Match family vs personal budget on add transaction` |
| 4 | ✅ done | 2 | Use case + repo spent | `[FEAT] Add upsert and delete family budget use cases` |
| 5 | ✅ done | 3 (DS) | Tone progress bar | `[FEAT] Add Warning and Caution tones to progress bar` |
| 6 | ✅ done | 3 (family) | Row + mapper 4 warna | `[FEAT] Map shared budgets to spent/limit awareness tones` |
| 7 | ✅ done | 4–5 | Sheet + VM | `[FEAT] Wire family budget target sheet for owners` |
| 8 | ✅ done | 7 | Pull Room | `[FEAT] Pull family budgets in syncFamilyData` |
| 9 | ✅ done | 8 | Rules + index | `[FEAT] Allow family members to read shared budgets` |

**11a = commit 2–7.** **11b = commit 8–9.** Semua slice kode/docs di git. Jangan close phase sebelum Publish rules + index Enabled + QA §18.

Task 6 (QA device) dan Task 9 (lint/build) **bukan** commit.

### 24.2 Isi tiap commit (arsip)

**Commit 1 — docs** (file ini, ikut push)

- Hanya `docs/dev/phases/PHASE_11_FAMILY_SHARED_BUDGET_AUTHORING.md`
- Jangan campur kode Task 1+

**Commit 3 — Task 1 (wajib sebelum UI authoring)**

- `BudgetDao`: `getByMonthCategoryAndFamily`, `getByMonthCategoryPersonal` (`familyId IS NULL`)
- `BudgetLocalDataSource(+Impl)` + `BudgetRepository` additive `findFamilyBudget` jika perlu
- `TransactionRepositoryImpl.addTransaction`: family → budget family; personal → budget `familyId IS NULL`
- Tes: expense family **tidak** `applySpentDelta` ke budget personal kategori sama
- Jangan sentuh sheet / ViewModel

**Commit 4 — Task 2**

- `UpsertFamilyBudgetUseCase` + tes: unauth, not in family, not owner, limit 0, create seeds spent, update keeps spent
- `DeleteFamilyBudgetUseCase` + tes: member ditolak; owner sukses
- Repo create **Opsi A**: hormati `spent` caller; update tes `BudgetRepositoryImplTest`
- Jangan seed di `CreateFamilyGroupUseCase`

**Commit 5 — Task 3 design system** ✅

- `:core:designsystem` saja: `KeuTrackProgressTone` additive `Warning` / `Caution` (atau `Warning` + `fillColor` w500)
- `KeuTrackProgressBar` bind token `success` / `warning.w300` / `warning.w500` / `semantic.error`
- Jangan hex ad-hoc di feature. Jangan mapper family di commit ini

**Commit 6 — Task 3 family models** ✅

- `FamilyBudgetRowUi`: `categoryId`, `hasLimit`; tone 4 status §12.3.1
- `FamilyUIState`: `canEditBudgets`, sheet fields (boleh stub, belum wajib sheet)
- Mapper: `hasLimit` → awareness; tanpa limit → `barColorHex` kategori
- Tes threshold `0.60` / `0.75` / `0.90` / over; 91% merah, 90% masih oranye
- Preview mock `hasLimit` true + false

**Commit 7 — Task 4 + 5 (satu commit)** ✅

- `FamilyBudgetTargetSheet` + `@Preview` light/dark
- Shared Budgets: tap row + header Atur; Adjust Targets → sheet
- ViewModel: open/close, upsert/delete, snackbar `budgetMessage` (jangan campur copy join)
- Tes VM: owner save invoke use case; member tidak buka sheet
- Jangan pecah sheet vs ViewModel — sheet tanpa `onSaveBudget` tidak reviewable

**Commit 8 — Task 7** ✅

- `BudgetFirestoreDataSource.getByFamilyId(familyId, month?)`
- `syncFamilyData` additive: hydrate budgets; skip overwrite `PENDING`/`FAILED`
- Tes sync fake DS (pola 6c/10)
- Jangan pull budget personal; jangan longgarkan Phase 10

**Commit 9 — Task 8** ✅ docs di git; Publish + index masih di Console

- `docs/database/firestore-rules.md`: `get`/`list` membership; `update`/`delete` tetap `userId` creator
- Catatan composite index `familyId` + `month`
- Boleh merge kode dulu; **jangan close phase** sebelum index enabled + QA 2 akun

### 24.3 Aturan pecah commit

| Lakukan | Jangan |
|---------|--------|
| Satu concern per commit; tes ikut kode yang diuji | Commit `[TEST]` terpisah untuk tes use case / mapper / VM |
| Commit 5 DS, lalu 6 mapper | Campur `:core:designsystem` + sheet di satu diff |
| Gabung Task 4+5 jadi commit 7 | Pecah sheet tanpa event ViewModel |
| 11a (2–7) terpisah 11b (8–9) | Satu commit “seluruh Phase 11” |
| Matching (commit 3) sebelum QA transaksi baru | Anggap limit benar jika Task 1 belum hijau |
| Pesan fokus *why* | Nomor phase di pesan (`Phase 11 Task 3`) |

Jangan ikut commit: auth/splash/build-plugin, seed budget di create/join family, debug Activity, `local.properties`.

### 24.4 Alternatif ringkas: 6 commit

Hanya jika PR 9 commit terasa terlalu pecah. Kurang ideal di commit D (DS + mapper + sheet + VM jadi satu diff besar).

| # | Menggabungkan | Pesan |
|---|----------------|--------|
| A | 1 | `[DOCS] Add Phase 11 family shared budget authoring plan` |
| B | 2+3 (Task 0+1) | `[FIX] Scope family budget matching and mapper filter` |
| C | 4 (Task 2) | `[FEAT] Add upsert and delete family budget use cases` |
| D | 5–7 (Task 3–5) | `[FEAT] Add family budget authoring UI with awareness tones` |
| E | 8 (Task 7) | `[FEAT] Pull family budgets in syncFamilyData` |
| F | 9 (Task 8) | `[FEAT] Allow family members to read shared budgets` |

**Default tetap 9 commit.** Semua 9 slice sudah di branch; jalur ringkas tidak dipakai mundur.

### 24.5 Setelah push — sisa di luar git

```
Commit 1–9  di branch                         ✅ siap push
   ↓
Task 6   QA 1 device owner (§18.1)            ⬜
   ↓
Publish  rules 11b di Firebase Console        ⬜
   ↓
Index    budgets familyId ASC + month ASC     ⬜ tunggu Enabled
   ↓
Task 8   QA 2 akun (§18.2)                    ⬜
   ↓
Task 9   tes + assembleDevDebug               ⬜
```

Jangan close phase / jangan klaim “shared” sampai index Enabled dan anggota B melihat cap yang sama. Pesan commit file ini: `[DOCS] Add Phase 11 family shared budget authoring plan`.
