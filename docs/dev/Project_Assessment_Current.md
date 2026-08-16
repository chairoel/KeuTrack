# KeuTrack Android — Project Assessment (Current)

> **Checkpoint date:** 2026-08-03  
> **Baseline HEAD:** `f3423a9` (merge PR #5 `feat/domain-entities` → `master`)  
> **Active branch:** `feat/settings-preferences` (Phase 7 — complete)  
> **Supersedes for status:** `docs/dev/Project_Assessment.md` (historical baseline post-auth; kept for archive)

Exploration covered plans, phase docs, feature modules, navigation, domain/data layers, sync, and tests against the current codebase.

---

## Executive Summary

The app is **past the core product milestone**. Auth, offline-first finance persistence, dashboard, transaction entry/history, family membership + shared data sync, and Settings all work end-to-end.

What remains is mainly **polish & tests** (Phase 9), cleanup (SMPOB boilerplate), and nice-to-haves (Google Sheets export, edit/delete transaction UI, budget authoring).

---

## 1. Plans & Phase Docs

| Location | Purpose | Status vs codebase |
|----------|---------|-------------------|
| `plans/KeuTrack_Development_Plan.md` | 10-phase roadmap | **Largely done** for Phases 0–8; Phase 9 not started |
| `plans/KeuTrack_Data_Design.md` | Offline-first Room + Firestore + WorkManager | **Implemented** for wallets, transactions, categories, budgets, family groups, sync push/pull |
| `docs/dev/DESIGN_SYSTEM_ATELIER.md` / `plans/DESIGN_SYSTEM_ATELIER.md` | Dual theme + component catalog | **Largely implemented** in `core/designsystem/` (+ money UI primitives) |
| `docs/dev/phases/PHASE_1_*.md` … `PHASE_6C_*.md` | Detailed phase plans | Phases 1–6c delivered in code; docs are implementation guides |
| `docs/dev/Project_Assessment.md` | Earlier assessment | **Stale** — describes pre–Phase 1 state |

---

## 2. Feature Modules — Completeness

### COMPLETE (working with real backend / local data)

#### Splash (`features/splashscreen/`)
- Session check → Home or Login
- `syncUserProfile()` on cold start when session exists

#### Auth (`features/auth/`)
- Login + Register with validation
- Google Sign-In via `GetSignInWithGoogleOption` (stabilized)
- Email login/register → `UserRepository`
- Error mapping, per-method loading, navigate Home on success

#### Dashboard (`features/dashboard/`)
- Real data via `GetWalletSummaryUseCase`, `GetTransactionsUseCase`, `GetMonthlySummaryUseCase`, `GetCategoriesUseCase`
- Personal/family balances, income/expense, recent transactions, sync badges
- `RetryPendingSyncUseCase` on screen open
- FAB / View All wired to transaction routes through `HomeShell`
- Mock content retained for `@Preview` only

#### Transaction (`features/transaction/`)
- Full-screen new entry: amount keypad, category, wallet picker, date, note
- `AddTransactionUseCase` closed-loop (Room → sync queue)
- Transaction history screen + navigation
- Routes: `navigateToTransaction()`, `navigateToTransactionHistory()` used from app shell

#### Family (`features/family/`)
- Insights bound to real UI state (IDR)
- Create / join family via invite code
- History/breakdown filtered by `familyId`
- `SyncFamilyDataUseCase` pull sync for shared family wallet + transactions
- Mock content for preview / empty-state fixtures

#### Core auth/data pipeline
- `User` + Firestore `/users/{uid}` + Proto DataStore
- `UserRepositoryImpl`: auth, upsert/get, rollback, sign-out, `syncUserProfile()`
- Financial repos: Transaction, Wallet, Category, Budget, Family, Sync

#### Navigation shell (`app/.../navigation/`)
```
SplashRoute (start)
  ├── authenticated → HomeRoute (HomeShell)
  │     ├── DashboardRoute
  │     ├── FamilyRoute
  │     └── SettingsRoute → sign out → LoginRoute
  └── unauthenticated → LoginRoute ↔ RegisterRoute

TransactionRoute / TransactionHistoryRoute (top-level, wired from FAB / View All / Family)
```

#### Design system (`core/designsystem/`)
- Theme, typography, colors, buttons, cards, fields, bottom nav, sheets, profile image
- Shared money UI: currency format, amount keypad, chips, progress, FAB primitives

#### Settings (`features/settings/`)
- Profile (name, email, avatar) from `UserRepository`
- Sign out + `syncUserProfile()` on init
- Family network: invite code from real membership, create/join/leave dialogs
- Connected wallets from `GetWalletSummaryUseCase` (Room) with empty state
- `LeaveFamilyGroupUseCase` with owner/member logic + local wallet cleanup
- `SettingsUiMapper` + single `SettingsUIState` (pola Dashboard `combine` Flows)
- Google Sheets: "Segera hadir" (deferred by design)
- Mock content retained for `@Preview` only

---

### PARTIALLY COMPLETE

*None — all feature modules are functionally complete.*

---

### DEFERRED (intentional product decisions)

#### Settings (`features/settings/`) — deferred items
- Currency picker/persistence — **removed from scope** (app stays IDR-only; `User.currency` field exists for future use)
- Google Sheets card — UI placeholder with "Segera hadir" (no backend integration)
- QR invite — not built (invite code copy path exists)

---

## 3. Domain Layer (`core/domain/`)

**Exists:**
- Models: `User`, `Transaction`, `Wallet`, `Category`, `CategorySummary`, `Budget`, `FamilyGroup`, enums (`TransactionType`, `WalletType`, `SyncStatus`, `FamilyRole`, …)
- Repositories: `User`, `Transaction`, `Wallet`, `Category`, `Budget`, `Family`, `Sync`
- Use cases: `AddTransaction`, `GetTransactions`, `GetWalletSummary`, `GetMonthlySummary`, `GetCategories`, `GetBudgetProgress`, `RetryPendingSync`, `CreateFamilyGroup`, `JoinFamilyGroup`, `LeaveFamilyGroup`, `SyncFamilyData`, `SignInWithGoogle` (defined; VMs often call repos directly for auth)

**Missing / deferred:**
- Edit/delete transaction use cases (repo may expose methods; UI not wired)
- Budget create/edit UX use cases at feature layer

---

## 4. Data Layer (`core/data/`)

**Exists:**
- Room: `AppDatabase`, entities, DAOs, converters for Transaction, Wallet, Category, Budget, CategorySummary
- Local + Firestore data sources for financial collections + `FamilyGroup`
- Repository impls + Hilt modules (`DatabaseModule`, `SyncModule`, financial binds)
- Offline sync: `SyncRepositoryImpl`, `SyncWorker`, `SyncScheduler`, Hilt `WorkerFactory` in app
- Family pull sync into Room (canonical family wallet)

**Tech debt:**
- Dashboard still contains SMPOB sample `RouteRepository` / `RouteServices` (documented as sample; not product path)

**Not done:**
- Google Sheets remote integration
- Full conflict-resolution strategy beyond sync status / last-write style used today

---

## 5. Tests

**Zero** production test files (`*Test*.kt` outside build) — Phase 9 not started.

---

## 6. Phase Completion (Current)

| Phase | Plan | Actual status |
|-------|------|---------------|
| 0 — Build plugin | Gradle conventions, modules | **Done** |
| 1 — Domain entities/use cases | Transaction, Wallet, repos, use cases | **Done** |
| 2 — Room + data repos | AppDatabase, DAOs, impls, sync sources | **Done** |
| 3 — Design system | Theme + components + money UI | **~95%** |
| 4 — Dashboard | Real data + FAB → transaction | **Done** |
| 5 — Transaction | Full new-entry + history | **Done** |
| 6a/6b — Family | Insights + create/join membership | **Done** |
| 6c — Shared family sync | Canonical wallet + pull sync | **Done** |
| 7 — Settings | Wallets + leave family + Sheets deferred | **Done** (currency picker removed from scope by design) |
| 8 — App navigation | NavHost + bottom nav + txn routes | **~95%** |
| 9 — Polish & tests | Previews, tests, lint | **~25%** — some Previews; no unit tests |

---

## 7. Key Remaining Work (Priority Order)

1. **Remove / isolate SMPOB Route boilerplate** on dashboard
2. **Phase 9 — Tests** — domain use cases, repository sync, critical ViewModel flows
3. **Google Sheets** — currently UI-only with "Segera hadir" (can stay deferred)
4. **Transaction edit/delete** — use cases + UI
5. **Budget authoring UI** — repo/create path exists; Family shows progress from data when present
6. **Nice-to-have** — QR invite, forgot password / email verification

---

## 8. Suggested Next Arc

Phase 7 (`feat/settings-preferences`) is complete.

Focus next:
1. Merge `feat/settings-preferences` → `master`
2. Remove SMPOB Route boilerplate (quick cleanup)
3. Phase 9: unit tests for critical paths (domain use cases, repo sync, ViewModel flows)
4. Optional product extras (Google Sheets API, transaction edit/delete, budget UI)

---

## 9. Diff vs Historical Assessment (`Project_Assessment.md`)

| Claim in old assessment | Current reality |
|-------------------------|-----------------|
| Before core product milestone | Core product (txn/wallet/family sync) **shipped** |
| Phase 1 ~10% | **Done** |
| Phase 2 ~5% / no Room | **Done** (Room + WorkManager sync) |
| Dashboard almost entirely mock | **Real data** |
| Transaction module placeholder | **Full entry + history** |
| Family 100% mock / empty VM | **Real insights + membership + pull sync** |
| `navigateToTransaction()` never called | **Wired** from HomeShell / Family |
| `SyncRepository` interface only | **Implemented** + Worker |
| Zero financial domain models | Full financial domain + use cases |

Use **this file** for checkpoint / planning. Keep `Project_Assessment.md` as the post-auth baseline snapshot.
