# Phase 3 — core:designsystem (Atelier Completion & Shared Money UI)

> **Modul target:** `:core:designsystem` (+ migrasi UI mekanis di feature yang mengekstrak komponen)
> **Estimasi:** ~1–1.5 hari
> **Prasyarat:** Phase 0 (selesai). **Boleh paralel dengan Phase 1–2** (tidak bergantung Room/domain finansial)
> **Status baseline:** ~90% selesai (assessment) — theme dual-palette + 7 komponen inti sudah ada
> **Hasil akhir:** Shared money-UI primitives siap dipakai Phase 4–7; Previews light/dark untuk komponen publik; katalog komponen terdokumentasi; feature tidak lagi menduplikasi keypad/format IDR/chip/progress

---

## Daftar Isi

1. [Konteks & Tujuan](#1-konteks--tujuan)
2. [Inventory — Apa yang Sudah Ada](#2-inventory--apa-yang-sudah-ada)
3. [Scope — Apa yang Dikerjakan](#3-scope--apa-yang-dikerjakan)
4. [Scope — Apa yang TIDAK Dikerjakan](#4-scope--apa-yang-tidak-dikerjakan)
5. [File Referensi (Read-Only)](#5-file-referensi-read-only)
6. [File yang TIDAK BOLEH Diubah](#6-file-yang-tidak-boleh-diubah)
7. [File yang BOLEH Diubah](#7-file-yang-boleh-diubah)
8. [Struktur File yang Akan Dibuat / Diubah](#8-struktur-file-yang-akan-dibuat--diubah)
9. [Task Breakdown Detail](#9-task-breakdown-detail)
10. [Acceptance Criteria](#10-acceptance-criteria)
11. [Catatan Arsitektur & Konvensi](#11-catatan-arsitektur--konvensi)
12. [Dependency Graph](#12-dependency-graph)
13. [Risiko & Mitigasi](#13-risiko--mitigasi)
14. [Urutan Pengerjaan yang Disarankan](#14-urutan-pengerjaan-yang-disarankan)

---

## 1. Konteks & Tujuan

Menurut `docs/dev/Project_Assessment.md`, Phase 3 (Design system) sudah **~90%**:

| Area | Status |
|------|--------|
| Dual theme Financial (light) / Midnight (dark) | ✅ `KeuTrackTheme` |
| Color / typography / shape / effect tokens | ✅ |
| Button, Card, TextField, TopBar, BottomNav, ModalBottomSheet, ProfileImage | ✅ |
| Digunakan di auth, splash, dashboard, family, settings, app shell | ✅ |
| Shared money UI (format IDR, keypad, chips, progress, FAB) | ❌ Masih **feature-local** / duplikat |
| Preview light+dark untuk semua komponen DS publik | ❌ Sebagian besar commented / belum ada |
| Katalog desain (`DESIGN_SYSTEM_ATELIER.md`) | ❌ File `plans/` tidak ada di repo |

Tanpa menyelesaikan sisa ~10%, Phase 4 (Dashboard real data) dan Phase 5 (Transaction screen) akan **menyalin lagi** `AmountKeypad`, `formatAmountIdr`, `CategoryChip`, dll. dari dashboard — melanggar prinsip satu sumber kebenaran UI.

**Tujuan Phase 3:**
- Audit & dokumentasikan komponen yang sudah stabil (jangan rebuild)
- Ekstrak pola UI finansial yang berulang ke `:core:designsystem`
- Migrasi pemanggil feature ke komponen baru (**hanya swap UI**, tanpa ubah ViewModel/data)
- Lengkapi `@Preview` light + dark untuk komponen publik DS
- Tulis katalog ringkas di `docs/dev/` agar Phase 4–7 punya referensi

**Bukan tujuan Phase 3:**
- Wiring data nyata (itu Phase 4+)
- Redesign visual Atelier dari nol
- Menambah dependency Gradle baru kecuali benar-benar perlu (kemungkinan tidak perlu)

---

## 2. Inventory — Apa yang Sudah Ada

### Theme & tokens (`core/designsystem/.../theme/`)

| File | API utama |
|------|-----------|
| `Theme.kt` | `KeuTrackTheme(darkTheme, content)`, object accessor tokens |
| `Colors.kt` | Raw palette + `KeuTrackPrimaryColors`, `Neutral`, `Text`, `Content`, `Success`, `Warning`, `Danger`, `Semantic` |
| `Typographies.kt` | `PTSansFamily`, `KeuTrackTypography` (48→10) |
| `Effects.kt` | `KeuTrackShapeTokens` (`radiusMd/Lg/Xl`, `buttonHeight`, `progressThickness`), `KeuTrackEffectTokens` |

### Komponen existing

| File | Composable |
|------|------------|
| `component/KeuTrackButton.kt` | `KeuTrackButton` (+ loading) |
| `component/KeuTrackCard.kt` | `KeuTrackCard` |
| `component/KeuTrackTextField.kt` | `KeuTrackTextField` |
| `component/KeuTrackTopBar.kt` | `KeuTrackTopBar` |
| `component/KeuTrackBottomNav.kt` | `KeuTrackBottomNav` |
| `component/KeuTrackModalBottomSheet.kt` | `KeuTrackModalBottomSheet` |
| `component/ProfileImage.kt` | `ProfileImage` |

### Models

| File | Isi |
|------|-----|
| `model/KeuTrackButtonStyle.kt` | Primary / Secondary / Tertiary |
| `model/KeuTrackBottomNavItem.kt` | Nav item |
| `model/KeuTrackTopBarTitleAlignment.kt` | Start / Center / End |

### Fonts

`res/font/ptsans_{regular,italic,bold,bolditalic}.ttf`

### Pemakaian per feature (baseline)

| Feature | Komponen DS yang dipakai |
|---------|--------------------------|
| `features/auth` | Theme, Button, Card, TextField |
| `features/splashscreen` | Theme |
| `features/dashboard` | Theme, ModalBottomSheet, TopBar, ProfileImage, Card, Button |
| `features/family` | Theme, TopBar, Card, Button |
| `features/settings` | Theme, TopBar, Card, Button, ProfileImage |
| `features/transaction` | Theme only (placeholder) |
| `app` | Theme root, BottomNav |

---

## 3. Scope — Apa yang Dikerjakan

### A. Shared money UI (prioritas utama — ~70% sisa effort)

| # | Komponen / util baru | Sumber ekstraksi | Dipakai nanti di |
|---|----------------------|------------------|------------------|
| 1 | `format/CurrencyFormat.kt` | `NewEntryBottomSheetContent.formatAmountIdr` | Dashboard, Transaction, Family, Settings |
| 2 | `KeuTrackCurrencyText` | Amount display + signed `+/-` di transaction row | Dashboard list, Family history |
| 3 | `KeuTrackAmountKeypad` | `AmountKeypad` / `KeypadCell` | Dashboard sheet → Phase 5 screen |
| 4 | `KeuTrackSegmentedControl` | `ExpenseIncomeToggle` | New entry |
| 5 | `KeuTrackCategoryChip` | `CategoryChip` | New entry, filters |
| 6 | `KeuTrackStatusChip` | `SettingsStatusChip` (+ pola chip wallet di txn row) | Settings, Dashboard |
| 7 | `KeuTrackProgressBar` | Progress track di `FamilyBudgetRow` (`progressThickness` sudah ada) | Family, Budget UI |
| 8 | `KeuTrackFab` | Raw `FloatingActionButton` di Dashboard & Family | Dashboard, Family, Transaction |

### B. Nice-to-have (kerjakan jika waktu cukup; boleh ditunda Phase 6)

| # | Komponen | Sumber |
|---|----------|--------|
| 9 | `KeuTrackSelectChip` | `WalletDateChip` di new-entry sheet |
| 10 | `KeuTrackDonutChart` | `FamilyDonutChart` |
| 11 | `KeuTrackGradientCard` | `PersonalWalletSurface` |
| 12 | `KeuTrackEmptyState` | Stub API ringan untuk Phase 4 empty Room states |

### C. Migrasi pemanggil (mekanis)

| # | File feature | Perubahan yang diizinkan |
|---|--------------|--------------------------|
| 13 | `NewEntryBottomSheetContent.kt` | Ganti private composable → import DS; hapus duplikat lokal |
| 14 | `FamilySharedBudgetsCard.kt` | Ganti progress bar lokal → `KeuTrackProgressBar` |
| 15 | `SettingsStatusChip.kt` | Thin-wrap atau deprecate → `KeuTrackStatusChip` |
| 16 | `DashboardScreen.kt` / `FamilyScreen.kt` | FAB → `KeuTrackFab` |

### D. Previews & polish (~15%)

| # | Item |
|---|------|
| 17 | Uncomment / tulis `@Preview` light + dark untuk **setiap** composable publik baru + existing yang belum punya (mulai `KeuTrackTopBar`, Button, Card, TextField, komponen baru) |
| 18 | Pastikan Preview memakai `KeuTrackTheme(darkTheme = …)` — **bukan** bare `MaterialTheme` |

### E. Dokumentasi (~15%)

| # | Item |
|---|------|
| 19 | Buat `docs/dev/DESIGN_SYSTEM_ATELIER.md` — token map, daftar komponen, aturan pakai, link Figma (dari KDoc `Colors.kt`) |
| 20 | Update singkat pointer di assessment/skill **hanya jika** diminta terpisah — **bukan wajib** di Phase 3 code work |

---

## 4. Scope — Apa yang TIDAK Dikerjakan

| Item | Alasan | Phase |
|------|--------|-------|
| Rebuild / ganti palette Financial–Midnight | Sudah stabil & dipakai auth | — |
| Wiring UseCase / Room / sync ke UI | Data layer | Phase 2 lalu 4–7 |
| Hapus `RouteRepository` SMPOB / mock dashboard data | Feature data cleanup | Phase 4 |
| Implement full `NewEntryScreen` di `:features:transaction` | Transaction flow | Phase 5 |
| Family invite / QR | Family product | Phase 6 |
| Persist currency / family ID di Settings | Settings | Phase 7 |
| Ubah navigation graph / orphan `TransactionRoute` | Nav | Phase 8 / 5 |
| Unit test screenshot / Paparazzi penuh | Testing | Phase 9 |
| Ubah domain / data / auth business logic | Di luar design system | — |
| Migrasi ke Material 3 penuh | Project convention = Material 2 + sheet M3 terbatas | — |

---

## 5. File Referensi (Read-Only)

Baca sebagai sumber kebenaran. Jangan diubah kecuali ada di Section 7.

### Dokumen & fase

| File | Gunakan untuk |
|------|---------------|
| `docs/dev/Project_Assessment.md` | Status ~90%, daftar komponen yang sudah disebut |
| `docs/dev/phases/PHASE_1_DOMAIN_ENTITIES_AND_USE_CASES.md` | Konteks `amount: Long` (IDR) — formatter harus terima `Long`, bukan `Double` |
| `docs/dev/phases/PHASE_2_ROOM_AND_REPOSITORY_IMPLEMENTATION.md` | Phase 3 **tidak** menyentuh Room; paham kapan empty-state dibutuhkan (Phase 4+) |
| `.cursor/rules/keutrack-architecture.mdc` | Semua screen pakai `KeuTrackTheme`; Material 2 |
| `.cursor/rules/keutrack-feature-module.mdc` | Prefix `KeuTrack*`, Preview light+dark, Screen stateless |
| `.cursor/skills/keutrack-dev/SKILL.md` | Protected files list (auth, splash, build-plugin) |

### Design system existing (pola yang harus ditiru)

| File | Pelajari |
|------|----------|
| `core/designsystem/.../theme/Theme.kt` | Cara expose token via `KeuTrackTheme.*` |
| `core/designsystem/.../theme/Colors.kt` | Naming warna + Figma/atelier notes |
| `core/designsystem/.../theme/Effects.kt` | `progressThickness` untuk progress bar |
| `core/designsystem/.../component/KeuTrackButton.kt` | Pola komponen + style enum + loading |
| `core/designsystem/.../component/KeuTrackCard.kt` | Ghost border / surface tokens |
| `core/designsystem/.../component/KeuTrackModalBottomSheet.kt` | Batas pemakaian Material 3 |
| `core/designsystem/build.gradle.kts` | Deps Compose Material + Coil (sudah cukup) |

### Sumber ekstraksi (baca dulu, lalu pindahkan API)

| File | Ekstrak apa |
|------|-------------|
| `features/dashboard/.../components/NewEntryBottomSheetContent.kt` | `formatAmountIdr`, `ExpenseIncomeToggle`, `CategoryChip`, `AmountKeypad` / `KeypadCell`, `WalletDateChip` |
| `features/dashboard/.../components/WalletCards.kt` | Pola visual `PersonalWalletSurface` (opsional `KeuTrackGradientCard`) |
| `features/dashboard/.../components/` transaction row (jika ada signed amount) | Warna income/expense untuk `KeuTrackCurrencyText` |
| `features/family/.../components/FamilySharedBudgetsCard.kt` | `FamilyBudgetRow` progress |
| `features/family/.../components/FamilyBreakdownCard.kt` | Donut (nice-to-have) |
| `features/settings/.../components/SettingsStatusChip.kt` | Status chip API |
| `features/dashboard/.../DashboardScreen.kt` | FAB usage |
| `features/family/.../FamilyScreen.kt` | FAB usage |

### Konsumen yang harus tetap visual-stabil (jangan “redesign”)

| File | Catatan |
|------|---------|
| `features/auth/**` screens | Hanya boleh benefit tidak langsung dari DS; **jangan** ubah layout/copy auth di Phase 3 kecuali bug visual kritis |
| `app/.../HomeShell.kt` | Bottom nav sudah pakai DS — jangan diutak-atik |

---

## 6. File yang TIDAK BOLEH Diubah

### Domain, data, auth pipeline

| File / Area | Alasan |
|-------------|--------|
| Semua `core/domain/**` | Kontrak Phase 1 — freeze |
| Semua `core/data/**` | Phase 2 scope |
| `core/datastore/**` | Session user |
| `core/network/**` | Networking |
| `features/auth/**` | Auth complete — **jangan sentuh** di Phase 3 |
| `features/splashscreen/**` | Splash complete |
| `UserRepository*` / `FirestoreNetworkDataSource` / Auth mappers | Production auth |

### Infra

| File / Area | Alasan |
|-------------|--------|
| `build-plugin/**` | Convention stable |
| `settings.gradle.kts`, root `build.gradle.kts` | Tidak perlu module baru |
| `gradle/libs.versions.toml` | Phase 3 seharusnya **tidak** butuh library baru |
| `gradle.properties`, `local.properties` | Config / secrets |

### Theme tokens — ubah hanya jika bug

| File | Policy |
|------|--------|
| `Colors.kt` raw palette hex | ❌ Jangan ganti hex “sekalian rapi” |
| Token data class structure di `Colors.kt` / `Effects.kt` | ❌ Jangan rename property yang sudah dipakai feature |
| `Theme.kt` CompositionLocal wiring | ❌ Jangan refactor besar |

Jika perlu warna baru untuk chip/progress, **tambah** semantic helper di komponen (pakai `successColors` / `dangerColors` / `semanticColors` existing) — jangan menambah 20 warna ad-hoc ke palette tanpa alasan.

### Feature logic (bukan UI swap)

| Jangan ubah | Alasan |
|-------------|--------|
| `*ViewModel.kt` di dashboard/family/settings/transaction | Data wiring = Phase 4–7 |
| Navigation routes / `KeuTrackNavHost` | Phase 5/8 |
| Mock data models (`DashboardMockUi`, dll.) isi field | Boleh tetap; hanya composable visual yang diganti |
| Hapus `RouteRepository` | Phase 4 |

---

## 7. File yang BOLEH Diubah

| File / Area | Jenis perubahan |
|-------------|-----------------|
| `core/designsystem/src/**` file **baru** | Formatter + komponen baru + Preview |
| `core/designsystem/.../component/*.kt` existing | Additive: Preview, bugfix visual kecil, **jangan** breaking API |
| `features/dashboard/.../NewEntryBottomSheetContent.kt` | Ganti implementasi lokal → call DS |
| `features/dashboard/.../DashboardScreen.kt` | FAB → `KeuTrackFab` (signature callback sama) |
| `features/dashboard/.../components/*` terkait chip/amount display | Swap ke DS |
| `features/family/.../FamilySharedBudgetsCard.kt` | Progress → `KeuTrackProgressBar` |
| `features/family/.../FamilyScreen.kt` | FAB → `KeuTrackFab` |
| `features/settings/.../SettingsStatusChip.kt` | Delegasi ke `KeuTrackStatusChip` atau hapus jika diganti import langsung |
| `docs/dev/DESIGN_SYSTEM_ATELIER.md` | **Buat baru** (katalog) |
| `core/designsystem/build.gradle.kts` | Hanya jika dependency benar-benar kurang (default: **tidak diubah**) |

**Aturan migrasi feature:**
- Diff harus didominasi delete private composable + import baru
- Jangan ubah string copy, navigation callback, atau ViewModel method di commit yang sama “sekalian”
- Visual harus setara (screenshot/Preview before-after mental check)

---

## 8. Struktur File yang Akan Dibuat / Diubah

```
core/designsystem/src/main/kotlin/com/mascill/keutrack/core/designsystem/
├── theme/
│   ├── Theme.kt              ← EXISTING (jangan refactor)
│   ├── Colors.kt             ← EXISTING (jangan ganti hex)
│   ├── Typographies.kt       ← EXISTING
│   └── Effects.kt            ← EXISTING (progressThickness sudah siap)
│
├── format/
│   └── CurrencyFormat.kt     ← BARU
│
├── component/
│   ├── KeuTrackButton.kt             ← EXISTING (+ Preview jika belum)
│   ├── KeuTrackCard.kt               ← EXISTING (+ Preview)
│   ├── KeuTrackTextField.kt          ← EXISTING (+ Preview)
│   ├── KeuTrackTopBar.kt             ← EXISTING (uncomment Preview)
│   ├── KeuTrackBottomNav.kt          ← EXISTING (+ Preview)
│   ├── KeuTrackModalBottomSheet.kt   ← EXISTING
│   ├── ProfileImage.kt               ← EXISTING (+ Preview)
│   ├── KeuTrackCurrencyText.kt       ← BARU
│   ├── KeuTrackAmountKeypad.kt       ← BARU
│   ├── KeuTrackSegmentedControl.kt   ← BARU
│   ├── KeuTrackCategoryChip.kt       ← BARU
│   ├── KeuTrackStatusChip.kt         ← BARU
│   ├── KeuTrackProgressBar.kt        ← BARU
│   ├── KeuTrackFab.kt                ← BARU
│   ├── KeuTrackSelectChip.kt         ← OPSIONAL
│   ├── KeuTrackDonutChart.kt         ← OPSIONAL
│   ├── KeuTrackGradientCard.kt       ← OPSIONAL
│   └── KeuTrackEmptyState.kt         ← OPSIONAL
│
└── model/
    ├── KeuTrackButtonStyle.kt        ← EXISTING
    ├── KeuTrackBottomNavItem.kt      ← EXISTING
    ├── KeuTrackTopBarTitleAlignment.kt ← EXISTING
    └── (opsional) KeuTrackChipStyle.kt / SegmentedOption.kt jika perlu

docs/dev/
└── DESIGN_SYSTEM_ATELIER.md          ← BARU (katalog)
```

---

## 9. Task Breakdown Detail

### Task 0: Audit cepat (30–60 menit)

1. Buka Preview di Android Studio untuk Dashboard + Family light/dark — catat inkonsistensi minor (jangan redesign).
2. List private composable di `NewEntryBottomSheetContent.kt` yang akan dipindah.
3. Pastikan `./gradlew :core:designsystem:compileDebugKotlin` hijau sebelum mulai.

---

### Task 1: `CurrencyFormat`

**File:** `core/designsystem/.../format/CurrencyFormat.kt`

```kotlin
object CurrencyFormat {
    /** Format Long rupiah → "Rp 12.500" (locale id-ID, tanpa desimal). */
    fun formatIdr(amount: Long): String { ... }

    /** Opsional: signed untuk expense/income UI. */
    fun formatIdrSigned(amount: Long, isExpense: Boolean): String { ... }
}
```

**Keputusan desain:**
- Input `Long` — selaras Phase 1 (`amount: Long`)
- Jangan pakai `Double` / `BigDecimal`
- Locale fixed `id-ID` untuk MVP (Settings currency persistence = Phase 7; formatter boleh terima `currencyCode` nanti dengan default `"IDR"`)
- Pindahkan logic dari `formatAmountIdr` di dashboard; hapus fungsi lokal setelah migrasi

**Referensi:** Phase 1 financial convention + `NewEntryBottomSheetContent.kt`

---

### Task 2: `KeuTrackCurrencyText`

```kotlin
@Composable
fun KeuTrackCurrencyText(
    amount: Long,
    modifier: Modifier = Modifier,
    signed: Boolean = false,
    isExpense: Boolean? = null, // jika signed: warnai danger/success
    style: TextStyle = KeuTrackTheme.typography.bodyBold16,
    color: Color = Color.Unspecified,
)
```

- Default warna: `textColors.title` / body
- Jika `signed && isExpense == true` → `dangerColors` / `semanticColors.error`
- Jika income → `successColors` / `semanticColors.success`
- Wajib `@Preview` light + dark (positif, negatif, nol)

---

### Task 3: `KeuTrackAmountKeypad`

Ekstrak dari `AmountKeypad` / `KeypadCell`:

```kotlin
@Composable
fun KeuTrackAmountKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onQuickAmount: ((Long) -> Unit)? = null, // jika UI existing punya shortcut
    modifier: Modifier = Modifier,
)
```

**Aturan:**
- Keypad **stateless** — tidak menyimpan amount; parent/ViewModel yang pegang state
- Visual (spacing, typography, ripple) samakan dengan existing sheet agar migrasi zero-surprise
- Jangan hardcode warna hex — pakai `KeuTrackTheme.*`

---

### Task 4: `KeuTrackSegmentedControl`

Ganti `ExpenseIncomeToggle`:

```kotlin
@Composable
fun <T> KeuTrackSegmentedControl(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
)
```

Atau overload khusus 2-tab jika generic berlebihan untuk MVP:

```kotlin
@Composable
fun KeuTrackSegmentedControl(
    leftLabel: String,
    rightLabel: String,
    leftSelected: Boolean,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Pilih **satu** API dan konsisten. Prefer sederhana (2-tab) jika hanya new-entry yang memakai di Phase 3–5.

---

### Task 5: `KeuTrackCategoryChip`

```kotlin
@Composable
fun KeuTrackCategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    // atau icon: String? jika existing pakai nama Material icon string
    containerColor: Color = Color.Unspecified,
)
```

Samakan perilaku selected/unselected dengan `CategoryChip` dashboard. Icon resolution (string → ImageVector) boleh tetap di feature jika mapping kategori domain belum ada di DS — DS hanya render.

---

### Task 6: `KeuTrackStatusChip`

Angkat dari `SettingsStatusChip`:

```kotlin
@Composable
fun KeuTrackStatusChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: KeuTrackStatusTone = KeuTrackStatusTone.Neutral, // Success / Warning / Danger / Neutral
)
```

Settings menjadi pemanggil; hapus duplikasi styling.

---

### Task 7: `KeuTrackProgressBar`

```kotlin
@Composable
fun KeuTrackProgressBar(
    progress: Float, // 0f..1f+ (clamp visual di 1f, boleh over-budget warna danger)
    modifier: Modifier = Modifier,
    isOverLimit: Boolean = false,
)
```

- Height default: `KeuTrackTheme.shapeTokens.progressThickness`
- Track: surface container; fill: primary gradient atau success; over → danger
- Sumber visual: `FamilyBudgetRow` di `FamilySharedBudgetsCard.kt`

---

### Task 8: `KeuTrackFab`

```kotlin
@Composable
fun KeuTrackFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    contentDescription: String?,
)
```

- Warna/elevation selaras primary Atelier (bukan default Material ungu)
- Ganti FAB di `DashboardScreen` & `FamilyScreen` tanpa mengubah callback navigation/sheet

---

### Task 9: Migrasi feature (setelah komponen hijau)

Urutan migrasi aman:

```
1. CurrencyFormat + ganti formatAmountIdr di NewEntryBottomSheetContent
2. AmountKeypad + SegmentedControl + CategoryChip di sheet yang sama
3. ProgressBar di FamilySharedBudgetsCard
4. StatusChip di Settings
5. Fab di Dashboard + Family
6. CurrencyText di transaction rows (jika ada signed amount)
```

Setelah tiap langkah: Preview / compile feature terkait.

```bash
./gradlew :core:designsystem:compileDebugKotlin
./gradlew :features:dashboard:compileDevDebugKotlin
./gradlew :features:family:compileDevDebugKotlin
./gradlew :features:settings:compileDevDebugKotlin
```

---

### Task 10: Previews (design system)

Untuk setiap composable publik di `component/` + `CurrencyFormat` (via CurrencyText):

```kotlin
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun KeuTrackXPreview() {
    KeuTrackTheme {
        // representative content
    }
}
```

Prioritas uncomment/fix:
1. Komponen **baru** (wajib)
2. `KeuTrackTopBar` (Preview saat ini commented)
3. Button, Card, TextField, BottomNav, ProfileImage

---

### Task 11: `docs/dev/DESIGN_SYSTEM_ATELIER.md`

Isi minimal:

1. **Brand / themes** — Financial light vs Midnight dark; kapan `KeuTrackTheme` wajib
2. **Token map** — tabel ringkas primary/semantic/success/danger + shape tokens
3. **Component catalog** — nama, kapan dipakai, contoh 3–5 baris
4. **Money UI rules** — `Long` + `CurrencyFormat`; jangan format di ViewModel dengan locale ad-hoc
5. **Do / Don't** — jangan bare `MaterialTheme` di Preview; jangan hex hardcoded di feature
6. **Figma** — salin link dari KDoc `Colors.kt` jika ada

Dokumen ini menggantikan `plans/DESIGN_SYSTEM_ATELIER.md` yang disebut assessment tetapi **tidak ada** di workspace.

---

### Task 12 (Opsional): Nice-to-have

Kerjakan hanya jika Task 1–11 selesai dan masih ada waktu:

| Komponen | Catatan |
|----------|---------|
| `KeuTrackSelectChip` | Wallet/date chip di sheet |
| `KeuTrackDonutChart` | Generic `List<Segment(value, color)>` — jangan hardcode Family copy |
| `KeuTrackGradientCard` | Hero personal wallet |
| `KeuTrackEmptyState` | icon + title + body + optional CTA — berguna Phase 4 |

---

## 10. Acceptance Criteria

### Harus Terpenuhi

- [ ] **Tidak ada rebuild theme** — palette / `KeuTrackTheme` wiring tetap
- [ ] **`CurrencyFormat.formatIdr(Long)`** tersedia & dipakai new-entry sheet (tidak ada `formatAmountIdr` lokal)
- [ ] **`KeuTrackAmountKeypad`**, **`KeuTrackSegmentedControl`**, **`KeuTrackCategoryChip`** dipakai dari dashboard sheet
- [ ] **`KeuTrackProgressBar`** dipakai Family shared budgets
- [ ] **`KeuTrackStatusChip`** dipakai Settings (atau Settings chip mendelegasi ke DS)
- [ ] **`KeuTrackFab`** dipakai Dashboard + Family
- [ ] **`KeuTrackCurrencyText`** tersedia (dipakai minimal di 1 tempat nyata atau Preview wajib)
- [ ] Setiap komponen **baru** punya `@Preview` light + dark dengan `KeuTrackTheme`
- [ ] `docs/dev/DESIGN_SYSTEM_ATELIER.md` ada dan list semua komponen publik
- [ ] **Auth / splash / domain / data** tidak berubah
- [ ] **ViewModel & navigation** tidak berubah (kecuali tidak relevan)
- [ ] Build sukses:

```bash
./gradlew :core:designsystem:compileDebugKotlin
./gradlew assembleDevDebug
```

### Verification Steps

```bash
# 1. Design system module
./gradlew :core:designsystem:compileDebugKotlin

# 2. Features yang dimigrasi
./gradlew :features:dashboard:compileDevDebugKotlin
./gradlew :features:family:compileDevDebugKotlin
./gradlew :features:settings:compileDevDebugKotlin

# 3. Full app
./gradlew assembleDevDebug

# 4. Pastikan auth & domain tidak tersentuh
git diff --stat -- features/auth features/splashscreen core/domain core/data
# Expected: kosong / tidak ada diff
```

### Definition of Ready untuk Phase 4–5

Phase 4 (Dashboard data) / Phase 5 (Transaction UI) boleh memakai DS jika:
1. New-entry building blocks (keypad, segment, category chip, currency format) sudah di DS
2. Feature transaction **tidak perlu copy-paste** dari dashboard sheet
3. Katalog Atelier bisa dirujuk untuk nama komponen

---

## 11. Catatan Arsitektur & Konvensi

### Aturan module

| Aturan | Detail |
|--------|--------|
| `:core:designsystem` → `:core:common` saja (atau no domain) | **Jangan** depend `:core:domain` / `:core:data` |
| Feature boleh depend designsystem | Sudah via `keutrack.feature` |
| Komponen prefix `KeuTrack` | Kecuali `ProfileImage` existing (jangan rename di Phase 3) |
| Material 2 default | Sheet boleh Material 3 seperti existing `KeuTrackModalBottomSheet` |

### API design

| Prefer | Hindari |
|--------|---------|
| Stateless composable + callback | State amount di dalam keypad |
| Token `KeuTrackTheme.*` | `Color(0xFF...)` di feature setelah migrasi |
| `Long` money | `Double` / `String` numerik untuk kalkulasi |
| Overload kecil yang jelas | Generic abstraksi berlebihan untuk 1 pemakai |

### Hubungan dengan Phase lain

```
Phase 3 (DS polish) ──paralel──▶ Phase 1 / 2 (domain / Room)
         │
         ▼
Phase 4 Dashboard real data  ── butuh CurrencyText, Card, Progress (opsional)
Phase 5 Transaction screen   ── butuh Keypad, Segment, CategoryChip, CurrencyFormat
Phase 6 Family               ── butuh ProgressBar, (opsional Donut)
Phase 7 Settings             ── butuh StatusChip, CurrencyText
Phase 9 Polish               ── Preview/test lanjutan
```

---

## 12. Dependency Graph

```
:core:designsystem
├── theme/          (stable — Phase 3 jangan rebuild)
├── format/         ← BARU (CurrencyFormat)
├── component/      ← BARU money UI + existing
└── model/          ← existing (+ opsional tone enums)

        ▲ digunakan oleh
        │
features/dashboard ── migrasi sheet + FAB
features/family    ── migrasi progress + FAB
features/settings  ── migrasi status chip
features/transaction ── (Phase 5) konsumsi DS; Phase 3 tidak wajib ubah placeholder
features/auth      ── TIDAK DIUBAH
app                ── BottomNav tetap; jangan ubah nav graph
```

---

## 13. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| Ekstraksi mengubah layout 1–2 dp | Regresi visual | Samakan Modifier/padding dari sumber; bandingkan Preview before/after |
| Scope creep “sekalian rapihin dashboard” | Phase 3 molor | Strict: hanya swap ke DS; tidak redesign section |
| Formatter beda dengan existing (`Rp` spacing) | User bingung | Copy exact logic `formatAmountIdr` dulu; polish format di Phase 9 jika perlu |
| Feature ViewModel ikut diubah | Campur Phase 4 | Review diff: nol perubahan logic |
| Depend domain dari designsystem untuk Category | Langgar arsitektur | Chip terima `label`/`icon` primitif, bukan domain `Category` |
| Mengubah hex Colors.kt | Auth/dashboard drift | Larang; pakai token semantic existing |
| Nice-to-have Donut memakan waktu | Delay Phase 4 | Tandai opsional; Family tetap Canvas lokal sampai Phase 6 |

---

## 14. Urutan Pengerjaan yang Disarankan

```
Step 1: Foundation util
  └── CurrencyFormat (+ unit sanity manual / Preview via CurrencyText)

Step 2: New-entry primitives
  └── SegmentedControl → CategoryChip → AmountKeypad → CurrencyText
  └── Migrasi NewEntryBottomSheetContent

Step 3: Cross-feature primitives
  └── ProgressBar → migrasi Family
  └── StatusChip → migrasi Settings
  └── Fab → migrasi Dashboard + Family

Step 4: Previews
  └── Semua komponen baru + uncomment TopBar/Button/Card

Step 5: Docs
  └── docs/dev/DESIGN_SYSTEM_ATELIER.md

Step 6: Optional
  └── SelectChip / Donut / GradientCard / EmptyState

Step 7: Verify
  └── ./gradlew :core:designsystem:compileDebugKotlin
  └── ./gradlew assembleDevDebug
  └── git diff: auth/domain/data harus bersih
```

---

## Ringkasan File Policy

| Kategori | Policy |
|----------|--------|
| `core/designsystem/**` komponen/util baru | ✅ Buat |
| `core/designsystem/**` theme tokens / hex | ❌ Jangan ubah (kecuali bug blocker) |
| Feature UI file untuk migrasi ekstraksi | ✅ Swap mekanis saja |
| `features/auth/**`, `features/splashscreen/**` | ❌ Jangan ubah |
| `core/domain/**`, `core/data/**` | ❌ Jangan ubah |
| `*ViewModel.kt`, navigation | ❌ Jangan ubah |
| `build-plugin/**`, `libs.versions.toml` | ❌ Jangan ubah (default) |
| `docs/dev/DESIGN_SYSTEM_ATELIER.md` | ✅ Buat baru |

---

## Estimasi Effort Breakdown

| Bucket | Porsi sisa ~10% |
|--------|-----------------|
| Formatters + keypad + chips + progress + FAB + migrasi | ~70% |
| Docs/katalog Atelier | ~15% |
| Preview light/dark pass | ~15% |

Setelah ini, Phase 3 bisa ditandai **Done**; lanjut Phase 4/5 mengonsumsi komponen — bukan menyalin lagi dari dashboard.

---

*Dokumen ini adalah referensi implementasi untuk Phase 3 KeuTrack. Phase 3 boleh dikerjakan paralel dengan Phase 2. Setelah selesai, Phase 4 (Dashboard real data) dan Phase 5 (Transaction flow) punya building block UI yang konsisten.*
