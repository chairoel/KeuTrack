package com.mascill.keutrack.feature.splashscreen.data.service

import com.mascill.keutrack.core.network.model.GenericResponse
import com.mascill.keutrack.feature.splashscreen.data.models.RouteResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Representation interface of the SMPOB-Mobile route API endpoints.
 */
interface RouteServices {
    /**
     * Get SMPOB-mobile route list.
     *
     * Note: currently this used as example, please update success response if this turn to real api
     */
    @GET("api/v1/smpob-mobile/routes")
    suspend fun getRoutes(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("is_mikrotrans") isMikrotrans: Boolean = false,
    ): GenericResponse<List<RouteResponse>, Any>
}