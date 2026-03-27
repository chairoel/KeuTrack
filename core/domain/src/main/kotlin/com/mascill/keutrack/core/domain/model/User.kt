package com.mascill.keutrack.core.domain.model

data class User(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
    val currency: String = "IDR",
    val familyId: String? = null,
    val familyRole: String? = null
)
