package com.mascill.keutrack.feature.dashboard.data.service

import com.mascill.keutrack.core.network.model.GenericListResponse
import com.mascill.keutrack.feature.dashboard.data.models.RouteResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Sample Retrofit service interface for a REST list endpoint.
 *
 * Kept as a reference for declaring API methods with `@GET` / `@Query` and a shared
 * [GenericListResponse] envelope. Not called by the production Dashboard.
 */
interface RouteServices {

    /**
     * Sample: GET a paginated list from a remote API.
     *
     * Update the path and response type when adapting this pattern to a real endpoint.
     */
    @GET("api/v1/smpob-mobile/routes")
    suspend fun getRoutes(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("is_mikrotrans") isMikrotrans: Boolean = false,
    ): GenericListResponse<RouteResponse, Any>
}
