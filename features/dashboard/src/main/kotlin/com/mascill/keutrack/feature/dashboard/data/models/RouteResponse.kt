package com.mascill.keutrack.feature.dashboard.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * data class to defined Route Response from api
 */
@JsonClass(generateAdapter = true)
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
