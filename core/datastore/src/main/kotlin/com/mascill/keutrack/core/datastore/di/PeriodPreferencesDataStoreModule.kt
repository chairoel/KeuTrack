package com.mascill.keutrack.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.mascill.keutrack.core.datastore.PeriodPreferences
import com.mascill.keutrack.core.datastore.serialization.PeriodPreferencesSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class PeriodPreferencesDataStoreModule {

    @Provides
    @Singleton
    fun providePeriodPreferencesDataStore(
        @ApplicationContext context: Context,
        serializer: PeriodPreferencesSerializer,
    ): DataStore<PeriodPreferences> =
        DataStoreFactory.create(
            serializer = serializer,
            corruptionHandler = ReplaceFileCorruptionHandler { serializer.defaultValue },
            migrations = listOf(),
            produceFile = { context.dataStoreFile("period_preferences.pb") },
        )
}
