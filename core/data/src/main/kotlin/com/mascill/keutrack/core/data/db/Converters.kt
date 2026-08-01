package com.mascill.keutrack.core.data.db

import androidx.room.TypeConverter
import com.mascill.keutrack.core.domain.model.BudgetPeriod
import com.mascill.keutrack.core.domain.model.CategoryBreakdown
import com.mascill.keutrack.core.domain.model.CategoryType
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.WalletType
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.Instant

/**
 * Room type converters for Instant, domain enums, and CategorySummary breakdown map.
 *
 * Entities primarily persist primitives (Long/String); these converters are registered
 * for Instant/enum columns and for Moshi JSON used by CategorySummary.
 */
class Converters {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val breakdownMapAdapter = moshi.adapter<Map<String, CategoryBreakdown>>(
        Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            CategoryBreakdown::class.java,
        ),
    )

    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun transactionTypeToString(value: TransactionType?): String? = value?.value

    @TypeConverter
    fun stringToTransactionType(value: String?): TransactionType? =
        value?.let(TransactionType::fromValue)

    @TypeConverter
    fun walletTypeToString(value: WalletType?): String? = value?.value

    @TypeConverter
    fun stringToWalletType(value: String?): WalletType? =
        value?.let(WalletType::fromValue)

    @TypeConverter
    fun categoryTypeToString(value: CategoryType?): String? = value?.value

    @TypeConverter
    fun stringToCategoryType(value: String?): CategoryType? =
        value?.let(CategoryType::fromValue)

    @TypeConverter
    fun budgetPeriodToString(value: BudgetPeriod?): String? = value?.value

    @TypeConverter
    fun stringToBudgetPeriod(value: String?): BudgetPeriod? =
        value?.let(BudgetPeriod::fromValue)

    @TypeConverter
    fun syncStatusToString(value: SyncStatus?): String? = value?.name

    @TypeConverter
    fun stringToSyncStatus(value: String?): SyncStatus? =
        value?.let { runCatching { SyncStatus.valueOf(it) }.getOrDefault(SyncStatus.PENDING) }

    @TypeConverter
    fun breakdownMapToJson(value: Map<String, CategoryBreakdown>?): String? =
        value?.let(breakdownMapAdapter::toJson)

    @TypeConverter
    fun jsonToBreakdownMap(value: String?): Map<String, CategoryBreakdown>? =
        value?.takeIf { it.isNotBlank() }?.let { json ->
            runCatching { breakdownMapAdapter.fromJson(json) }.getOrNull()
        } ?: emptyMap()
}
