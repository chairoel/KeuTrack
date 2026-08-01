package com.mascill.keutrack.feature.dashboard.domain.models

/**
 * Sample domain model for a Retrofit API response mapped from [com.mascill.keutrack.feature.dashboard.data.models.RouteResponse].
 *
 * Kept as a reference for DTO → domain modeling. Not used by the production Dashboard.
 */
data class RouteDomain(
    val countTrip: Int,
    val maxSpeed: Int,
    val routeCode: String, // unique
    val routeColor: String,
    val routeName: String,
    val routeTextColor: String,
)
