package com.mascill.keutrack.core.data.mapper

import com.mascill.keutrack.core.domain.model.WalletUiPreferences
import com.mascill.keutrack.core.datastore.WalletUiPreferences as WalletUiPreferencesProto

class WalletUiPreferencesProtoMapper {

    fun toDomain(proto: WalletUiPreferencesProto): WalletUiPreferences =
        WalletUiPreferences(
            isPersonalBalanceVisible = !proto.personalBalanceHidden,
            isFamilyBalanceVisible = !proto.familyBalanceHidden,
        )

    fun toProto(preferences: WalletUiPreferences): WalletUiPreferencesProto =
        WalletUiPreferencesProto.newBuilder()
            .setPersonalBalanceHidden(!preferences.isPersonalBalanceVisible)
            .setFamilyBalanceHidden(!preferences.isFamilyBalanceVisible)
            .build()
}
