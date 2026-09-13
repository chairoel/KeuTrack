package com.mascill.keutrack.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val migration1To2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pending_transaction_deletes (
                id TEXT NOT NULL,
                walletId TEXT NOT NULL,
                userId TEXT NOT NULL,
                familyId TEXT,
                type TEXT NOT NULL,
                amount INTEGER NOT NULL,
                categoryId TEXT NOT NULL,
                dateEpochMs INTEGER NOT NULL,
                queuedAtEpochMs INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
    }
}
