# Phase 9 — Testing & Polish (Unit Tests + Lint + Preview Coverage)

> **Target modules:** All modules (`:core:domain`, `:core:data`, `:features:*`, `:app`)  
> **Estimate:** ~4–6 days (iterative; can be done in priority tiers)  
> **Prerequisites:** Phase 0–8 ✅ · All features functionally complete  
> **Baseline status:** Zero test files (`*Test*.kt`) · Some `@Preview` coverage · No lint pass done  
> **End state:** Critical-path unit tests for domain use cases, repository layer, and ViewModel flows; lint clean; full Preview coverage for light/dark themes

---

## Table of Contents

1. [Context & Goals](#1-context--goals)
2. [Inventory — Current Test State](#2-inventory--current-test-state)
3. [Test Strategy & Priorities](#3-test-strategy--priorities)
4. [Scope — What to Do](#4-scope--what-to-do)
5. [Scope — What NOT to Do](#5-scope--what-not-to-do)
6. [Prerequisites (Definition of Ready)](#6-prerequisites-definition-of-ready)
7. [Infrastructure Setup](#7-infrastructure-setup)
8. [Test Plan — Domain Use Cases (Tier 1)](#8-test-plan--domain-use-cases-tier-1)
9. [Test Plan — Repository Layer (Tier 2)](#9-test-plan--repository-layer-tier-2)
10. [Test Plan — ViewModel Flows (Tier 3)](#10-test-plan--viewmodel-flows-tier-3)
11. [Test Plan — Mapper & Utility (Tier 4)](#11-test-plan--mapper--utility-tier-4)
12. [Polish — Lint & Preview (Tier 5)](#12-polish--lint--preview-tier-5)
13. [Files NOT to Modify](#13-files-not-to-modify)
14. [Files to Create / Modify](#14-files-to-create--modify)
15. [Target Directory Structure](#15-target-directory-structure)
16. [Architecture Notes & Conventions](#16-architecture-notes--conventions)
17. [Acceptance Criteria](#17-acceptance-criteria)
18. [Risk & Mitigation](#18-risk--mitigation)
19. [Suggested Execution Order](#19-suggested-execution-order)
20. [Relation to Other Phases](#20-relation-to-other-phases)

---

## 1. Context & Goals

Per `Project_Assessment_Current.md`:

| Aspect | Current state |
|--------|--------------|
| Production test files | **Zero** |
| Test directories (`test/`, `androidTest/`) | **Do not exist** |
| Testing dependencies in `libs.versions.toml` | Minimal: `junit`, `espresso-core`, `ui-test-junit4` |
| Missing critical test deps | `mockk`, `turbine`, `kotlinx-coroutines-test`, `truth` |
| `@Preview` coverage | Partial — most screens have light/dark but not all |
| Lint pass | Never run systematically |

**Phase 9 goals:**
1. Set up test infrastructure (dependencies, convention plugin support, shared test utilities)
2. Write unit tests for all domain use cases (highest ROI — pure Kotlin, no Android deps)
3. Write unit tests for critical repository implementations (mock data sources)
4. Write unit tests for ViewModel flows (test state emissions)
5. Write tests for mappers and utility functions
6. Run lint pass and fix errors/warnings
7. Ensure all screens have `@Preview` (light + dark)

**Not Phase 9 goals:**
- UI/instrumented tests (Espresso, Compose UI tests) — future
- Screenshot tests (Paparazzi) — future
- E2E tests — future
- Integration tests with real Firebase — future
- Code coverage tooling (JaCoCo) — future
- CI/CD pipeline setup — future

---

## 2. Inventory — Current Test State

### Modules and their testable components

| Module | Testable units | Priority |
|--------|---------------|----------|
| `:core:domain` | 12 use cases, 7 repository interfaces (contract verification) | **Tier 1** |
| `:core:data` | 7 repository impls, 7 mappers, data sources | **Tier 2** |
| `:features:dashboard` | `DashboardViewModel`, `DashboardUiMapper` | **Tier 3** |
| `:features:settings` | `SettingsViewModel`, `SettingsUiMapper` | **Tier 3** |
| `:features:family` | `FamilyViewModel`, `FamilyUiMapper` | **Tier 3** |
| `:features:transaction` | `NewEntryViewModel`, `TransactionHistoryViewModel` | **Tier 3** |
| `:features:auth` | `LoginViewModel`, `RegisterViewModel` | **Tier 3** |
| `:core:designsystem` | `CurrencyFormat` utility | **Tier 4** |
| `:core:common` | `CommonDispatcher`, utilities | **Tier 4** |

### Existing dependencies available

```toml
# Already in libs.versions.toml:
junit = "4.13.2"
androidx-junit = "1.3.0"
espresso-core = "3.7.0"
androidx-ui-test-junit4 (Compose)
androidx-ui-test-manifest (Compose)
```

### Dependencies to add

```toml
# Required for Phase 9:
mockk = "1.13.x"
kotlinx-coroutines-test = "1.8.x"  # matches project coroutines version
turbine = "1.2.x"
truth = "1.4.x"  # or assertk
```

---

## 3. Test Strategy & Priorities

### Testing pyramid for KeuTrack

```
         ┌─────────────────┐
         │   UI Tests      │  ← Future (Compose Test / Paparazzi)
         │   (deferred)    │
         ├─────────────────┤
         │  ViewModel      │  ← Tier 3: test state emissions via Turbine
         │  Integration    │
         ├─────────────────┤
         │  Repository     │  ← Tier 2: mock data sources, verify orchestration
         │  Unit Tests     │
         ├─────────────────┤
         │  Domain         │  ← Tier 1: pure Kotlin, highest ROI
         │  Use Cases      │
         └─────────────────┘
```

### Priority rationale

| Tier | Why first? |
|------|-----------|
| **Tier 1 — Domain** | Pure Kotlin; no Android deps; validates business rules (amount > 0, name length, etc.); fast execution |
| **Tier 2 — Repository** | Validates orchestration (Firestore → DataStore, error handling, CancellationException propagation) |
| **Tier 3 — ViewModel** | Validates UI state correctness; tests `combine` flows and action handling |
| **Tier 4 — Mapper/Utility** | Simple mapping logic; catches formatting bugs |
| **Tier 5 — Polish** | Lint and Preview are quality gates, not correctness tests |

---

## 4. Scope — What to Do

### A. Infrastructure (one-time setup)

| # | Item |
|---|------|
| 1 | Add test dependencies to `libs.versions.toml` (`mockk`, `coroutines-test`, `turbine`, `truth`) |
| 2 | Create test bundles in version catalog: `[bundles]` section for grouping |
| 3 | Add `testImplementation` lines to convention plugins or individual `build.gradle.kts` |
| 4 | Create shared test utility: `TestDispatcher` rule / extension for coroutine tests |
| 5 | Create `test/` directories in target modules |

### B. Domain Use Case Tests (Tier 1)

| # | Use Case | Key test scenarios |
|---|----------|--------------------|
| 6 | `AddTransactionUseCase` | Amount <= 0 fails; blank walletId fails; blank categoryId fails; success delegates to repo |
| 7 | `CreateFamilyGroupUseCase` | Name too short; name too long; user already in family; user not logged in; success creates + wallet |
| 8 | `JoinFamilyGroupUseCase` | Invalid code; user already in family; success joins + updates membership |
| 9 | `LeaveFamilyGroupUseCase` | Not in family; owner with members blocked; owner sole → deletes; member → removes |
| 10 | `GetWalletSummaryUseCase` | Returns combined personal + family wallets |
| 11 | `GetTransactionsUseCase` | Passes params to repo; returns flow |
| 12 | `GetMonthlySummaryUseCase` | Month format validation; returns summary |
| 13 | `GetCategoriesUseCase` | Filters by type |
| 14 | `GetBudgetProgressUseCase` | Returns budget with progress calculation |
| 15 | `RetryPendingSyncUseCase` | Delegates to sync repo |
| 16 | `SyncFamilyDataUseCase` | Calls pull sync |
| 17 | `SignInWithGoogleUseCase` | Delegates to repo |

### C. Repository Tests (Tier 2)

| # | Repository | Key test scenarios |
|---|------------|--------------------|
| 18 | `UserRepositoryImpl` | `signInWithGoogle` success/failure; `syncUserProfile` preserves local currency/membership; `signOut` clears; `updateFamilyMembership` orchestration |
| 19 | `TransactionRepositoryImpl` | `addTransaction` writes to local; `observeTransactions` emits from local source |
| 20 | `WalletRepositoryImpl` | `createWallet` writes; `observeWallets` emits; `observeWalletsByType` filters |
| 21 | `FamilyRepositoryImpl` | `createFamily` calls Firestore; `observeCurrentFamily` emits |
| 22 | `SyncRepositoryImpl` | Pending sync detection; retry logic |
| 23 | `CategoryRepositoryImpl` | Observe categories; filter by type |
| 24 | `BudgetRepositoryImpl` | Observe budgets; monthly summary |

### D. ViewModel Tests (Tier 3)

| # | ViewModel | Key test scenarios |
|---|----------|--------------------|
| 25 | `DashboardViewModel` | Initial loading state; combine emits correct UIState; save transaction success/error |
| 26 | `SettingsViewModel` | Combine emits profile + wallets; leave family updates state; sign out flow |
| 27 | `FamilyViewModel` | Family + user combine; insights mapping |
| 28 | `LoginViewModel` | Email validation; loading state; error mapping |
| 29 | `RegisterViewModel` | Validation rules; success navigates |
| 30 | `NewEntryViewModel` | Amount validation; category selection; save flow |
| 31 | `TransactionHistoryViewModel` | List loading; empty state |

### E. Mapper & Utility Tests (Tier 4)

| # | Target | Key test scenarios |
|---|--------|--------------------|
| 32 | `SettingsUiMapper` | Greeting extraction; wallet mapping; family role label |
| 33 | `DashboardUiMapper` | Transaction → row UI; wallet → balance label |
| 34 | `FamilyUiMapper` | Breakdown calculation; IDR formatting |
| 35 | `CurrencyFormat` | Zero amount; large numbers; negative (if supported) |
| 36 | `TransactionMapper` (data) | Entity ↔ domain round-trip |
| 37 | `WalletMapper` (data) | Entity ↔ domain round-trip |
| 38 | `CategoryMapper` (data) | Entity ↔ domain round-trip |
| 39 | `SignedInUserProtoMapper` | Proto ↔ domain; preserves all fields |
| 40 | `AuthUserMapper` | FirebaseUser → domain User |

### F. Lint & Preview (Tier 5)

| # | Item |
|---|------|
| 41 | Run `./gradlew lint` on all modules; fix errors |
| 42 | Fix lint warnings that are easy (unused imports, accessibility) |
| 43 | Verify all `@Preview` functions compile and render (light + dark) |
| 44 | Add missing `@Preview` to screens that lack them |
| 45 | Ensure `@Preview` uses `KeuTrackTheme` (not bare `MaterialTheme`) |

---

## 5. Scope — What NOT to Do

| Item | Reason | Deferred to |
|------|--------|-------------|
| UI / Instrumented tests (Espresso, Compose Test Rule) | Requires device/emulator; slower ROI | Future |
| Screenshot tests (Paparazzi) | Infra setup cost; nice-to-have | Future |
| E2E tests with real Firebase | Requires test project setup | Future |
| Code coverage tooling (JaCoCo) | Metrics after tests exist | Future |
| CI/CD pipeline (GitHub Actions) | Separate infra task | Future |
| Performance / benchmark tests | Not critical yet | Future |
| Refactoring production code for testability | Only minimal changes if truly needed | — |
| Feature logic changes | Freeze production behavior | — |
| New features | Out of scope | — |

---

## 6. Prerequisites (Definition of Ready)

- [x] All features functionally complete (Phases 0–8)
- [x] `./gradlew assembleDevDebug` passes
- [ ] Decision: which assertion library (`truth` vs `assertk` vs plain JUnit assertions)
- [ ] Decision: mock framework (`mockk` — recommended for Kotlin)

```bash
# Sanity before starting
./gradlew assembleDevDebug
```

---

## 7. Infrastructure Setup

### 7.1 Dependencies to add in `gradle/libs.versions.toml`

```toml
[versions]
mockk = "1.13.16"
coroutinesTest = "1.8.1"  # must match project kotlinx-coroutines version
turbine = "1.2.0"
truth = "1.4.4"

[libraries]
test-mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
test-coroutines = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
test-turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
test-truth = { group = "com.google.truth", name = "truth", version.ref = "truth" }

[bundles]
unit-test = ["junit", "test-mockk", "test-coroutines", "test-turbine", "test-truth"]
```

### 7.2 Convention plugin or per-module `build.gradle.kts`

Option A — Add to convention plugin (recommended for consistency):

```kotlin
// In KeuTrackLibPlugin or a new KeuTrackTestPlugin
dependencies {
    add("testImplementation", libs.findBundle("unit-test").get())
}
```

Option B — Per-module (if convention plugin change is undesirable):

```kotlin
// In each module's build.gradle.kts
dependencies {
    testImplementation(libs.bundles.unit.test)
}
```

### 7.3 Shared Test Utility — `MainDispatcherRule`

Create in `:core:common` test source set (or a new `:core:testing` module if preferred):

```kotlin
package com.mascill.keutrack.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

### 7.4 Test `CommonDispatcher` Implementation

```kotlin
package com.mascill.keutrack.core.testing

import com.mascill.keutrack.core.common.utils.CommonDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher

class TestCommonDispatcher(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : CommonDispatcher {
    override val io: CoroutineDispatcher get() = testDispatcher
    override val main: CoroutineDispatcher get() = testDispatcher
    override val default: CoroutineDispatcher get() = testDispatcher
}
```

---

## 8. Test Plan — Domain Use Cases (Tier 1)

### `AddTransactionUseCaseTest`

```kotlin
class AddTransactionUseCaseTest {
    private val repo = mockk<TransactionRepository>()
    private val useCase = AddTransactionUseCase(repo)

    @Test fun `amount zero returns failure`()
    @Test fun `amount negative returns failure`()
    @Test fun `blank walletId returns failure`()
    @Test fun `blank categoryId returns failure`()
    @Test fun `valid transaction delegates to repository`()
    @Test fun `repository exception returns failure`()
    @Test fun `CancellationException is rethrown`()
}
```

### `CreateFamilyGroupUseCaseTest`

```kotlin
class CreateFamilyGroupUseCaseTest {
    private val familyRepo = mockk<FamilyRepository>()
    private val userRepo = mockk<UserRepository>()
    private val walletRepo = mockk<WalletRepository>()
    private val syncRepo = mockk<SyncRepository>()
    private val useCase = CreateFamilyGroupUseCase(familyRepo, userRepo, walletRepo, syncRepo)

    @Test fun `name shorter than 2 chars returns failure`()
    @Test fun `name longer than 40 chars returns failure`()
    @Test fun `user not logged in returns failure`()
    @Test fun `user already in family returns failure`()
    @Test fun `success creates family, updates membership, ensures wallet`()
    @Test fun `existing family wallet is not duplicated`()
    @Test fun `sync failure falls back to enqueue`()
}
```

### `JoinFamilyGroupUseCaseTest`

```kotlin
class JoinFamilyGroupUseCaseTest {
    @Test fun `blank invite code returns failure`()
    @Test fun `user already in family returns failure`()
    @Test fun `invalid code (family not found) returns failure`()
    @Test fun `success joins family and updates membership`()
    @Test fun `pulls canonical family wallet after join`()
}
```

### `LeaveFamilyGroupUseCaseTest`

```kotlin
class LeaveFamilyGroupUseCaseTest {
    @Test fun `user not logged in returns failure`()
    @Test fun `user not in family returns failure`()
    @Test fun `owner with other members is blocked`()
    @Test fun `sole owner deletes family group`()
    @Test fun `member removes self from memberIds`()
    @Test fun `clears local family wallets`()
    @Test fun `clears user familyId and familyRole`()
}
```

### `GetWalletSummaryUseCaseTest`

```kotlin
class GetWalletSummaryUseCaseTest {
    @Test fun `returns personal and family wallets`()
    @Test fun `empty wallets returns empty summary`()
    @Test fun `calculates total balances correctly`()
}
```

### Other Use Cases (lighter tests)

| Use Case | Key assertions |
|----------|---------------|
| `GetTransactionsUseCase` | Passes limit param; returns repo flow |
| `GetMonthlySummaryUseCase` | Returns monthly totals |
| `GetCategoriesUseCase` | Filters by income/expense type |
| `GetBudgetProgressUseCase` | Calculates remaining correctly |
| `RetryPendingSyncUseCase` | Calls syncRepository |
| `SyncFamilyDataUseCase` | Calls family pull sync |
| `SignInWithGoogleUseCase` | Delegates to user repo |

---

## 9. Test Plan — Repository Layer (Tier 2)

### `UserRepositoryImplTest`

```kotlin
class UserRepositoryImplTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val authDS = mockk<AuthNetworkDataSource>()
    private val firestoreDS = mockk<FirestoreNetworkDataSource>()
    private val localDS = mockk<UserProfileLocalDataSource>()
    private val mapper = mockk<AuthUserMapper>()
    private val repo = UserRepositoryImpl(authDS, firestoreDS, mapper, localDS)

    // getCurrentUser
    @Test fun `getCurrentUser emits from local data source`()
    @Test fun `getCurrentUser refreshes identity from Auth on start`()
    @Test fun `getCurrentUser preserves local currency and membership`()

    // signInWithGoogle
    @Test fun `signInWithGoogle success persists user`()
    @Test fun `signInWithGoogle network error returns Network`()
    @Test fun `signInWithGoogle null user returns UserNotFound`()

    // syncUserProfile
    @Test fun `syncUserProfile upserts then resolves from Firestore`()
    @Test fun `syncUserProfile failure does not sign out`()

    // signOut
    @Test fun `signOut clears local and signs out auth`()

    // updateFamilyMembership
    @Test fun `updateFamilyMembership writes Firestore then persists local`()
    @Test fun `updateFamilyMembership failure returns Result failure`()
}
```

### Other Repositories (pattern)

| Repository | Key assertions |
|------------|---------------|
| `TransactionRepositoryImpl` | `addTransaction` inserts entity via DAO; observe emits mapped domain |
| `WalletRepositoryImpl` | Create writes DAO; observe by type filters correctly |
| `FamilyRepositoryImpl` | `createFamily` calls Firestore DS; observe emits from local |
| `SyncRepositoryImpl` | Detects pending items; retry calls Firestore DS |
| `CategoryRepositoryImpl` | Seed categories available; filter works |
| `BudgetRepositoryImpl` | Monthly summary aggregation |

---

## 10. Test Plan — ViewModel Flows (Tier 3)

### Testing pattern with Turbine

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val userRepo = mockk<UserRepository>()
    private val getWalletSummary = mockk<GetWalletSummaryUseCase>()
    // ... other mocks

    @Test
    fun `initial state is loading`() = runTest {
        val vm = createViewModel()
        vm.uiState.test {
            val first = awaitItem()
            assertThat(first.isLoading).isTrue()
        }
    }

    @Test
    fun `successful data load emits content state`() = runTest {
        // Setup mocks to emit data
        val vm = createViewModel()
        vm.uiState.test {
            skipItems(1) // skip loading
            val content = awaitItem()
            assertThat(content.isLoading).isFalse()
            assertThat(content.recentTransactions).isNotEmpty()
        }
    }
}
```

### ViewModels to test

| ViewModel | Priority scenarios |
|-----------|--------------------|
| `DashboardViewModel` | Loading → content; save transaction success/error; combine emits correct state |
| `SettingsViewModel` | Profile + wallets combine; leave family; sign out; sheets coming soon |
| `FamilyViewModel` | Family data combine; insights mapping |
| `LoginViewModel` | Validation errors; auth error mapping; success state |
| `RegisterViewModel` | Validation; duplicate email error; success |
| `NewEntryViewModel` | Amount/category validation; save success; wallet not found |
| `TransactionHistoryViewModel` | Loading; empty list; populated list |

---

## 11. Test Plan — Mapper & Utility (Tier 4)

### `SettingsUiMapperTest`

```kotlin
class SettingsUiMapperTest {
    @Test fun `greetingFirstName extracts first word of displayName`()
    @Test fun `greetingFirstName falls back to email local part`()
    @Test fun `greetingFirstName returns fallback when both empty`()
    @Test fun `mapConnectedWallets maps personal wallet correctly`()
    @Test fun `mapConnectedWallets maps family wallet with accent`()
    @Test fun `mapConnectedWallets handles empty summary`()
    @Test fun `from sets familyNetworkActive when familyId present`()
    @Test fun `from sets empty family code when not in family`()
}
```

### `CurrencyFormatTest`

```kotlin
class CurrencyFormatTest {
    @Test fun `formatIdr formats zero correctly`()
    @Test fun `formatIdr formats thousands with dot separator`()
    @Test fun `formatIdr formats millions correctly`()
    @Test fun `formatIdr handles large numbers`()
}
```

### Data layer mappers

| Mapper | Test focus |
|--------|-----------|
| `TransactionMapper` | Entity → Domain preserves all fields; Domain → Entity round-trip |
| `WalletMapper` | Type enum mapping; balance Long preservation |
| `CategoryMapper` | Type enum; icon string |
| `BudgetMapper` | Period parsing; limit/spent |
| `SignedInUserProtoMapper` | All User fields; null handling |
| `AuthUserMapper` | FirebaseUser with missing fields; null user |

---

## 12. Polish — Lint & Preview (Tier 5)

### Lint

```bash
# Run lint on all modules
./gradlew lint

# Or per module for faster feedback:
./gradlew :core:domain:lint
./gradlew :core:data:lint
./gradlew :features:dashboard:lint
./gradlew :features:settings:lint
# etc.
```

**Fix priority:**
1. Errors (must fix)
2. Warnings: unused imports, missing content descriptions, hardcoded strings
3. Informational: optional

### Preview audit

| Screen | Expected previews |
|--------|-------------------|
| `DashboardScreen` | Light + Dark |
| `SettingsScreen` | Light + Dark ✅ (exists) |
| `FamilyScreen` | Light + Dark |
| `LoginScreen` | Light + Dark |
| `RegisterScreen` | Light + Dark |
| `TransactionScreen` (NewEntry) | Light + Dark |
| `TransactionHistoryScreen` | Light + Dark |
| `SplashScreen` | Light + Dark |

All previews must use `KeuTrackTheme(darkTheme = ...)` wrapper.

---

## 13. Files NOT to Modify

| File / Area | Reason |
|-------------|--------|
| Production feature logic | Tests verify existing behavior — don't change it |
| `core/datastore/**/*.proto` | Freeze |
| `build-plugin/**` (logic) | Only additive test config if needed |
| `google-services.json`, `local.properties` | Secrets |
| Auth / splash production code | Complete |
| Navigation logic | Phase 8 |

**Exception:** Minimal production changes allowed only if:
- A class needs to be made `open` or an interface extracted for mocking
- A dependency needs to be injectable (should already be the case with Hilt)

---

## 14. Files to Create / Modify

### Infrastructure (modify)

| Action | File | Change |
|--------|------|--------|
| UPDATE | `gradle/libs.versions.toml` | Add test versions + libraries + bundle |
| UPDATE | Convention plugin or module `build.gradle.kts` | Add `testImplementation` |

### Test utilities (create)

| Action | File |
|--------|------|
| CREATE | `core/common/src/test/kotlin/.../testing/MainDispatcherRule.kt` |
| CREATE | `core/common/src/test/kotlin/.../testing/TestCommonDispatcher.kt` |

### Domain tests (create)

| File |
|------|
| `core/domain/src/test/kotlin/.../usecase/AddTransactionUseCaseTest.kt` |
| `core/domain/src/test/kotlin/.../usecase/CreateFamilyGroupUseCaseTest.kt` |
| `core/domain/src/test/kotlin/.../usecase/JoinFamilyGroupUseCaseTest.kt` |
| `core/domain/src/test/kotlin/.../usecase/LeaveFamilyGroupUseCaseTest.kt` |
| `core/domain/src/test/kotlin/.../usecase/GetWalletSummaryUseCaseTest.kt` |
| `core/domain/src/test/kotlin/.../usecase/GetTransactionsUseCaseTest.kt` |
| `core/domain/src/test/kotlin/.../usecase/GetCategoriesUseCaseTest.kt` |
| `core/domain/src/test/kotlin/.../usecase/GetMonthlySummaryUseCaseTest.kt` |
| `core/domain/src/test/kotlin/.../usecase/GetBudgetProgressUseCaseTest.kt` |
| `core/domain/src/test/kotlin/.../usecase/RetryPendingSyncUseCaseTest.kt` |
| `core/domain/src/test/kotlin/.../usecase/SyncFamilyDataUseCaseTest.kt` |
| `core/domain/src/test/kotlin/.../usecase/SignInWithGoogleUseCaseTest.kt` |

### Data tests (create)

| File |
|------|
| `core/data/src/test/kotlin/.../repository/UserRepositoryImplTest.kt` |
| `core/data/src/test/kotlin/.../repository/TransactionRepositoryImplTest.kt` |
| `core/data/src/test/kotlin/.../repository/WalletRepositoryImplTest.kt` |
| `core/data/src/test/kotlin/.../repository/FamilyRepositoryImplTest.kt` |
| `core/data/src/test/kotlin/.../repository/SyncRepositoryImplTest.kt` |
| `core/data/src/test/kotlin/.../repository/CategoryRepositoryImplTest.kt` |
| `core/data/src/test/kotlin/.../repository/BudgetRepositoryImplTest.kt` |
| `core/data/src/test/kotlin/.../mapper/TransactionMapperTest.kt` |
| `core/data/src/test/kotlin/.../mapper/WalletMapperTest.kt` |
| `core/data/src/test/kotlin/.../mapper/CategoryMapperTest.kt` |
| `core/data/src/test/kotlin/.../mapper/SignedInUserProtoMapperTest.kt` |
| `core/data/src/test/kotlin/.../mapper/AuthUserMapperTest.kt` |

### Feature tests (create)

| File |
|------|
| `features/dashboard/src/test/kotlin/.../DashboardViewModelTest.kt` |
| `features/dashboard/src/test/kotlin/.../DashboardUiMapperTest.kt` |
| `features/settings/src/test/kotlin/.../SettingsViewModelTest.kt` |
| `features/settings/src/test/kotlin/.../SettingsUiMapperTest.kt` |
| `features/family/src/test/kotlin/.../FamilyViewModelTest.kt` |
| `features/auth/src/test/kotlin/.../LoginViewModelTest.kt` |
| `features/auth/src/test/kotlin/.../RegisterViewModelTest.kt` |
| `features/transaction/src/test/kotlin/.../NewEntryViewModelTest.kt` |
| `features/transaction/src/test/kotlin/.../TransactionHistoryViewModelTest.kt` |

### Design system tests (create)

| File |
|------|
| `core/designsystem/src/test/kotlin/.../format/CurrencyFormatTest.kt` |

---

## 15. Target Directory Structure

```
core/common/src/test/kotlin/com/mascill/keutrack/core/testing/
├── MainDispatcherRule.kt
└── TestCommonDispatcher.kt

core/domain/src/test/kotlin/com/mascill/keutrack/core/domain/usecase/
├── AddTransactionUseCaseTest.kt
├── CreateFamilyGroupUseCaseTest.kt
├── JoinFamilyGroupUseCaseTest.kt
├── LeaveFamilyGroupUseCaseTest.kt
├── GetWalletSummaryUseCaseTest.kt
├── GetTransactionsUseCaseTest.kt
├── GetCategoriesUseCaseTest.kt
├── GetMonthlySummaryUseCaseTest.kt
├── GetBudgetProgressUseCaseTest.kt
├── RetryPendingSyncUseCaseTest.kt
├── SyncFamilyDataUseCaseTest.kt
└── SignInWithGoogleUseCaseTest.kt

core/data/src/test/kotlin/com/mascill/keutrack/core/data/
├── repository/
│   ├── UserRepositoryImplTest.kt
│   ├── TransactionRepositoryImplTest.kt
│   ├── WalletRepositoryImplTest.kt
│   ├── FamilyRepositoryImplTest.kt
│   ├── SyncRepositoryImplTest.kt
│   ├── CategoryRepositoryImplTest.kt
│   └── BudgetRepositoryImplTest.kt
└── mapper/
    ├── TransactionMapperTest.kt
    ├── WalletMapperTest.kt
    ├── CategoryMapperTest.kt
    ├── SignedInUserProtoMapperTest.kt
    └── AuthUserMapperTest.kt

features/dashboard/src/test/kotlin/com/mascill/keutrack/feature/dashboard/
├── DashboardViewModelTest.kt
└── DashboardUiMapperTest.kt

features/settings/src/test/kotlin/com/mascill/keutrack/feature/settings/
├── SettingsViewModelTest.kt
└── SettingsUiMapperTest.kt

features/family/src/test/kotlin/com/mascill/keutrack/feature/family/
└── FamilyViewModelTest.kt

features/auth/src/test/kotlin/com/mascill/keutrack/feature/auth/
├── LoginViewModelTest.kt
└── RegisterViewModelTest.kt

features/transaction/src/test/kotlin/com/mascill/keutrack/feature/transaction/
├── NewEntryViewModelTest.kt
└── TransactionHistoryViewModelTest.kt

core/designsystem/src/test/kotlin/com/mascill/keutrack/core/designsystem/
└── format/CurrencyFormatTest.kt
```

---

## 16. Architecture Notes & Conventions

### Test naming convention

```kotlin
@Test fun `amount zero returns failure`()          // behavior-driven
@Test fun `success creates family and wallet`()    // describe outcome
```

Use backtick method names (Kotlin feature) for readability.

### MockK patterns

```kotlin
// Relaxed mock for Flows
private val walletRepo = mockk<WalletRepository> {
    every { observeWallets() } returns flowOf(emptyList())
}

// Suspend function mock
coEvery { transactionRepo.addTransaction(any()) } just Runs

// Verify
coVerify(exactly = 1) { transactionRepo.addTransaction(match { it.amount == 5000L }) }
```

### Coroutine test patterns

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class SomeUseCaseTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()

    @Test fun `some test`() = runTest {
        // arrange
        val useCase = SomeUseCase(mockRepo)
        // act
        val result = useCase(params)
        // assert
        assertThat(result.isSuccess).isTrue()
    }
}
```

### Flow testing with Turbine

```kotlin
@Test fun `emits loading then content`() = runTest {
    val vm = createViewModel()
    vm.uiState.test {
        assertThat(awaitItem().isLoading).isTrue()
        assertThat(awaitItem().isLoading).isFalse()
        cancelAndConsumeRemainingEvents()
    }
}
```

### Key rules

| Rule | Detail |
|------|--------|
| No Android framework in domain tests | Pure JVM — fastest execution |
| Mock interfaces, not classes | Repositories are interfaces; data sources should be mockable |
| Test one behavior per test | Clear failure messages |
| Don't test private methods directly | Test via public API |
| CancellationException must propagate | Verify it's rethrown (add specific test) |
| Avoid flaky timing | Use `advanceUntilIdle()` / Turbine's `awaitItem()` |

---

## 17. Acceptance Criteria

### Tier 1 — Domain (minimum viable)

- [ ] All 12 use cases have test files
- [ ] Each use case has at minimum: 1 success test + 1 validation failure test + 1 exception handling test
- [ ] `./gradlew :core:domain:test` passes green

### Tier 2 — Repository

- [ ] All 7 repository impls have test files
- [ ] `UserRepositoryImpl` has comprehensive tests (auth is critical path)
- [ ] `./gradlew :core:data:test` passes green

### Tier 3 — ViewModel

- [ ] Critical VMs tested: Dashboard, Settings, Login, NewEntry
- [ ] Each VM test verifies initial state + at least one action
- [ ] `./gradlew :features:dashboard:test :features:settings:test` pass green

### Tier 4 — Mapper/Utility

- [ ] `CurrencyFormat` tested
- [ ] `SettingsUiMapper` tested
- [ ] Data mappers have round-trip tests

### Tier 5 — Polish

- [ ] `./gradlew lint` has zero errors
- [ ] All screens have `@Preview` (light + dark)
- [ ] All previews use `KeuTrackTheme`

### Overall

- [ ] `./gradlew test` passes on all modules
- [ ] No production behavior changed
- [ ] Test count: minimum 80+ test methods across all tiers

### Verification Commands

```bash
# Run all unit tests
./gradlew test

# Per-tier verification
./gradlew :core:domain:test                           # Tier 1
./gradlew :core:data:test                             # Tier 2
./gradlew :features:dashboard:test :features:settings:test :features:auth:test  # Tier 3
./gradlew :core:designsystem:test                     # Tier 4
./gradlew lint                                        # Tier 5

# Build still passes
./gradlew assembleDevDebug
```

---

## 18. Risk & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| MockK + coroutines-test version conflicts | Compile failures | Pin versions; test in isolation first |
| `internal` visibility blocks testing | Can't instantiate class under test | Use `@VisibleForTesting` or test from same package; or make `internal` → `public` if safe |
| ViewModel tests flaky due to timing | Unreliable CI | Use Turbine `test {}` blocks; `advanceUntilIdle()` |
| Data layer tests need Android context (Room, Proto) | Can't run as JVM tests | Use `mockk` for data sources; skip Room-in-memory for now |
| Too many tests → scope creep | Phase 9 takes too long | Stick to tier priority; Tier 1 is the minimum |
| Changing production code for testability | Risk regressions | Only extract interface or add `open` modifier; never change logic |
| `FirebaseFirestore` hard to mock | Complex setup | Mock at data source level (interface), not Firestore directly |

---

## 19. Suggested Execution Order

```
Step 1: Infrastructure Setup (~0.5 day)
  ├── Add deps to libs.versions.toml
  ├── Configure testImplementation in build files
  ├── Create MainDispatcherRule + TestCommonDispatcher
  └── Verify: ./gradlew :core:domain:test (no tests yet, but compiles)

Step 2: Domain Use Case Tests — Tier 1 (~1.5 days)
  ├── AddTransactionUseCaseTest
  ├── CreateFamilyGroupUseCaseTest
  ├── JoinFamilyGroupUseCaseTest
  ├── LeaveFamilyGroupUseCaseTest
  ├── GetWalletSummaryUseCaseTest
  ├── Remaining use case tests (lighter)
  └── Verify: ./gradlew :core:domain:test ALL GREEN

Step 3: Repository Tests — Tier 2 (~1.5 days)
  ├── UserRepositoryImplTest (most critical)
  ├── TransactionRepositoryImplTest
  ├── WalletRepositoryImplTest
  ├── Remaining repo tests
  └── Verify: ./gradlew :core:data:test ALL GREEN

Step 4: ViewModel Tests — Tier 3 (~1 day)
  ├── DashboardViewModelTest
  ├── SettingsViewModelTest
  ├── LoginViewModelTest
  ├── NewEntryViewModelTest
  └── Verify: ./gradlew test on feature modules

Step 5: Mapper & Utility Tests — Tier 4 (~0.5 day)
  ├── CurrencyFormatTest
  ├── SettingsUiMapperTest / DashboardUiMapperTest
  ├── Data mapper tests
  └── Verify: all tests green

Step 6: Lint & Preview — Tier 5 (~0.5 day)
  ├── ./gradlew lint → fix errors
  ├── Audit @Preview coverage
  ├── Add missing previews
  └── Verify: lint clean, previews compile

Step 7: Final Verification
  ├── ./gradlew test (all modules)
  ├── ./gradlew lint
  ├── ./gradlew assembleDevDebug
  └── Report: total test count, coverage areas
```

---

## 20. Relation to Other Phases

| Phase | Relation |
|-------|----------|
| **1** | Domain use cases and repos — primary test targets |
| **2** | Repository impls and mappers — Tier 2 targets |
| **3** | `CurrencyFormat` — Tier 4 target |
| **4** | `DashboardViewModel` + mapper — Tier 3 target |
| **5** | Transaction VMs — Tier 3 target |
| **6** | Family VM + use cases — Tier 1 & 3 targets |
| **7** | Settings VM + mapper — Tier 3 target |
| **8** | Navigation — not tested in Phase 9 (future UI tests) |
| **Future** | UI tests, screenshot tests, CI/CD, coverage tools |

---

## Effort Estimate

| Tier | Share | Approx days |
|------|-------|-------------|
| Infrastructure setup | ~10% | 0.5 |
| Tier 1 — Domain use cases | ~30% | 1.5 |
| Tier 2 — Repository layer | ~25% | 1.5 |
| Tier 3 — ViewModels | ~20% | 1.0 |
| Tier 4 — Mappers/utilities | ~8% | 0.5 |
| Tier 5 — Lint/Preview | ~7% | 0.5 |
| **Total** | **100%** | **~5.5 days** |

---

## File Policy Summary

| Category | Policy |
|----------|--------|
| `gradle/libs.versions.toml` | ✅ Add test deps |
| Convention plugin / module build files | ✅ Add testImplementation |
| `*/src/test/kotlin/**` | ✅ Create all test files |
| `core/common/src/test/` | ✅ Shared test utilities |
| Production source code | ❌ Do not change (except minimal testability fixes) |
| `core/datastore/**/*.proto` | ❌ Freeze |
| `build-plugin/**` logic | ❌ Only additive test config |
| Navigation / UI code | ❌ Not tested in Phase 9 |

---

## Quick Reference — Test Template

```kotlin
package com.mascill.keutrack.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class ExampleUseCaseTest {

    private val repo = mockk<SomeRepository>(relaxed = true)
    private val useCase = ExampleUseCase(repo)

    @Test
    fun `valid input returns success`() = runTest {
        val result = useCase(validInput)
        assertThat(result.isSuccess).isTrue()
        coVerify { repo.doSomething(validInput) }
    }

    @Test
    fun `invalid input returns failure with message`() = runTest {
        val result = useCase(invalidInput)
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("expected error")
    }

    @Test
    fun `CancellationException propagates`() = runTest {
        coEvery { repo.doSomething(any()) } throws CancellationException()
        try {
            useCase(validInput)
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            // Expected
        }
    }
}
```

---

*This document is the implementation reference for Phase 9 KeuTrack. Start with infrastructure setup, then work through tiers in priority order. Tier 1 (domain use cases) alone provides significant confidence in business logic correctness. After Phase 9, the project has production-quality test coverage for its critical paths.*
