package com.mascill.keutrack.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mascill.keutrack.core.data.db.entity.PendingTransactionDeleteEntity

@Dao
interface PendingTransactionDeleteDao {

    @Query("SELECT * FROM pending_transaction_deletes WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getPending(): List<PendingTransactionDeleteEntity>

    @Query("SELECT id FROM pending_transaction_deletes")
    suspend fun getAllIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingTransactionDeleteEntity)

    @Query("DELETE FROM pending_transaction_deletes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE pending_transaction_deletes SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
