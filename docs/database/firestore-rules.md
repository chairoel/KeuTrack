# Firestore Security Rules (KeuTrack)

**Last updated:** 16 August 2026 (Phase 10 — personal wallet restore pull)

This document explains the Firestore security rules used by KeuTrack and includes a copy you can paste into the Firebase Console.

> **Important:** These rules live in the Firebase project (Console or `firebase deploy`). This markdown file is documentation only — editing it does **not** change production rules until you publish them.

---

## Quick concepts

| Term | Meaning |
|------|---------|
| `rules_version = '2'` | Version of the **Rules language** (syntax engine). Not a version of “our” rules content. Keep `'2'`. |
| `request.auth` | The signed-in Firebase Auth user. `null` if logged out. |
| `request.resource.data` | Document fields **after** a create/update (the new data). |
| `resource.data` | Document fields **already stored** (used for update/delete/read checks). |
| `resource == null` | Document does **not** exist yet (important for `get` before create). |
| Default deny | Any path **without** a matching `allow` is denied. |

KeuTrack is offline-first: the app writes to Room first, then WorkManager syncs to Firestore. If rules block a path, sync fails and items stay `PENDING` / `FAILED` locally.

---

## What each collection allows

| Path | Who can access | Ownership field |
|------|----------------|-----------------|
| `/users/{uid}` | Only that user | Path `uid` == `request.auth.uid` |
| `/users/{uid}/category_summaries/{period}` | Only that user | Same as parent `uid` |
| `/wallets/{walletId}` | Owner **or** family member (read); owner write; members may update `balance` only | `ownerId` / `familyId` |
| `/transactions/{txId}` | Author **or** family member (read); author write | `userId` / `familyId` |
| `/budgets/{budgetId}` | Owner of the budget | `userId` |
| `/family_groups/{familyId}` | Signed-in members (create/join) | `ownerId` / `memberIds` |

**Not covered yet:** `/categories`. Those writes will be denied until rules are added.

### Phase 6 notes

- `/family_groups` enables create/join and invite-code lookup (`list` must allow signed-in users so join can query by `inviteCode`).
- **Join** uses `arrayUnion(uid)` on `memberIds`. The joiner is **not** a member yet, so `allow update` must include a dedicated join clause — member-only update will `PERMISSION_DENIED` on join.
- `/users/{uid}` already allows owner `update`, so writing `familyId` / `familyRole` via the membership API is covered (no separate field whitelist required).
- User profiles **cannot** be deleted from the client (`allow delete: if false`).

### Phase 6c notes (shared family data)

- Family members may **get** wallet/transaction docs when `isFamilyMember(resource.data.familyId)`.
- **List** on `/wallets` and `/transactions` is `allow list: if signedIn()` for MVP so clients can query `where familyId == F`. This is intentionally loose — harden with query-scoped constraints soon after green QA.
- Shared family wallet sync: when member B adds a transaction, Firestore updates `wallets/{id}.balance` via `FieldValue.increment`. Members must be allowed to change **only** `balance` on family wallets — otherwise SyncWorker stays on `RETRY` with `PERMISSION_DENIED`.
- Transaction create/update/delete remain author-only (`userId`); wallet delete remains owner-only.
- Pull sync: equality-only `familyId` query (no composite index required for MVP).

### Phase 10 notes (personal wallet restore)

- Personal restore queries `wallets` with `where ownerId == uid` and `transactions` with `where userId == uid`. Both already pass `allow list: if signedIn()`.
- Filter `type == personal` and `walletId == canonical` on the client (equality-only; no composite index required for MVP).
- **Do not** loosen `/budgets` list (`allow list: if false` stays). Budget pull is out of scope.
- Orphan personal wallets from earlier reinstalls (extra `type=personal` docs for the same `ownerId`) are **not** auto-deleted remotely. Pick the oldest as canonical in the app; clean extras in Console if needed.
- Harden follow-up (same caveat as 6c): `list` + `resource.data` on queries does **not** always constrain the queried field. Do not treat query-scoped `ownerId == request.auth.uid` as a trivial rules change.

---

## Helper functions

