package com.mascill.keutrack.core.data.datasource.firestore

/**
 * Pre-read identity (amount, type, wallet, category, family, date) no longer
 * matches the Firestore snapshot inside [com.google.firebase.firestore.FirebaseFirestore.runTransaction].
 * The sync worker should retry with a fresh pre-read.
 */
class SideEffectSnapshotMismatch(
    transactionId: String,
) : IllegalStateException("Firestore snapshot changed for transaction $transactionId")
