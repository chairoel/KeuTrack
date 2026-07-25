# KeuTrack Android — Project Assessment

Exploration covered `/Users/chairulamri/Documents/Irul/MyProject/KeuTrack` across plans, docs, feature modules, navigation, domain/data layers, and TODO markers.

---

## Executive Summary

The app is **past the infrastructure/auth milestone** but **before the core product milestone**. What works end-to-end today is: splash routing, Firebase Auth (Google + email), Firestore user profile sync, local DataStore cache, and navigation into a polished home shell. What does **not** work yet is the actual expense-tracking product: transactions, wallets, budgets, family groups, offline sync, and real dashboard/family/settings data.

---

## 1. Plans Directory (`/Users/chairulamri/Documents/Irul/MyProject/KeuTrack/plans/`)

| File | Purpose | Status vs codebase |
|------|---------|-------------------|
| `KeuTrack_Development_Plan.md` | 10-phase roadmap (~10–13 days): domain entities, Room, use cases, feature UIs, nav, tests | **Partially done** — Phases 0, 3, 8 largely complete; Phases 1–2, 5–7, 9 mostly not |
| `KeuTrack_Data_Design.md` | Full offline-first architecture: Room + Firestore collections (`users`, `wallets`, `transactions`, `budgets`, `categories`, `family_groups`), WorkManager sync, security rules | **Design only** — almost none implemented in code |
| `DESIGN_SYSTEM_ATELIER.md` | Dual theme (Financial/Midnight Atelier), component catalog | **Largely implemented** in `core/designsystem/` |
| `Compose_Learning.md` | Empty file (0 bytes) | No content |

### Future work implied by plans

**From `KeuTrack_Development_Plan.md`:**
- Domain: `Transaction`, `Wallet`, `Category`, enums, `TransactionRepository`, `WalletRepository`, use cases (`GetTransactionsUseCase`, `AddTransactionUseCase`, `GetWalletSummaryUseCase`)
- Data: Room (`AppDatabase`, DAOs, entities, mappers), repository implementations
- Transaction feature: full `TransactionViewModel`, numpad, category picker, save flow
- Family feature: invite screens, QR code, access levels (plan differs from current “Family Insights” UI)
- Settings: wired family ID copy, currency persistence, Google Sheets sync
- Phase 9: dark-mode polish, lint, unit tests

**From `KeuTrack_Data_Design.md`:**
- Offline-first writes to Room → Firestore batch writes
- Wallet balance increments, category summaries, budgets
- Family group collections and shared insights from real Firestore data
- WorkManager retry queue
- Firestore security rules and composite indexes

---

## 2. Documentation (`/Users/chairulamri/Documents/Irul/MyProject/KeuTrack/docs/firebase/`)

Six Firebase integration docs describe a completed auth pipeline:

| Doc | Topic |
|-----|-------|
| `FIRESTORE_LOGIN_INTEGRATION.md` | Master plan: Auth → Firestore `/users/{uid}` → DataStore, with rollback on Firestore failure |
| `PR_A` through `PR_D` | Incremental PR breakdown (infra, repository orchestration, UI wiring, `syncUserProfile`) |
| `FIREBASE_AUTH_ERROR_MAPPING.md` | Auth error → UI message mapping |

**Note:** Docs still describe `syncUserProfile()` as a TODO, but the latest commit (`105a314`) implements it in `UserRepositoryImpl`. The docs are slightly stale.

**Explicitly out of scope in docs (not built):**
- Forgot password / email verification
- Family join / invite flow

---

## 3. Feature Modules — Completeness

### COMPLETE (working with real backend/data)

#### Splash (`features/splashscreen/`)
- Checks signed-in user from DataStore/Auth
- Routes to Home or Login
- Calls `syncUserProfile()` on cold start when session exists

#### Auth (`features/auth/`)
- Login + Register screens with validation
- Google Sign-In via `GoogleSignInTokenProvider`
- Email login/register wired to `UserRepository`
- Loading states per method (Google vs Email)
- Error mapping to user-facing messages
- Navigation to Home on success

#### Core auth/data pipeline (`core/data/`, `core/datastore/`, `core/domain/`)
- `User` model with `currency`, `familyId`, `familyRole`
- `UserRepository` + `UserRepositoryImpl`: Google/email auth, Firestore upsert/get, DataStore persist, rollback on failure, sign-out, **`syncUserProfile()` implemented**
- `FirestoreNetworkDataSource`: real upsert/get for `/users/{uid}`
- `AuthNetworkDataSourceImpl`: Firebase Auth operations
- Proto DataStore (`signed_in_user.proto`) for local session cache

