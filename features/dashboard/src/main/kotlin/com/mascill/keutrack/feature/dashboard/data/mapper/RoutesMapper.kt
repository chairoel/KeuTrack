package com.mascill.keutrack.feature.dashboard.data.mapper

import com.mascill.keutrack.core.common.utils.orZero
import com.mascill.keutrack.core.common.utils.toHexColor
import com.mascill.keutrack.feature.dashboard.data.models.RouteResponse
import com.mascill.keutrack.feature.dashboard.domain.models.RouteDomain

/**
 * Sample mapper from Retrofit/Moshi DTOs ([RouteResponse]) to domain models ([RouteDomain]).
 *
 * Kept as a reference for null-safe list mapping and field defaults. Not used by the
 * production Dashboard.
 */
class RoutesMapper {

    /**
     * Maps a nullable list of [RouteResponse] into a non-null list of [RouteDomain].
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
