package com.mascill.keutrack.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mascill.keutrack.core.data.db.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {

    @Query("SELECT * FROM wallets ORDER BY createdAtEpochMs ASC")
    fun observeAll(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE type = :type ORDER BY createdAtEpochMs ASC")
    fun observeByType(type: String): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE id = :walletId LIMIT 1")
    fun observeById(walletId: String): Flow<WalletEntity?>

    @Query("SELECT * FROM wallets WHERE id = :walletId LIMIT 1")
    suspend fun getById(walletId: String): WalletEntity?

    @Query("SELECT * FROM wallets WHERE type = 'personal' ORDER BY createdAtEpochMs ASC LIMIT 1")
    suspend fun getPersonal(): WalletEntity?

    @Query("SELECT * FROM wallets WHERE type = :type ORDER BY createdAtEpochMs ASC")
    suspend fun getByType(type: String): List<WalletEntity>

    @Query("SELECT * FROM wallets WHERE familyId = :familyId ORDER BY createdAtEpochMs ASC")
    suspend fun getByFamilyId(familyId: String): List<WalletEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WalletEntity)

    @Update
    suspend fun update(entity: WalletEntity)

    @Query("UPDATE wallets SET balance = balance + :delta, syncStatus = :syncStatus WHERE id = :walletId")
    suspend fun applyBalanceDelta(walletId: String, delta: Long, syncStatus: String)

    @Query("DELETE FROM wallets WHERE id = :walletId")
    suspend fun deleteById(walletId: String)

    @Query("SELECT * FROM wallets WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getPending(): List<WalletEntity>

    @Query("UPDATE wallets SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