#### Navigation shell (`app/.../navigation/`)
- Top-level `KeuTrackNavHost`: Splash → Auth → Home → Transaction route
- `HomeShell`: bottom nav (Dashboard | Family | Settings) with nested NavHost
- Sign-out from Settings resets stack to Login

#### Design system (`core/designsystem/`)
- Theme, typography, colors, buttons, cards, text fields, bottom nav, modal bottom sheet, profile image

---

### PARTIALLY COMPLETE (UI done, data/actions stubbed)

#### Dashboard (`features/dashboard/`)
**UI:** Rich Compose screen — wallet cards, income/expense stats, recent transactions, FAB, new-entry bottom sheet with numpad/categories.

**Data:** Almost entirely **mock** via `DefaultDashboardMockContent` in `DashboardMockUi.kt`. Only real data injected:
- User first name (from `User.displayName` or email)
- Avatar URL (`User.photoUrl`)

**ViewModel:** Observes `UserRepository.getCurrentUser()` but financial data is not connected. `fetchRoute()` calls a leftover `RouteRepository` hitting a **SMPOB-Mobile API** (`RouteServices.kt`) — unrelated boilerplate, logged only.

**Actions not wired:**
- `onSettingsClick = {}`
- `onViewAllTransactions = {}`
- FAB opens in-dashboard bottom sheet, **not** `navigateToTransaction()`
- “Add transaction” button only dismisses sheet — **no persistence**

#### Settings (`features/settings/`)
**UI:** Full settings screen — profile, family network, currency picker, connected wallets, Google Sheets card, sign out.

**Real data wired:**
- Profile display name, email, avatar from `UserRepository`
- Sign out via `UserRepository.signOut()` with loading/error states
- `syncUserProfile()` on screen init

**Still mock / no-op:**
- Family ID (`KEU-992-KRT`) — **not** from `User.familyId`
- Currency selection — **not** persisted to Firestore
- Connected wallets — hardcoded mock amounts
- Google Sheets toggle/export — UI only
- `onCopyFamilyId`, `onInviteMember`, `onManageCircle` — all `{}`

#### Family (`features/family/`)
**UI:** Polished “Family Insights” screen — breakdown chart, shared budgets, history log, saving-together card, FAB.

**Data:** 100% **mock** via `DefaultFamilyInsightsMockContent` (uses `$` amounts, not IDR).

**ViewModel:** Empty shell — `FamilyViewModel` has no dependencies or logic. Comment in `FamilyRouting`: “Reserved for binding family insights UI state from viewModel.”

**Missing vs development plan:** No invite flow, QR code, or family group management screens described in Phase 6.

---

### INCOMPLETE / SCAFFOLD ONLY

#### Transaction (`features/transaction/`)
Only 2 Kotlin files beyond build config:
- `NewEntryScreen.kt` — centered placeholder text `"New Transaction"`
- `TransactionNavigation.kt` — route registered in top-level NavHost

**Missing:** ViewModel, domain integration, form UI (the real new-entry UI lives in dashboard’s `NewEntryBottomSheetContent.kt`), save logic, navigation from dashboard FAB.

**Navigation gap:** `navigateToTransaction()` exists but is **never called** anywhere in the codebase.

---

## 4. Domain Layer (`core/domain/`)

**Exists:**
- `User`, `AuthResult`, `TokenResult`
- `UserRepository` (auth + profile sync)
- `SyncRepository` — interface only: `syncPendingTransactions()` with comment referencing data design; **no implementation**
- `SignInWithGoogleUseCase` — defined but **unused** (ViewModels call repository directly)

**Missing (per plans):**
- `Transaction`, `Wallet`, `Category` entities
- `TransactionType`, `WalletType` enums
- `TransactionRepository`, `WalletRepository`
- Use cases: `GetTransactionsUseCase`, `AddTransactionUseCase`, `GetWalletSummaryUseCase`
- Use case base classes

---

## 5. Data Layer (`core/data/`)

**Exists:**
- `UserRepositoryImpl` (only production repository)
- Data sources: `AuthNetworkDataSourceImpl`, `FirestoreNetworkDataSource`, `UserProfileLocalDataStore`
- DI modules: Firebase, mappers, repositories, data sources

