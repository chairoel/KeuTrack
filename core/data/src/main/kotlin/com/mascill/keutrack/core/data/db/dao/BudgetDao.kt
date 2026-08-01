package com.mascill.keutrack.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mascill.keutrack.core.data.db.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE month = :month ORDER BY categoryId ASC")
    fun observeByMonth(month: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BudgetEntity?

    @Query(
        """
        SELECT * FROM budgets
        WHERE month = :month AND categoryId = :categoryId
        LIMIT 1
        """,
    )
    suspend fun getByMonthAndCategory(month: String, categoryId: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BudgetEntity)

    @Update
    suspend fun update(entity: BudgetEntity)

    @Query("UPDATE budgets SET spent = spent + :delta, syncStatus = :syncStatus WHERE id = :budgetId")
    suspend fun applySpentDelta(budgetId: String, delta: Long, syncStatus: String)

    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteById(budgetId: String)

    @Query("SELECT * FROM budgets WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getPending(): List<BudgetEntity>

    @Query("UPDATE budgets SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