```javascript
function signedIn() {
  return request.auth != null;
}

function isOwner(uid) {
  return signedIn() && request.auth.uid == uid;
}

function isFamilyMember(familyId) {
  return signedIn()
    && familyId is string
    && familyId.size() > 0
    && request.auth.uid in
      get(/databases/$(database)/documents/family_groups/$(familyId)).data.memberIds;
}
```

- `signedIn()` — user must be logged in.
- `isOwner(uid)` — logged-in user’s UID must match the path or document owner.
- `isFamilyMember(familyId)` — UID is listed in `family_groups/{familyId}.memberIds`.

---

## How create / get / update is checked

For top-level money collections (`wallets`, `transactions`, `budgets`):

1. **Create** — `request.resource.data.<ownerField> == request.auth.uid`  
   Client cannot create a document owned by someone else.
2. **Get (including missing docs)** — allow when `resource == null` **or** the existing doc belongs to the signed-in user **or** (wallets/transactions) the user is a family member on `familyId`.  
   Required because sync does `get()` before create (idempotency). A rule that only checks `resource.data.userId` **denies** reads of non-existent docs → `PERMISSION_DENIED`.
3. **Update / delete** — `resource.data.<ownerField> == request.auth.uid`  
   Only the current owner can change or remove an existing document.
4. **List** — wallets/transactions allow signed-in list for Phase 6c pull MVP; budgets remain `list: false`.

---

## Why `get` must allow missing documents

`TransactionFirestoreDataSource.upsertTransactionWithSideEffects` runs inside a Firestore transaction:

1. `get(transactions/{id})` — check if already synced  
2. `set(transactions/{id})` — create if missing  
3. `update(wallets/{id})` — increment balance  
4. `set(users/{uid}/category_summaries/{period})` — upsert summary  

If step 1 is denied, the whole sync fails with `PERMISSION_DENIED` even when create rules are correct.

---

## Indexes (Phase 6c + Phase 10)

**Current app (MVP):** family pull uses `whereEqualTo("familyId", …)` and personal restore uses `whereEqualTo("ownerId", …)` / `whereEqualTo("userId", …)` only, then sorts by `date` on device — **no composite index required**.

Optional later (server-side `orderBy` + `limit`, or two-field server filters):

| Collection | Fields | When |
|------------|--------|------|
| `transactions` | `familyId` Asc, `date` Desc | If you restore remote `orderBy` + `limit` |
| `wallets` | `familyId` Asc (+ optional `type` Asc) | Usually not needed for equality-only |
| `wallets` | `ownerId` Asc + `type` Asc | If personal restore queries both fields on the server |
| `transactions` | `userId` Asc + `date` Desc | If personal restore uses remote `orderBy` + `limit` |
| `transactions` | `walletId` Asc + `date` Desc | If restore switches to `getByWalletId` |

If you hit `FAILED_PRECONDITION: The query requires an index`, open the link in the error (project `keutrack-dev`) and create the composite, then wait until status is **Enabled**.

---

## Rules to publish

Copy the block below into **Firebase Console → Firestore → Rules**, then **Publish**.