**Missing (per plans and data design):**
- Room database entirely — **no `@Entity`, `@Dao`, `AppDatabase`, or Room dependency usage**
- `TransactionRepositoryImpl`, `WalletRepositoryImpl`
- `SyncRepository` implementation
- WorkManager sync workers
- Firestore data sources for wallets, transactions, budgets, categories, family groups

**Dashboard anomaly:** `RouteRepository` / `RouteRepositoryImpl` / `RouteServices` are template code from another project, not KeuTrack financial APIs.

---

## 6. Navigation Architecture

```
SplashRoute (start)
  ├── authenticated → HomeRoute (HomeShell)
  │     ├── DashboardRoute
  │     ├── FamilyRoute
  │     └── SettingsRoute → sign out → LoginRoute
  └── unauthenticated → LoginRoute ↔ RegisterRoute

TransactionRoute (top-level, reachable but unused)
```

**Working:** Splash gating, auth flow, bottom-nav tab switching with state save/restore, sign-out stack reset.

**Not wired:**
- Dashboard FAB → `TransactionRoute` (uses local bottom sheet instead)
- Settings top-bar settings icon on dashboard
- “View All” transactions
- Family FAB
- Any deep links or nested feature navigation beyond tabs

---

## 7. TODO Comments in Codebase

**Kotlin:** No `TODO`, `FIXME`, or `HACK` comments found in `.kt` files.

**Non-Kotlin:**
- `app/src/main/res/xml/data_extraction_rules.xml` — Android Studio template TODO for backup rules
- `docs/firebase/PR_D_SYNC_USER_PROFILE.md` and `FIRESTORE_LOGIN_INTEGRATION.md` — document `syncUserProfile` as TODO (code is now implemented)

**Implicit TODOs in code (comments, not tagged):**
- `FamilyRouting`: “Reserved for binding family insights UI state from viewModel”
- `RouteServices.kt`: “currently this used as example, please update…”

---

## 8. Tests

**Zero test files** found (`*Test*.kt` glob returned nothing). Phase 9 testing from the development plan is not started.

---

## 9. Key Missing Functionality (Priority Order)

1. **Transaction domain + persistence** — Room entities, repositories, use cases, save from new-entry UI
2. **Wallet & balance model** — personal/family wallets, balance updates on transaction write
3. **Dashboard real data** — replace mock content with use case-driven state; remove SMPOB route boilerplate
4. **Transaction module** — move/build full screen, ViewModel, wire FAB or bottom sheet save to repository
5. **Offline-first sync** — `SyncRepository`, WorkManager, Firestore batch writes per data design
6. **Family groups** — Firestore `/family_groups`, invite/join flow, real insights from category summaries
7. **Settings persistence** — family ID from `User.familyId`, currency to Firestore, wallet list from backend
8. **Budgets & categories** — Firestore collections + UI binding on Family tab
9. **Google Sheets integration** — currently UI-only toggle
10. **Forgot password / email verification** — documented as future
11. **Unit/integration tests** — none exist

---

## 10. Phase Completion vs Development Plan

| Phase | Plan | Actual status |
|-------|------|---------------|
| 0 — Build plugin | Gradle conventions, modules | **Done** |
| 1 — Domain entities/use cases | Transaction, Wallet, repos, use cases | **~10%** — only `User` + auth |
| 2 — Room + data repos | AppDatabase, DAOs, impls | **~5%** — DataStore for user only; no Room |
| 3 — Design system | Theme + components | **~90%** — implemented in `core/designsystem` |
| 4 — Dashboard | Real data + FAB → transaction | **~50%** — UI done, data mocked |
| 5 — Transaction | Full new-entry flow | **~15%** — UI prototype in dashboard bottom sheet; module is placeholder |
| 6 — Family | Invite/QR flow | **~40%** — different UI (insights) with mock data |
| 7 — Settings | Full settings | **~60%** — UI + sign out; rest mocked |
| 8 — App navigation | NavHost + bottom nav | **~85%** — working; transaction route orphaned |
| 9 — Polish & tests | Previews, tests, lint | **~20%** — some `@Preview`s; no tests |

**Recent git work (auth/Firestore track) is complete.** The next major arc per your own plans is **Phase 1–2 (domain + Room)** followed by **transaction write path**, which unlocks dashboard, family insights, and settings with real data.