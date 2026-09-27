package com.mascill.keutrack.feature.transaction.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.common.utils.PeriodBounds
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.PeriodTotals
import com.mascill.keutrack.core.domain.model.TransactionWriteResult
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.DeleteTransactionUseCase
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetPeriodTotalsUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionsUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.ObservePeriodPreferencesUseCase
import com.mascill.keutrack.core.domain.usecase.RetryPendingSyncUseCase
import com.mascill.keutrack.feature.transaction.presentation.model.HistoryAuthorOption
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
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val familyRepository: FamilyRepository,
    private val getTransactions: GetTransactionsUseCase,
    private val getPeriodTotals: GetPeriodTotalsUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val getWalletSummary: GetWalletSummaryUseCase,
    private val retryPendingSync: RetryPendingSyncUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    observePeriodPreferences: ObservePeriodPreferencesUseCase,
    private val dispatcher: CommonDispatcher,
) : ViewModel() {

    private val scope = readHistoryScope(savedStateHandle)
    private val period = MutableStateFlow(readPeriod(savedStateHandle))
    private val selectedUserId = MutableStateFlow(readAuthor(savedStateHandle))
    private val periodRangeError = MutableStateFlow<String?>(null)
    private val noticeMessage = MutableStateFlow<String?>(null)
    private val isDeleting = MutableStateFlow(false)
    private val cycleStartDay = observePeriodPreferences().map { it.cycleStartDay }
    private val periodContext =
        combine(period, cycleStartDay) { selection, startDay -> selection to startDay }
    private val authorContext =
        combine(
            userRepository.getCurrentUser(),
            familyRepository.observeCurrentFamily(),
            selectedUserId,
        ) { user, family, selected ->
            val options = buildAuthorOptions(scope, family, user?.uid)
            val effective = effectiveAuthorId(selected, options)
            if (selected != effective) {
                persistAuthor(effective)
            }
            AuthorContext(options = options, userId = effective)
        }

    private val queryContext =
        when (scope) {
            HistoryScope.Family -> {
                combine(
                    userRepository.getCurrentUser(),
                    periodContext,
                    authorContext,
                ) { user, context, author ->
                    val familyId = user?.familyId
                    val (selection, startDay) = context
                    HistoryQuery(
                        familyId = familyId,
                        userId = author.userId,
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
                combine(periodContext, authorContext) { context, author ->
                    val (selection, startDay) = context
                    HistoryQuery(
                        userId = author.userId,
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
                        userId = query.userId,
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
                        userId = query.userId,
                    ),
                )
            }
        }

    val uiState: StateFlow<HistoryUIState> =
        combine(
            combine(transactionsFlow, totalsFlow) { transactions, totals ->
                transactions to totals
            },
            combine(userRepository.getCurrentUser(), periodRangeError, noticeMessage) {
                    user,
                    rangeError,
                    notice,
                ->
                Triple(user?.uid, rangeError, notice)
            },
            getCategories(),
            getWalletSummary(),
            combine(periodContext, isDeleting, authorContext) { context, deleting, author ->
                Triple(context, deleting, author)
            },
        ) { listAndTotals, userContext, categories, walletSummary, periodDeletingAuthor ->
            val (transactions, totals) = listAndTotals
            val (currentUserId, rangeError, notice) = userContext
            val (context, deleting, author) = periodDeletingAuthor
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
                        currentUserId = currentUserId,
                    ),
                errorMessage = notice,
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
                isDeleting = deleting,
                authorUserId = author.userId,
                authorOptions = author.options,
                hasActiveAuthorFilter = author.userId != null,
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

    fun onAuthorSelected(userId: String?) {
        val normalized = userId?.trim()?.takeIf { it.isNotBlank() }
        selectedUserId.value = normalized
        persistAuthor(normalized)
    }

    fun onClearAuthorFilter() {
        onAuthorSelected(null)
    }

    fun onReadOnlyTransactionTapped() {
        noticeMessage.value = ERR_NOT_OWNER
    }

    fun onDeleteConfirmed(id: String) {
        if (id.isBlank() || !isDeleting.compareAndSet(expect = false, update = true)) return
        noticeMessage.value = null
        viewModelScope.launch(dispatcher.io) {
            try {
                when (val result = deleteTransaction(id)) {
                    TransactionWriteResult.Success -> Unit
                    TransactionWriteResult.Error.NotOwner -> {
                        noticeMessage.value = ERR_NOT_OWNER
                    }
                    TransactionWriteResult.Error.MissingId,
                    TransactionWriteResult.Error.NotFound,
                    TransactionWriteResult.Error.InvalidAmount,
                    TransactionWriteResult.Error.MissingWallet,
                    TransactionWriteResult.Error.MissingCategory,
                    -> {
                        noticeMessage.value = ERR_DELETE_FAILED
                    }
                    is TransactionWriteResult.Error.Unknown -> {
                        noticeMessage.value = result.cause.message ?: ERR_DELETE_FAILED
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                noticeMessage.value = e.message ?: ERR_DELETE_FAILED
            } finally {
                isDeleting.value = false
            }
        }
    }

    fun dismissNotice() {
        noticeMessage.value = null
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

    private fun persistAuthor(userId: String?) {
        if (userId == null) {
            savedStateHandle.remove<String>(KEY_AUTHOR_USER_ID)
        } else {
            savedStateHandle[KEY_AUTHOR_USER_ID] = userId
        }
    }

    private fun transactionParams(
        period: HistoryPeriod,
        cycleStartDay: Int,
        walletId: String? = null,
        familyId: String? = null,
        userId: String? = null,
    ): GetTransactionsUseCase.Params {
        val range = instantRange(period, cycleStartDay)
        return GetTransactionsUseCase.Params(
            walletId = walletId,
            familyId = familyId,
            startDate = range?.start,
            endDate = range?.endInclusive,
            limit = if (scope == HistoryScope.Family) FAMILY_HISTORY_LIMIT else HISTORY_LIMIT,
            userId = userId,
        )
    }

    private fun periodTotalsParams(
        period: HistoryPeriod,
        cycleStartDay: Int,
        walletId: String? = null,
        familyId: String? = null,
        userId: String? = null,
    ): GetPeriodTotalsUseCase.Params {
        val range = instantRange(period, cycleStartDay)
        return GetPeriodTotalsUseCase.Params(
            walletId = walletId,
            familyId = familyId,
            startDate = range?.start,
            endDate = range?.endInclusive,
            userId = userId,
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
        val userId: String? = null,
        val period: HistoryPeriod,
        val cycleStartDay: Int,
        val canQuery: Boolean,
    )

    private data class AuthorContext(
        val options: List<HistoryAuthorOption>,
        val userId: String?,
    )

    private companion object {
        const val ARG_FAMILY_ONLY = "familyOnly"
        const val ARG_PERSONAL_ONLY = "personalOnly"
        const val KEY_PERIOD_PRESET = "periodPreset"
        const val KEY_CUSTOM_FROM = "customFromEpochDay"
        const val KEY_CUSTOM_TO = "customToEpochDay"
        const val KEY_AUTHOR_USER_ID = "authorUserId"
        const val AUTHOR_LABEL_ALL = "Semua"
        const val AUTHOR_LABEL_ME = "Saya"
        const val AUTHOR_LABEL_FALLBACK = "Anggota"
        val AUTHOR_LABEL_LOCALE: Locale = Locale.forLanguageTag("id-ID")
        const val HISTORY_LIMIT = 50
        const val FAMILY_HISTORY_LIMIT = 200
        const val LAST_7_INCLUSIVE_OFFSET = 6L
        const val ERR_LOAD_FAILED = "Gagal memuat riwayat transaksi"
        const val ERR_INVALID_RANGE = "Tanggal mulai tidak boleh setelah tanggal akhir."
        const val ERR_NOT_OWNER = "Hanya penulis yang bisa mengubah transaksi ini"
        const val ERR_DELETE_FAILED = "Gagal menghapus transaksi"

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

        fun readAuthor(handle: SavedStateHandle): String? =
            handle.get<String>(KEY_AUTHOR_USER_ID)?.trim()?.takeIf { it.isNotBlank() }

        fun buildAuthorOptions(
            scope: HistoryScope,
            family: FamilyGroup?,
            currentUid: String?,
        ): List<HistoryAuthorOption> {
            if (scope == HistoryScope.Personal || family == null) return emptyList()
            val otherIds =
                family.memberIds
                    .distinct()
                    .filter { it.isNotBlank() && it != currentUid }
            if (otherIds.isEmpty()) return emptyList()
            val others =
                otherIds
                    .map { id ->
                        HistoryAuthorOption(
                            userId = id,
                            label = family.memberNames[id]?.takeIf { it.isNotBlank() }
                                ?: AUTHOR_LABEL_FALLBACK,
                        )
                    }
                    .sortedBy { it.label.lowercase(AUTHOR_LABEL_LOCALE) }
            return buildList {
                add(HistoryAuthorOption(userId = null, label = AUTHOR_LABEL_ALL))
                currentUid?.takeIf { it.isNotBlank() }?.let { uid ->
                    add(HistoryAuthorOption(userId = uid, label = AUTHOR_LABEL_ME))
                }
                addAll(others)
            }
        }

        fun effectiveAuthorId(
            selected: String?,
            options: List<HistoryAuthorOption>,
        ): String? {
            if (options.isEmpty() || selected.isNullOrBlank()) return null
            return selected.takeIf { id -> options.any { it.userId == id } }
        }
    }
}
