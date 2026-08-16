package com.mascill.keutrack.core.data.mapper

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.CategoryType
import org.junit.Test

class CategoryMapperTest {

    private val mapper = CategoryMapper()

    @Test
    fun `type enum and icon string survive round-trip`() {
        val original = Category(
            id = "cat_makanan",
            userId = null,
            familyId = "fam-1",
            name = "Makanan",
            icon = "Restaurant",
            color = "#FF7043",
            type = CategoryType.EXPENSE,
            isDefault = true,
        )

        val restored = mapper.toDomain(mapper.toEntity(original))

        assertThat(restored).isEqualTo(original)
        assertThat(restored.icon).isEqualTo("Restaurant")
        assertThat(restored.type).isEqualTo(CategoryType.EXPENSE)
    }

    @Test
    fun `unknown type falls back to BOTH`() {
        val entity = mapper.toEntity(
            Category(
                id = "c",
                name = "X",
                icon = "MoreHoriz",
                color = "#000",
                type = CategoryType.INCOME,
            ),
        ).copy(type = "mystery")

        assertThat(mapper.toDomain(entity).type).isEqualTo(CategoryType.BOTH)
    }
}
