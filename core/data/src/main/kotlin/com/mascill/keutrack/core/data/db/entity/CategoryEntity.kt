package com.mascill.keutrack.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index("type"),
        Index("isDefault"),
    ],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val userId: String?,
    val familyId: String?,
    val name: String,
    val icon: String,
    val color: String,
    val type: String,
    val isDefault: Boolean,
)
