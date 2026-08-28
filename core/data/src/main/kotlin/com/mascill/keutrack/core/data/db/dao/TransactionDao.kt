package com.mascill.keutrack.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mascill.keutrack.core.data.db.entity.TransactionEntity
import com.mascill.keutrack.core.data.db.model.AmountByTypeRow
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query(
        """
        SELECT * FROM transactions
        WHERE (:walletId IS NULL OR walletId = :walletId)
          AND (:familyId IS NULL OR familyId = :familyId)
          AND (:type IS NULL OR type = :type)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:startMs IS NULL OR dateEpochMs >= :startMs)
          AND (:endMs IS NULL OR dateEpochMs <= :endMs)
        ORDER BY dateEpochMs DESC
        LIMIT :limit
        """,
    )
    fun observeFiltered(
        walletId: String?,
        familyId: String?,
        type: String?,
        categoryId: String?,
        startMs: Long?,
        endMs: Long?,
        limit: Int,
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT type AS type, SUM(amount) AS total FROM transactions
        WHERE dateEpochMs >= :startMs AND dateEpochMs <= :endMs
        GROUP BY type
        """,
    )
    fun observeSumsByType(startMs: Long, endMs: Long): Flow<List<AmountByTypeRow>>

    @Query("SELECT * FROM transactions ORDER BY dateEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TransactionEntity)

    @Update
    suspend fun update(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM transactions WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getPending(): List<TransactionEntity>

    @Query("UPDATE transactions SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
