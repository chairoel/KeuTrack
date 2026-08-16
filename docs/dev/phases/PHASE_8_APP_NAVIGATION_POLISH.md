# Phase 8 — App Navigation Polish (Deep Links + Transitions + Edge Cases)

> **Target modules:** `:app` (navigation layer) · minimal additive in `:features:*` navigation packages  
> **Estimate:** ~1–1.5 days  
> **Prerequisites:** Phase 0–7 ✅ · Phase 8 core (NavHost + bottom nav + txn routes) already ~95% functional  
> **Baseline status:** All screens reachable; bottom nav works; sign-out resets stack; transaction routes wired from HomeShell  
> **End state:** Deep link support for transactions; smooth nav transitions; process death / config change resilience verified; no orphan back stack entries

---

## Table of Contents

1. [Context & Goals](#1-context--goals)
2. [Inventory — What Already Exists](#2-inventory--what-already-exists)
3. [Scope — What to Do](#3-scope--what-to-do)
4. [Scope — What NOT to Do](#4-scope--what-not-to-do)
5. [Prerequisites (Definition of Ready)](#5-prerequisites-definition-of-ready)
6. [Reference Files (Read-Only)](#6-reference-files-read-only)
7. [Files NOT to Modify](#7-files-not-to-modify)
8. [Files to Modify / Create](#8-files-to-modify--create)
9. [Design Decisions](#9-design-decisions)
10. [Task Breakdown](#10-task-breakdown)
11. [Acceptance Criteria](#11-acceptance-criteria)
12. [Architecture Notes](#12-architecture-notes)
13. [Risk & Mitigation](#13-risk--mitigation)
14. [Suggested Execution Order](#14-suggested-execution-order)
15. [Relation to Other Phases](#15-relation-to-other-phases)

---

## 1. Context & Goals

Per `Project_Assessment_Current.md`, Phase 8 is at **~95%**. The navigation graph is fully functional:

| Component | Status |
|-----------|--------|
| `KeuTrackNavHost` — top-level graph (Splash → Home / Auth) | ✅ |
| `HomeShell` — nested NavHost with bottom nav (Dashboard / Family / Settings) | ✅ |
| Transaction routes — wired from FAB / View All / Family | ✅ |
| Type-safe routes (`@Serializable`) | ✅ |
| Sign-out → LoginRoute stack reset | ✅ |
| `KeuTrackAppState` — nav helpers + snackbar | ✅ |

**What's missing for 100%:**

| Gap | Impact | Priority |
|-----|--------|----------|
| Deep links for notifications / external navigation | Cannot open specific transaction from push notification | Medium |
| Nav transition animations | Abrupt screen switches (default instant transition) | Low-Medium |
| Process death / config change resilience | Potential state loss on rotation or backgrounded kill | Medium |
| Keyboard / IME back gesture edge cases | Potential navigation confusion on gesture nav devices | Low |
| Back button behavior on nested NavHost tabs | Possible orphan entries if user rapidly switches tabs | Low |

**Phase 8 goals:**
- Add deep link URI patterns for transaction screens (future push notification support)
- Add enter/exit transition animations to nav graph
- Verify and fix process death state restoration
- Ensure gesture navigation (predictive back) works correctly
- Final navigation QA pass

**Not Phase 8 goals:**
- Push notification implementation (just the deep link URI pattern)
- UI redesign of any screen
- Feature logic changes
- Unit tests (Phase 9)

---

## 2. Inventory — What Already Exists

### Navigation infrastructure (`app/.../navigation/`)

| File | Role |
|------|------|
| `KeuTrackNavHost.kt` | Top-level NavHost: Splash → Home/Auth → Transaction |
| `HomeShell.kt` | Nested NavHost for bottom-nav tabs + Scaffold |
| `HomeRoute.kt` | `@Serializable object HomeRoute` + navigate extension |
| `HomeNavDestination.kt` | Enum mapping tabs → routes + nav items |
| `KeuTrackAppState.kt` | `navigateAndResetStack`, `onBackClick`, `showSnackBar` |

### Feature navigation packages

| File | Route type |
|------|------------|
| `features/splashscreen/.../SplashNavigation.kt` | `SplashRoute` (start destination) |
| `features/auth/.../AuthNavigation.kt` | `LoginRoute`, `RegisterRoute`, `authGraph()` |
| `features/dashboard/.../DashboardNavigation.kt` | `DashboardRoute`, `dashboardGraph(onSettingsClick, onAddTx, onViewAll)` |
| `features/family/.../FamilyNavigation.kt` | `FamilyRoute`, `familyGraph(onAddTx, onViewAll)` |
| `features/settings/.../SettingsNavigation.kt` | `SettingsRoute`, `settingsGraph(onSignOutSuccess)` |
| `features/transaction/.../TransactionNavigation.kt` | `TransactionRoute`, `TransactionHistoryRoute`, `transactionGraph(onBack, onAddTx)` |

### Compose Navigation version

Using `androidx.navigation:navigation-compose` with type-safe routes (`@Serializable` objects/classes).

---

## 3. Scope — What to Do

### A. Deep Link URI Patterns

| # | Item |
|---|------|
| 1 | Define deep link URI scheme: `keutrack://transaction/{transactionId}` |
| 2 | Add `deepLinks` parameter to `composable<TransactionRoute>` in `transactionGraph()` |
| 3 | Update `TransactionRoute` to accept optional `transactionId: String?` parameter |
| 4 | Add deep link URI for transaction history: `keutrack://transactions` |
| 5 | Register deep link intent filter in `AndroidManifest.xml` |
| 6 | Verify deep link via `adb shell am start -d "keutrack://transactions"` |

### B. Navigation Transitions

| # | Item |
|---|------|
| 7 | Add `enterTransition` / `exitTransition` to top-level `NavHost` (slide horizontal for push, slide back for pop) |
| 8 | Add `enterTransition` / `exitTransition` to nested `HomeNavHost` (crossfade for tab switches) |
| 9 | Transaction route: slide up from bottom (modal-style) for new entry; slide horizontal for history |
| 10 | Auth graph: fade transitions |
| 11 | Splash → Home/Auth: fade out splash |

### C. Process Death & State Restoration

| # | Item |
|---|------|
| 12 | Verify `rememberSaveable` is used for critical local state (bottom sheet open state, dialog mode) |
| 13 | Verify `SavedStateHandle` in ViewModels that hold navigation-relevant state |
| 14 | Test: open transaction form → rotate device → state preserved |
| 15 | Test: app in background → system kills process → reopen → correct screen |
| 16 | Ensure `HomeNavDestination` selected tab survives config change (already handled by NavController state save) |

### D. Back Navigation / Gesture Polish

| # | Item |
|---|------|
| 17 | Verify predictive back animation works with Navigation Compose |
| 18 | Ensure bottom sheet dismisses before nav back (no "stuck" sheet state) |
| 19 | Transaction screen back → returns to correct tab (not resets to Dashboard) |
| 20 | Double-back on Dashboard → exits app (or shows confirmation — product decision) |

### E. Edge Cases

| # | Item |
|---|------|
| 21 | Rapid tab switching: no duplicate destinations on back stack |
| 22 | Auth session expired while on Home: graceful redirect to Login |
| 23 | Sign-out during active transaction entry: no crash, clean navigation to Login |

---

## 4. Scope — What NOT to Do

| Item | Reason | Deferred to |
|------|--------|-------------|
| Push notification service (FCM) | Backend/infra not ready | Future |
| Edit/delete transaction navigation | UI not built | Future |
| Shared element transitions | Complex; not MVP | Future polish |
| Navigation Compose version upgrade | Stable version works | — |
| Feature logic changes | Out of scope | — |
| Unit tests for navigation | Phase 9 | Phase 9 |
| Auth changes (splash, login, register) | Protected | — |
| Bottom nav redesign | Design system stable | — |

---

## 5. Prerequisites (Definition of Ready)

- [x] All feature screens reachable via navigation
- [x] Bottom nav functional with 3 tabs
- [x] Transaction routes wired from multiple entry points
- [x] Sign-out clears stack correctly
- [ ] `assembleDevDebug` passes before starting

```bash
./gradlew assembleDevDebug
```

---

## 6. Reference Files (Read-Only)

| File | Learn from |
|------|------------|
| `docs/dev/Project_Assessment_Current.md` | Phase 8 status (~95%) |
| `.cursor/rules/keutrack-architecture.mdc` | Module dependency rules |
| `.cursor/rules/keutrack-feature-module.mdc` | Navigation route patterns |
| `core/designsystem/.../component/KeuTrackBottomNav.kt` | Bottom nav API |
| Android official: [Navigation Compose deep links](https://developer.android.com/jetpack/compose/navigation#deeplinks) | Pattern reference |
| Android official: [Navigation transitions](https://developer.android.com/develop/ui/compose/animation/navigation) | Animation API |

---

## 7. Files NOT to Modify

| File / Area | Reason |
|-------------|--------|
| `features/auth/**` (logic/VM/screen) | Complete — only navigation file for deep link if needed |
| `features/splashscreen/**` (logic) | Complete |
| `core/domain/**` | Freeze |
| `core/data/**` | Freeze |
| `core/datastore/**` | Freeze |
| `build-plugin/**` | Stable |
| Feature VM logic / repositories | Not navigation scope |
| `libs.versions.toml` | No new deps needed (animations are built-in) |

---

## 8. Files to Modify / Create

### App navigation (primary)

| Action | File | Change |
|--------|------|--------|
| UPDATE | `KeuTrackNavHost.kt` | Add transition animations to top-level `NavHost` |
| UPDATE | `HomeShell.kt` | Add crossfade transitions to nested `NavHost`; verify tab state save |
| UPDATE | `KeuTrackAppState.kt` | Add helper for deep link nav if needed |
| OPTIONAL | `HomeRoute.kt` | No change expected |

### Feature navigation files (additive deep link / transition only)

| Action | File | Change |
|--------|------|--------|
| UPDATE | `features/transaction/.../TransactionNavigation.kt` | Add `deepLinks` to route; optional `transactionId` param |
| UPDATE | `features/transaction/.../TransactionRoute` | Add optional arg if not present |

### Manifest

| Action | File | Change |
|--------|------|--------|
| UPDATE | `app/src/main/AndroidManifest.xml` | Add `<intent-filter>` for deep link scheme |

### Feature screens (minimal — only `rememberSaveable` fixes if found)

| Action | File | Change |
|--------|------|--------|
| VERIFY/FIX | Settings / Dashboard / Transaction screens | `rememberSaveable` for sheet/dialog state |

---

## 9. Design Decisions

### 9.1 Deep Link Scheme

| Decision | Detail |
|----------|--------|
| Scheme | `keutrack://` (custom scheme — simple for app-only deep links) |
| Transaction | `keutrack://transaction/{id}` |
| Transaction history | `keutrack://transactions` |
| Future extensibility | `keutrack://family`, `keutrack://settings` (not implemented now) |
| HTTP links | Not needed (no web counterpart) |

### 9.2 Transition Animations

| Navigation | Transition |
|------------|------------|
| Tab switches (bottom nav) | Crossfade (300ms) |
| Push screen (Transaction, History) | Slide in from right / Slide up (modal) |
| Pop screen (back) | Slide out to right / Slide down |
| Auth flow | Fade (200ms) |
| Splash → Home/Auth | Fade out (300ms) |

### 9.3 Process Death Handling

| Decision | Detail |
|----------|--------|
| NavController state | Automatically saved by Compose Navigation |
| ViewModel state | `SavedStateHandle` for nav-relevant data only |
| Bottom sheet state | Use `rememberSaveable` (not plain `remember`) |
| Dialog state | Use `rememberSaveable` |

### 9.4 Back Behavior

| Screen | Back action |
|--------|------------|
| Dashboard (start tab) | Exit app (system default) |
| Family / Settings tab | Switch to Dashboard (popUpTo start) |
| Transaction (from FAB) | Pop back to previous tab |
| Transaction History | Pop back |
| Login (after sign-out) | Exit app (no back to Home) |

---

## 10. Task Breakdown

### Task 8.1 — Deep Link Setup

1. Update `TransactionRoute` to support optional `transactionId` argument:
   ```kotlin
   @Serializable
   data class TransactionRoute(val transactionId: String? = null)
   ```
   (If currently `object`, convert to `data class`.)

2. Add `deepLinks` in `transactionGraph()`:
   ```kotlin
   composable<TransactionRoute>(
       deepLinks = listOf(
           navDeepLink { uriPattern = "keutrack://transaction/{transactionId}" }
       )
   ) { ... }
   ```

3. Add intent filter in `AndroidManifest.xml`:
   ```xml
   <intent-filter>
       <action android:name="android.intent.action.VIEW" />
       <category android:name="android.intent.category.DEFAULT" />
       <category android:name="android.intent.category.BROWSABLE" />
       <data android:scheme="keutrack" />
   </intent-filter>
   ```

4. Verify with adb:
   ```bash
   adb shell am start -d "keutrack://transactions" com.mascill.keutrack.dev
   ```

### Task 8.2 — Transition Animations

1. Define animation specs (can be in a shared object):
   ```kotlin
   object NavTransitions {
       val slideIn = slideInHorizontally(initialOffsetX = { it })
       val slideOut = slideOutHorizontally(targetOffsetX = { -it })
       val popSlideIn = slideInHorizontally(initialOffsetX = { -it })
       val popSlideOut = slideOutHorizontally(targetOffsetX = { it })
       val fadeIn = fadeIn(animationSpec = tween(300))
       val fadeOut = fadeOut(animationSpec = tween(300))
   }
   ```

2. Apply to `KeuTrackNavHost`:
   ```kotlin
   NavHost(
       enterTransition = { slideIn },
       exitTransition = { slideOut },
       popEnterTransition = { popSlideIn },
       popExitTransition = { popSlideOut },
   )
   ```

3. Override for `HomeShell` nested NavHost (crossfade):
   ```kotlin
   NavHost(
       enterTransition = { fadeIn(tween(300)) },
       exitTransition = { fadeOut(tween(300)) },
   )
   ```

4. Override for specific routes if needed (transaction = slide up).

### Task 8.3 — Process Death Verification

1. Audit all `remember { mutableStateOf(...) }` in Routing composables — convert to `rememberSaveable` where state is user-meaningful (dialog open, sheet open).

2. Known locations to check:
   - `SettingsRouting`: `dialogMode`, `showLeaveDialog`
   - `DashboardRouting`: bottom sheet state (if applicable)
   - Transaction entry: amount, selected category

3. Test protocol:
   - Enable "Don't keep activities" in Developer Options
   - Navigate to Transaction → background app → return → verify state
   - Rotate device on each screen → verify no crash

### Task 8.4 — Back Navigation Edge Cases

1. Verify tab back behavior: Family/Settings back → Dashboard (handled by `popUpTo` in bottom nav click)

2. Verify Transaction back returns to correct origin tab (not always Dashboard)

3. Test rapid tab switching — no duplicate entries (handled by `launchSingleTop = true` + `restoreState = true`)

4. Test sign-out during active sheet/dialog — no crash

### Task 8.5 — Final Verification

```bash
./gradlew assembleDevDebug

# Protected areas must stay clean
git diff --stat -- core/domain core/data features/auth features/splashscreen build-plugin
# Expected: empty
```

Manual test matrix:

| # | Scenario | Expected |
|---|----------|----------|
| 1 | Deep link `keutrack://transactions` | Opens TransactionHistory directly |
| 2 | Tab switch Dashboard ↔ Family ↔ Settings | Crossfade animation; state preserved |
| 3 | FAB → Transaction → Back | Returns to previous tab |
| 4 | Rotate on Transaction form | State preserved |
| 5 | Process death (Don't keep activities) | Correct screen on return |
| 6 | Rapid tab switching | No duplicate back stack entries |
| 7 | Sign out from any screen | Clean navigation to Login |
| 8 | Back on Login after sign-out | Exits app |

---

## 11. Acceptance Criteria

### Functional

- [ ] Deep link `keutrack://transactions` opens transaction history
- [ ] Deep link `keutrack://transaction/{id}` opens transaction detail (or form — depending on impl)
- [ ] Tab switches have crossfade animation
- [ ] Push/pop screens have slide animations
- [ ] Process death does not lose current screen
- [ ] Config change (rotation) preserves local UI state (dialogs, sheets)
- [ ] Back navigation behaves correctly from all screens
- [ ] No duplicate back stack entries from rapid interaction

### Non-functional

- [ ] No changes to `core/domain/**`, `core/data/**`, `build-plugin/**`
- [ ] No feature logic changes (only navigation wiring)
- [ ] Build passes: `./gradlew assembleDevDebug`

### Definition of Done

1. All functional criteria checked
2. Manual QA matrix (Task 8.5) passed
3. Protected areas untouched
4. Deep link testable via adb

---

## 12. Architecture Notes

### Navigation hierarchy

```
KeuTrackNavHost (top-level)
├── SplashRoute (startDestination)
├── LoginRoute / RegisterRoute (authGraph)
├── HomeRoute → HomeShell
│   └── HomeNavHost (nested, bottom nav)
│       ├── DashboardRoute (startDestination)
│       ├── FamilyRoute
│       └── SettingsRoute
├── TransactionRoute (top-level, modal-style)
└── TransactionHistoryRoute (top-level)
```

### Key principles

| Principle | Application |
|-----------|-------------|
| Features don't import each other | Navigation callbacks pass through `:app` |
| Type-safe routes | `@Serializable` objects/data classes |
| Single activity | All screens in one `NavHost` hierarchy |
| Tab state preserved | `saveState = true` + `restoreState = true` |
| Stack reset on auth transitions | `popUpTo(start) { inclusive = true }` |

### Why deep links are at `:app` level

Deep link intents arrive at the Activity. The `NavHost` resolves them automatically via `navDeepLink` definitions inside the graph. Feature modules define their routes; `:app` wires the graph and registers the manifest intent filter.

---

## 13. Risk & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Deep link opens wrong screen when not authenticated | User sees error | Add auth check: if no session, redirect to Login → then navigate to deep link target |
| Transition animations feel laggy on low-end devices | Bad UX | Use 200-300ms max; test on mid-range device |
| `rememberSaveable` for complex state fails serialization | Crash on restore | Use primitive types or custom `Saver`; keep saved state minimal |
| Nested NavHost state conflicts with outer | Tab state lost | Already mitigated by `saveState`/`restoreState` — verify only |
| TransactionRoute change from object to data class breaks existing nav | Compile error | Update all call sites (`navigateToTransaction()`) |
| Manifest deep link collides with other app | Incorrect app opens | Custom scheme `keutrack://` is unique enough |

---

## 14. Suggested Execution Order

```
Step 1: Deep link setup
  └── Update TransactionRoute → data class with optional arg
  └── Add deepLinks to transactionGraph
  └── AndroidManifest intent-filter
  └── Test with adb

Step 2: Transition animations
  └── Define NavTransitions object
  └── Apply to KeuTrackNavHost (slide)
  └── Apply to HomeNavHost (crossfade)
  └── Override for specific routes if needed

Step 3: Process death / config change
  └── Audit remember → rememberSaveable
  └── Test with "Don't keep activities"
  └── Test rotation on each screen

Step 4: Back navigation polish
  └── Verify tab back behavior
  └── Verify Transaction back to correct origin
  └── Test rapid switching

Step 5: Final verification
  └── Full manual QA matrix
  └── Protected diff check
  └── assembleDevDebug
```

---

## 15. Relation to Other Phases

| Phase | Relation |
|-------|----------|
| **0–2** | Build plugin / domain / data — not modified |
| **3** | Design system bottom nav — consumed, not changed |
| **4** | Dashboard nav callbacks (onSettingsClick, onAddTx) — already wired |
| **5** | Transaction routes — deep link extends them |
| **6** | Family tab route — already in HomeNavHost |
| **7** | Settings route — already in HomeNavHost |
| **9** | Navigation tests can be added in Phase 9 |
| **Future** | Push notifications will use deep links defined here |

---

## Effort Estimate

| Bucket | Share |
|--------|-------|
| Deep link setup + manifest | ~25% |
| Transition animations | ~30% |
| Process death / state restoration audit | ~25% |
| Back nav polish + edge cases + QA | ~20% |

---

## File Policy Summary

| Category | Policy |
|----------|--------|
| `app/.../navigation/` | ✅ Primary target |
| `features/*/navigation/` (deep link additive) | ✅ Minimal |
| `app/src/main/AndroidManifest.xml` | ✅ Intent filter |
| Feature screens (rememberSaveable fix) | ✅ Minimal if needed |
| `core/domain/**`, `core/data/**` | ❌ Don't touch |
| `features/auth/**`, `splashscreen/**` logic | ❌ Don't touch |
| `build-plugin/**`, `libs.versions.toml` | ❌ Don't touch |
| Feature VM logic / repositories | ❌ Don't touch |

---

*This document completes Phase 8 — navigation polish. After this phase, all navigation paths are production-ready with deep link support, smooth transitions, and resilient state handling. Proceed to Phase 9 for testing and final polish.*
