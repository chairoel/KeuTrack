package com.mascill.keutrack.core.domain.model

enum class WalletType(val value: String) {
    PERSONAL("personal"),
    FAMILY("family");

    companion object {
        fun fromValue(value: String): WalletType =
            entries.firstOrNull { it.value == value } ?: PERSONAL
    }
}
