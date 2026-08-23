# Phase 12 — New Entry Amount Keypad Bottom Sheet

> **Modul target:** `:features:transaction`  
> **Estimasi:** ~0.5 hari  
> **Prasyarat:** Phase 5 ✅ (New Entry form + history) · Phase 3 ✅ (`KeuTrackAmountKeypad`, `KeuTrackModalBottomSheet`, `KeuTrackCard`)  
> **Status:** Done (implemented)  
> **Status baseline (sebelum perubahan):** Keypad numerik menempel di body form di bawah Category; Note di bawah keypad. Kartu AMOUNT tidak bisa diklik.  
> **Hasil akhir:** Tap kartu **AMOUNT** membuka keypad seperti **keyboard custom** (sheet tanpa header amount, scrim transparan); hardware/emulator number keys selaras dengan keypad; Note langsung di bawah Category; logic amount (`onDigit` / `000` / backspace) tidak berubah

---

## Daftar Isi

1. [Konteks & Tujuan](#1-konteks--tujuan)
2. [Inventory — Apa yang Sudah Ada](#2-inventory--apa-yang-sudah-ada)
3. [Keputusan UX](#3-keputusan-ux)
4. [Scope — Apa yang Dikerjakan](#4-scope--apa-yang-dikerjakan)
5. [Scope — Apa yang TIDAK Dikerjakan](#5-scope--apa-yang-tidak-dikerjakan)
6. [Prasyarat (Definition of Ready)](#6-prasyarat-definition-of-ready)
7. [File Referensi (Read-Only)](#7-file-referensi-read-only)
8. [File yang TIDAK BOLEH Diubah](#8-file-yang-tidak-boleh-diubah)
9. [File yang BOLEH Diubah / Dibuat](#9-file-yang-boleh-diubah--dibuat)
10. [Struktur File Target](#10-struktur-file-target)
11. [Desain UX](#11-desain-ux)
12. [Task Breakdown Detail](#12-task-breakdown-detail)
13. [Acceptance Criteria](#13-acceptance-criteria)
14. [Catatan Arsitektur & Konvensi](#14-catatan-arsitektur--konvensi)
15. [Dependency Graph](#15-dependency-graph)
16. [Risiko & Mitigasi](#16-risiko--mitigasi)
17. [Urutan Pengerjaan yang Disarankan](#17-urutan-pengerjaan-yang-disarankan)
18. [Relasi ke Phase Lain](#18-relasi-ke-phase-lain)
19. [Manual Test Plan](#19-manual-test-plan)

---

## 1. Konteks & Tujuan

Phase 5 memproductize `NewEntryScreen` dengan form lengkap. Keypad amount **inline** di body form (di antara Category dan Note) menekan field Note ke bawah viewport dan membuat form terasa seperti kalkulator, bukan form transaksi.

| Area | Status sebelum Phase 12 |
|------|-------------------------|
| Segment Expense / Income | ✅ |
| Kartu AMOUNT (`KeuTrackCurrencyText`) | ✅ tampil, **tidak** clickable |
| Wallet picker + date picker sheet | ✅ overlay di `NewEntryScreen` |
| Category chips + See all sheet | ✅ |
| `KeuTrackAmountKeypad` | ✅ **inline** di `NewEntryFormContent` |
| Note | ✅ di bawah keypad |
| Save via `AddTransactionUseCase` | ✅ |

**Tujuan Phase 12:**

1. Keypad numerik pindah ke **bottom sheet** (`KeuTrackModalBottomSheet`), muncul **hanya** saat kartu AMOUNT diklik.
2. Field **Note** langsung di bawah **Category** (lalu error + tombol Add).
3. Sheet berperilaku seperti keyboard custom: **tanpa** label/nilai AMOUNT (sudah ada di form), **scrim transparan** agar kartu amount di form tetap terlihat dan update live.
4. Keyboard laptop / hardware (emulator) memasukkan **hanya digit** lewat handler keypad yang sama; huruf/simbol tidak masuk amount atau Note.
5. Tidak mengubah ViewModel, domain, data, atau aturan digit (`MAX_AMOUNT_RUPIAH`).

**Bukan tujuan:**

- Edit transaksi / keypad di history
- Keyboard IME sistem untuk amount (soft keyboard Android)
- Redesign visual Atelier / dark-token baru
- Family budget sheet memakai keypad yang sama (boleh follow-up; di luar scope)

---

## 2. Inventory — Apa yang Sudah Ada

### Feature transaction

| File | Peran vs Phase 12 |
|------|-------------------|
| `NewEntryScreen.kt` | Overlay state: wallet / date / category see-all + `BackHandler` |
| `NewEntryFormContent.kt` | Urutan lama: Amount → Wallet/Date → Category → **Keypad** → **Note** → Save |
| `WalletPickerBottomSheet.kt` | Pola sheet: `KeuTrackModalBottomSheet` + padding + `navigationBarsPadding` |
| `CategorySeeAllSheet.kt` | Pola sheet yang sama |
| `NewEntryViewModel.kt` | `onDigit` / `onTripleZero` / `onBackspace` — **jangan diubah** |
| `NewEntryRouting.kt` | Wiring ViewModel → Screen — tidak perlu ubah signature |

### Design system

| Item | Lokasi | Dipakai Phase 12 |
|------|--------|------------------|
| `KeuTrackAmountKeypad` | `core/designsystem/.../KeuTrackAmountKeypad.kt` | Isi sheet; **tidak diubah** |
| `KeuTrackModalBottomSheet` | `.../KeuTrackModalBottomSheet.kt` | Chrome sheet (satu-satunya M3 di project) |
| `KeuTrackCard.onClick` / `focused` | `.../KeuTrackCard.kt` | Tap target + border primary saat keypad terbuka |
| `KeuTrackCurrencyText` | `.../KeuTrackCurrencyText.kt` | Amount **hanya** di form (sumber kebenaran visual) |
| `KeuTrackButton` | `.../KeuTrackButton.kt` | Tombol Done di sheet |

---

## 3. Keputusan UX

| Keputusan | Pilihan | Alasan |
|-----------|---------|--------|
| Trigger keypad | Tap kartu AMOUNT | Bukan auto-open; form tetap bisa di-scroll tanpa keypad |
| Host overlay | `NewEntryScreen` + `rememberSaveable` | Sama seperti wallet / category; survive rotation |
| Header amount di sheet | **Tidak ada** | Redundan dengan kartu AMOUNT di form |
| Scrim keypad | `Color.Transparent` | Form (termasuk angka) tetap kelihatan; terasa keyboard custom. Wallet/category sheet **tetap** scrim 50% |
| Dismiss | Drag handle, tap area di atas sheet, Back, tombol **Done** | Back sudah di-handle overlay; Done supaya first-time user tidak bingung |
| Note position | Langsung setelah Category | Permintaan produk; Note tidak lagi “terjepit” keypad |
| Digit logic | Tetap di ViewModel | Sheet hanya UI; cap `MAX_AMOUNT_RUPIAH` tidak berubah |
| Focus indicator | Border primary 2.dp + caret berkedip saat keypad terbuka | Kartu AMOUNT harus terasa field aktif (bukan display statis); selaras `KeuTrackTextField` (`focusedIndicatorColor = primary`) |
| Hardware keyboard | `onPreviewKeyEvent` di sheet; digit/numpad → `onDigit`; Backspace/Delete → `onBackspace`; Enter/Esc → dismiss; **selain itu consume** | Emulator + laptop harus selaras dengan keypad on-screen; huruf tidak boleh bocor ke Note. Bukan IME sistem. |

Keypad **tidak** auto-open saat screen pertama kali muncul.

---

## 4. Scope — Apa yang Dikerjakan

1. Kartu AMOUNT `onClick` → buka sheet keypad.
2. Komponen baru `AmountKeypadBottomSheet` (`KeuTrackAmountKeypad` + Done, scrim transparan, tanpa header amount).
3. Pindahkan Note ke bawah Category di `NewEntryFormContent`.
4. Hapus keypad inline dari form.
5. `BackHandler` overlay mencakup `showAmountKeypad`.
6. Preview light/dark untuk content sheet.
7. Focus indicator kartu AMOUNT saat keypad terbuka (`KeuTrackCard.focused` + caret).
8. Hardware / emulator keyboard: intercept di `AmountKeypadBottomSheet` (`AmountHardwareKey.kt`); bukan `TextField`.
9. Dokumentasi phase ini.

---

## 5. Scope — Apa yang TIDAK Dikerjakan

- `NewEntryViewModel` digit/backspace/`000` rules
- `AddTransactionUseCase`, Room, Firestore
- IME `KeyboardType.Number` untuk amount
- Shared keypad sheet di Family Budget (masih `KeuTrackTextField` number)
- Animasi sheet (peek height / partially expanded — `skipPartiallyExpanded = true` tetap)
- Copy i18n / string resources (tetap hardcoded seperti form sekarang)

---

## 6. Prasyarat (Definition of Ready)

- [x] Phase 5 New Entry form berjalan (save, wallet, date, category, note)
- [x] `KeuTrackModalBottomSheet` dipakai wallet/category sheet
- [x] `KeuTrackCard` mendukung `onClick`
- [x] `KeuTrackAmountKeypad` stateless (parent punya amount)

---

## 7. File Referensi (Read-Only)

| File | Kenapa dibaca |
|------|----------------|
| `docs/dev/phases/PHASE_5_TRANSACTION_FLOW.md` | Kontrak form New Entry; keypad DS; note field |
| `docs/dev/phases/PHASE_3_DESIGN_SYSTEM.md` | `KeuTrackAmountKeypad`, card, modal sheet |
| `WalletPickerBottomSheet.kt` | Pola padding / dismiss / `navigationBarsPadding` |
| `KeuTrackModalBottomSheet.kt` | Feature **tidak** opt-in `ExperimentalMaterial3Api` |
| `NewEntryViewModel.kt` | Digit handlers yang harus tetap di-wire dari Screen |

---

## 8. File yang TIDAK BOLEH Diubah

- `:core:domain`, `:core:data`, `:core:datastore`
- `features/auth/`, `features/splashscreen/`
- `build-plugin/`, `settings.gradle.kts`, `gradle.properties`
- `KeuTrackAmountKeypad.kt` (reuse as-is)
- `NewEntryViewModel.kt` / `NewEntryRouting.kt` (signature Screen tetap: `onDigit` / `onTripleZero` / `onBackspace`)

---

## 9. File yang BOLEH Diubah / Dibuat

| File | Aksi |
|------|------|
| `.../components/AmountKeypadBottomSheet.kt` | **Buat** |
| `.../components/AmountHardwareKey.kt` | **Buat** — mapping Key → digit / backspace / dismiss / consume |
| `.../components/NewEntryFormContent.kt` | Ubah urutan + `onAmountClick`; hapus keypad inline |
| `.../presentation/NewEntryScreen.kt` | State `showAmountKeypad` + host sheet |
| `.../test/.../AmountHardwareKeyTest.kt` | **Buat** |
| `docs/dev/phases/PHASE_12_NEW_ENTRY_AMOUNT_KEYPAD_SHEET.md` | Dokumen ini |

---

## 10. Struktur File Target

```
features/transaction/src/main/kotlin/.../feature/transaction/presentation/
├── NewEntryScreen.kt              ← overlay showAmountKeypad
├── NewEntryRouting.kt             ← tidak berubah
├── NewEntryViewModel.kt           ← tidak berubah
└── components/
    ├── NewEntryFormContent.kt     ← Amount clickable; Note setelah Category
    ├── AmountKeypadBottomSheet.kt ← BARU
    ├── AmountHardwareKey.kt       ← BARU (hardware/emulator keys)
    ├── WalletPickerBottomSheet.kt
    ├── CategorySeeAllSheet.kt
    └── DatePickerField.kt
```

---

## 11. Desain UX

### Urutan form (setelah)

```
Subtitle
Expense | Income
AMOUNT  (tap → sheet)
Wallet | Date
CATEGORY  (+ See all)
Note (optional)
[error]
Add transaction
```

### Isi bottom sheet (keyboard custom)

```
[1] [2] [3]
[4] [5] [6]
[7] [8] [9]
[000] [0] [⌫]
Done
```

Tidak ada label AMOUNT / `Rp …` di sheet — angka hanya di kartu form, yang tetap terlihat karena scrim transparan.

### Hardware / emulator keyboard (saat sheet terbuka)

| Key | Aksi |
|-----|------|
| `0`–`9` / numpad | `onDigit` (sama dengan tombol on-screen) |
| Backspace / Delete | `onBackspace` |
| Enter / Esc | tutup sheet |
| Huruf, spasi, `.` `,` `-`, dll. | **consume** — tidak mengubah amount, tidak masuk Note |
| Tombol `000` | hanya on-screen (ketik `0` tiga kali = efek sama) |

Sheet `requestFocus()` setelah frame pertama. Bukan `TextField` — IME sistem tidak muncul.

Hanya aktif **saat keypad amount terbuka**. Sheet tertutup + fokus Note → huruf tetap bisa diketik di note.

### Overlay state

Satu overlay terbuka pada satu waktu secara praktis (tap AMOUNT menutup via BackHandler reset-all, sama seperti overlay lain). `rememberSaveable` supaya sheet tetap terbuka setelah rotation.

---

## 12. Task Breakdown Detail

### Task 1 — AmountKeypadBottomSheet

- Wrap `KeuTrackModalBottomSheet` dengan `scrimColor = Color.Transparent`.
- Content: `KeuTrackAmountKeypad` + tombol Done → `onDismiss`. **Tanpa** label/nilai AMOUNT.
- Extract `AmountKeypadSheetContent` internal untuk `@Preview` light + dark.
- Padding horizontal 20 / bottom 16 + `navigationBarsPadding()` (selaras wallet picker).
- `focusable` + `onPreviewKeyEvent` + `requestFocus()`; mapping di `AmountHardwareKey.kt`.

### Task 2 — Form reorder + clickable amount

- `NewEntryFormContent`: ganti `onDigit` / `onTripleZero` / `onBackspace` dengan `onAmountClick`.
- `KeuTrackCard(onClick = onAmountClick, focused = amountFocused)`.
- Hapus `KeuTrackAmountKeypad` dari form.
- Note langsung setelah Category (+ spacer section).

### Task 3 — Wire overlay di Screen

- `var showAmountKeypad by rememberSaveable { mutableStateOf(false) }`.
- Masukkan ke `overlayOpen` + `BackHandler`.
- `onAmountClick = { showAmountKeypad = true }`.
- Host sheet; wrap digit callbacks dengan `onClearError()` (perilaku lama form).

### Task 4 — Docs + QA

- Dokumen ini.
- Manual test §19.

---

## 13. Acceptance Criteria

- [x] Keypad **tidak** terlihat di body form saat sheet tertutup.
- [x] Tap kartu AMOUNT membuka bottom sheet berisi keypad saja (tanpa header amount).
- [x] Scrim keypad **transparan**; kartu AMOUNT di form tetap terlihat dan update live saat digit / `000` / backspace.
- [x] Wallet / category sheet **tidak** ikut transparan (scrim 50% tetap).
- [x] Done / Back / tap di atas sheet / drag handle menutup sheet; amount **tetap** di form.
- [x] Note berada langsung di bawah Category, di atas tombol Add.
- [x] Save, wallet, date, category, validasi amount > 0 tidak regresi.
- [x] Rotation: sheet tetap terbuka jika sudah terbuka (`rememberSaveable`).
- [x] Preview light/dark sheet content ada.
- [x] Saat keypad terbuka, kartu AMOUNT punya **focus indicator** (border `semantic.primary` + caret berkedip); hilang saat sheet ditutup.
- [x] Keyboard laptop/emulator: digit & numpad mengubah amount lewat `onDigit`; Backspace menghapus; huruf/simbol **tidak** masuk amount atau Note.
- [x] Enter / Esc menutup sheet keypad.
- [x] IME sistem **tidak** muncul untuk amount (bukan `TextField`).

---

## 14. Catatan Arsitektur & Konvensi

- Feature **tidak** depend feature lain; sheet di `presentation/components/`.
- Pakai `KeuTrackTheme`, bukan raw `MaterialTheme`.
- `KeuTrackModalBottomSheet` menyembunyikan experimental M3 dari feature module.
- Amount tetap `Long` (IDR, tanpa desimal).
- `CancellationException` / dispatcher: tidak relevan (UI-only).
- Magic number spacing → `private const val` di file composable.

---

## 15. Dependency Graph

```
Task 1 (AmountKeypadBottomSheet)
  └── Task 2 (Form clickable + Note reorder)
        └── Task 3 (NewEntryScreen overlay)
              └── Task 4 (docs + QA)
```

Tidak ada perubahan Gradle / Hilt.

---

## 16. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| User mengetik “buta” | Salah input | Scrim transparan → kartu AMOUNT di form tetap terlihat dan update live |
| Sheet tertutup tanpa konfirmasi | Amount sudah di state; tidak hilang | Done + persist ViewModel state |
| Dua overlay sekaligus | Wallet + keypad | BackHandler reset semua; tap target terpisah |
| IME sistem muncul di Note lalu bentrok sheet | Keyboard + sheet | Sheet hanya dari tap AMOUNT, bukan fokus Note |
| Keyboard laptop tidak merespons di emulator | Digit tidak masuk | AVD `hw.keyboard=yes` / Enable keyboard input; sheet `requestFocus` |
| Huruf bocor ke Note saat keypad terbuka | Data salah | `onPreviewKeyEvent` consume semua non-digit |
| Digit ter-double (KeyDown+KeyUp) | Amount 2× | Hanya `KeyEventType.KeyDown` yang memicu digit |
| Cap amount terlewati | Data invalid | Logic tetap di ViewModel (`MAX_AMOUNT_RUPIAH`) |

---

## 17. Urutan Pengerjaan yang Disarankan

1. Task 1 — sheet keypad.  
2. Task 2 — form.  
3. Task 3 — overlay Screen.  
4. Task 4 — docs + manual QA.  
5. Commit saat diminta user, contoh message:  
   `[FEAT] Move new-entry amount keypad into a bottom sheet`

---

## 18. Relasi ke Phase Lain

| Dokumen | Relasi |
|---------|--------|
| `PHASE_5_TRANSACTION_FLOW.md` | Parent product surface; Phase 12 **mengganti** layout inline keypad di New Entry, bukan kontrak save |
| `PHASE_3_DESIGN_SYSTEM.md` | Konsumsi keypad + modal sheet; tidak menambah komponen DS baru |
| Phase 11 Family budget sheet | Tetap text field number; **bukan** migrasi ke keypad sheet ini |
| Phase 9 polish | Tema UX; Phase 12 lebih sempit & sudah diimplementasi |

Phase 5 § form “keypad di body” dianggap **superseded** untuk New Entry. Domain mapping Amount → `Transaction.amount` tidak berubah.

---

## 19. Manual Test Plan

### Setup

- Build: `./gradlew :features:transaction:compileDevDebugKotlin` lalu Run `devDebug`
- Masuk ke **Transaksi Baru** dari FAB dashboard

### Kasus

| # | Langkah | Expected |
|---|---------|----------|
| 1 | Buka form, jangan tap AMOUNT | Keypad tidak di body; Note di bawah Category; kartu **tanpa** border primary / caret |
| 2 | Tap kartu AMOUNT | Sheet keypad muncul; form **tidak** gelap; angka tetap di kartu form |
| 3 | Kartu AMOUNT saat keypad terbuka | Border primary + caret berkedip di kanan angka |
| 4 | Ketik `1` `5` `0` `000` | Kartu form = `Rp 150.000` (live, tanpa duplikat di sheet) |
| 5 | Backspace | Digit terakhir hilang di kartu form |
| 6 | Tap **Done** | Sheet tutup; amount tetap; border/caret hilang |
| 7 | Buka lagi → tap area di atas sheet / Back sistem | Sheet tutup; amount tetap; focus hilang |
| 8 | Buka wallet picker | Overlay **tetap** gelap (50%); tidak regresi |
| 9 | Isi Note, pilih kategori, Add | Transaksi tersimpan seperti Phase 5 |
| 10 | Amount `0` → Add | Error “Amount must be greater than 0” (atau copy existing) |
| 11 | Buka keypad → rotate | Sheet tetap terbuka, amount tidak reset, focus tetap |
| 12 | Dark theme | Sheet + form token benar; border primary terlihat |
| 13 | Emulator, keypad terbuka, ketik `1` `2` `5` di laptop | Kartu form = `Rp 125` |
| 14 | Numpad `0` dan Backspace laptop | Sama seperti tombol on-screen |
| 15 | Ketik `abc`, spasi, `.` `,` `-` | Amount **tidak** berubah; Note **tidak** terisi |
| 16 | Enter atau Esc | Sheet tutup; amount tetap |
| 17 | Tutup keypad, fokus Note, ketik huruf | Note menerima teks seperti biasa |

### Verify compile

```
./gradlew :features:transaction:compileDevDebugKotlin
```
