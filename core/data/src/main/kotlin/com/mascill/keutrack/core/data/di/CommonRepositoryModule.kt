package com.mascill.keutrack.core.data.di

import com.mascill.keutrack.core.data.repository.BudgetRepositoryImpl
import com.mascill.keutrack.core.data.repository.CategoryRepositoryImpl
import com.mascill.keutrack.core.data.repository.FamilyRepositoryImpl
import com.mascill.keutrack.core.data.repository.SyncRepositoryImpl
import com.mascill.keutrack.core.data.repository.TransactionRepositoryImpl
import com.mascill.keutrack.core.data.repository.UserRepositoryImpl
import com.mascill.keutrack.core.data.repository.WalletRepositoryImpl
import com.mascill.keutrack.core.data.repository.WalletUiPreferencesRepositoryImpl
import com.mascill.keutrack.core.domain.repository.BudgetRepository
import com.mascill.keutrack.core.domain.repository.CategoryRepository
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.SyncRepository
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.repository.WalletRepository
import com.mascill.keutrack.core.domain.repository.WalletUiPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface CommonRepositoryModule {

    @Binds
    fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    fun bindTransactionRepository(
        impl: TransactionRepositoryImpl,
    ): TransactionRepository

    @Binds
    fun bindWalletRepository(
        impl: WalletRepositoryImpl,
    ): WalletRepository

    @Binds
    fun bindCategoryRepository(
        impl: CategoryRepositoryImpl,
    ): CategoryRepository

    @Binds
    fun bindBudgetRepository(
        impl: BudgetRepositoryImpl,
    ): BudgetRepository

    @Binds
    fun bindSyncRepository(
        impl: SyncRepositoryImpl,
    ): SyncRepository

    @Binds
    fun bindFamilyRepository(
        impl: FamilyRepositoryImpl,
    ): FamilyRepository

    @Binds
    fun bindWalletUiPreferencesRepository(
        impl: WalletUiPreferencesRepositoryImpl,
    ): WalletUiPreferencesRepository
}
