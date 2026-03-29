package com.mascill.keutrack.core.data.datasource

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreNetworkDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Add Firestore data interactions here (based on docs/KeuTrack_Data_Design.md)
}
