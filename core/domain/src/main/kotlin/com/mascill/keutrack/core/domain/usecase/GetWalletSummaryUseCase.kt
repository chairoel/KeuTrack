package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class WalletSummary(
    val personalWallet: Wallet?,
    val familyWallets: List<Wallet>,
    val totalPersonalBalance: Long,
    val totalFamilyBalance: Long,
)

class GetWalletSummaryUseCase @Inject constructor(
    private val walletRepository: WalletRepository,
) {
    operator fun invoke(): Flow<WalletSummary> =
        walletRepository.observeWallets().map { wallets ->
            val personal = wallets.filter { it.type == WalletType.PERSONAL }
            val family = wallets.filter { it.type == WalletType.FAMILY }
            WalletSummary(
                personalWallet = personal.firstOrNull(),
                familyWallets = family,
                totalPersonalBalance = personal.sumOf { it.balance },
                totalFamilyBalance = family.sumOf { it.balance },
            )
        }
}
