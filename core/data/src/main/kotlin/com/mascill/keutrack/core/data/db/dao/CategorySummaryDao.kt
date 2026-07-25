package com.mascill.keutrack.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mascill.keutrack.core.data.db.entity.CategorySummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategorySummaryDao {

    @Query(
        """
        SELECT * FROM category_summaries
        WHERE period = :period AND userId = :userId
        LIMIT 1
        """,
    )
    fun observeByPeriod(period: String, userId: String): Flow<CategorySummaryEntity?>

    @Query(
        """
        SELECT * FROM category_summaries
        WHERE userId = :userId AND period IN (:periods)
        ORDER BY period DESC
        """,
    )
    fun observeByPeriods(userId: String, periods: List<String>): Flow<List<CategorySummaryEntity>>

    @Query(
        """
        SELECT * FROM category_summaries
        WHERE period = :period AND userId = :userId
        LIMIT 1
        """,
    )
    suspend fun getByPeriod(period: String, userId: String): CategorySummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CategorySummaryEntity)
}
