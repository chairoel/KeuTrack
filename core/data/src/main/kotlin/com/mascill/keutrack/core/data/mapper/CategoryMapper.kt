package com.mascill.keutrack.core.data.mapper

import com.mascill.keutrack.core.data.db.entity.CategoryEntity
import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.CategoryType
import javax.inject.Inject

class CategoryMapper @Inject constructor() {

    fun toDomain(entity: CategoryEntity): Category =
        Category(
            id = entity.id,
            userId = entity.userId,
            familyId = entity.familyId,
            name = entity.name,
            icon = entity.icon,
            color = entity.color,
            type = CategoryType.fromValue(entity.type),
            isDefault = entity.isDefault,
        )

    fun toEntity(domain: Category): CategoryEntity =
        CategoryEntity(
            id = domain.id,
            userId = domain.userId,
            familyId = domain.familyId,
            name = domain.name,
            icon = domain.icon,
            color = domain.color,
            type = domain.type.value,
            isDefault = domain.isDefault,
        )
}
