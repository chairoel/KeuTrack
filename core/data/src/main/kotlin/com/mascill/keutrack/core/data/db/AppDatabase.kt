package com.mascill.keutrack.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mascill.keutrack.core.data.db.dao.BudgetDao
import com.mascill.keutrack.core.data.db.dao.CategoryDao
import com.mascill.keutrack.core.data.db.dao.CategorySummaryDao
import com.mascill.keutrack.core.data.db.dao.TransactionDao
import com.mascill.keutrack.core.data.db.dao.WalletDao
import com.mascill.keutrack.core.data.db.entity.BudgetEntity
import com.mascill.keutrack.core.data.db.entity.CategoryEntity
import com.mascill.keutrack.core.data.db.entity.CategorySummaryEntity
import com.mascill.keutrack.core.data.db.entity.TransactionEntity
import com.mascill.keutrack.core.data.db.entity.WalletEntity

@Database(
    entities = [
        TransactionEntity::class,
        WalletEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        CategorySummaryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categorySummaryDao(): CategorySummaryDao
}
