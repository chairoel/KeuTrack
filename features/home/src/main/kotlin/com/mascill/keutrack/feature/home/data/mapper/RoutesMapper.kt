package com.mascill.keutrack.feature.home.data.mapper

import com.mascill.keutrack.core.common.utils.orZero
import com.mascill.keutrack.core.common.utils.toHexColor
import com.mascill.keutrack.feature.home.data.models.RouteResponse
import com.mascill.keutrack.feature.home.domain.models.RouteDomain

/**
 * Mapper class to map data model to domain model or vise versa
 */
class RoutesMapper {
    /**
     * Mapping listOf [RouteResponse] data model to listOf [RouteDomain] domain model
     */
    fun mapRoutesToDomain(routes: List<RouteResponse?>?): List<RouteDomain> = routes?.map {
        RouteDomain(
            countTrip = it?.countTrip.orZero(),
            maxSpeed = it?.maxSpeed.orZero(),
            routeCode = it?.routeCode.orEmpty(),
            routeColor = it?.routeColor?.toHexColor().orEmpty(),
            routeName = it?.routeName.orEmpty(),
            routeTextColor = it?.routeTextColor?.toHexColor().orEmpty()
        )
    }.orEmpty()
}