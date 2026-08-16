package com.mascill.keutrack.core.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.data.datasource.AuthNetworkDataSource
import com.mascill.keutrack.core.data.datasource.local.WalletLocalDataSource
import com.mascill.keutrack.core.data.db.entity.WalletEntity
import com.mascill.keutrack.core.data.mapper.WalletMapper
import com.mascill.keutrack.core.data.model.AuthUserResponse
import com.mascill.keutrack.core.data.sync.SyncScheduler
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class WalletRepositoryImplTest {

    private val local = mockk<WalletLocalDataSource>(relaxed = true)
    private val mapper = WalletMapper()
    private val authDS = mockk<AuthNetworkDataSource>(relaxed = true)
    private val syncScheduler = mockk<SyncScheduler>(relaxed = true)
    private val repo = WalletRepositoryImpl(local, mapper, authDS, syncScheduler)

    @Test
    fun `createWallet writes DAO as pending and enqueues sync`() = runTest {
        val wallet = domainWallet()
        coEvery { local.upsert(any()) } just runs

        repo.createWallet(wallet)

        coVerify {
            local.upsert(match { it.id == "w-1" && it.syncStatus == SyncStatus.PENDING.name })
        }
        verify { syncScheduler.enqueueSync() }
    }

    @Test
    fun `observeWallets emits mapped domain`() = runTest {
        every { local.observeAll() } returns flowOf(listOf(entityWallet()))
        coEvery { local.getPersonal() } returns entityWallet()

        repo.observeWallets().test {
            val wallets = awaitItem()
            assertThat(wallets).hasSize(1)
            assertThat(wallets.first().id).isEqualTo("w-1")
            assertThat(wallets.first().type).isEqualTo(WalletType.PERSONAL)
            awaitComplete()
        }
    }

    @Test
    fun `observeWalletsByType filters via local source`() = runTest {
        every { local.observeByType("family") } returns flowOf(listOf(entityWallet(type = "family")))
        coEvery { local.getPersonal() } returns entityWallet()

        repo.observeWalletsByType(WalletType.FAMILY).test {
            val wallets = awaitItem()
            assertThat(wallets.first().type).isEqualTo(WalletType.FAMILY)
            awaitComplete()
        }
        verify { local.observeByType("family") }
    }

    @Test
    fun `getPersonalWallet creates default when missing`() = runTest {
        coEvery { local.getPersonal() } returns null
        every { authDS.getCurrentUser() } returns AuthUserResponse("user-1", "Irul", "a@b.c", null)
        coEvery { local.upsert(any()) } just runs

        val created = repo.getPersonalWallet()

        assertThat(created).isNotNull()
        assertThat(created!!.type).isEqualTo(WalletType.PERSONAL)
        assertThat(created.name).isEqualTo("Dompet Utama")
        coVerify { local.upsert(match { it.type == "personal" && it.ownerId == "user-1" }) }
    }

    private fun domainWallet() = Wallet(
        id = "w-1",
        ownerId = "user-1",
        name = "Dompet Utama",
        type = WalletType.PERSONAL,
        balance = 10_000L,
        createdAt = Instant.parse("2026-08-01T00:00:00Z"),
    )

    private fun entityWallet(type: String = "personal") = WalletEntity(
        id = "w-1",
        ownerId = "user-1",
        familyId = if (type == "family") "fam-1" else null,
        name = "Dompet",
        type = type,
        balance = 10_000L,
        currency = "IDR",
        icon = null,
        color = null,
        syncStatus = "SYNCED",
        createdAtEpochMs = Instant.parse("2026-08-01T00:00:00Z").toEpochMilli(),
    )
}
