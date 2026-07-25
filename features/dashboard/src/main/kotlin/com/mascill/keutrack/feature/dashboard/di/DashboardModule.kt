package com.mascill.keutrack.feature.dashboard.di

import com.mascill.keutrack.feature.dashboard.data.mapper.RoutesMapper
import com.mascill.keutrack.feature.dashboard.data.repositories.RouteRepositoryImpl
import com.mascill.keutrack.feature.dashboard.data.service.RouteServices
import com.mascill.keutrack.feature.dashboard.domain.repository.RouteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import retrofit2.Retrofit

/**
 * Sample Hilt module that wires a Retrofit API stack (service → mapper → repository).
 *
 * Kept as a reference for how to consume REST APIs with Retrofit + Hilt in a feature module.
 * Not used by the production Dashboard (Phase 4+); financial data comes from domain use cases.
 *
 * @see RouteServices
 * @see RouteRepository
 */
@Module
@InstallIn(ViewModelComponent::class)
class DashboardModule {

    /**
     * Provides a Retrofit-generated [RouteServices] implementation.
     *
     * Sample: create an API interface instance from the shared [Retrofit] client.
     */
    @ViewModelScoped
    @Provides
    fun provideRouteService(
        retrofit: Retrofit
    ): RouteServices = retrofit.create(RouteServices::class.java)

    /**
     * Provides [RouteRepository] backed by [RouteRepositoryImpl].
     *
     * Sample: bind the feature repository so a ViewModel can inject it.
     */
    @ViewModelScoped
    @Provides
    fun provideRouteRepository(
        service: RouteServices,
        mapper: RoutesMapper
    ): RouteRepository = RouteRepositoryImpl(service = service, mapper = mapper)

    /**
     * Provides [RoutesMapper] for DTO → domain mapping.
     *
     * Sample: keep mappers injectable when they have (or may gain) dependencies.
     */
    @Provides
    fun provideDataMapper(): RoutesMapper = RoutesMapper()
}
