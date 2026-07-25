# Firestore Security Rules (KeuTrack)

**Last updated:** 25 July 2026

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
| `/wallets/{walletId}` | Owner of the wallet | `ownerId` |
| `/transactions/{txId}` | Owner of the transaction | `userId` |
| `/budgets/{budgetId}` | Owner of the budget | `userId` |

**Not covered yet:** `/categories` (and family-shared access). Those writes will be denied until rules are added.

User profiles **cannot** be deleted from the client (`allow delete: if false`).

---

## Helper functions

```javascript
function signedIn() {
  return request.auth != null;
}

function isOwner(uid) {
  return signedIn() && request.auth.uid == uid;
}
```

- `signedIn()` — user must be logged in.
- `isOwner(uid)` — logged-in user’s UID must match the path or document owner.

---

## How create / get / update is checked

For top-level money collections (`wallets`, `transactions`, `budgets`):

1. **Create** — `request.resource.data.<ownerField> == request.auth.uid`  
   Client cannot create a document owned by someone else.
2. **Get (including missing docs)** — allow when `resource == null` **or** the existing doc belongs to the signed-in user.  
   Required because sync does `get()` before create (idempotency). A rule that only checks `resource.data.userId` **denies** reads of non-existent docs → `PERMISSION_DENIED`.
3. **Update / delete** — `resource.data.<ownerField> == request.auth.uid`  
   Only the current owner can change or remove an existing document.
4. **List** — denied for these collections (`allow list: if false`) until query-scoped rules are designed.

---

## Why `get` must allow missing documents

`TransactionFirestoreDataSource.upsertTransactionWithSideEffects` runs inside a Firestore transaction:

1. `get(transactions/{id})` — check if already synced  
2. `set(transactions/{id})` — create if missing  
3. `update(wallets/{id})` — increment balance  
4. `set(users/{uid}/category_summaries/{period})` — upsert summary  

If step 1 is denied, the whole sync fails with `PERMISSION_DENIED` even when create rules are correct.

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

    // Wallets: ownership field = ownerId
    match /wallets/{walletId} {
      allow create: if signedIn()
        && request.resource.data.ownerId == request.auth.uid;

      // Allow get on missing docs; owner-only if the doc exists
      allow get: if signedIn() && (
        resource == null ||
        resource.data.ownerId == request.auth.uid
      );

      allow list: if false;

      allow update, delete: if signedIn()
        && resource.data.ownerId == request.auth.uid;
    }

    // Transactions: ownership field = userId
    match /transactions/{txId} {
      allow create: if signedIn()
        && request.resource.data.userId == request.auth.uid;

      // Required: SyncWorker does get() before create (idempotency check)
      allow get: if signedIn() && (
        resource == null ||
        resource.data.userId == request.auth.uid
      );

      allow list: if false;

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
  }
}
```

---

## Publish checklist

1. Paste rules in Firebase Console (or deploy via Firebase CLI).
2. Confirm you are signed in on the device under test.
3. Add a transaction in the app (online), or open the dashboard to retry pending sync.
4. In Firestore Console, confirm documents appear under `wallets` and `transactions`.
5. On the dashboard, the **Local** / **Gagal sync** chip should clear after a successful sync (`SYNCED`).
If sync still fails with `PERMISSION_DENIED`:

- Confirm these rules were **Published** (not only saved in this markdown file).
- Check ownership fields match the app (`ownerId` on wallets, `userId` on transactions/budgets).
- Confirm `transaction.userId` equals the signed-in Firebase Auth UID.
- Confirm the wallet document already exists in Firestore before transaction side-effect `update` runs.

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
