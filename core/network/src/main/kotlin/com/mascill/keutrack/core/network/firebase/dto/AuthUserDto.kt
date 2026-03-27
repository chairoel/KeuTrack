package com.mascill.keutrack.core.network.firebase.dto

/**
 * DTO returned by the network layer.
 * This shields :core:data from any direct Firebase SDK dependency.
 */
data class AuthUserDto(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?
)
