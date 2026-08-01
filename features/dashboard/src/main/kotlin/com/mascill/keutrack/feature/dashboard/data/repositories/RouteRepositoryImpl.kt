package com.mascill.keutrack.feature.dashboard.data.repositories

import com.mascill.keutrack.core.network.model.DomainResult
import com.mascill.keutrack.core.network.utils.processResponse
import com.mascill.keutrack.feature.dashboard.data.mapper.RoutesMapper
import com.mascill.keutrack.feature.dashboard.data.service.RouteServices
import com.mascill.keutrack.feature.dashboard.domain.models.RouteDomain
import com.mascill.keutrack.feature.dashboard.domain.repository.RouteRepository

/**
 * Sample [RouteRepository] implementation that calls Retrofit and maps DTOs to domain models.
 *
 * Kept as a reference for: invoke service → [processResponse] → map with [RoutesMapper] →
 * return [DomainResult]. Not used by the production Dashboard ViewModel.
 */
class RouteRepositoryImpl(
    private val service: RouteServices,
    private val mapper: RoutesMapper
) : RouteRepository {

    override suspend fun getRouteList(): DomainResult<List<RouteDomain>> {
        val response = service.getRoutes(limit = 999, offset = 0)
        return processResponse(response) {
            val mappedData = mapper.mapRoutesToDomain(routes = it.data)
            DomainResult.Success(mappedData)
        }
    }
}
