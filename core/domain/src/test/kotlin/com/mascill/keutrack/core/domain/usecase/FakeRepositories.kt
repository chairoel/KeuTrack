package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.SyncRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

internal class FakeFamilyRepository : FamilyRepository {
    var createResult: Result<FamilyGroup> = Result.failure(IllegalStateException("unstubbed"))
    val createCalls = mutableListOf<Pair<String, String>>()
    var joinResult: Result<FamilyGroup> = Result.failure(IllegalStateException("unstubbed"))
    var lastJoin: Pair<String, String>? = null

    override fun observeCurrentFamily(): Flow<FamilyGroup?> = flowOf(null)

    override suspend fun createFamily(name: String, ownerId: String): Result<FamilyGroup> {
        createCalls += name to ownerId
        return createResult
    }

    override suspend fun joinFamily(inviteCode: String, userId: String): Result<FamilyGroup> {
        lastJoin = inviteCode to userId
        return joinResult
    }

    override suspend fun getFamilyById(familyId: String): FamilyGroup? = null

    override suspend fun removeMember(familyId: String, userId: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun deleteFamily(familyId: String): Result<Unit> = Result.success(Unit)
}

internal class FakeUserRepository : UserRepository {
    val currentUser = MutableStateFlow<com.mascill.keutrack.core.domain.model.User?>(null)
    var lastMembership: Pair<String?, String?>? = null

    override fun getCurrentUser(): Flow<com.mascill.keutrack.core.domain.model.User?> = currentUser

    override suspend fun signInWithGoogle(idToken: String) = error("unused")
    override suspend fun registerWithEmail(fullName: String, email: String, password: String) =
        error("unused")
    override suspend fun signInWithEmail(email: String, password: String) = error("unused")
    override suspend fun signOut() = Unit
    override suspend fun syncUserProfile() = Unit

    override suspend fun updateFamilyMembership(
        familyId: String?,
        familyRole: String?,
    ): Result<Unit> {
        lastMembership = familyId to familyRole
        return Result.success(Unit)
    }
}

internal class FakeWalletRepository : WalletRepository {
    var familyWallets: List<Wallet> = emptyList()
    val created = mutableListOf<Wallet>()

    override fun observeWallets(): Flow<List<Wallet>> = flowOf(emptyList())

    override fun observeWalletsByType(type: WalletType): Flow<List<Wallet>> =
        flowOf(if (type == WalletType.FAMILY) familyWallets else emptyList())

    override fun observeWalletById(walletId: String): Flow<Wallet?> = flowOf(null)

    override suspend fun getPersonalWallet(): Wallet? = null

    override suspend fun createWallet(wallet: Wallet) {
        created += wallet
    }

    override suspend fun updateWallet(wallet: Wallet) = Unit

    override suspend fun deleteWallet(walletId: String) = Unit
}

internal class FakeSyncRepository : SyncRepository {
    var syncPendingWalletCalls = 0
    var enqueueForceCalls = 0
    var syncPendingWalletsError: Exception? = null
    val familySyncs = mutableListOf<String>()
    val personalSyncs = mutableListOf<String>()

    override suspend fun syncPendingTransactions() = Unit

    override suspend fun syncPendingWallets() {
        syncPendingWalletCalls++
        syncPendingWalletsError?.let { throw it }
    }

    override suspend fun syncPendingBudgets() = Unit
    override suspend fun syncAll() = Unit

    override suspend fun syncFamilyData(familyId: String) {
        familySyncs += familyId
    }

    override suspend fun syncPersonalData(userId: String) {
        personalSyncs += userId
    }

    override suspend fun hasPendingSync(): Boolean = false

    override fun enqueuePendingSync(force: Boolean) {
        if (force) enqueueForceCalls++
    }
}
