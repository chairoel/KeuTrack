package com.mascill.keutrack.core.data.db.entity

import androidx.room.Entity

@Entity(
    tableName = "category_summaries",
    primaryKeys = ["period", "userId"],
)
data class CategorySummaryEntity(
    val period: String,
    val userId: String,
    val familyId: String?,
    val totalIncome: Long,
    val totalExpense: Long,
    val byCategoryJson: String,
    val topExpenseCategoryId: String?,
)
