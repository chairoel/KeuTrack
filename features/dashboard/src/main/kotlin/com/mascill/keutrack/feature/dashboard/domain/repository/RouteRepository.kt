package com.mascill.keutrack.feature.dashboard.domain.repository

import com.mascill.keutrack.core.network.model.DomainResult
import com.mascill.keutrack.feature.dashboard.domain.models.RouteDomain

/**
 * Sample domain repository contract for a Retrofit-backed remote API.
 *
 * Kept as a reference for the feature-local repository pattern when consuming REST endpoints.
 * Not part of KeuTrack's production financial data path (use cases in `:core:domain`).
 */
interface RouteRepository {

    /**
     * Sample: fetch a remote list and wrap the outcome in [DomainResult].
     */
    suspend fun getRouteList(): DomainResult<List<RouteDomain>>
}
