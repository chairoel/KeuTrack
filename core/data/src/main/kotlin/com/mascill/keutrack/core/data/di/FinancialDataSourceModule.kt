package com.mascill.keutrack.core.data.di

import com.mascill.keutrack.core.data.datasource.local.BudgetLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.BudgetLocalDataSourceImpl
import com.mascill.keutrack.core.data.datasource.local.CategoryLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.CategoryLocalDataSourceImpl
import com.mascill.keutrack.core.data.datasource.local.CategorySummaryLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.CategorySummaryLocalDataSourceImpl
import com.mascill.keutrack.core.data.datasource.local.TransactionLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.TransactionLocalDataSourceImpl
import com.mascill.keutrack.core.data.datasource.local.WalletLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.WalletLocalDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface FinancialDataSourceModule {

    @Binds
    fun bindTransactionLocalDataSource(
        impl: TransactionLocalDataSourceImpl,
    ): TransactionLocalDataSource

    @Binds
    fun bindWalletLocalDataSource(
        impl: WalletLocalDataSourceImpl,
    ): WalletLocalDataSource

    @Binds
    fun bindCategoryLocalDataSource(
        impl: CategoryLocalDataSourceImpl,
    ): CategoryLocalDataSource

    @Binds
    fun bindBudgetLocalDataSource(
        impl: BudgetLocalDataSourceImpl,
    ): BudgetLocalDataSource

    @Binds
    fun bindCategorySummaryLocalDataSource(
        impl: CategorySummaryLocalDataSourceImpl,
    ): CategorySummaryLocalDataSource
}
