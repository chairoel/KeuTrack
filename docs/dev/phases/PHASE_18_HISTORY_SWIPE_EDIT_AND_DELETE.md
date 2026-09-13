# Phase 18 — History Swipe Reveal Edit & Delete

> **Modul target:** `:features:transaction` (History list + dialog) · domain/data **tidak** berubah  
> **Estimasi:** ~0.8–1.2 hari · **18a** ~0.4–0.5 hari (gesture + Ubah) · **18b** ~0.2–0.3 hari (Hapus dari list) · **18c** ~0.2 hari (tes + preview)  
> **Prasyarat:** Phase 16c ✅ (tap row → form edit; hapus di form + dialog) · Phase 17e app ✅ (`canEdit` / `NotOwner` / tap orang lain snackbar)  
> **Status:** **Not started** (dokumen saja, 2026-09-13)  
> **Hasil akhir:** Di Riwayat, swipe kiri pada transaksi **milik sendiri** membuka dua aksi seperti keranjang e-commerce: **Ubah** (oranye) dan **Hapus** (merah). Ubah memakai form `NewEntryScreen` yang sudah ada. Hapus memakai dialog konfirmasi yang sama, lalu `DeleteTransactionUseCase`. Transaksi penulis lain **tidak** bisa di-swipe.  
> **Asal-usul:** Phase 16 P2/P4 menunda swipe (tap cukup; Material 2 belum rapi). Phase 17 menandai swipe-to-delete **sengaja belum**. Referensi UX: swipe-to-reveal (tombol tetap, user harus tap) — **bukan** swipe-to-dismiss.  
> **Tidak memblokir Phase 17:** Publish rules Console 17e tetap wajib untuk QA 2 akun, tetapi **bukan** DoR Phase 18. Jalur hapus lokal + outbox sudah 16a + 17a.

---

## Progress

| Slice | Task | Status |
|-------|------|--------|
| 18a | Task 1 — `SwipeRevealRow` + wrap History | **Not started** |
| 18a | Task 2 — Ubah + tap/swipe conflict + ACL swipe | **Not started** |
| 18b | Task 3 — Dialog hapus di History + VM delete | **Not started** |
| 18c | Task 4 — Tes VM + preview | **Not started** |