```javascript
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {

    function signedIn() {
      return request.auth != null;
    }

    function isOwner(uid) {
      return signedIn() && request.auth.uid == uid;
    }

    // Phase 6c: shared read for family wallet / transactions
    function isFamilyMember(familyId) {
      return signedIn()
        && familyId is string
        && familyId.size() > 0
        && request.auth.uid in
          get(/databases/$(database)/documents/family_groups/$(familyId)).data.memberIds;
    }

    // Profile: /users/{uid}
    match /users/{uid} {
      allow read: if isOwner(uid);
      allow create, update: if isOwner(uid);
      allow delete: if false;

      // Monthly rollups: /users/{uid}/category_summaries/{period}
      match /category_summaries/{period} {
        allow read, write: if isOwner(uid);
      }
    }

    // Wallets: ownership field = ownerId; family members may read/list (Phase 6c)
    match /wallets/{walletId} {
      allow create: if signedIn()
        && request.resource.data.ownerId == request.auth.uid;

      allow get: if signedIn() && (
        resource == null
        || resource.data.ownerId == request.auth.uid
        || isFamilyMember(resource.data.familyId)
      );

      // MVP: signed-in list for pull by familyId — harden later
      allow list: if signedIn();

      // Owner full update; family members may only increment/update balance
      // (required for shared-wallet transaction side-effects in SyncWorker).
      allow update: if signedIn() && (
        resource.data.ownerId == request.auth.uid
        || (
          isFamilyMember(resource.data.familyId)
          && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['balance'])
        )
      );

      allow delete: if signedIn()
        && resource.data.ownerId == request.auth.uid;
    }

    // Transactions: ownership field = userId; family members may read/list (Phase 6c)
    match /transactions/{txId} {
      allow create: if signedIn()
        && request.resource.data.userId == request.auth.uid;

      // Required: SyncWorker does get() before create (idempotency check)
      allow get: if signedIn() && (
        resource == null
        || resource.data.userId == request.auth.uid
        || isFamilyMember(resource.data.familyId)
      );

      // MVP: signed-in list for pull by familyId — harden later
      allow list: if signedIn();

      allow update, delete: if signedIn()
        && resource.data.userId == request.auth.uid;
    }

    // Budgets: ownership field = userId
    match /budgets/{budgetId} {
      allow create: if signedIn()
        && request.resource.data.userId == request.auth.uid;

      allow get: if signedIn() && (
        resource == null ||
        resource.data.userId == request.auth.uid
      );

      allow list: if false;

      allow update, delete: if signedIn()
        && resource.data.userId == request.auth.uid;
    }

    // Family groups (Phase 6): create / join / invite
    match /family_groups/{familyId} {
      allow get: if signedIn()
        && (resource == null
          || request.auth.uid in resource.data.memberIds
          || request.auth.uid == resource.data.ownerId);

      // Needed so join can query by inviteCode
      allow list: if signedIn();

      allow create: if signedIn()
        && request.resource.data.ownerId == request.auth.uid
        && request.auth.uid in request.resource.data.memberIds;

      // Owner/members may update; non-members may only arrayUnion themselves into memberIds.
      allow update: if signedIn() && (
        request.auth.uid == resource.data.ownerId
        || request.auth.uid in resource.data.memberIds
        || (
          !(request.auth.uid in resource.data.memberIds)
          && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['memberIds'])
          && request.resource.data.memberIds is list
          && request.resource.data.memberIds.size() == resource.data.memberIds.size() + 1
          && request.resource.data.memberIds.hasAll(resource.data.memberIds)
          && request.auth.uid in request.resource.data.memberIds
        )
      );

      allow delete: if signedIn()
        && request.auth.uid == resource.data.ownerId;
    }
  }
}
```

---

## Publish checklist

1. Paste rules in Firebase Console (or deploy via Firebase CLI), then **Publish**.
2. Confirm you are signed in on the device under test.
3. Add a transaction in the app (online), or open the dashboard to retry pending sync.
4. In Firestore Console, confirm documents appear under `wallets` and `transactions`.
5. On the dashboard, the **Local** / **Gagal sync** chip should clear after a successful sync (`SYNCED`).
6. Phase 6: create a family from the Family tab, then confirm a doc appears under `family_groups`. Join with invite code from another account if available.
7. Phase 6c: after A syncs a family transaction, B opens Family tab (pull) and sees History from A. Confirm indexes exist if query fails.

If sync or membership still fails with `PERMISSION_DENIED`:

- Confirm these rules were **Published** (not only saved in this markdown file).
- Check ownership fields match the app (`ownerId` on wallets, `userId` on transactions/budgets).
- Confirm `transaction.userId` equals the signed-in Firebase Auth UID.
- Confirm the wallet document already exists in Firestore before transaction side-effect `update` runs.
- For create/join family: confirm the `family_groups` match block is present and published (including the join `memberIds` append clause — not member-only update).
- For family pull: confirm `isFamilyMember` helper is published and B is in `memberIds`.
- For member transaction sync stuck on RETRY: confirm wallet `allow update` includes the family-member `balance`-only clause (side-effect increment on shared wallet).

---

## Temporary open rules (debug only)

Do **not** use in production. Useful only for a short debug window:

```javascript
match /{document=**} {
  allow read, write: if request.auth != null
    && request.time < timestamp.date(2026, 8, 5);
}
```

This expires on the date in `timestamp.date(...)`. Prefer the owner-based rules above.
