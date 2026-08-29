package com.mascill.keutrack.feature.transaction.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.common.utils.PeriodBounds
import com.mascill.keutrack.core.domain.model.PeriodTotals
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetPeriodTotalsUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionsUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.ObservePeriodPreferencesUseCase
import com.mascill.keutrack.core.domain.usecase.RetryPendingSyncUseCase
import com.mascill.keutrack.feature.transaction.presentation.model.HistoryPeriod
import com.mascill.keutrack.feature.transaction.presentation.model.HistoryPeriodLabels
import com.mascill.keutrack.feature.transaction.presentation.model.HistoryPeriodPreset
import com.mascill.keutrack.feature.transaction.presentation.model.HistoryScope
import com.mascill.keutrack.feature.transaction.presentation.model.HistoryUIState
import com.mascill.keutrack.feature.transaction.presentation.model.TransactionUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    userRepository: UserRepository,
    private val getTransactions: GetTransactionsUseCase,
    private val getPeriodTotals: GetPeriodTotalsUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val getWalletSummary: GetWalletSummaryUseCase,
    private val retryPendingSync: RetryPendingSyncUseCase,
    observePeriodPreferences: ObservePeriodPreferencesUseCase,
    private val dispatcher: CommonDispatcher,
) : ViewModel() {

    private val scope = readHistoryScope(savedStateHandle)
    private val period = MutableStateFlow(readPeriod(savedStateHandle))
    private val periodRangeError = MutableStateFlow<String?>(null)
    private val cycleStartDay = observePeriodPreferences().map { it.cycleStartDay }
    private val periodContext =
        combine(period, cycleStartDay) { selection, startDay -> selection to startDay }

    private val queryContext =
        when (scope) {
            HistoryScope.Family -> {
                combine(userRepository.getCurrentUser(), periodContext) { user, context ->
                    val familyId = user?.familyId
                    val (selection, startDay) = context
                    HistoryQuery(
                        familyId = familyId,
                        period = selection,
                        cycleStartDay = startDay,
                        canQuery = !familyId.isNullOrBlank(),
                    )
                }
            }

            HistoryScope.Personal -> {
                combine(getWalletSummary(), periodContext) { summary, context ->
                    val walletId = summary.personalWallet?.id
                    val (selection, startDay) = context
                    HistoryQuery(
                        walletId = walletId,
                        period = selection,
                        cycleStartDay = startDay,
                        canQuery = !walletId.isNullOrBlank(),
                    )
                }
            }

            HistoryScope.All -> {
                periodContext.map { (selection, startDay) ->
                    HistoryQuery(
                        period = selection,
                        cycleStartDay = startDay,
                        canQuery = true,
                    )
                }
            }
        }

    private val transactionsFlow =
        queryContext.flatMapLatest { query ->
            if (!query.canQuery) {
                flowOf(emptyList())
            } else {
                getTransactions(
                    transactionParams(
                        period = query.period,
                        cycleStartDay = query.cycleStartDay,
                        walletId = query.walletId,
                        familyId = query.familyId,
                    ),
                )
            }
        }

    private val totalsFlow =
        queryContext.flatMapLatest { query ->
            if (!query.canQuery) {
                flowOf(PeriodTotals())
            } else {
                getPeriodTotals(
                    periodTotalsParams(
                        period = query.period,
                        cycleStartDay = query.cycleStartDay,
                        walletId = query.walletId,
                        familyId = query.familyId,
                    ),
                )
            }
        }

    val uiState: StateFlow<HistoryUIState> =
        combine(
            combine(transactionsFlow, totalsFlow) { transactions, totals ->
                transactions to totals
            },
            getCategories(),
            getWalletSummary(),
            periodContext,
            periodRangeError,
        ) { listAndTotals, categories, walletSummary, context, rangeError ->
            val (transactions, totals) = listAndTotals
            val (selection, startDay) = context
            val categoriesById = categories.associateBy { it.id }
            val walletsById = TransactionUiMapper.mapWallets(walletSummary)
            HistoryUIState(
                isLoading = false,
                items =
                    TransactionUiMapper.toTransactionRows(
                        transactions = transactions,
                        categoriesById = categoriesById,
                        walletsById = walletsById,
                    ),
                errorMessage = null,
                scope = scope,
                periodPreset = selection.preset,
                customFrom = selection.customFrom,
                customTo = selection.customTo,
                periodSummaryLabel =
                    HistoryPeriodLabels.summary(
                        preset = selection.preset,
                        customFrom = selection.customFrom,
                        customTo = selection.customTo,
                        cycleStartDay = startDay,
                    ),
                hasActivePeriodFilter = selection.hasActiveFilter,
                periodRangeError = rangeError,
                incomeTotal = totals.incomeTotal,
                expenseTotal = totals.expenseTotal,
            )
        }.catch { e ->
            emit(
                HistoryUIState(
                    isLoading = false,
                    errorMessage = e.message ?: ERR_LOAD_FAILED,
                    scope = scope,
                ),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUIState(scope = scope),
        )

    fun onScreenRendered() {
        viewModelScope.launch(dispatcher.io) {
            try {
                retryPendingSync()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort; rows already show local sync badges.
            }
        }
    }

    fun onPeriodPresetSelected(preset: HistoryPeriodPreset) {
        if (preset == HistoryPeriodPreset.Custom) return
        applyPeriod(HistoryPeriod(preset = preset))
    }

    fun onCustomRangeConfirmed(from: LocalDate, to: LocalDate) {
        val today = LocalDate.now()
        val clampedFrom = minOf(from, today)
        val clampedTo = minOf(to, today)
        if (clampedFrom.isAfter(clampedTo)) {
            periodRangeError.value = ERR_INVALID_RANGE
            return
        }
        applyPeriod(
            HistoryPeriod(
                preset = HistoryPeriodPreset.Custom,
                customFrom = clampedFrom,
                customTo = clampedTo,
            ),
        )
    }

    fun onClearPeriodFilter() {
        applyPeriod(HistoryPeriod())
    }

    private fun applyPeriod(next: HistoryPeriod) {
        period.value = next
        periodRangeError.value = null
        persistPeriod(next)
    }

    private fun persistPeriod(next: HistoryPeriod) {
        savedStateHandle[KEY_PERIOD_PRESET] = next.preset.name
        savedStateHandle[KEY_CUSTOM_FROM] = next.customFrom?.toEpochDay()
        savedStateHandle[KEY_CUSTOM_TO] = next.customTo?.toEpochDay()
    }

    private fun transactionParams(
        period: HistoryPeriod,
        cycleStartDay: Int,
        walletId: String? = null,
        familyId: String? = null,
    ): GetTransactionsUseCase.Params {
        val range = instantRange(period, cycleStartDay)
        return GetTransactionsUseCase.Params(
            walletId = walletId,
            familyId = familyId,
            startDate = range?.start,
            endDate = range?.endInclusive,
            limit = if (scope == HistoryScope.Family) FAMILY_HISTORY_LIMIT else HISTORY_LIMIT,
        )
    }

    private fun periodTotalsParams(
        period: HistoryPeriod,
        cycleStartDay: Int,
        walletId: String? = null,
        familyId: String? = null,
    ): GetPeriodTotalsUseCase.Params {
        val range = instantRange(period, cycleStartDay)
        return GetPeriodTotalsUseCase.Params(
            walletId = walletId,
            familyId = familyId,
            startDate = range?.start,
            endDate = range?.endInclusive,
        )
    }

    private fun instantRange(
        period: HistoryPeriod,
        cycleStartDay: Int,
    ): ClosedRange<Instant>? =
        when (period.preset) {
            HistoryPeriodPreset.All -> null
            HistoryPeriodPreset.Last7Days -> {
                val today = LocalDate.now()
                PeriodBounds.ofLocalDates(today.minusDays(LAST_7_INCLUSIVE_OFFSET), today)
            }
            HistoryPeriodPreset.CurrentMonth ->
                PeriodBounds.containing(LocalDate.now(), cycleStartDay).toInstantRange()
            HistoryPeriodPreset.Custom -> {
                val from = period.customFrom ?: return null
                val to = period.customTo ?: return null
                PeriodBounds.ofLocalDates(from, to)
            }
        }

    private data class HistoryQuery(
        val walletId: String? = null,
        val familyId: String? = null,
        val period: HistoryPeriod,
        val cycleStartDay: Int,
        val canQuery: Boolean,
    )

    private companion object {
        const val ARG_FAMILY_ONLY = "familyOnly"
        const val ARG_PERSONAL_ONLY = "personalOnly"
        const val KEY_PERIOD_PRESET = "periodPreset"
        const val KEY_CUSTOM_FROM = "customFromEpochDay"
        const val KEY_CUSTOM_TO = "customToEpochDay"
        const val HISTORY_LIMIT = 50
        const val FAMILY_HISTORY_LIMIT = 200
        const val LAST_7_INCLUSIVE_OFFSET = 6L
        const val ERR_LOAD_FAILED = "Gagal memuat riwayat transaksi"
        const val ERR_INVALID_RANGE = "Tanggal mulai tidak boleh setelah tanggal akhir."

        fun readHistoryScope(savedStateHandle: SavedStateHandle): HistoryScope =
            when {
                readFlag(savedStateHandle, ARG_FAMILY_ONLY) -> HistoryScope.Family
                readFlag(savedStateHandle, ARG_PERSONAL_ONLY) -> HistoryScope.Personal
                else -> HistoryScope.All
            }

        fun readFlag(savedStateHandle: SavedStateHandle, key: String): Boolean =
            when (val value = savedStateHandle.get<Any>(key)) {
                is Boolean -> value
                is String -> value.toBoolean()
                else -> false
            }

        fun readPeriod(handle: SavedStateHandle): HistoryPeriod {
            val preset = readPreset(handle) ?: HistoryPeriodPreset.All
            val from = readEpochDay(handle, KEY_CUSTOM_FROM)?.let { LocalDate.ofEpochDay(it) }
            val to = readEpochDay(handle, KEY_CUSTOM_TO)?.let { LocalDate.ofEpochDay(it) }
            return if (preset == HistoryPeriodPreset.Custom) {
                if (from != null && to != null && !from.isAfter(to)) {
                    HistoryPeriod(preset, from, to)
                } else {
                    HistoryPeriod()
                }
            } else {
                HistoryPeriod(preset = preset)
            }
        }

        fun readPreset(handle: SavedStateHandle): HistoryPeriodPreset? {
            val raw = handle.get<String>(KEY_PERIOD_PRESET) ?: return null
            return try {
                HistoryPeriodPreset.valueOf(raw)
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        fun readEpochDay(handle: SavedStateHandle, key: String): Long? =
            when (val value = handle.get<Any>(key)) {
                is Long -> value
                is Int -> value.toLong()
                is String -> value.toLongOrNull()
                else -> null
            }
    }
}
