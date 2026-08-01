package com.mascill.keutrack.core.data.mapper

import com.mascill.keutrack.core.data.db.Converters
import com.mascill.keutrack.core.data.db.entity.CategorySummaryEntity
import com.mascill.keutrack.core.domain.model.CategorySummary
import javax.inject.Inject

class CategorySummaryMapper @Inject constructor() {

    private val converters = Converters()

    fun toDomain(entity: CategorySummaryEntity): CategorySummary =
        CategorySummary(
            period = entity.period,
            userId = entity.userId,
            familyId = entity.familyId,
            totalIncome = entity.totalIncome,
            totalExpense = entity.totalExpense,
            byCategory = converters.jsonToBreakdownMap(entity.byCategoryJson).orEmpty(),
            topExpenseCategoryId = entity.topExpenseCategoryId,
        )

    fun toEntity(domain: CategorySummary): CategorySummaryEntity =
        CategorySummaryEntity(
            period = domain.period,
            userId = domain.userId,
            familyId = domain.familyId,
            totalIncome = domain.totalIncome,
            totalExpense = domain.totalExpense,
            byCategoryJson = converters.breakdownMapToJson(domain.byCategory).orEmpty(),
            topExpenseCategoryId = domain.topExpenseCategoryId,
        )
}
