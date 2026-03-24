package com.mascill.keutrack.feature.splashscreen.di

import com.mascill.keutrack.feature.splashscreen.data.mapper.RoutesMapper
import com.mascill.keutrack.feature.splashscreen.data.repositories.RouteRepositoryImpl
import com.mascill.keutrack.feature.splashscreen.data.service.RouteServices
import com.mascill.keutrack.feature.splashscreen.domain.repository.RouteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import retrofit2.Retrofit

/**
 * Hilt Module that represent splash screen component and contributes to the object
 * graph [ViewModelComponent].
 *
 * @see Module
 */
@Module
@InstallIn(ViewModelComponent::class)
class SplashModule {

    /**
     * Create a provider method binding for [RouteServices] API.
     *
     * @return Instance of service.
     * @see Provides
     * @see ViewModelScoped
     */
    @ViewModelScoped
    @Provides
    fun provideRouteService(
        retrofit: Retrofit
    ): RouteServices = retrofit.create(RouteServices::class.java)

    /**
     * Create a provide method binding for [RouteRepository].
     *
     * @return Instance of [RouteRepository]
     */
    @ViewModelScoped
    @Provides
    fun provideRouteRepository(
        service: RouteServices,
        mapper: RoutesMapper
    ): RouteRepository = RouteRepositoryImpl(service = service, mapper = mapper)

    /**
     * Create a provide method binding for [RoutesMapper].
     *
     * @return Instance of [RoutesMapper]
     */
    @Provides
    fun provideDataMapper(): RoutesMapper = RoutesMapper()
}