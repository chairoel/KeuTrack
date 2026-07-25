# KeuTrack Design System — Atelier

Katalog ringkas komponen & token di `:core:designsystem`.  
Figma: [KeuTrack Design System](https://www.figma.com/design/omc8qRxtrvUnEASl8J3Kvm/KeuTrack---Design-System?node-id=2001-13&p=f&m=dev)

---

## 1. Brand / Themes

| Theme | Mode | Kapan |
|-------|------|--------|
| **Financial** | Light | Default siang / `darkTheme = false` |
| **Midnight** | Dark | Sistem dark / `darkTheme = true` |

**Wajib:** semua screen & `@Preview` memakai `KeuTrackTheme { … }` — jangan bare `MaterialTheme`.

```kotlin
KeuTrackTheme(darkTheme = false) {
    // content
}
```

Akses token via object accessor:

```kotlin
KeuTrackTheme.semanticColors.primary
KeuTrackTheme.typography.bodyBold16
KeuTrackTheme.shapeTokens.radiusMd
```

---

## 2. Token Map

### Semantic (paling sering dipakai UI)

| Token | Light (Financial) | Dark (Midnight) | Pakai untuk |
|-------|-------------------|-----------------|-------------|
| `primary` | Financial primary | Midnight primary | CTA, FAB, accent |
| `secondary` | Financial secondary | Emerald | Success progress |
| `tertiary` | Financial tertiary | Carmine pink | Expense accent |
| `success` / `error` | Green / Carmine | Midnight success/error | Status, signed amounts |
| `surfaceContainer*` | Surface ladder | Surface ladder | Cards, chips, keypad |
| `onSurface` / `onSurfaceVariant` | Text hierarchy | Text hierarchy | Title / body |

### Shape

| Token | Default | Pakai |
|-------|---------|-------|
| `radiusMd` | 12.dp | Chip, keypad cell |
| `radiusLg` | 16.dp | Card, FAB, segment track |
| `radiusXl` | 24.dp | Button, progress track |
| `buttonHeight` | 56.dp | Primary actions |
| `progressThickness` | 8.dp | Budget bars |

### Typography

PT Sans family — scale `headingBold36` → `bodyBold10` via `KeuTrackTheme.typography`.

---

## 3. Component Catalog

### Core (existing)

| Composable | Kapan dipakai |
|------------|---------------|
| `KeuTrackButton` | Primary / Secondary / Tertiary actions (+ loading) |
| `KeuTrackCard` | Surface container dengan ghost border |
| `KeuTrackTextField` | Form input |
| `KeuTrackTopBar` | Judul screen + leading/trailing slots |
| `KeuTrackBottomNav` | App shell tab bar |
| `KeuTrackModalBottomSheet` | Sheet (Material 3 bounded) |
| `ProfileImage` | Avatar URL / placeholder |

### Money UI (Phase 3)

| Composable / util | Kapan dipakai |
|-------------------|---------------|
| `CurrencyFormat.formatIdr(Long)` | Format IDR `"Rp 12.500"` — input selalu `Long` |
| `CurrencyFormat.formatIdrSigned` | Prefix `+/-` untuk expense/income |
| `KeuTrackCurrencyText` | Tampilkan amount (opsional signed + warna danger/success) |
| `KeuTrackAmountKeypad` | Keypad digit / 000 / ⌫ (stateless) |
| `KeuTrackSegmentedControl` | Toggle 2-tab (Expense / Income) |
| `KeuTrackCategoryChip` | Pilih kategori (icon + accent) |
| `KeuTrackStatusChip` | Pill status (`Success` / `Warning` / `Danger` / `Neutral`) |
| `KeuTrackProgressBar` | Budget / progress track (`Primary` / `Success` / `Danger`) |
| `KeuTrackFab` | FAB Atelier (primary, radiusLg) |

### Contoh singkat

```kotlin
KeuTrackCurrencyText(amount = 12_500L, signed = true, isExpense = true)

KeuTrackAmountKeypad(
    onDigit = { /* append */ },
    onBackspace = { /* /= 10 */ },
    onTripleZero = { /* *= 1000 */ },
)

KeuTrackSegmentedControl(
    leftLabel = "Expense",
    rightLabel = "Income",
    leftSelected = true,
    onLeftClick = {},
    onRightClick = {},
)

KeuTrackProgressBar(progress = 0.72f, tone = KeuTrackProgressTone.Success)
KeuTrackFab(onClick = {}, contentDescription = "Add transaction")
```

---

## 4. Money UI Rules

1. **Amount = `Long`** (IDR whole rupiah). Jangan `Double` / `BigDecimal` di UI layer.
2. **Format hanya lewat `CurrencyFormat`** — jangan `NumberFormat` ad-hoc di feature/ViewModel.
3. **Keypad stateless** — parent/ViewModel pegang `amountRupiah`.
4. **Chip tidak depend domain** — terima `label` / `ImageVector` / `Color`, bukan entity `Category`.
5. Cap keypad: `MAX_AMOUNT_RUPIAH` di `format/CurrencyFormat.kt`.

---

## 5. Do / Don't

| Do | Don't |
|----|-------|
| `KeuTrackTheme` di setiap Preview | Bare `MaterialTheme` di Preview |
| `KeuTrackTheme.semanticColors.*` | Hardcode `Color(0xFF…)` di feature |
| Prefix komponen publik `KeuTrack*` | Duplikasi keypad/format di feature |
| Preview light + dark | Preview hanya light |
| Swap UI ke DS tanpa ubah ViewModel | Redesign layout “sekalian” saat migrasi |

---

## 6. Module Boundaries

```
:core:designsystem  →  :core:common (only)
features:*          →  designsystem ✅
designsystem        →  domain / data ❌
```

Theme hex di `Colors.kt` **jangan diganti** kecuali bug blocker. Warna baru: pakai semantic/success/danger yang sudah ada.

---

## 7. Consumers (post Phase 3)

| Feature | Money / shared UI |
|---------|-------------------|
| `dashboard` | Segment, CurrencyText, CategoryChip, Keypad, Fab |
| `family` | ProgressBar, Fab |
| `settings` | StatusChip (via thin `SettingsStatusChip` wrap) |
| `transaction` | Siap konsumsi Keypad / Segment / Chip di Phase 5 |
| `auth` / `splash` | Theme only — jangan diubah di polish DS |
