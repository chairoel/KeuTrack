package com.mascill.keutrack.core.domain.model

enum class FamilyRole(val value: String) {
    OWNER("owner"),
    MEMBER("member");

    companion object {
        fun fromValue(value: String): FamilyRole =
            entries.firstOrNull { it.value == value } ?: MEMBER
    }
}
