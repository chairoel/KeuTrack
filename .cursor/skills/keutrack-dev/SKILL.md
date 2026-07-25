---
name: keutrack-dev
description: >-
  KeuTrack Android development workflow and phased implementation guide.
  Use when building new features, implementing domain models, creating
  repositories, wiring ViewModels, adding navigation routes, or when
  the user asks about KeuTrack architecture, data design, or next steps.
---

# KeuTrack Development Workflow

## Project Context

KeuTrack is an offline-first family expense tracking Android app. Read these files for full context:

- **Architecture & roadmap**: `plans/KeuTrack_Development_Plan.md`
- **Data design (Firestore + Room)**: `plans/KeuTrack_Data_Design.md`
- **Current assessment**: `plans/Project_Assessment.md`
- **Phase plans**: `plans/phases/` directory

## Development Phases

| Phase | Module | Status |
|-------|--------|--------|
| 0 — Build plugin & Gradle | `:build-plugin` | Done |
| 1 — Domain entities & use cases | `:core:domain` | Next up — see `plans/phases/PHASE_1_*.md` |
| 2 — Room + Repository impl | `:core:data` | Pending |
| 3 — Design system | `:core:designsystem` | ~90% done |
| 4 — Dashboard real data | `:features:dashboard` | UI done, data mocked |
| 5 — Transaction flow | `:features:transaction` | Placeholder only |
| 6 — Family feature | `:features:family` | UI mocked |
| 7 — Settings persistence | `:features:settings` | Partial |
| 8 — Navigation | `:app` | ~85% done |
| 9 — Polish & tests | All | Not started |

## Workflow: Adding a New Domain Entity

1. Create entity data class in `core/domain/src/.../model/`
2. Create enum(s) if needed (with `value: String` + `fromValue()`)
3. Define repository interface in `core/domain/src/.../repository/`
4. Create use case(s) in `core/domain/src/.../usecase/`
5. Verify: `./gradlew :core:domain:compileDebugKotlin`

## Workflow: Implementing a Repository

1. Add Room entity in `core/data/src/.../entity/` with `@Entity` annotation
2. Create DAO in `core/data/src/.../dao/`
3. Register in `AppDatabase`
4. Create Firestore data source (if needed) in `core/data/src/.../datasource/`
5. Create mapper (entity ↔ domain) in `core/data/src/.../mapper/`
6. Implement repository in `core/data/src/.../repository/`
7. Add Hilt `@Binds` in appropriate DI module
8. Verify: `./gradlew :core:data:compileDebugKotlin`

## Workflow: Building a Feature Screen

1. Define route (`@Serializable object`) in `features/{name}/.../navigation/`
2. Create UI state data class in `features/{name}/.../model/`
3. Create ViewModel (`@HiltViewModel`) in `features/{name}/.../presentation/`
4. Create Screen composable (stateless) + Routing composable
5. Add `@Preview` (light + dark)
6. Add `NavGraphBuilder.{name}Graph()` extension
7. Wire in `:app`'s `KeuTrackNavHost`
8. Verify: `./gradlew assembleDevDebug`

## Offline-First Data Flow

```
User action → ViewModel → UseCase (validate) → Repository
    → Room DB write (syncStatus=PENDING, immediate UI update)
    → WorkManager queue
    → [Online?] Firestore Batch Write (transaction + wallet balance + category summary)
    → Room DB update (syncStatus=SYNCED)
```

All reads come from Room DB. Firestore is only for sync.

## Protected Files (Do Not Modify)

These files are stable and should not be changed without explicit request:

- `core/domain/src/.../model/User.kt`
- `core/domain/src/.../model/AuthResult.kt`
- `core/domain/src/.../model/TokenResult.kt`
- `core/domain/src/.../repository/UserRepository.kt`
- `core/data/src/.../repository/UserRepositoryImpl.kt`
- All files in `features/auth/` and `features/splashscreen/`
- All files in `build-plugin/`
- `settings.gradle.kts`, `gradle.properties`, `local.properties`

## Verification Commands

```bash
# Compile single module
./gradlew :core:domain:compileDebugKotlin
./gradlew :core:data:compileDebugKotlin

# Full build (dev flavor, debug)
./gradlew assembleDevDebug

# Check for lint issues
./gradlew :core:domain:lintDevDebug
```
