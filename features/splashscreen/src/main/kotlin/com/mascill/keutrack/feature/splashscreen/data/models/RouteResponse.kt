package com.mascill.keutrack.feature.splashscreen.data.models

import com.squareup.moshi.Json

/**
 * data class to defined Route Response from api
 */
data class RouteResponse(
    @Json(name = "count_trip")
    val countTrip: Int?,
    @Json(name = "max_speed")
    val maxSpeed: Int?,
    @Json(name = "route_code")
    val routeCode: String, // unique and mandatory
    @Json(name = "route_color")
    val routeColor: String?,
    @Json(name = "route_name")
    val routeName: String?,
    @Json(name = "route_text_color")
    val routeTextColor: String?,
)
