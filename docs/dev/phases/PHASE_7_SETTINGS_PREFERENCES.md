# Phase 7 — features:settings (Preferences Persistence + Connected Wallets)

> **Modul target:** `:features:settings` (+ domain `LeaveFamilyGroupUseCase`; konsumsi UseCase existing)  
> **Status:** ✅ **COMPLETE** (2026-08-03)  
> **Keputusan produk:** Currency picker/persistence **dihapus dari scope** — app tetap IDR-only; field `User.currency` tersedia untuk future use  
> **Prasyarat:** Phase 1–2 ✅ · Phase 4–5 ✅ · Phase 6a/6b ✅ · Phase 6c ✅ (shared family sync)  
> **Hasil akhir:** Settings menampilkan connected wallets dari Room; leave family wired; Google Sheets deferred dengan UX jujur ("Segera hadir"); single `SettingsUIState` + mapper (pola Dashboard)

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
11. [Pemetaan UI → Data / Use Case](#11-pemetaan-ui--data--use-case)
12. [Desain Currency Persistence](#12-desain-currency-persistence)
13. [Task Breakdown Detail](#13-task-breakdown-detail)
14. [Acceptance Criteria](#14-acceptance-criteria)
15. [Catatan Arsitektur & Konvensi](#15-catatan-arsitektur--konvensi)
16. [Dependency Graph](#16-dependency-graph)
17. [Risiko & Mitigasi](#17-risiko--mitigasi)
18. [Urutan Pengerjaan yang Disarankan](#18-urutan-pengerjaan-yang-disarankan)
19. [Relasi ke Phase Lain](#19-relasi-ke-phase-lain)

---

## 1. Konteks & Tujuan

Menurut `docs/dev/Project_Assessment_Current.md` (checkpoint aktif) dan handoff Phase 6 / 6c:

| Area | Status sekarang |
|------|-----------------|
| Tab Settings di `HomeShell` + `SettingsRoute` | ✅ |
| Profile (nama, email, avatar) dari `UserRepository` | ✅ |
| Sign out + `syncUserProfile()` on init | ✅ |
| Family Network: invite code real, create/join dialog, copy clipboard | ✅ (Phase 6b) |
| Primary Currency picker UI | ✅ Compose — **`onCurrencySelected = {}`** |
| `User.currency` field (domain + Proto + Firestore read) | ✅ Field ada; **tidak ada write path preference** |
| `upsertUserProfile` existing user | ❌ **Tidak** menulis `currency` (hanya identity) — by design auth |
| Connected Wallets section | ❌ Hardcoded mock (`Main Savings` / `Emergency Fund`) |
| Google Sheets card | ❌ Toggle/export UI only |
| `SettingsUIState` | ❌ Hampir mati — hanya `SignOutState`; content digabung ad-hoc di Routing |
| Leave family | ❌ Tidak ada |

Assessment historis (`Project_Assessment.md`) menyebut Settings ~60% dengan family ID mock. **Itu sudah usang** — family wired di 6b. Phase 7 fokus **preferences + wallets**, bukan membangun membership dari nol.

**Tujuan Phase 7:**
- **7a:** Persist currency preference (Firestore + DataStore); wire connected wallets dari `GetWalletSummaryUseCase`; konsolidasi `SettingsUIState` + mapper (pola Dashboard/Family)
- **7b (tipis):** Harden family ID empty/copy UX; opsional `LeaveFamilyGroupUseCase`; Google Sheets → “Coming soon” (bukan fake sync)

**Bukan tujuan Phase 7:**
- Integrasi Google Sheets API / OAuth Sheets export
- FX conversion / multi-currency ledger (amount tetap `Long` canonical)
- Redesign visual Atelier besar
- Auth / splash / build-plugin changes
- QR invite, budget authoring UI, transaction edit/delete
- Phase 9 unit tests (boleh smoke manual saja)

---

## 2. Inventory — Apa yang Sudah Ada

### Feature settings

| File | Peran | Status vs Phase 7 |
|------|-------|-------------------|
| `presentation/SettingsScreen.kt` | `SettingsRouting` + `SettingsScreen` | UPDATE — bind state real |
| `presentation/SettingsViewModel.kt` | User + Family + create/join + signOut | UPDATE — currency + wallets |
| `presentation/navigation/SettingsNavigation.kt` | `SettingsRoute`, `settingsGraph()` | Baca; ubah hanya jika callback baru perlu |
| `presentation/model/SettingsUIState.kt` | Hanya `SignOutState` | REWRITE / perluas |
| `presentation/model/SettingsScreenContent.kt` | Content model + `DefaultSettingsMockContent` | UPDATE — Preview fixture |
| `components/SettingsProfileCard.kt` | Profile row | Pertahankan props |
| `components/SettingsFamilyIdHeroCard.kt` | Family ID + copy | Pertahankan; empty state polish |
| `components/SettingsFamilyActionTile.kt` | Invite / Manage tiles | Pertahankan |
| `components/SettingsPrimaryCurrencyRow.kt` | Dropdown currency | Wire callback |
| `components/SettingsConnectedWalletCard.kt` | Satu wallet card | Data real via mapper |
| `components/SettingsGoogleSheetsCard.kt` | Toggle + Export | UX deferred |
| `components/SettingsSectionHeader.kt` / `SettingsStatusChip.kt` | Layout helpers | Consume |
| `membership/SettingsFamilyMembershipDialog.kt` | Create/Join dialog | Sudah jalan — jangan regress |

### Domain hooks siap konsumsi

| Item | Lokasi | Catatan |
|------|--------|---------|
| `User.currency` | `core/domain/.../model/User.kt` | Default `"IDR"` — **jangan rename** |
| `UserRepository.getCurrentUser()` | Flow session | Source of truth lokal |
| `UserRepository.updateFamilyMembership` | Additive write terpisah dari auth upsert | **Pola yang ditiru** untuk currency |
| `GetWalletSummaryUseCase` | `WalletSummary` personal + family | Connected wallets |
| `Wallet` / `WalletType` | Domain | Map ke `ConnectedWalletUi` |
| `FamilyRepository.observeCurrentFamily()` | Invite code / name | Sudah di-Settings VM |
| `CreateFamilyGroupUseCase` / `JoinFamilyGroupUseCase` | Membership | Jangan ubah behavior |

### Data layer — gap currency write

| Item | Perilaku hari ini |
|------|-------------------|
| `FirestoreNetworkDataSource.upsertUserProfile` | Existing user: update **hanya** displayName/email/photoUrl |
| Create user doc | Set `currency = "IDR"` sekali |
| `getUserProfile` | Baca `currency` |
| `updateFamilyMembership` | Dedicated field update — **template untuk `updateCurrency`** |
| Proto `SignedInUser.currency` | Sudah ada field 5 |
| `SignedInUserProtoMapper` | Map currency ↔ domain |

### Design system

| API | Dipakai Settings? |
|-----|-------------------|
| `CurrencyFormat.formatIdr` | Belum di Settings wallets (mock string `Rp …`) |
| `KeuTrackCurrencyText` | Belum |
| `KeuTrackStatusChip` | Settings punya `SettingsStatusChip` lokal — OK consume |
| `KeuTrackCard` / `KeuTrackButton` / `KeuTrackTopBar` | Sudah |

### App nav

| File | Catatan |
|------|---------|
| `app/.../HomeShell.kt` | `settingsGraph(onSignOutSuccess)` — biasanya **tidak perlu** ubah untuk 7a |
| Sibling features | Dashboard/Family/Transaction **tidak** diubah di Phase 7 |

---

## 3. Keputusan Produk

### 3.1 Currency = preference profile, bukan FX ledger

| Keputusan | Detail |
|-----------|--------|
| Storage | `User.currency` di Firestore `/users/{uid}` + Proto DataStore |
| Amounts | Tetap `Long` tanpa desimal; **tidak ada konversi kurs** |
| Formatter | Overview/amounts tetap `CurrencyFormat.formatIdr` (canonical IDR) di Phase 7 |
| Picker options | Pertahankan `IDR` / `USD` / `EUR` di UI sebagai **preference label** yang di-persist |
| Semantik UX | Subtitle jujur: preference tersimpan untuk profil (dan future Sheets/export); **saldo tidak diubah** saat ganti currency |
| Follow-up (bukan Phase 7) | Prefix formatter multi-currency + FX = phase terpisah / polish |

**Mengapa tidak mengubah `CurrencyFormat` di semua screen sekarang:** Phase 3–6 sudah mengunci IDR display; mengubah formatter global berisiko regress Dashboard/Family/Transaction. Phase 7 hanya **menyimpan** preference + menampilkan pilihan terpilih di Settings.

Jika produk ingin picker **hanya IDR** sampai FX siap: boleh sederhanakan options menjadi `listOf("IDR")` + disable dropdown — dokumentasikan di PR. Default plan: biarkan 3 opsi + persist.

### 3.2 Connected wallets = read-only list dari Room

| Keputusan | Detail |
|-----------|--------|
| Sumber | `GetWalletSummaryUseCase()` → personal + family wallets |
| Aksi | **Tidak** create/edit/delete wallet dari Settings di Phase 7 |
| Empty | Tampilkan empty copy jika belum ada wallet |
| Format amount | `CurrencyFormat.formatIdr(wallet.balance)` |
| Status chip | `PERSONAL` → Active; `FAMILY` → Shared (+ `leadingAccent = true`) |

### 3.3 Google Sheets = deferred dengan UX jujur

| Keputusan | Detail |
|-----------|--------|
| Card UI | Boleh tetap ada (investasi layout) |
| Toggle / Export | Toast / snackbar “Segera hadir” — **jangan** simulasikan sync enabled palsu |
| Persist `sheetsSyncEnabled` | ❌ Tidak di Phase 7 (tidak ada field domain) |
| Default content | `sheetsSyncEnabled = false` di state real |

### 3.4 Family polish vs leave

| Item | Tier |
|------|------|
| Copy invite code + empty “Belum bergabung” | Wajib (sudah ada — harden labels) |
| Manage circle menampilkan nama keluarga | Wajib tipis (sudah Toast nama) |
| `LeaveFamilyGroupUseCase` | **Opsional 7b** — hanya jika waktu memungkinkan |
| QR invite | ❌ Out of scope |

### 3.5 Mengapa split 7a / 7b

| Tahap | Goal | Domain baru? |
|-------|------|--------------|
| **7a** | Currency write path + wallets + UIState | Ya — additive `updateCurrency` + UseCase |
| **7b** | Leave family / copy polish ekstra | Opsional Leave use case |

7a saja sudah menutup gap assessment #1 untuk Settings.

---

## 4. Scope — Apa yang Dikerjakan

### A. Phase 7a — Preferences + wallets (wajib)

| # | Item |
|---|------|
| 1 | Domain: `UserRepository.updateCurrency(currency: String): Result<Unit>` (**additive**) |
| 2 | Domain: `UpdateCurrencyUseCase` — validasi kode (whitelist `IDR`/`USD`/`EUR`), trim, uppercase |
| 3 | Data: `FirestoreNetworkDataSource.updateCurrency(uid, currency)` — dedicated field update (mirror membership) |
| 4 | Data: `UserRepositoryImpl.updateCurrency` — Firestore lalu persist DataStore lokal; `CancellationException` propagate |
| 5 | Pastikan `upsertUserProfile` / `syncUserProfile` **tidak** men-clobber currency yang baru di-set (sudah demikian untuk existing update; verifikasi resolve + local preserve) |
| 6 | Feature: `SettingsUIState` tunggal (profile, family, currency, wallets, signOut, membership, currencySaving/error) |
| 7 | Feature: `SettingsUiMapper` — `User` + `FamilyGroup?` + `WalletSummary` → content UI |
| 8 | Rewrite `SettingsViewModel` — `combine` Flows (pola Dashboard); inject UseCase + dispatcher |
| 9 | Wire `onCurrencySelected` → `UpdateCurrencyUseCase`; optimistic UI atau loading kecil + error snackbar |
| 10 | Connected wallets dari Room; hapus mock amounts di runtime |
| 11 | `DefaultSettingsMockContent` hanya untuk `@Preview` |
| 12 | Google Sheets: no-op → “Segera hadir”; default sync off |
| 13 | Previews light/dark tetap hijau |

### B. Phase 7b — Polish (setelah 7a hijau)

| # | Item |
|---|------|
| 14 | Harden copy/empty family strings (konsisten ID/EN dengan app) |
| 15 | Opsional: `LeaveFamilyGroupUseCase` + confirm dialog + clear membership + clear local family wallet link sesuai 6c rules |
| 16 | Opsional: tampilkan `familyRole` chip di Family Network |

---

## 5. Scope — Apa yang TIDAK Dikerjakan

| Item | Alasan | Ditunda ke |
|------|--------|------------|
| Google Sheets API / export CSV otomatis | Belum ada kontrak backend | Future / product |
| FX rate / konversi saldo | Ledgers tetap IDR Long | Future |
| Ubah `CurrencyFormat` global di Dashboard/Family/Transaction | Regress risk | Polish terpisah |
| Create/rename/delete wallet UI | Settings read-only list | Future wallet mgmt |
| QR invite | Nice-to-have | Future |
| Forgot password / email verification | Auth docs out of scope | Future |
| Edit profile (display name) | Tidak diminta assessment Phase 7 | Future |
| Breaking change `User` / auth sign-in upsert | Protected | — |
| Unit tests penuh | Phase 9 | Phase 9 |
| Hapus SMPOB dashboard boilerplate | Assessment remaining #2; **bukan** Settings | Cleanup terpisah |
| Ubah Firestore rules user | `currency` sudah field owner doc; rules user existing cukup | Hanya jika Console belum allow update field |

---

## 6. Prasyarat (Definition of Ready)

Dari Phase 6 DoR + assessment current:

- [x] Family tab insights real + IDR (6a)
- [x] Create/join family + `User.familyId` (6b)
- [x] Shared family wallet pull sync (6c) — Settings family code punya sumber kebenaran
- [x] `GetWalletSummaryUseCase` + Room wallets
- [x] `User.currency` di model + Proto + Firestore read
- [ ] Branch kerja: `feat/settings-preferences` (atau setara)
- [ ] Baca dokumen ini + inventory Settings sebelum coding

**Definition of Ready untuk Phase 9 (setelah Phase 7):**
1. Currency preference survive cold start + `syncUserProfile`
2. Connected wallets mirror Dashboard balances (same Room source)
3. Settings tidak lagi mengandalkan mock finansial di runtime

---

## 7. File Referensi (Read-Only)

> Gunakan untuk belajar pola / kontrak. **Jangan** “sekalian refactor” file di tabel ini kecuali kolom Policy bilang boleh additive di §9.

### Dokumen fase & assessment

| File | Gunakan untuk |
|------|---------------|
| `docs/dev/Project_Assessment_Current.md` | **Checkpoint aktif** — Settings ~70%, prioritas Phase 7 |
| `docs/dev/Project_Assessment.md` | Historis saja (pre–Phase 1); **jangan** dipakai sebagai status terkini |
| `docs/dev/phases/PHASE_1_DOMAIN_ENTITIES_AND_USE_CASES.md` | `User.currency`, `Wallet`, repo signatures |
| `docs/dev/phases/PHASE_2_ROOM_AND_REPOSITORY_IMPLEMENTATION.md` | Offline-first; jangan campur financial DS ke auth user path |
| `docs/dev/phases/PHASE_3_DESIGN_SYSTEM.md` | `CurrencyFormat`; catatan Settings currency = Phase 7 |
| `docs/dev/phases/PHASE_4_DASHBOARD_REAL_DATA.md` | Pola `UIState` + `combine` Flows + mapper |
| `docs/dev/phases/PHASE_5_TRANSACTION_FLOW.md` | Batas Settings vs Transaction |
| `docs/dev/phases/PHASE_6_FAMILY_INSIGHTS_AND_MEMBERSHIP.md` | Family thin wiring sudah di Settings; DoR Phase 7 |
| `docs/dev/phases/PHASE_6C_SHARED_FAMILY_DATA_SYNC.md` | Canonical family wallet; jangan pecahkan pull sync |
| `docs/dev/DESIGN_SYSTEM_ATELIER.md` | Katalog komponen |
| `plans/KeuTrack_Data_Design.md` | Field `users.currency` di desain |
| `.cursor/rules/keutrack-feature-module.mdc` | Screen / Routing / VM |
| `.cursor/rules/keutrack-domain-layer.mdc` | UseCase `Result` + validasi |
| `.cursor/rules/keutrack-data-layer.mdc` | `CancellationException`; DS constants |
| `.cursor/skills/keutrack-dev/SKILL.md` | Protected files global |

### Domain / use case (consume; edit hanya additive di §9)

| File | Pelajari |
|------|----------|
| `core/domain/.../model/User.kt` | Field `currency` — **jangan rename** |
| `core/domain/.../model/Wallet.kt` / `WalletType.kt` | Map connected wallets |
| `core/domain/.../repository/UserRepository.kt` | Pola `updateFamilyMembership` |
| `core/domain/.../repository/WalletRepository.kt` | `observeWallets` (via UseCase) |
| `core/domain/.../usecase/GetWalletSummaryUseCase.kt` | `WalletSummary` |
| `core/domain/.../usecase/CreateFamilyGroupUseCase.kt` | Jangan ubah wallet ensure 6c |
| `core/domain/.../usecase/JoinFamilyGroupUseCase.kt` | Idem |
| `core/domain/.../usecase/SyncFamilyDataUseCase.kt` | Jangan sentuh |

### Pola UI yang ditiru

| File | Pelajari |
|------|----------|
| `features/dashboard/.../DashboardViewModel.kt` | `combine` + `stateIn` + `CommonDispatcher` |
| `features/dashboard/.../model/DashboardUiMapper.kt` | Domain → UI labels + `CurrencyFormat` |
| `features/family/.../FamilyViewModel.kt` | Family + user combine |
| `features/settings/.../SettingsViewModel.kt` | Baseline membership/signOut (pertahankan behavior) |
| `features/settings/.../SettingsScreen.kt` | Layout sections yang dipertahankan |
| `features/settings/.../model/SettingsScreenContent.kt` | Field UI yang harus dipetakan |

### Auth user persistence (pelajari — ubah **hanya** additive currency path)

| File | Pelajari | Policy edit |
|------|----------|-------------|
| `core/data/.../datasource/FirestoreNetworkDataSource.kt` | Upsert identity vs membership update | ✅ Additive `updateCurrency` saja |
| `core/data/.../repository/UserRepositoryImpl.kt` | Orchestration; preserve currency on auth refresh | ✅ Additive method + jangan regress auth |
| `core/data/.../mapper/SignedInUserProtoMapper.kt` | Local session fields | ❌ Biasanya tidak perlu ubah |
| `core/datastore/.../signed_in_user.proto` | Field `currency = 5` | ❌ **Jangan** ubah proto numbering |

### Design system (consume)

| File | Pakai untuk |
|------|-------------|
| `core/designsystem/.../format/CurrencyFormat.kt` | Format saldo wallet |
| `core/designsystem/.../component/KeuTrackCurrencyText.kt` | Opsional di wallet amount |
| `core/designsystem/.../theme/*` | `KeuTrackTheme` di Preview |

---

## 8. File yang TIDAK BOLEH Diubah

### Auth / splash / infra (HARD FREEZE)

| File / Area | Alasan |
|-------------|--------|
| `features/auth/**` | Complete — Google/email flows stabil |
| `features/splashscreen/**` | Complete |
| `build-plugin/**` | Stable conventions |
| `settings.gradle.kts`, root Gradle, `gradle/libs.versions.toml` | Tidak perlu module/dep baru |
| `local.properties`, `google-services.json` | Secrets / Firebase config |
| `core/datastore/**/*.proto` | Field currency sudah ada — **jangan** renumber / rename |

### Sibling features (jangan “sekalian”)

| File / Area | Alasan |
|-------------|--------|
| `features/dashboard/**` | Phase 4 selesai |
| `features/transaction/**` | Phase 5 selesai |
| `features/family/**` | Phase 6/6c selesai — kecuali bug blocker yang ditemukan saat Settings QA (minimal fix + catat) |
| `app/.../KeuTrackNavHost.kt` / `HomeShell.kt` | Default tidak perlu; hanya jika sign-out callback rusak |

### Domain / data contracts — no breaking changes

| Policy | Detail |
|--------|--------|
| `User` field names / defaults | ❌ Jangan rename `currency` / `familyId` / `familyRole` |
| Auth methods `signIn*` / `register*` / `signOut` / `syncUserProfile` | ❌ Jangan ubah behavior |
| `upsertUserProfile` identity-only update | ❌ Jangan mulai menulis semua field User di sini — gunakan dedicated `updateCurrency` |
| Financial entities / DAOs / SyncWorker | ❌ Bukan scope Settings |
| `CreateFamilyGroupUseCase` / `JoinFamilyGroupUseCase` / canonical wallet | ❌ Jangan regress 6c |
| UseCase finansial signatures existing | ❌ Consume only |

### Design system theme

| File | Alasan |
|------|--------|
| `core/designsystem/.../theme/Colors.kt` hex / token rename | Freeze — Settings hanya consume |
| `CurrencyFormat.kt` API breaking rename `formatIdr` | ❌ Jangan; additive overload boleh **hanya jika** benar-benar perlu (default: tidak) |

### Protected user model caution (dari skill)

| File | Policy |
|------|--------|
| `core/domain/.../model/User.kt` | Field sudah ada — **jangan** ubah struktur |
| `core/domain/.../model/AuthResult.kt` / `TokenResult.kt` | Freeze |
| `UserRepository` auth surface | Freeze; **hanya** additive `updateCurrency` |
| `UserRepositoryImpl` | Additive method OK; review ketat agar auth/sign-out/sync tidak rusak |

---

## 9. File yang BOLEH Diubah / Dibuat

### Domain (additive)

| Aksi | File |
|------|------|
| UPDATE | `core/domain/.../repository/UserRepository.kt` — tambah `updateCurrency` |
| BARU | `core/domain/.../usecase/UpdateCurrencyUseCase.kt` |
| OPSIONAL BARU | `LeaveFamilyGroupUseCase.kt` (7b) |

### Data (additive currency path saja)

| Aksi | File |
|------|------|
| UPDATE | `FirestoreNetworkDataSource.kt` — `updateCurrency(...)` |
| UPDATE | `UserRepositoryImpl.kt` — impl `updateCurrency` + pastikan `getCurrentUser` onStart **tetap** preserve currency (sudah ada komentar) |

**Jangan** edit: Wallet/Transaction Firestore DS, SyncWorker, FamilyGroup DS, Room entities — kecuali bug blocker.

### Feature settings (primary)

| Aksi | File |
|------|------|
| REWRITE / perluas | `SettingsViewModel.kt` |
| UPDATE | `SettingsScreen.kt` / `SettingsRouting` |
| REWRITE / perluas | `model/SettingsUIState.kt` |
| UPDATE | `model/SettingsScreenContent.kt` — mock = Preview only |
| BARU | `model/SettingsUiMapper.kt` |
| UPDATE tipis | Components bila props empty-state / disabled Sheets perlu |
| UPDATE tipis | `membership/*` hanya jika Leave dialog (7b) |

### App

| File | Perubahan |
|------|-----------|
| `HomeShell.kt` / nav | **Biasanya tidak ada** |

### Docs (opsional di PR yang sama)

| File | Perubahan |
|------|-----------|
| `docs/dev/Project_Assessment_Current.md` | Setelah merge: centang Phase 7 items |
| Dokumen ini | Checklist progress jika perlu |

---

## 10. Struktur File Target

### Setelah 7a

```
core/domain/.../
├── repository/UserRepository.kt          ← + updateCurrency
└── usecase/UpdateCurrencyUseCase.kt      ← BARU

core/data/.../
├── datasource/FirestoreNetworkDataSource.kt  ← + updateCurrency
└── repository/UserRepositoryImpl.kt         ← + updateCurrency

features/settings/.../presentation/
├── SettingsScreen.kt                      ← bind SettingsUIState
├── SettingsViewModel.kt                   ← combine + currency + wallets
├── navigation/SettingsNavigation.kt       ← biasanya unchanged
├── model/
│   ├── SettingsUIState.kt                 ← state lengkap
│   ├── SettingsUiMapper.kt                ← BARU
│   └── SettingsScreenContent.kt           ← Preview fixture
├── components/                            ← props only bila perlu
└── membership/                            ← create/join tetap
```

### Tambahan 7b (opsional)

```
core/domain/.../usecase/LeaveFamilyGroupUseCase.kt
features/settings/.../membership/SettingsLeaveFamilyDialog.kt  (atau reuse confirm)
```

---

## 11. Pemetaan UI → Data / Use Case

| UI section | Sumber data | Aksi user |
|------------|-------------|-----------|
| Profile card | `User` via `getCurrentUser()` | — (read-only) |
| Family ID hero | `FamilyGroup.inviteCode` ?: `User.familyId` | Copy clipboard |
| Invite member | Membership state | Copy code / buka Join dialog |
| Manage circle | `FamilyGroup.name` / create | Create dialog atau tampil nama |
| Primary currency | `User.currency` | `UpdateCurrencyUseCase` |
| Connected wallets | `GetWalletSummaryUseCase` | Read-only |
| Google Sheets | Local UI flag `false` | Toast “Segera hadir” |
| Sign out | `UserRepository.signOut()` | Existing |

### Target `SettingsUIState` (contoh bentuk)

```kotlin
data class SettingsUIState(
    val isLoading: Boolean = true,
    val profile: SettingsProfileUi = SettingsProfileUi(/* empty defaults */),
    val familyNetworkActive: Boolean = false,
    val familyIdCode: String = "",
    val familyDisplayName: String? = null,
    val primaryCurrencyOptions: List<String> = listOf("IDR", "USD", "EUR"),
    val primaryCurrencySelected: String = "IDR",
    val isCurrencyUpdating: Boolean = false,
    val currencyError: String? = null,
    val connectedWallets: List<ConnectedWalletUi> = emptyList(),
    val sheetsSyncEnabled: Boolean = false,
    val signOutState: SignOutState = SignOutState.Idle,
    val membershipLoading: Boolean = false,
    val membershipMessage: String? = null,
    val errorMessage: String? = null,
)
```

Routing mengumpulkan **satu** `uiState` (bukan banyak StateFlow terpisah) — boleh tetap expose membership helpers jika migrasi bertahap, tetapi arah akhir = satu state.

### Mapper wallets (sketch)

```
WalletType.PERSONAL →
  title = wallet.name
  subtitle = "Personal"
  amountLabel = CurrencyFormat.formatIdr(balance)
  status = Active
  leadingAccent = false

WalletType.FAMILY →
  subtitle = "Family Vault" / "Shared"
  status = Shared
  leadingAccent = true
```

Urutan: personal dulu, lalu family. Jika `personalWallet == null` && `familyWallets.isEmpty()` → empty section message.

---

## 12. Desain Currency Persistence

### Flow

```
Settings dropdown select "USD"
  → ViewModel.updateCurrency("USD")
  → UpdateCurrencyUseCase
       validate ∈ {IDR, USD, EUR}
  → UserRepository.updateCurrency
       → FirestoreNetworkDataSource.updateCurrency(uid, "USD")  // field-only
       → UserProfileLocalDataSource.persist(local.copy(currency = "USD"))
  → getCurrentUser() Flow emit → UI selected = USD
```

### Firestore API (mirror membership)

```kotlin
suspend fun updateCurrency(uid: String, currency: String) {
    firestore.collection(COLLECTION_USERS)
        .document(uid)
        .update(
            mapOf(
                FIELD_CURRENCY to currency,
                FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
            ),
        )
        .await()
}
```

### Aturan penting vs `syncUserProfile` / auth upsert

| Path | Boleh tulis currency? |
|------|------------------------|
| `upsertUserProfile` create | ✅ default `IDR` |
| `upsertUserProfile` existing | ❌ jangan overwrite currency |
| `updateCurrency` | ✅ dedicated |
| `updateFamilyMembership` | ❌ jangan sentuh currency |
| `getCurrentUser` onStart Auth refresh | ❌ jangan overwrite currency dari Auth stub (sudah preserve) |
| `resolveUserForPersist` / `syncUserProfile` | Membaca Firestore → boleh **mengisi** currency dari remote (sumber kebenaran cloud) |

**Regression test manual:** set currency → kill app → cold start splash `syncUserProfile` → Settings masih menampilkan currency yang sama.

### Offline / error

- Firestore gagal → `Result.failure`; **jangan** persist lokal alone (hindari drift), kecuali keputusan eksplisit offline-first preference (MVP: require online untuk ganti currency — selaras membership write).
- Tampilkan snackbar error; selected kembali ke nilai Flow terakhir.

### UseCase validasi

```kotlin
class UpdateCurrencyUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(currency: String): Result<Unit> {
        val normalized = currency.trim().uppercase()
        if (normalized !in SUPPORTED) {
            return Result.failure(IllegalArgumentException("Currency tidak didukung"))
        }
        return userRepository.updateCurrency(normalized)
    }

    private companion object {
        val SUPPORTED = setOf("IDR", "USD", "EUR")
    }
}
```

---

## 13. Task Breakdown Detail

### Task 7.1 — Domain contract currency

1. Tambah di `UserRepository`:
   ```kotlin
   suspend fun updateCurrency(currency: String): Result<Unit>
   ```
2. Buat `UpdateCurrencyUseCase` dengan whitelist.
3. Compile: `./gradlew :core:domain:compileDebugKotlin`

### Task 7.2 — Data write path

1. `FirestoreNetworkDataSource.updateCurrency`.
2. `UserRepositoryImpl.updateCurrency` — pola sama `updateFamilyMembership` (auth uid check → Firestore → local persist).
3. Audit cepat: `upsertUserProfile` existing branch **tetap** tanpa `FIELD_CURRENCY`.
4. Compile: `./gradlew :core:data:compileDebugKotlin`

### Task 7.3 — SettingsUiMapper + UIState

1. Perluas / rewrite `SettingsUIState`.
2. Buat `SettingsUiMapper.from(user, family, walletSummary)`.
3. `DefaultSettingsMockContent` hanya Preview — pastikan tidak dipakai sebagai fallback finansial di runtime (profile fallback nama OK selama user null transient).

### Task 7.4 — SettingsViewModel rewrite

1. Inject: `UserRepository`, `FamilyRepository`, `GetWalletSummaryUseCase`, `UpdateCurrencyUseCase`, create/join use cases, `CommonDispatcher`.
2. `combine(user, family, wallets) { … mapper … }` → `uiState`.
3. Pertahankan `createFamily` / `joinFamily` / `signOut` / `syncUserProfile` init behavior.
4. `fun onCurrencySelected(code: String)` dengan loading/error flags.
5. Sheets handlers: message “Segera hadir”.

### Task 7.5 — SettingsRouting / Screen bind

1. Ganti ad-hoc `remember(DefaultSettingsMockContent.copy(…))` dengan `uiState`.
2. Wire `onCurrencySelected = viewModel::onCurrencySelected`.
3. Empty wallets UI.
4. Snackbar untuk `currencyError` + membership message (reuse pola existing).

### Task 7.6 — QA manual

| # | Scenario | Expected |
|---|----------|----------|
| 1 | Buka Settings | Profile real; currency dari `User.currency` |
| 2 | Ganti IDR → USD | Selected USD; Firestore field `currency=USD`; DataStore match |
| 3 | Cold start | Currency tetap USD setelah sync |
| 4 | Airplane mode ganti currency | Error; UI tidak “berhasil palsu” |
| 5 | Connected wallets | Saldo = Dashboard personal/family |
| 6 | Belum ada wallet | Empty state, bukan mock 12jt |
| 7 | Family copy / create / join | Tidak regress 6b |
| 8 | Sheets toggle/export | “Segera hadir”, bukan sync palsu |
| 9 | Sign out | Stack ke Login seperti sekarang |

### Task 7.7 — 7b opsional Leave family

Hanya jika 7a hijau dan masih ada bandwidth:

1. `LeaveFamilyGroupUseCase`: clear `User.familyId`/`familyRole` via `updateFamilyMembership(null, null)`; update `FamilyGroup.memberIds` (arrayRemove) jika rules mengizinkan; putuskan apakah hapus local family wallet row atau biarkan orphan read-only.
2. Confirm dialog di Settings Manage / menu.
3. **Hati-hati** rules + owner leave — jika rumit, **skip** Leave di Phase 7 dan catat di assessment remaining.

---

## 14. Acceptance Criteria

### Functional — 7a

- [ ] Memilih currency memanggil UseCase dan mem-persist ke Firestore + DataStore
- [ ] `User.currency` ter-reflect di dropdown setelah emit Flow
- [ ] Cold start + `syncUserProfile` tidak mengembalikan currency ke default kecuali remote memang IDR
- [ ] `upsertUserProfile` / login path **tidak** men-reset currency ke IDR untuk user existing
- [ ] Connected wallets dari Room; amounts `CurrencyFormat.formatIdr`; Personal/Shared chip benar
- [ ] Runtime Settings **tidak** menampilkan mock `Rp 12.450.000` / `KEU-992-KRT` bila user/family real tersedia
- [ ] Google Sheets tidak mengklaim “Real-time sync enabled” saat belum diimplementasi
- [ ] Sign out + create/join family tetap berfungsi
- [ ] `@Preview` Settings masih render dengan mock fixture

### Functional — 7b (jika dikerjakan)

- [ ] Leave family membersihkan membership lokal + remote sesuai desain
- [ ] Insights Family empty state setelah leave (tanpa crash)

### Verification commands

```bash
./gradlew :core:domain:compileDebugKotlin
./gradlew :core:data:compileDebugKotlin
./gradlew :features:settings:compileDevDebugKotlin
./gradlew assembleDevDebug

# Protected areas must stay clean
git diff --stat -- features/auth features/splashscreen build-plugin
git diff --stat -- features/dashboard features/transaction features/family
# Expected: kosong (kecuali hotfix blocker yang disetujui)
```

### Definition of Done

1. Semua AC 7a tercentang
2. Tidak ada perubahan di auth/splash/build-plugin
3. Tidak ada breaking change domain finansial
4. Manual QA tabel 7.6 lulus
5. PR description menyebut deferral Google Sheets + no-FX currency semantics

---

## 15. Catatan Arsitektur & Konvensi

### Feature rules

| Aturan | Phase 7 |
|--------|---------|
| Feature ↛ feature | Tidak import dashboard/family/transaction |
| Feature ↛ Room / Firestore | Hanya UseCase + `UserRepository` |
| Screen stateless | Ya — state + callbacks |
| Money `Long` + format IDR | Ya untuk wallet amounts |
| `CommonDispatcher` | Ya |
| `CancellationException` | `throw e` sebelum catch generik |
| `KeuTrackTheme` di Preview | Ya |

### Jangan overload auth upsert

```
Auth upsert  = identity (name, email, photo)
Membership   = familyId, familyRole          (Phase 6b)
Preferences  = currency                      (Phase 7)  ← path terpisah
```

Mencampur preference ke `upsertUserProfile` akan membuat setiap sync login menimpa preference — **dilarang**.

### Konsistensi dengan Dashboard

Connected wallets Settings dan kartu wallet Dashboard harus membaca **sumber yang sama** (`GetWalletSummaryUseCase`). Jika angka beda → bug mapper/filter, bukan “Settings punya API sendiri”.

---

## 16. Dependency Graph

```
Phase 6b/6c (family membership + shared wallet)
        │
        ▼
Phase 7a
  ┌─────────────────────────────────────┐
  │ UpdateCurrencyUseCase               │
  │   └─ UserRepository.updateCurrency  │
  │        └─ Firestore + DataStore     │
  │ GetWalletSummaryUseCase (existing)  │
  │ SettingsUIState + Mapper + VM       │
  └─────────────────────────────────────┘
        │
        ▼
Phase 7b (opsional Leave)
        │
        ▼
Phase 9 (tests) / future Sheets & FX
```

Tidak ada dependency baru Gradle. Tidak memblokir Phase 9.

---

## 17. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| `syncUserProfile` men-clobber currency | Preference hilang | Dedicated update; upsert existing tanpa currency; QA cold start |
| User pilih USD mengira saldo diubah | Confusion produk | Copy subtitle jujur; jangan ubah balances |
| Offline currency write partial | Drift local vs remote | Persist lokal hanya setelah Firestore sukses (MVP) |
| Leave family merusak 6c wallet | Data shared rusak | Skip Leave jika rules/owner edge case belum jelas |
| Regress create/join saat rewrite VM | Membership putus | Pertahankan method existing; tes manual family flow |
| Menyentuh `User.kt` / proto | Break auth pipeline | Additive API only; proto freeze |
| Scope creep Sheets API | Molor | Explicit defer + toast |

---

## 18. Urutan Pengerjaan yang Disarankan

```
Step 1: Domain UserRepository.updateCurrency + UpdateCurrencyUseCase
Step 2: FirestoreNetworkDataSource.updateCurrency + UserRepositoryImpl
Step 3: Compile domain + data; manual unit-of-work di debugger/log bila perlu
Step 4: SettingsUIState + SettingsUiMapper
Step 5: SettingsViewModel combine (user + family + wallets) — preserve membership/signOut
Step 6: Wire Routing callbacks (currency, sheets coming-soon, wallets bind)
Step 7: QA tabel 7.6 (terutama cold start currency + wallet parity Dashboard)
Step 8: (Opsional) Leave family 7b
Step 9: assembleDevDebug + protected diff check
Step 10: Update Project_Assessment_Current.md status Phase 7
```

---

## 19. Relasi ke Phase Lain

| Phase | Relasi |
|-------|--------|
| **1–2** | `User.currency`, wallets Room — dikonsumsi |
| **3** | `CurrencyFormat` untuk label saldo Settings |
| **4** | Pola VM/mapper; parity saldo Dashboard |
| **5** | Tidak mengubah Transaction |
| **6 / 6b** | Family Network sudah wired — Phase 7 jangan rebuild |
| **6c** | Canonical family wallet muncul di Connected Wallets |
| **8** | Nav Settings sudah ada — biasanya no-op |
| **9** | Unit test `UpdateCurrencyUseCase` + repo — belakangan |

---

## Estimasi Effort

| Bucket | Porsi |
|--------|-------|
| Domain + data currency write | ~25% |
| SettingsUIState + mapper + VM combine | ~35% |
| Routing bind + Sheets/empty UX | ~15% |
| QA cold start / parity / membership regression | ~20% |
| 7b Leave (opsional) | +15–25% di atas estimasi |

---

## Ringkasan File Policy

| Kategori | Policy |
|----------|--------|
| `:features:settings` presentation | ✅ Primary |
| `UserRepository` + Firestore user DS **additive currency** | ✅ |
| `UpdateCurrencyUseCase` | ✅ BARU |
| `GetWalletSummaryUseCase` | ✅ Consume only |
| Proto / auth / splash / build-plugin | ❌ |
| Dashboard / Transaction / Family feature code | ❌ |
| Breaking `User` / auth upsert behavior | ❌ |
| Google Sheets backend | ❌ Deferred |
| `CurrencyFormat` global breaking change | ❌ |

---

## Quick Reference — Do / Don't

| Do | Don't |
|----|-------|
| Tiru `updateFamilyMembership` untuk currency | Menulis currency lewat `upsertUserProfile` existing |
| `combine` Flows seperti Dashboard | Tetap mengandalkan `DefaultSettingsMockContent` di runtime finansial |
| Format wallet dengan `CurrencyFormat.formatIdr` | Hardcode `"Rp 12.450.000"` |
| Toast jujur untuk Sheets | Toggle yang menyimpan state sync palsu |
| Additive API saja di `UserRepository` | Rename field `User.currency` atau edit proto numbers |
| Preserve create/join/signOut | Refactor auth atau 6c sync “sekalian” |

---

*Dokumen ini adalah referensi implementasi untuk Phase 7 KeuTrack. Kerjakan 7a sampai currency preference persist + connected wallets real + Sheets deferred jujur. Setelah itu lanjut Phase 9 (tests/lint) atau product extras (Sheets API, FX, Leave family, QR).*
