package com.mascill.keutrack.core.network.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.mascill.keutrack.core.network.adapter.EmptyAdapter
import com.mascill.keutrack.core.network.adapter.NetworkResponseAdapterFactory
import com.mascill.keutrack.core.network.utils.NetworkNativeWrapper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Class that contributes to the object graph [SingletonComponent].
 *
 * @see Module
 */

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val NETWORK_TIMEOUT: Long = 30 // seconds

    /**
     * Create Native C++ Network provider
     */
    @Singleton
    @Provides
    fun provideNetworkNativeWrapper(): NetworkNativeWrapper = NetworkNativeWrapper()

    /**
     * Create a provider method binding for [Moshi].
     *
     * @return Instance of moshi.
     * @see Provides
     */
    @Singleton
    @Provides
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /**
     * Create a base provider method binding for [ChuckerInterceptor] configuration.
     *
     * @return Instance of ChuckerInterceptor.
     * @see Provides
     *
     * https://m-smpobapi-dev.transjakarta.co.id/api/v1/smpob-mobile/routes?limit=999&offset=0&is_mikrotrans=true
     */
    @Singleton
    @Provides
    fun provideChuckerConfig(
        @ApplicationContext context: Context
    ): ChuckerInterceptor = ChuckerInterceptor.Builder(context)
        // The max body content length in bytes, after this responses will be truncated.
        .maxContentLength(250_000L)
        // List of headers to replace with ** in the Chucker UI
        .redactHeaders("Authorization", "Auth")
        // Read the whole response body even when the client does not consume the response completely.
        // This is useful in case of parsing errors or when the response body
        // is closed before being read like in Retrofit with Void and Unit types.
        .alwaysReadResponseBody(true)
        // Controls Android shortcut creation.
        .createShortcut(true)
        .build()

    /**
     * Create a base provider method binding for [OkHttpClient.Builder].
     *
     * @return Instance of OkHttpClient Builder.
     * @see Provides
     */
    @Singleton
    @Provides
    fun provideOkhttpClient(chuckerInterceptor: ChuckerInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .callTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            .addNetworkInterceptor(chuckerInterceptor)
            .build()
    }

    /**
     * Create a provider method binding for [Retrofit].
     *
     * @return Instance of retrofit.
     * @see Provides
     */
    @Singleton
    @Provides
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
        networkNativeWrapper: NetworkNativeWrapper
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(networkNativeWrapper.getBaseUrl())
            .client(okHttpClient)
            .addCallAdapterFactory(NetworkResponseAdapterFactory())
            .addConverterFactory(EmptyAdapter())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

}