**Berikutnya:** Implement 18a → 18b → 18c. Jangan mulai 18b sebelum reveal Ubah terasa benar di device/emulator.

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
9. [File yang Diubah / Dibuat](#9-file-yang-diubah--dibuat)
10. [Struktur File Target](#10-struktur-file-target)
11. [Desain UX](#11-desain-ux)
12. [Desain Gesture & Komponen](#12-desain-gesture--komponen)
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

Phase 16c menutup mutasi dari History lewat **tap seluruh kartu** → form. Hapus hanya di form. Phase 17e menambah ACL penulis: `TransactionRowUi.canEdit`; tap row orang lain **tidak** buka form.

**Sesudah 16c + 17e (sebelum 18):**

| Permukaan | Perilaku |
|-----------|----------|
| History row milik sendiri | Tap → `navigateToTransaction(id)` (edit) |
| History row orang lain | Tap → snackbar *Hanya penulis yang bisa mengubah transaksi ini* |
| Hapus | `TextButton` di form + `AlertDialog` → `DeleteTransactionUseCase` |
| Swipe | Tidak ada |
| History VM | Read-only (list, filter, snackbar ACL) |

Trigger tap-only kurang discoverable untuk hapus: user harus masuk form dulu. Referensi produk (keranjang): swipe kiri, dua kotak aksi di kanan, tap tombol — item **tidak** hilang hanya karena geser.

**Tujuan 18a — Reveal:**

1. Komponen swipe-to-reveal (dua jangkar: tertutup / terbuka).
2. History membungkus `TransactionHistoryRow`; swipe kiri milik sendiri membuka **Ubah** + **Hapus**.
3. Satu row terbuka pada satu waktu.
4. Row `canEdit == false` tidak menerima horizontal drag.
5. Tap kartu tertutup tetap edit (16c). Tap kartu terbuka menutup reveal, **bukan** navigate.

**Tujuan 18b — Hapus dari list:**

6. Tap **Hapus** → dialog copy yang sama dengan form (16 P9).
7. Confirm → `DeleteTransactionUseCase` dari History VM (bukan repository di Screen).
8. Sukses: Flow Room menghilangkan row; totals ikut (Phase 15). Gagal / `NotOwner`: snackbar existing.

**Tujuan 18c — Verifikasi:**

9. Tes VM: delete milik sendiri; `NotOwner` / error tidak menghapus; tes 17e tap read-only tetap hijau.
10. Preview: tertutup, terbuka, `canEdit = false`.

**Bukan tujuan Phase 18:**

- Swipe-to-dismiss (hapus otomatis lewat threshold)
- Undo snackbar / tombstone UI
- Swipe di Dashboard recent
- Long-press, multi-select, mode “Ubah” massal
- Material 3 `SwipeToDismissBox` / dependency M3 baru
- Use case / Room / Firestore / rules baru
- Mengizinkan anggota mengedit tx penulis lain

---

## 2. Inventory — Apa yang Sudah Ada

### Feature transaction

| Item | Lokasi | Status vs Phase 18 |
|------|--------|-------------------|
| `TransactionHistoryScreen` | `.../history/TransactionHistoryScreen.kt` | `LazyColumn` + `TransactionHistoryRow`; tap pakai `canEdit` |
| `TransactionHistoryRouting` | sama package | Wiring read-only; **belum** delete callback |
| `TransactionHistoryViewModel` | sama package | Tidak inject write use case (16 P3) |
| `TransactionHistoryRow` | `.../components/TransactionHistoryRow.kt` | Kartu + `onClick`; tidak tahu swipe |
| `TransactionRowUi.canEdit` | `.../model/TransactionRowUi.kt` | Diisi mapper 17e |
| `TransactionUiMapper.toTransactionRows` | `.../model/TransactionUiMapper.kt` | `userId == currentUserId` |
| `NewEntryScreen` + `DeleteTransactionDialog` (private) | `NewEntryScreen.kt` | Dialog hapus **hanya** di form |
| `NewEntryViewModel.onDelete` | `NewEntryViewModel.kt` | `DeleteTransactionUseCase` + `isReadOnly` guard |
| `DeleteTransactionUseCase` | `:core:domain` | `NotOwner` jika bukan penulis |
| Nav | `TransactionNavigation.kt` | `onTransactionClick` → `onEditTransaction` → `navigateToTransaction(id)` |

### Design system / Compose

| Item | Status vs Phase 18 |
|------|-------------------|
| `KeuTrackCard` | `clip` + `clickable` — gesture swipe harus di **wrapper**, bukan di dalam kartu |
| `KeuTrackTheme.warningColors.w500` / `dangerColors.d500` | Token tombol Ubah / Hapus |
| Material 2 | Project **tidak** memakai M3 di feature (kecuali sheet existing) |
| Compose BOM `2024.09` | Foundation `anchoredDraggable` tersedia; **belum** dipakai di repo |
| `SwipeToDismiss` M2 | Deprecated + pola dismiss — **jangan** dipakai |

### Yang 18 tidak menyentuh

Room atomic delete, outbox 17a, snapshot-diff 17b, pull sweep 17c, rules 17e. Hapus dari History memakai jalur yang sama dengan form: UseCase → repo → enqueue.

---

## 3. Keputusan Produk

| # | Keputusan | Pilihan | Alasan |
|---|-----------|---------|--------|
| P1 | Pola gesture | **Swipe-to-reveal**, bukan swipe-to-dismiss | Referensi keranjang; hapus uang tidak boleh “kecolongan” geser |
| P2 | Arah | Swipe **kiri** (offset X negatif) membuka aksi di **kanan** | LTR; Indonesia default LTR |
| P3 | Tombol | Tepat **dua**: **Ubah** lalu **Hapus** (Hapus paling kanan) | Cermin referensi; Hapus di tepi = destructive |
| P4 | Warna | Ubah = `warning.w500` (Beer); Hapus = `danger.d500` (CarminePink); teks `on` / putih | Token existing; jangan hex baru |
| P5 | Lebar aksi | ~72.dp per tombol (total reveal ~144.dp); tinggi = tinggi kartu | Cukup tap; jangan full-width |
| P6 | Trigger edit tetap | Tap kartu **tertutup** **atau** tap **Ubah** | 16c tidak dihilangkan; swipe menambah affordance |
| P7 | Tap kartu terbuka | **Tutup** reveal, jangan navigate | Hindari tap tidak sengaja ke form |
| P8 | Satu reveal | Hanya satu `revealedId` | List tidak berantakan |
| P9 | ACL | Swipe **hanya** `canEdit`; orang lain: tap snackbar 17e | Jangan reveal palsu |
| P10 | Konfirmasi hapus | Dialog **wajib** (copy 16 P9) | Destructive; sama form |
| P11 | Tempat delete | History VM + `DeleteTransactionUseCase` | Feature → UseCase; supersede 16 P3 “History read-only” **hanya** untuk delete |
| P12 | State reveal | `remember` di Screen, **bukan** ViewModel / `SavedStateHandle` | Gesture ephemeral; rotasi boleh menutup |
| P13 | Dialog host | Screen `remember` `pendingDeleteId`; VM `isDeleting` + `onDeleteConfirmed(id)` | Dialog bukan domain |
| P14 | Dialog shared | Ekstrak `DeleteTransactionDialog` ke `components/` | Satu copy; form tetap memakai yang sama |
| P15 | Form hapus | **Tetap** ada | Deep link / tap Ubah masih butuh hapus di form |
| P16 | Komponen swipe | Feature-local `SwipeRevealRow` | Jangan naik ke DS sampai Dashboard/Family butuh |
| P17 | Library | Foundation `AnchoredDraggable` | Jangan M3; jangan library swipe pihak ketiga |
| P18 | Scroll vs drag | Horizontal drag milik reveal; vertikal tetap `LazyColumn` | Jangan `nestedScroll` custom kecuali bug nyata |
| P19 | Copy | `Ubah` / `Hapus` hardcoded ID (bukan i18n) | Sama History/form |
| P20 | Dashboard recent | **Tidak** di 18 | Sama 16 P16 |
| P21 | Ship slice | **18a → 18b → 18c**. 18b dilarang jika reveal masih “nempel” ke scroll | Gesture dulu |
| P22 | Cloud | Tidak ada kerja Firestore di 18 | Delete list = delete form; 17 yang menjamin remote |

Phase 16 P2 (tap-only) **tetap** sebagai jalur primer. Phase 16 P3/P4 **disupersede** untuk list: swipe + hapus dari History diizinkan, dengan dialog.

---

## 4. Scope — Apa yang Dikerjakan

### 18a — Reveal + Ubah

1. `SwipeRevealRow`: aksi di belakang, content offset, dua jangkar (`Closed` / `Open`).
2. Wrap item di `TransactionHistoryScreen`; `revealedId` satu nilai.
3. **Ubah** → `onTransactionClick(id)` (nav existing).
4. `enabled = row.canEdit`.
5. Tap tertutup / terbuka sesuai P6–P7.
6. Clip outer box ke `radiusLg` kartu supaya tombol tidak nyembul ke spacing `10.dp`.
7. Preview komponen (tertutup / terbuka).

### 18b — Hapus dari list

8. Ekstrak `DeleteTransactionDialog` (copy + `isBusy` + Batal / Hapus).
9. `NewEntryScreen` memakai komponen shared (perilaku form **tidak** berubah).
10. History: tap **Hapus** → dialog; confirm → VM.
11. Inject `DeleteTransactionUseCase` + `isDeleting` / error lewat `errorMessage` existing.
12. Setelah sukses: tutup reveal; list/totals dari Flow.

### 18c — Tes

13. VM: delete sukses memanggil use case; `NotOwner` / `Unknown` → notice, tidak crash.
14. Tes 17e (`canEdit`, read-only tap) tetap hijau.
15. `assembleDevDebug`.

---

## 5. Scope — Apa yang TIDAK Dikerjakan

| Item | Alasan |
|------|--------|
| Swipe-to-dismiss / hapus tanpa dialog | P1 / P10 |
| Undo “Transaksi dihapus” | Tidak ada undo domain; outbox 17a bukan UI |
| Swipe Dashboard recent / Family insights list | P20 |
| Multi-select / “Ubah” header keranjang | Bukan model History |
| Long-press contextual menu | P4 16 diganti swipe saja |
| Material 3 / library swipe | P17 |
| `SwipeReveal` di `:core:designsystem` | P16 |
| Use case / `TransactionRepository` / Room / Firestore / rules | P22 |
| Badge outbox delete | Phase 17 P22 |
| i18n `strings.xml` | P19 |
| Auth / splash / Settings / Family invite | Protected / di luar |
| Pagination History | Phase 7/9+ |
| RTL mirroring khusus | App LTR |

---

## 6. Prasyarat (Definition of Ready)

- [x] Phase 16c: tap → edit; form hapus + dialog
- [x] Phase 16a/16b: delete lokal + `DeleteTransactionUseCase`
- [x] Phase 17e app: `canEdit`, snackbar bukan penulis, form read-only
- [x] Phase 15: totals Flow (hapus list otomatis koreksi angka)
- [x] Phase 17a: outbox delete (hapus list tetap enqueue lewat repo)
- [ ] **Bukan DoR:** Publish Console 17e — kerjakan 18 tanpa menunggu; QA family 2 akun tetap ikut checklist 17 §24.4

---

## 7. File Referensi (Read-Only)

| File | Kenapa |
|------|--------|
| [`PHASE_16_TRANSACTION_EDIT_AND_DELETE.md`](./PHASE_16_TRANSACTION_EDIT_AND_DELETE.md) §12 dialog, P9 copy | Kontrak hapus |
| [`PHASE_17_TRANSACTION_FIRESTORE_UPDATE_AND_DELETE.md`](./PHASE_17_TRANSACTION_FIRESTORE_UPDATE_AND_DELETE.md) 17e | ACL; swipe dulu di luar 17 |
| `TransactionHistoryScreen.kt` | Titik wrap `LazyColumn` |
| `TransactionHistoryRow.kt` + `KeuTrackCard` | Tinggi/clip; jangan pecah layout row |
| `TransactionUiMapper.toTransactionRows` | Sumber `canEdit` |
| `NewEntryScreen.DeleteTransactionDialog` | Copy yang diekstrak |
| `DeleteTransactionUseCase` | Satu pintu hapus |
| `TransactionHistoryViewModelTest` | Pola turbine + mockk |

---

## 8. File yang TIDAK BOLEH Diubah

- `features/auth/**`, `features/splashscreen/**`
- Domain user/auth + `UserRepository` / `UserRepositoryImpl`
- `build-plugin/**`, `settings.gradle.kts`, `gradle.properties`, `local.properties`
- `:core:data` (Room, outbox, Firestore DS, sync)
- `docs/database/firestore-rules.md` + Publish Console (itu 17e)
- Signature `TransactionRepository` / `UpdateTransactionUseCase` / `AddTransactionUseCase`
- `features/dashboard/**`, `features/family/**`, `features/settings/**`
- `TransactionUiMapper` **kecuali** bug ACL yang ketemu saat 18 (seharusnya tidak)
- Token warna / `Colors.kt` Atelier
- Query History / `GetPeriodTotalsUseCase`

Boleh menambah **dependency feature** hanya jika Foundation belum ter-export lewat convention Compose — **jangan** tambah artifact baru di catalog tanpa alasan compile.

---

## 9. File yang Diubah / Dibuat

### 18a

| Path | Aksi |
|------|------|
| `features/transaction/.../components/SwipeRevealRow.kt` | **Baru** — wrapper reveal |
| `TransactionHistoryScreen.kt` | Wrap row; `revealedId`; callbacks Ubah/Hapus |
| `TransactionHistoryRouting.kt` | Teruskan `onDeleteConfirmed` jika 18b sudah ada; 18a boleh Hapus no-op / belum tampil dialog |

**Pilih:** 18a tampilkan kedua tombol; **Hapus** boleh `pendingDeleteId` local tanpa VM dulu **atau** tunggu 18b di commit yang sama jika slice disatukan. Jangan biarkan tap Hapus no-op di `main`.

### 18b

| Path | Aksi |
|------|------|
| `.../components/DeleteTransactionDialog.kt` | **Baru** — ekstrak dari `NewEntryScreen` |
| `NewEntryScreen.kt` | Pakai shared dialog |
| `TransactionHistoryViewModel.kt` | Inject `DeleteTransactionUseCase`; `onDeleteConfirmed`; `isDeleting` |
| `HistoryUIState.kt` | `isDeleting: Boolean = false` (additive) |
| `TransactionHistoryRouting.kt` | Wire delete |

### 18c

| Path | Aksi |
|------|------|
| `TransactionHistoryViewModelTest.kt` | Tes delete + `NotOwner` |
| Preview History / `SwipeRevealRow` | Terbuka + read-only |

Tidak ada route baru. `transactionGraph` signature **tidak** wajib berubah (`onEditTransaction` tetap).

---

## 10. Struktur File Target

```
features/transaction/.../presentation/
├── components/
│   ├── SwipeRevealRow.kt              ← baru
│   ├── DeleteTransactionDialog.kt     ← baru (shared)
│   └── TransactionHistoryRow.kt       ← tidak wajib diubah
├── history/
│   ├── TransactionHistoryScreen.kt    ← wrap + dialog
│   ├── TransactionHistoryRouting.kt
│   └── TransactionHistoryViewModel.kt ← delete
├── model/
│   └── HistoryUIState.kt              ← isDeleting
└── NewEntryScreen.kt                  ← pakai dialog shared
```

Tidak ada file di `:core:domain` / `:core:data` / `:app`.

---

## 11. Desain UX

### 11.1 Layout item

```
┌─────────────────────────────────────────────────────┐
│  [ikon]  judul / kategori • jam      − Rp …        │  ← kartu (geser kiri)
│          wallet chip                                │
└──────────────────────────────────────┬──────┬───────┘
                                       │ Ubah │ Hapus │  ← di belakang, kanan
                                       └──────┴───────┘
```

- Kartu existing (`KeuTrackCard`) **tidak** didesain ulang.
- Tombol full-height, teks tengah, tanpa ikon wajib (boleh ikon kecil + teks jika muat; default **teks saja** seperti referensi).
- Outer `clip` = `RoundedCornerShape(radiusLg)` supaya sudut kartu + aksi satu kapsul.
- `LazyColumn` `spacedBy(10.dp)` tetap.

### 11.2 Interaksi

| Gesture / tap | `canEdit` | Hasil |
|---------------|-----------|--------|
| Swipe kiri cukup threshold | ya | Snap `Open` |
| Swipe kanan / tap kartu saat `Open` | ya | Snap `Closed` |
| Tap kartu `Closed` | ya | Edit form (16c) |
| Tap **Ubah** | ya | Edit form; tutup reveal |
| Tap **Hapus** | ya | Dialog; reveal boleh tetap sampai confirm/dismiss |
| Swipe | tidak | Tidak bergerak; tap → snackbar 17e |
| Swipe row lain | ya | Row lama `Closed`, yang baru mengikuti drag |

Jangan buka dua reveal. Saat `items` berubah (filter / delete sukses), reset `revealedId` jika id hilang.

### 11.3 Dialog hapus

Sama 16 §12.2:

- Judul: `Hapus transaksi?`
- Body: `Transaksi ini akan dihapus dari riwayat. Saldo dan anggaran akan disesuaikan.`
- `Batal` / `Hapus`
- `isDeleting` → ignore dismiss confirm dobel; disable tombol seperti form `isSaving`

Setelah sukses: dialog tutup; row hilang dari Flow.

### 11.4 Empty / loading

Swipe tidak relevan. Empty CTA “Tambah transaksi” tidak berubah.

### 11.5 Form edit

Tidak ada perubahan perilaku selain dialog pindah file. Judul, CTA, tombol Hapus form, read-only 17e **tetap**.

---

## 12. Desain Gesture & Komponen

### 12.1 Kenapa bukan `SwipeToDismiss`

API M2 untuk **menghilangkan** item setelah threshold. Dua tombol tetap + konfirmasi tidak cocok. `SwipeToDismissBox` M3 menambah dependency yang dihindari konvensi feature.

### 12.2 `SwipeRevealRow` (kontrak)

```kotlin
@Composable
fun SwipeRevealRow(
    revealed: Boolean,
    enabled: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
)
```

- Aksi: `Row` `Alignment.End` di belakang (`matchParentSize`).
- Content: `Modifier.offset` + `anchoredDraggable` horizontal.
- Anchors: `Closed` di `0f`, `Open` di `-revealPx` (`2 * actionWidth`).
- `LaunchedEffect(revealed)` menyelaraskan animasi jika parent menutup row lain.
- `enabled == false`: tidak attach drag; content full width.

Jangan hardcode px di call-site — `dp` + `LocalDensity`.

API `AnchoredDraggableState` / `updateAnchors` mengikuti Foundation di BOM `2024.09`. Jika signature experimental, `@OptIn` di file komponen saja, bukan Screen.

### 12.3 Konflik klik

`KeuTrackCard.onClick` tetap. Wrapper harus:

1. Saat offset ~0 dan tap → `onClick` row (edit / snackbar).
2. Saat `Open` dan tap kartu → `onRevealedChange(false)` saja.
3. Drag horizontal tidak boleh terhitung sebagai tap.

Jika `clickable` kartu “mencuri” pointer, pindahkan `onClick` ke wrapper (`pointerInput` tap) dan set `TransactionHistoryRow.onClick` kosong / nullable. **Pilih:** tambah `onClick: (() -> Unit)? = null` di row **hanya jika** perlu; jangan pecah preview existing tanpa default.

### 12.4 State list

```kotlin
var revealedId by remember { mutableStateOf<String?>(null) }
var pendingDeleteId by remember { mutableStateOf<String?>(null) }
```

Bukan `rememberSaveable` (P12). Bukan field `HistoryUIState`.

---

## 13. Pemetaan UI → State / Use Case

### History Screen

| Event | Aksi |
|-------|------|
| Swipe open | `revealedId = id` |
| Swipe close / tap kartu open | `revealedId = null` |
| Ubah | `revealedId = null`; `onTransactionClick(id)` |
| Hapus | `pendingDeleteId = id` |
| Dialog Batal | `pendingDeleteId = null` |
| Dialog Hapus | `onDeleteConfirmed(id)` |
| `isDeleting` → false + sukses | `pendingDeleteId = null`; `revealedId = null` |
| Row `!canEdit` tap | `onReadOnlyTransactionClick()` (17e) |

### `HistoryUIState` (additive)

| Field | Sumber |
|-------|--------|
| `isDeleting` | VM; default `false` |
| field existing | tidak berubah |

Jangan masukkan `revealedId` / `pendingDeleteId` ke UI state Flow.

### `TransactionHistoryViewModel`

```
onDeleteConfirmed(id):
  if (isDeleting || id blank) return
  isDeleting = true
  result = deleteTransaction(id)
  when (result) {
    Success → isDeleting = false
              // list update via Room Flow
    Error.NotOwner → notice ERR_NOT_OWNER (copy 17e)
    Error.MissingId / NotFound / Unknown → notice ERR_DELETE_FAILED
  }
  selalu isDeleting = false di finally (kecuali CancellationException rethrow)
```

Copy usulan:

- `ERR_NOT_OWNER` sudah ada: `Hanya penulis yang bisa mengubah transaksi ini`
- `ERR_DELETE_FAILED`: `Gagal menghapus transaksi` (selaras form)

Jangan inject `UpdateTransactionUseCase`. Jangan panggil `TransactionRepository`.

`NewEntryViewModel.onDelete` **tetap**; dua entry point, satu use case.

---

## 14. Task Breakdown Detail

Kerjakan **18a → 18b → 18c**.

### 18a — Task 1: `SwipeRevealRow` + wrap

- Komponen + preview light/dark.
- Wrap `items { }` di History; `key` tetap `it.id`.
- Clip + tinggi tombol mengikuti kartu.
- Verify: `./gradlew :features:transaction:compileDebugKotlin`

### 18a — Task 2: Ubah + ACL + tap

- **Ubah** / tap tertutup → nav existing.
- Tap terbuka → close.
- `canEdit == false` tidak reveal.
- Satu `revealedId`.
- Cek manual: scroll vertikal tidak “nyangkut” horizontal.

### 18b — Task 3: Dialog + VM

- Ekstrak dialog; form compile + preview edit masih punya Hapus.
- VM inject use case; `CancellationException` rethrow.
- Routing wire.
- Verify compile.

### 18c — Task 4: Tes

Di `TransactionHistoryViewModelTest`:

1. `onDeleteConfirmed` milik sendiri → `deleteTransaction` sekali; `isDeleting` kembali false.
2. Use case `NotOwner` → notice author-only; (mock) tidak perlu assert Room.
3. Tes `family row by another member is read only` + `read only tap` **jangan** pecah.
4. Constructor tes: mock `DeleteTransactionUseCase` (relaxed atau stub `Success`).

Verify:

```
./gradlew :features:transaction:testDevDebugUnitTest --tests "*TransactionHistoryViewModel*" --tests "*NewEntryViewModel*"
./gradlew assembleDevDebug
```

`NewEntryViewModelTest` harus tetap hijau setelah ekstrak dialog (Screen only — tes VM form tidak peduli file dialog).

---

## 15. Acceptance Criteria

### Harus terpenuhi

- [ ] Swipe kiri row milik sendiri membuka **Ubah** + **Hapus** (reveal, bukan dismiss)
- [ ] Tap **Ubah** atau tap kartu tertutup membuka form edit (16c)
- [ ] Tap kartu terbuka menutup aksi, tidak navigate
- [ ] Tap **Hapus** → dialog 16 P9; Batal tidak menghapus
- [ ] Confirm hapus: row hilang; saldo/budget/totals lokal terkoreksi (jalur 16a)
- [ ] Row orang lain tidak bisa di-swipe; tap tetap snackbar 17e
- [ ] Satu row reveal pada satu waktu
- [ ] Form edit/hapus + read-only 17e tidak regresi
- [ ] Create FAB / History filter / totals tidak regresi
- [ ] Auth / splash / Settings / Family invite tidak disentuh
- [ ] Tes 18c + tes History/NewEntry existing hijau
- [ ] Tidak ada dependency M3 baru / library swipe

### Sengaja belum

- [ ] Swipe Dashboard recent
- [ ] Undo delete
- [ ] Hapus tanpa konfirmasi
- [ ] Multi-select
- [ ] Komponen di design system
- [ ] Publish rules 17e (bukan AC 18)

---

## 16. Catatan Arsitektur & Konvensi

- Feature → UseCase → Repository. Screen tidak tahu Room/Firestore.
- History tetap baca Flow Room; delete hanya menambah **satu** write path yang sudah ada.
- `canEdit` adalah ACL UI; use case tetap `NotOwner` (defense in depth 17e).
- `CancellationException` selalu di-rethrow.
- Uang tetap `Long`; 18 tidak menyentuh amount.
- Kotlin only. `KeuTrackTheme` di semua `@Preview`.
- Material 2. Jangan `MaterialTheme` mentah.
- Jangan i18n resources.
- Protected skill: `User*`, auth, splash, build-plugin, gradle root.

---

## 17. Dependency Graph

```
Swipe kiri (canEdit)
  → SwipeRevealRow Open
      ├─ Ubah / tap kartu Closed
      │     → onTransactionClick(id)
      │           → navigateToTransaction(id)     // existing 16c
      │                 → NewEntryViewModel update/delete
      └─ Hapus
            → DeleteTransactionDialog
                  → TransactionHistoryViewModel.onDeleteConfirmed
                        → DeleteTransactionUseCase
                              → NotOwner? stop
                              → TransactionRepository.deleteTransaction
                                    → outbox + reverse Room (17a)
                                    → enqueueSync()
```

Tidak ada method sync baru. Tidak ada route baru.

---

## 18. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| `clickable` kartu vs drag | Swipe tidak jalan / tap saat drag = edit | P7; wrapper dulu; uji device |
| Swipe-to-dismiss terpasang | Hapus tanpa dialog | P1; review PR |
| Reveal di row orang lain | Melanggar 17e | P9; `enabled = canEdit` |
| History VM delete tanpa dialog | Destructive | P10; dialog sebelum `onDeleteConfirmed` |
| Dua copy dialog diverge | Form vs list beda copy | P14 ekstrak |
| `revealedId` di ViewModel | Recompose / tes rumit | P12 |
| Nested scroll “nyangkut” | User tidak bisa scroll list | P18; gestur threshold default Foundation |
| 18 dicampur commit 17 rules | Review kabur | P22; commit UI saja |
| Hapus list sebelum 17e Console | Cloud member-write jika Console masih 17d | Bukan bug 18; tetap Publish 17e sebelum QA 2 akun |
| Tinggi tombol ≠ kartu | Terlihat “Shopee palsu” / gap | `matchParentSize` + clip |

---

## 19. Urutan Pengerjaan yang Disarankan

1. Task 1 — komponen + wrap (preview terbuka).
2. Task 2 — Ubah + ACL + satu reveal. **Berhenti jika scroll/tap rusak.**
3. Task 3 — dialog shared + VM delete.
4. Task 4 — tes + `assembleDevDebug` + QA §22.
5. Jangan merge 18b tanpa dialog.

Boleh satu PR 18a+18b+18c. Jangan satukan dengan Publish rules / sync 17.

---

## 20. Relasi ke Phase Lain

| Phase | Relasi |
|-------|--------|
| **16** | 18 menambah trigger; form + dialog + `id` tetap. **16 P3/P4 disupersede** untuk list. P9 copy tetap |
| **17** | ACL 17e wajib dihormati. Sync/outbox **bukan** kerja 18. Swipe yang “sengaja belum” di 17 = dokumen ini |
| **15** | Totals Flow otomatis setelah delete |
| **13** | Filter periode tidak berubah; reset reveal jika id tidak ada di `items` |
| **5 / 12** | Form create/keypad tidak berubah |
| **9** | Tes UI Compose penuh tidak wajib; 18 wajib tes VM delete |
| **Dashboard** | Recent tap/swipe tetap di luar (16 P16 / 17) |

---

## 21. Rencana Commit

Ikuti tag repo. Branch usulan: `feat/history-swipe-edit-delete`.

```
[FEAT] Reveal edit and delete actions on history swipe
[TEST] Cover history swipe delete and NotOwner
[DOCS] Add Phase 18 history swipe edit and delete plan
```

Jika dipecah: 18a `[FEAT] Add swipe-to-reveal on transaction history` lalu 18b `[FEAT] Delete owned history rows from swipe`. Docs boleh commit sendiri lebih dulu (`[DOCS]`).

---

## 22. Manual Test Plan

Pakai akun yang punya tx sendiri. Family: dua akun jika memungkinkan (A penulis, B anggota). Offline hapus tetap harus benar di Room.

### 22.1 Reveal

| # | Langkah | Expected |
|---|---------|----------|
| 1 | History, swipe kiri tx sendiri | Kartu geser; **Ubah** oranye + **Hapus** merah |
| 2 | Swipe tidak sampai threshold, lepas | Snap tertutup |
| 3 | Buka row 1, swipe row 2 | Row 1 tertutup; row 2 terbuka |
| 4 | Scroll list vertikal (reveal tertutup) | List jalan; tidak “ketarik” horizontal |
| 5 | Rotasi device saat terbuka | Boleh tertutup (P12) |

### 22.2 Edit

| # | Langkah | Expected |
|---|---------|----------|
| 6 | Tap kartu tertutup milik sendiri | Form `Edit Transaksi` |
| 7 | Reveal → tap **Ubah** | Form edit; reveal tidak “nyangkut” saat back |
| 8 | Reveal → tap kartu (bukan tombol) | Reveal tutup; tetap di History |
| 9 | Simpan perubahan di form | Perilaku 16c; back ke History |

### 22.3 Hapus

| # | Langkah | Expected |
|---|---------|----------|
| 10 | Reveal → **Hapus** | Dialog copy 16 P9 |
| 11 | Batal | Row tetap; reveal boleh tetap terbuka |
| 12 | Confirm | Row hilang; totals berubah; saldo wallet terkoreksi |
| 13 | Airplane → confirm hapus | Row hilang lokal; setelah online, 17c menghapus remote (bukan AC visual 18) |
| 14 | Hapus dari **form** (bukan swipe) | Tetap jalan (regresi 16c) |

### 22.4 ACL (17e)

| # | Langkah | Expected |
|---|---------|----------|
| 15 | History family, swipe tx milik B (sebagai A) | Tidak reveal |
| 16 | Tap tx B | Snackbar penulis; form tidak terbuka |
| 17 | B swipe + hapus **tx B** | Sukses; A pull (setelah 17 sync) tidak melihat row |

### 22.5 Regresi

| # | Langkah | Expected |
|---|---------|----------|
| 18 | Chip periode + totals | Tidak pecah (15) |
| 19 | Empty / filtered empty | CTA tidak berubah |
| 20 | FAB create | Tidak kena swipe |
| 21 | Auth / Settings / Family invite | Tidak berubah |

---

## Appendix — Sketsa wrap History (ide, bukan copy-paste wajib)

```kotlin
items(uiState.items, key = { it.id }) { row ->
    SwipeRevealRow(
        revealed = revealedId == row.id,
        enabled = row.canEdit,
        onRevealedChange = { open ->
            revealedId = if (open) row.id else revealedId.takeUnless { it == row.id }
        },
        onEdit = { onTransactionClick(row.id) },
        onDelete = { pendingDeleteId = row.id },
    ) {
        TransactionHistoryRow(
            row = row,
            onClick = {
                when {
                    revealedId == row.id -> revealedId = null
                    row.canEdit -> onTransactionClick(row.id)
                    else -> onReadOnlyTransactionClick()
                }
            },
        )
    }
}
```

`onRevealedChange(false)` harus hanya menutup **row itu**, bukan semua — rumus `takeUnless` / `if (open)` di atas.

---

## Appendix — Pemetaan utang 16 / 17 → 18

| Utang | Di mana di 18 |
|-------|----------------|
| 16 P2 tap-only / P4 “swipe belakangan” | P6 + seluruh 18a |
| 16 P3 History VM read-only | P11 — delete saja |
| 16 P9 dialog | P10 / P14 / §11.3 |
| 17 “bukan tujuan: swipe-to-delete” / AC sengaja belum | Dokumen ini |
| 17e `canEdit` / snackbar | P9 / §22.4 |
