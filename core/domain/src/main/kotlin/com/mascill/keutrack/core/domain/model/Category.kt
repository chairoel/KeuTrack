package com.mascill.keutrack.core.domain.model

data class Category(
    val id: String,
    val userId: String? = null,
    val familyId: String? = null,
    val name: String,
    val icon: String,
    val color: String,
    val type: CategoryType,
    val isDefault: Boolean = false,
)
