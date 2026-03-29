package com.mascill.keutrack.core.data.model

/**
 * DTO returned by the network layer.
 * This shields :core:data from any direct Firebase SDK dependency.
 */
data class AuthUserResponse(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?
)