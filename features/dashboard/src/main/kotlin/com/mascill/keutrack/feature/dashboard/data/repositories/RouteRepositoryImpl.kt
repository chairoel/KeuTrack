package com.mascill.keutrack.feature.dashboard.data.repositories

import com.mascill.keutrack.core.network.model.DomainResult
import com.mascill.keutrack.core.network.utils.processResponse
import com.mascill.keutrack.feature.dashboard.data.mapper.RoutesMapper
import com.mascill.keutrack.feature.dashboard.data.service.RouteServices
import com.mascill.keutrack.feature.dashboard.domain.models.RouteDomain
import com.mascill.keutrack.feature.dashboard.domain.repository.RouteRepository

class RouteRepositoryImpl(
    private val service: RouteServices,
    private val mapper: RoutesMapper
): RouteRepository {
    override suspend fun getRouteList(): DomainResult<List<RouteDomain>> {
        val response = service.getRoutes(limit = 999, offset = 0)
        return processResponse(response) {
            val mappedData = mapper.mapRoutesToDomain(routes = it.data)
            DomainResult.Success(mappedData)
        }
    }
}