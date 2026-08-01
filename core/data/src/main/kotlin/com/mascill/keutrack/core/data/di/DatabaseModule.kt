package com.mascill.keutrack.core.data.di

import android.content.Context
import androidx.room.Room
import com.mascill.keutrack.core.data.db.AppDatabase
import com.mascill.keutrack.core.data.db.dao.BudgetDao
import com.mascill.keutrack.core.data.db.dao.CategoryDao
import com.mascill.keutrack.core.data.db.dao.CategorySummaryDao
import com.mascill.keutrack.core.data.db.dao.TransactionDao
import com.mascill.keutrack.core.data.db.dao.WalletDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "keutrack.db")
            // OK for v1 pre-production; replace with Migration before release.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideWalletDao(db: AppDatabase): WalletDao = db.walletDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideCategorySummaryDao(db: AppDatabase): CategorySummaryDao = db.categorySummaryDao()
}
