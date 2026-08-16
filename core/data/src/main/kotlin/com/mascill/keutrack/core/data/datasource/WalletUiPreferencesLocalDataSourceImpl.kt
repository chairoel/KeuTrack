package com.mascill.keutrack.core.data.datasource

import androidx.datastore.core.DataStore
import com.mascill.keutrack.core.data.mapper.WalletUiPreferencesProtoMapper
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.model.WalletUiPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.mascill.keutrack.core.datastore.WalletUiPreferences as WalletUiPreferencesProto

class WalletUiPreferencesLocalDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<WalletUiPreferencesProto>,
    private val mapper: WalletUiPreferencesProtoMapper,
) : WalletUiPreferencesLocalDataSource {

    override fun observe(): Flow<WalletUiPreferences> =
        dataStore.data.map(mapper::toDomain)

    override suspend fun setBalanceVisible(
        walletType: WalletType,
        visible: Boolean,
    ) {
        dataStore.updateData { current ->
            val builder = current.toBuilder()
            when (walletType) {
                WalletType.PERSONAL -> builder.setPersonalBalanceHidden(!visible)
                WalletType.FAMILY -> builder.setFamilyBalanceHidden(!visible)
            }
            builder.build()
        }
    }
}
