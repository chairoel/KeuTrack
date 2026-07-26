# Phase 6 — features:family (Family Insights Real Data + Membership)

> **Modul target:** `:features:family` (+ domain/data **additive** untuk membership; wiring tipis `:app` + Settings entry untuk invite)
> **Estimasi:** ~3.5–5 hari total · **6a** ~2–2.5 hari · **6b** ~1.5–2.5 hari
> **Prasyarat:** Phase 1–3 ✅ · Phase 4–5 ✅ (Dashboard + Transaction new-entry/history usable — lihat DoR Phase 5)
> **Status baseline:** ~40% — UI “Family Insights” polished; data **100% mock** (`$` bukan IDR); `FamilyViewModel` kosong; **tidak ada** invite/QR/`FamilyGroup`
> **Hasil akhir:** Tab Family menampilkan insights dari Room (IDR); FAB/View All ter-wire; lalu (6b) identitas keluarga nyata via `FamilyGroup` + join/invite sehingga `User.familyId` terisi dan wallet/budget shared punya arti

---

## Daftar Isi

1. [Konteks & Tujuan](#1-konteks--tujuan)
2. [Inventory — Apa yang Sudah Ada](#2-inventory--apa-yang-sudah-ada)
3. [Keputusan Produk: Insights-First (6a) lalu Membership (6b)](#3-keputusan-produk-insights-first-6a-lalu-membership-6b)
4. [Scope — Apa yang Dikerjakan](#4-scope--apa-yang-dikerjakan)
5. [Scope — Apa yang TIDAK Dikerjakan](#5-scope--apa-yang-tidak-dikerjakan)
6. [Prasyarat (Definition of Ready)](#6-prasyarat-definition-of-ready)
7. [File Referensi (Read-Only)](#7-file-referensi-read-only)
8. [File yang TIDAK BOLEH Diubah](#8-file-yang-tidak-boleh-diubah)
9. [File yang BOLEH Diubah / Dibuat](#9-file-yang-boleh-diubah--dibuat)
10. [Struktur File Target](#10-struktur-file-target)
11. [Pemetaan UI → Use Case (6a)](#11-pemetaan-ui--use-case-6a)
12. [Desain Membership (6b)](#12-desain-membership-6b)
13. [Task Breakdown Detail](#13-task-breakdown-detail)
14. [Acceptance Criteria](#14-acceptance-criteria)
15. [Catatan Arsitektur & Konvensi](#15-catatan-arsitektur--konvensi)
16. [Dependency Graph](#16-dependency-graph)
17. [Risiko & Mitigasi](#17-risiko--mitigasi)
18. [Urutan Pengerjaan yang Disarankan](#18-urutan-pengerjaan-yang-disarankan)

---

## 1. Konteks & Tujuan

Menurut `docs/dev/Project_Assessment.md` dan handoff Phase 1–5:

| Area | Status sekarang |
|------|-----------------|
| Tab Family di `HomeShell` + `FamilyRoute` | ✅ |
| UI Insights: breakdown, shared budgets, history, saving-together, FAB | ✅ Compose |
| Data Insights | ❌ `DefaultFamilyInsightsMockContent` (USD `$`) |
| `FamilyViewModel` | ❌ Empty shell |
| Invite / QR / join / manage circle | ❌ Tidak ada screen |
| Domain `FamilyGroup` / `FamilyRepository` | ❌ Ditunda sejak Phase 1 |
| Hooks `User.familyId`, `WalletType.FAMILY`, `*.familyId` di entity finansial | ✅ Field ada; membership flow belum |
| Settings “Family Network” | ❌ Mock ID `KEU-992-KRT`; callbacks no-op (wire penuh ID → Phase 7, entry invite boleh 6b) |

Assessment menyebut rencana lama “invite/QR”, tetapi **investasi UI yang sudah ada** adalah Family Insights. Phase 6 **tidak** mengganti tab menjadi invite-only — Insights tetap home; membership mengisi empty state + Settings.

**Tujuan Phase 6:**
- **6a:** Wire Insights ke UseCase existing; IDR; empty states; FAB/View All via `:app`
- **6b:** Introduksi `FamilyGroup` + create/join (invite code; QR opsional); set `User.familyId` / `familyRole`; tautkan family wallet; Insights filter by `familyId`

**Bukan tujuan Phase 6:**
- Settings currency persistence / Google Sheets (Phase 7)
- Redesign visual Atelier besar
- Auth/splash changes
- Menjadikan Family tab sebagai clone Transaction History

---

## 2. Inventory — Apa yang Sudah Ada

### Feature family

| File | Peran |
|------|-------|
| `presentation/FamilyScreen.kt` | `FamilyRouting` + `FamilyScreen`; selalu mock; callbacks `{}` |
| `presentation/FamilyViewModel.kt` | `@HiltViewModel` kosong |
| `presentation/navigation/FamilyNavigation.kt` | `FamilyRoute`, `familyGraph()` |
| `presentation/model/FamilyInsightsMockContent.kt` | Model UI + `DefaultFamilyInsightsMockContent` (`$`) |
| `components/FamilyBreakdownCard.kt` | Breakdown + **local** `FamilyDonutChart` |
| `components/FamilySharedBudgetsCard.kt` | Budgets + `KeuTrackProgressBar` |
| `components/FamilyHistoryLogSection.kt` | Header + View All |
| `components/FamilyHistoryRow.kt` | Row + attribution pill (Husband/Wife mock) |
| `components/FamilySavingTogetherCard.kt` | Insight CTA card |

### Domain / data hooks (bukan FamilyGroup)

| Item | Lokasi |
|------|--------|
| `User.familyId`, `User.familyRole` | `core/domain/.../model/User.kt` |
| `WalletType.FAMILY` | `WalletType.kt` |
| `familyId` di Transaction / Wallet / Budget / Category / CategorySummary | Domain + Room + Firestore DS |
| `GetWalletSummaryUseCase` → `familyWallets`, `totalFamilyBalance` | Existing |
| `GetBudgetProgressUseCase` | Existing |
| `GetMonthlySummaryUseCase` | Existing |
| `GetTransactionsUseCase.Params(walletId, …)` | Filter by wallet — **tidak** ada `familyId` param |

### App nav

| File | Catatan |
|------|---------|
| `app/.../HomeShell.kt` | `familyGraph()` di nested tabs |
| Phase 5 pattern | Dashboard sudah punya `onAddTransaction` / `onViewAllTransactions` — **reuse** untuk Family FAB / View All |

### Design system relevan

| API | Status vs Family |
|-----|------------------|
| `KeuTrackProgressBar`, `KeuTrackFab`, `KeuTrackTopBar`, `KeuTrackCard`, `KeuTrackButton` | Sudah dipakai |
| `CurrencyFormat`, `KeuTrackCurrencyText` | **Belum** dipakai di family — wajib 6a |
| `KeuTrackDonutChart` | Belum di DS; donut lokal OK untuk 6a |
| `KeuTrackStatusChip` | Opsional untuk sync di history family |

---

## 3. Keputusan Produk: Insights-First (6a) lalu Membership (6b)

| Tahap | Goal | Domain baru? |
|-------|------|--------------|
| **6a — Insights data** | Ganti mock → Flows Room; IDR; wire aksi | Tidak wajib |
| **6b — Membership** | Create/join family; set `User.familyId`; seed/link family wallet | **Ya** — `FamilyGroup` + repo + use cases + data |

**Mengapa urutan ini:**
1. UI Insights sudah jadi — value cepat tanpa menunggu Firestore rules membership
2. Phase 5 DoR: NewEntry + history siap dikonsumsi FAB Family
3. Invite/QR butuh kontrak baru + hati-hati terhadap auth `User` upsert (jangan clobber profile)
4. Tanpa membership, Insights masih berguna untuk `WalletType.FAMILY` lokal / budgets bulan berjalan

**Jangan** ganti title tab menjadi invite flow. Pola empty state:

```
Belum join keluarga?
  → CTA “Buat / Gabung Keluarga” (6b)
  → setelah join → Insights terisi
```

---

## 4. Scope — Apa yang Dikerjakan

### A. Phase 6a — Family Insights real data (wajib dulu)

| # | Item |
|---|------|
| 1 | `FamilyUIState` + `FamilyUiMapper` (Long + format IDR; mock hanya Preview) |
| 2 | Rewrite `FamilyViewModel` — inject UseCase + `UserRepository` + `CommonDispatcher` |
| 3 | Bind breakdown dari `GetMonthlySummaryUseCase` (`CategorySummary.byCategory` / totals) |
| 4 | Bind shared budgets dari `GetBudgetProgressUseCase` |
| 5 | Bind history dari `GetTransactionsUseCase` pada **family wallet id(s)** (`GetWalletSummaryUseCase`) |
| 6 | Saving Together: insight sederhana dari trend bulan (atau hide jika data kurang) |
| 7 | Hapus `$` runtime; pakai `CurrencyFormat` / `KeuTrackCurrencyText` |
| 8 | Empty states: no family wallet / no budgets / no txs / (opsional) `familyId == null` banner |
| 9 | Wire FAB → `onAddTransaction` (prefer preselect family wallet di Phase 5 jika API nav mengizinkan; jika belum, buka NewEntry biasa) |
| 10 | Wire View All → `onViewAllTransactions` / history (filter family wallet ideal; MVP: history umum dulu) |
| 11 | Attribution history: pakai `Transaction.addedByName` (bukan enum Husband/Wife keras) |

### B. Phase 6b — Family membership (setelah 6a hijau)

| # | Item |
|---|------|
| 12 | Domain: `FamilyGroup`, `FamilyRole` (atau reuse string role), `FamilyRepository` |
| 13 | UseCase: `CreateFamilyGroupUseCase`, `JoinFamilyGroupUseCase`, (`LeaveFamilyGroupUseCase` opsional) |
| 14 | Data: Room entity opsional + Firestore `/family_groups/{id}` + impl + Hilt binds |
| 15 | Saat create/join: update `User.familyId` / `familyRole` (API eksplisit — jangan andalkan upsert auth yang skip field ini) |
| 16 | Seed/ensure `WalletType.FAMILY` dengan `familyId` yang sama |
| 17 | UI: empty-state CTA di Family tab +/atau thin Settings “Invite / Join” (copy family code) |
| 18 | QR: **nice-to-have** — encode invite code; scan join (boleh ditunda jika waktu habis) |
| 19 | Insights filter ketat by `familyId` setelah membership ada |

### C. App / Settings wiring tipis

| # | Item |
|---|------|
| 20 | `familyGraph(onAddTransaction, onViewAllTransactions, onJoinOrCreateFamily?)` via `HomeShell` |
| 21 | Settings: tampilkan `User.familyId` real + `onInviteMember` / copy code → 6b (persist currency tetap Phase 7) |

---

## 5. Scope — Apa yang TIDAK Dikerjakan

| Item | Alasan | Phase |
|------|--------|-------|
| Persist currency / Google Sheets / connected wallets list penuh | Settings product | Phase 7 |
| Edit budget targets dari “Adjust Targets” penuh | Bisa stub CTA; CRUD budget UI terpisah | 6a stub / later |
| Multi-admin ACL rumit / approval join | Over-scope | Future |
| Cloud Functions aggregate member % | Opsional; 6a pakai category summary | Future |
| Auth/splash redesign | Stable | — |
| Mengubah signature UseCase finansial existing secara breaking | Freeze | — kecuali additive filter |
| Depend feature↔feature | Arsitektur | — |
| Unit test penuh | Phase 9 | — |

---

## 6. Prasyarat (Definition of Ready)

Dari Phase 5 DoR:

1. User bisa mencatat transaksi (NewEntry full-screen) termasuk memilih wallet Family jika ada
2. Transaction history (“View All”) berfungsi
3. Dashboard overview tetap sehat
4. `GetBudgetProgressUseCase`, `GetMonthlySummaryUseCase`, `GetWalletSummaryUseCase`, `GetTransactionsUseCase` injectible
5. Categories/budgets bisa muncul di Room (seed / create path Phase 2)

```bash
./gradlew :features:transaction:compileDevDebugKotlin
./gradlew :features:family:compileDevDebugKotlin
./gradlew assembleDevDebug
```

---

## 7. File Referensi (Read-Only)

### Dokumen fase

| File | Gunakan untuk |
|------|---------------|
| `docs/dev/Project_Assessment.md` | Gap Family ~40%, Settings family mock, prioritas #6 |
| `docs/dev/phases/PHASE_1_DOMAIN_ENTITIES_AND_USE_CASES.md` | Family group ditunda ke Phase 6; Budget/CategorySummary fields |
| `docs/dev/phases/PHASE_2_ROOM_AND_REPOSITORY_IMPLEMENTATION.md` | Offline-first; jangan campur financial DS ke auth Firestore user |
| `docs/dev/phases/PHASE_3_DESIGN_SYSTEM.md` | CurrencyText, ProgressBar; Donut opsional |
| `docs/dev/phases/PHASE_4_DASHBOARD_REAL_DATA.md` | Pola UIState/mapper/ViewModel combine Flows |
| `docs/dev/phases/PHASE_5_TRANSACTION_FLOW.md` | Nav callback pattern; NewEntry/History; DoR Phase 6 |
| `docs/dev/DESIGN_SYSTEM_ATELIER.md` | Katalog komponen |
| `.cursor/rules/keutrack-feature-module.mdc` | Screen/Routing/VM |
| `.cursor/rules/keutrack-domain-layer.mdc` | Entity/UseCase conventions untuk 6b |
| `.cursor/rules/keutrack-data-layer.mdc` | Repo impl + CancellationException untuk 6b |
| `.cursor/skills/keutrack-dev/SKILL.md` | Protected files |

### Domain / use case (6a consume)

| File | Pelajari |
|------|----------|
| `core/domain/.../usecase/GetWalletSummaryUseCase.kt` | `familyWallets` |
| `core/domain/.../usecase/GetMonthlySummaryUseCase.kt` | `CategorySummary`, trend |
| `core/domain/.../usecase/GetBudgetProgressUseCase.kt` | `Budget` list |
| `core/domain/.../usecase/GetTransactionsUseCase.kt` | Filter `walletId` |
| `core/domain/.../model/Budget.kt` | `progressPercent`, `spent`, `limit`, `isOverBudget` |
| `core/domain/.../model/CategorySummary.kt` | `byCategory`, totals |
| `core/domain/.../model/Transaction.kt` | `addedByName`, `familyId`, `walletId` |
| `core/domain/.../model/User.kt` | `familyId`, `familyRole` |
| `core/domain/.../model/WalletType.kt` | `FAMILY` |

### Pola UI yang ditiru

| File | Pelajari |
|------|----------|
| `features/dashboard/.../DashboardViewModel.kt` | Combine Flows + UIState |
| `features/dashboard/.../*UiMapper*` | Domain → row UI |
| `features/transaction/.../NewEntryViewModel.kt` | Wallet familyId on save |
| `features/transaction/.../history/*` | History list pattern |
| `features/family/.../model/FamilyInsightsMockContent.kt` | Field UI yang harus dipetakan |
| `features/family/.../components/*` | Layout yang dipertahankan |
| `app/.../HomeShell.kt` | Callback wiring |
| `features/settings/...` Family Network card | Entry point 6b (baca dulu) |

### Auth user persistence (hati-hati 6b)

| File | Pelajari — **jangan sembarangan ubah** |
|------|----------------------------------------|
| `core/data/.../datasource/FirestoreNetworkDataSource.kt` | Upsert user — field family sering **tidak** di-overwrite saat profile sync |
| `core/data/.../repository/UserRepositoryImpl.kt` | Orchestration auth |
| `core/data/.../mapper/SignedInUserProtoMapper.kt` | Local session fields |

---

## 8. File yang TIDAK BOLEH Diubah

### Auth / splash / infra

| File / Area | Alasan |
|-------------|--------|
| `features/auth/**` | Complete |
| `features/splashscreen/**` | Complete |
| `build-plugin/**` | Stable |
| `settings.gradle.kts`, root Gradle | Tidak perlu module baru |
| `gradle/libs.versions.toml` | Default tidak perlu (QR lib hanya jika 6b QR in-scope — dokumentasikan) |
| `local.properties` | Secrets |
| DS theme hex / token rename | Consume only |

### Sibling features (jangan “sekalian”)

| File / Area | Alasan |
|-------------|--------|
| `features/dashboard/**` | Phase 4 selesai — jangan refactor |
| `features/transaction/**` | Phase 5 — hanya konsumsi via nav callback; jangan ubah form kecuali butuh query param wallet (additive, minimal) |
| Settings **selain** family-network thin wiring | Currency/Sheets = Phase 7 |

### Domain / data contracts existing

| Policy | Detail |
|--------|--------|
| Field `Transaction` / `Wallet` / `Budget` / `CategorySummary` | ❌ Jangan breaking change |
| UseCase finansial existing signatures | ❌ Jangan breaking; filter baru harus additive/default |
| `UserRepository` auth methods | ❌ Jangan ubah login/register/signOut behavior |
| `FirestoreNetworkDataSource` user upsert paths | ❌ Jangan campur create family ke sini; buat API/DS terpisah |

### Protected user model caution

| File | Policy |
|------|--------|
| `User.kt` | Field `familyId`/`familyRole` **sudah ada** — jangan rename |
| Menambah method `UserRepository.updateFamilyMembership(...)` | ⚠️ Boleh **additive** di 6b dengan review ketat + implementasi di `UserRepositoryImpl` yang tidak merusak auth |

---

## 9. File yang BOLEH Diubah / Dibuat

### 6a — Family feature

| Aksi | File |
|------|------|
| REWRITE | `FamilyViewModel.kt` |
| UPDATE | `FamilyScreen.kt` / Routing |
| BARU | `model/FamilyUIState.kt`, `FamilyUiMapper.kt` |
| UPDATE | `FamilyInsightsMockContent.kt` — Preview-only; ganti attribution model jika perlu |
| UPDATE | Components — terima state real / empty; format IDR |
| UPDATE | `FamilyNavigation.kt` — callbacks |

### 6a — App

| File | Perubahan |
|------|-----------|
| `HomeShell.kt` | Pass `onAddTransaction` / `onViewAllTransactions` ke `familyGraph` (sama seperti dashboard) |

### 6b — Domain additive

| File | Peran |
|------|-------|
| `core/domain/.../model/FamilyGroup.kt` | BARU |
| `core/domain/.../model/FamilyRole.kt` (enum) | BARU — mis. `OWNER`, `MEMBER` |
| `core/domain/.../repository/FamilyRepository.kt` | BARU |
| `core/domain/.../usecase/CreateFamilyGroupUseCase.kt` | BARU |
| `core/domain/.../usecase/JoinFamilyGroupUseCase.kt` | BARU |
| `LeaveFamilyGroupUseCase` | OPSIONAL |
| `UserRepository` + impl | Additive method update membership |

### 6b — Data additive

| File | Peran |
|------|-------|
| `FamilyGroupEntity` / DAO / Firestore DS / Mapper / `FamilyRepositoryImpl` | BARU |
| `DatabaseModule` / `CommonRepositoryModule` | Additive binds |
| Jangan edit financial sync batch kecuali perlu set `familyId` konsisten | Hati-hati |

### 6b — Settings / Family UI tipis

| File | Perubahan |
|------|-----------|
| Settings family network components | Tampilkan code real; copy; navigate/join sheet |
| Family empty-state CTA | Create / Join |

---

## 10. Struktur File Target

### Setelah 6a

```
features/family/.../presentation/
├── FamilyScreen.kt                 ← bind uiState
├── FamilyViewModel.kt              ← UseCases
├── navigation/FamilyNavigation.kt  ← callbacks
├── model/
│   ├── FamilyUIState.kt            ← BARU
│   ├── FamilyUiMapper.kt           ← BARU
│   └── FamilyInsightsMockContent.kt ← Preview fixture
└── components/                     ← UPDATE props only
```

### Tambahan 6b

```
core/domain/.../
├── model/FamilyGroup.kt
├── model/FamilyRole.kt
├── repository/FamilyRepository.kt
└── usecase/CreateFamilyGroupUseCase.kt
    JoinFamilyGroupUseCase.kt

core/data/.../
├── db/entity/FamilyGroupEntity.kt   (opsional lokal cache)
├── datasource/firestore/FamilyGroupFirestoreDataSource.kt
├── mapper/FamilyGroupMapper.kt
└── repository/FamilyRepositoryImpl.kt

features/family/.../presentation/
├── membership/
│   ├── JoinFamilySheet.kt / CreateFamilyScreen.kt  ← sesuai UX
│   └── FamilyMembershipViewModel.kt               ← atau gabung di FamilyViewModel
```

---

## 11. Pemetaan UI → Use Case (6a)

| Section UI | Sumber | Mapping |
|------------|--------|---------|
| Monthly total | `GetMonthlySummaryUseCase` → `currentMonth.totalExpense` (atau net) | `CurrencyFormat.formatIdr` |
| Spend segments / donut | `CategorySummary.byCategory` top N by expense | `fraction = totalExpense / sum`; label = category name |
| Shared budgets | `GetBudgetProgressUseCase(month)` | `progress`, tone dari `isOverBudget` / percent thresholds; labels IDR |
| History rows | `GetTransactionsUseCase(Params(walletId = familyWalletId, limit = 5))` | title/note/category; `addedByName` → pill text |
| Saving Together body | Bandingkan expense bulan ini vs bulan lalu dari trend | Copy template; hide jika null |
| FAB | Callback → NewEntry | Ideal: family wallet terpilih |
| View All | Callback → History | Ideal: filter wallet family |
| Empty: no family wallet | `WalletSummary.familyWallets.isEmpty()` | CTA create wallet (6a) / join family (6b) |

### Attribution (ganti mock Husband/Wife)

```kotlin
// UI pill
addedByLabel = transaction.addedByName.ifBlank { "Anggota" }
```

Enum `FamilyMemberAttribution` boleh dihapus atau diganti `String` di `FamilyHistoryRowUi`.

### Filter transaksi family tanpa `familyId` di Params

```kotlin
val familyWalletId = walletSummary.familyWallets.firstOrNull()?.id
// lalu GetTransactionsUseCase(Params(walletId = familyWalletId, limit = 5))
```

Jika banyak family wallet: ambil semua id → combine/filter di VM (atau limit ke wallet “primary” family).

**Jangan** breaking-change `GetTransactionsUseCase` hanya untuk Family — filter di VM cukup untuk 6a.

---

## 12. Desain Membership (6b)

### `FamilyGroup` (usulan)

```kotlin
data class FamilyGroup(
    val id: String,                 // = invite code / UUID
    val name: String,
    val inviteCode: String,         // short code shareable (boleh = id shortened)
    val ownerId: String,
    val memberIds: List<String>,
    val createdAt: Instant,
)
```

### `FamilyRepository` (usulan)

```kotlin
interface FamilyRepository {
    fun observeCurrentFamily(): Flow<FamilyGroup?>
    suspend fun createFamily(name: String, ownerId: String): Result<FamilyGroup>
    suspend fun joinFamily(inviteCode: String, userId: String): Result<FamilyGroup>
    suspend fun leaveFamily(userId: String): Result<Unit> // opsional
}
```

### Alur create

1. `CreateFamilyGroupUseCase` → tulis Firestore (+ lokal)
2. Update user membership (`familyId`, `familyRole = OWNER`)
3. Ensure family wallet (`WalletType.FAMILY`, `familyId`, `ownerId`)
4. UI → Insights refresh

### Alur join

1. User input invite code (Settings / sheet)
2. `JoinFamilyGroupUseCase` → validasi code → append member → update user `MEMBER`
3. Observability: family wallet muncul (shared) — detail sync multi-device mengikuti Phase 2 strategi

### User profile write (kritis)

Hari ini profile upsert auth sering **tidak** menimpa `familyId`. 6b harus punya jalur eksplisit:

```kotlin
// Contoh API additive — nama final bebas selama jelas
suspend fun updateFamilyMembership(familyId: String?, familyRole: String?): Result<Unit>
```

Implementasi: update Firestore user fields + DataStore lokal; **bukan** lewat Google/email sign-in path.

### QR (opsional)

- Encode: invite code string / deep link
- Decode: prefill join field
- Library: evaluasi `zxing` / ML Kit — hanya jika Task QR in-scope; jika ya, additive di version catalog

---

## 13. Task Breakdown Detail

### Task 0: Pastikan DoR Phase 5

Manual: NewEntry save + History list. Jika belum, selesaikan Phase 5 dulu.

---

### Task 1 (6a): `FamilyUIState` + Mapper

```kotlin
data class FamilyUIState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showJoinBanner: Boolean = false, // familyId == null (persiapan 6b)
    val monthlyTotalExpense: Long = 0L,
    val spendSegments: List<FamilySpendSegment> = emptyList(),
    val budgetRows: List<FamilyBudgetRowUi> = emptyList(),
    val historyRows: List<FamilyHistoryRowUi> = emptyList(),
    val insightTitle: String = "",
    val insightBody: String = "",
    val insightCtaLabel: String = "",
    val hasFamilyWallet: Boolean = false,
    val familyWalletId: String? = null,
    // label strings statis boleh tetap di composable
)
```

- Simpan `Long` di state; format di UI
- Preview tetap pakai `DefaultFamilyInsightsMockContent` **setelah** dikonversi ke IDR di fixture Preview (ganti `$` → contoh `Rp …`)

---

### Task 2 (6a): `FamilyViewModel`

Inject:

- `GetWalletSummaryUseCase`
- `GetMonthlySummaryUseCase`
- `GetBudgetProgressUseCase`
- `GetTransactionsUseCase`
- `GetCategoriesUseCase` (join icon/name)
- `UserRepository`
- `CommonDispatcher`

`combine` Flows → mapper → `_uiState`.

`currentMonth = YearMonth.now().toString()` (`yyyy-MM`).  
Trend: pass previous month untuk insight.

---

### Task 3 (6a): Bind Screen + empty states

1. `FamilyRouting` collect `uiState`
2. Jangan pass `DefaultFamilyInsightsMockContent` di production
3. Empty variants:
   - No family wallet → copy + (6a) hint “Buat dompet keluarga di Settings/New Entry” / (6b) join CTA
   - No budgets → hide section atau empty card
   - No history → empty list copy
4. Progress tones dari budget percent (align `KeuTrackProgressBar` / existing `FamilyBudgetBarTone`)

---

### Task 4 (6a): Nav callbacks

```kotlin
fun NavGraphBuilder.familyGraph(
    onAddTransaction: () -> Unit = {},
    onViewAllTransactions: () -> Unit = {},
)
```

`HomeShell`: pass callback yang sama dengan dashboard (root navigate NewEntry / History).

FAB Family → `onAddTransaction`.  
History “View All” → `onViewAllTransactions`.

---

### Task 5 (6a): Verify Insights

1. Buat transaksi di family wallet (atau personal dulu + pastikan empty state benar)
2. Budgets: create budget bulan ini (via data seed/debug jika belum ada UI) → muncul di Shared Budgets
3. Summary segments update setelah expense
4. Tidak ada `$` di UI runtime
5. Auth/dashboard/transaction tidak regres

---

### Task 6 (6b): Domain FamilyGroup

1. `FamilyGroup`, `FamilyRole`
2. `FamilyRepository` interface
3. UseCases create/join (+ validasi code tidak kosong, name length)
4. Compile `:core:domain`

---

### Task 7 (6b): Data + User membership API

1. Firestore DS `family_groups` collection (constants companion object — pola Phase 2)
2. `FamilyRepositoryImpl` offline-first ringan: tulis remote (+ cache lokal jika perlu)
3. Additive `UserRepository.updateFamilyMembership`
4. Di create/join: panggil membership update + `WalletRepository.createWallet` family jika belum ada
5. Hilt `@Binds`
6. **Jangan** ubah login rollback logic

Dokumentasikan security rules (file docs) — deploy rules di luar scope coding murni tetapi wajib disebut di PR.

---

### Task 8 (6b): UI membership

1. Family empty banner → “Buat Keluarga” / “Gabung dengan Kode”
2. Simple dialog/sheet: nama keluarga ATAU input kode
3. Settings Family Network: tampilkan `familyId`/`inviteCode`, copy clipboard, invite CTA → sheet yang sama
4. Setelah sukses → Insights refresh (`familyId` non-null, wallet muncul)

QR: hanya jika Task 5–8 selesai lebih cepat dari estimasi.

---

### Task 9: QA gabungan

| Kasus | 6a | 6b |
|-------|----|----|
| Insights IDR + budgets + history | ✅ | ✅ |
| FAB → NewEntry | ✅ | ✅ |
| User tanpa family | Empty/banner | Create/join |
| Dua device join code yang sama | — | Ideal; minimal single-device join path |
| Sign-out/in | Session user tetap | `familyId` tetap di profile |

---

## 14. Acceptance Criteria

### 6a harus terpenuhi

- [ ] Runtime Insights **bukan** `DefaultFamilyInsightsMockContent`
- [ ] Tidak ada format `$` di production UI Family
- [ ] Monthly total / segments dari `GetMonthlySummaryUseCase` (atau empty jelas)
- [ ] Budget rows dari `GetBudgetProgressUseCase`
- [ ] History dari transaksi family wallet (atau empty jelas)
- [ ] Attribution memakai `addedByName`
- [ ] FAB & View All ter-wire via `:app`
- [ ] `FamilyViewModel` tidak kosong
- [ ] Preview masih jalan (fixture terpisah)
- [ ] Build: `:features:family:compileDevDebugKotlin` + `assembleDevDebug`

### 6b harus terpenuhi (setelah 6a)

- [ ] `FamilyGroup` + `FamilyRepository` + Create/Join UseCase ada
- [ ] Create/join mengisi `User.familyId` (+ role) tanpa merusak auth
- [ ] Family wallet ter-link / terbuat dengan `familyId`
- [ ] Empty-state / Settings bisa memicu create/join
- [ ] Insights memakai data setelah join
- [ ] Auth login/register/sign-out regresi = nol
- [ ] QR opsional — tidak memblokir “6b done” jika ditunda

### Verification

```bash
./gradlew :features:family:compileDevDebugKotlin
./gradlew :core:domain:compileDebugKotlin   # setelah 6b
./gradlew :core:data:compileDebugKotlin     # setelah 6b
./gradlew assembleDevDebug

git diff --stat -- features/auth features/splashscreen build-plugin
# Expected: kosong (kecuali UserRepository additive method di 6b)
```

### Definition of Ready untuk Phase 7

1. Family tab berguna dengan data real (minimal 6a)
2. Idealnya user bisa create/join family (6b) sehingga Settings family ID punya sumber kebenaran
3. Phase 7 fokusokus persist currency, wire family ID copy/invite polish, wallets list — bukan membangun Insights dari nol

---

## 15. Catatan Arsitektur & Konvensi

### Feature rules

| Aturan | Phase 6 |
|--------|---------|
| Feature ↛ feature | Nav lewat `:app` callbacks |
| Feature ↛ Room | UseCase only |
| Screen stateless | Ya |
| Money `Long` + IDR format | Ya — larang `$` mock production |
| Offline-first | Insights baca Room; membership write lokal/remote sesuai pola data layer |

### Insights vs Membership

```
6a: baca agregat finansial (wallet FAMILY / budgets / summaries / txs)
6b: tulis identitas sosial (FamilyGroup + User.familyId) yang membuat 6a “shared” bermakna multi-anggota
```

### Jangan overload User

Simpan anggota & invite code di `FamilyGroup`, bukan hanya di `User`. `User` hanya menyimpan membership pointer (`familyId`, `familyRole`).

---

## 16. Dependency Graph

```
Phase 6a
────────
FamilyViewModel
  ├── GetWalletSummaryUseCase
  ├── GetMonthlySummaryUseCase
  ├── GetBudgetProgressUseCase
  ├── GetTransactionsUseCase
  ├── GetCategoriesUseCase
  └── UserRepository (banner familyId?)
        │
        ▼
FamilyScreen (Insights UI existing)
        │
        ├── onAddTransaction ──▶ :app ──▶ TransactionRoute
        └── onViewAllTransactions ──▶ :app ──▶ HistoryRoute

Phase 6b
────────
Create/Join UseCase → FamilyRepository → Firestore family_groups
                 └→ UserRepository.updateFamilyMembership
                 └→ WalletRepository.createWallet (FAMILY)
```

---

## 17. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| Tidak ada family wallet / budget di DB | Insights kosong total | Empty states jelas; seed wallet on 6b create; dokumentasikan cara buat budget |
| Mock Husband/Wife menyesatkan | Product lie | Ganti ke `addedByName` di 6a |
| Donut member-% tanpa data anggota | Scope creep | 6a = category breakdown, bukan member split |
| Update `UserRepository` merusak auth | Login break | Additive method only; test sign-in/out; jangan ubah `completeSignIn` rollback |
| Profile sync menimpa `familyId` jadi null | Membership hilang | Audit Firestore upsert; field update terpisah |
| Dual-device conflict membership | Join race | MVP single-writer; rules + transaction Firestore |
| QR library membengkak APK | Delay | QR opsional |
| Settings Phase 7 overlap | Duplikasi kerja | 6b hanya membership; currency/Sheets tetap Phase 7 |
| Filter history family tidak sempurna | UX | MVP filter `walletId`; improve later |

---

## 18. Urutan Pengerjaan yang Disarankan

```
── Phase 6a ──
Step 1: FamilyUIState + Mapper (IDR) + Preview fixture tanpa $
Step 2: FamilyViewModel combine UseCases
Step 3: Bind Screen + empty states
Step 4: HomeShell callbacks FAB / View All
Step 5: Device QA Insights

── Phase 6b ──
Step 6: Domain FamilyGroup + Repository + UseCases
Step 7: Data impl + User membership API + family wallet ensure
Step 8: UI create/join (Family empty + Settings thin)
Step 9: (Opsional) QR
Step 10: QA membership + Insights after join
Step 11: assembleDevDebug + protected diff check
```

---

## Ringkasan File Policy

| Kategori | Policy |
|----------|--------|
| `features/family/**` | ✅ Primary |
| `app/.../HomeShell.kt` | ✅ Additive callbacks |
| Settings family-network thin wiring | ✅ 6b only |
| Domain/data **FamilyGroup** baru | ✅ 6b additive |
| `UserRepository` membership API | ⚠️ Additive + hati-hati auth |
| Existing finansial UseCase/entity signatures | ❌ No breaking changes |
| `features/auth/**`, `splashscreen/**` | ❌ |
| `features/dashboard/**` | ❌ |
| `features/transaction/**` | ❌ kecuali param nav minimal |
| Settings currency/Sheets | ❌ Phase 7 |
| `build-plugin/**`, theme hex | ❌ |

---

## Estimasi Effort

| Bucket | Porsi |
|--------|-------|
| 6a UIState/VM/mapper/bind + IDR + empty | ~40% |
| 6a nav callbacks + QA | ~10% |
| 6b domain + data + user membership | ~30% |
| 6b UI create/join + Settings thin + QA | ~15% |
| QR opsional | ~5% |

---

## Phase 5 vs Phase 6 (Ringkas)

```
Phase 5 (done / in progress)          Phase 6
─────────────────────────────────     ─────────────────────────────────
NewEntry + History milik transaction  Family Insights baca agregat shared
FAB Dashboard → TransactionRoute      FAB Family → callback yang sama
WalletType.FAMILY di picker           FamilyGroup + User.familyId (6b)
                                      Budgets + CategorySummary di tab Family
```

---

*Dokumen ini adalah referensi implementasi untuk Phase 6 KeuTrack. Kerjakan 6a sampai Insights real + IDR, lalu 6b untuk membership. Setelah itu lanjut Phase 7 (Settings persistence: currency, family ID polish, wallets list).*
