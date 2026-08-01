package com.mascill.keutrack.core.domain.model

import java.time.Instant

data class FamilyGroup(
    val id: String,
    val name: String,
    val inviteCode: String,
    val ownerId: String,
    val memberIds: List<String>,
    val createdAt: Instant = Instant.now(),
)
