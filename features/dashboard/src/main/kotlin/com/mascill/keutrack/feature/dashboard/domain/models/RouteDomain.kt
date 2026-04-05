package com.mascill.keutrack.feature.dashboard.domain.models

/**
 * data class to defined Route Domain
 */
data class RouteDomain(
    val countTrip: Int,
    val maxSpeed: Int,
    val routeCode: String, // unique
    val routeColor: String,
    val routeName: String,
    val routeTextColor: String,
)
