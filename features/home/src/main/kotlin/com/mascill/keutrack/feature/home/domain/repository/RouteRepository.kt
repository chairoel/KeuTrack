package com.mascill.keutrack.feature.home.domain.repository

import com.mascill.keutrack.core.network.model.DomainResult
import com.mascill.keutrack.feature.home.domain.models.RouteDomain

/**
 * Abstract class that used as bridge between domain layer and data layer for
 * SMPOB-mobile Route API network operations.
 */
interface RouteRepository {

    /**
     * Get list of SMPOB-mobile route
     */
    suspend fun getRouteList(): DomainResult<List<RouteDomain>>
}