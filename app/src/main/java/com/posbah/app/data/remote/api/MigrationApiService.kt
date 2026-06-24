package com.posbah.app.data.remote.api

import retrofit2.Response
import retrofit2.http.*

// ─────────────────────────────────────────────────────────────────────────────
// Migration API
// ─────────────────────────────────────────────────────────────────────────────

data class VerifyTableRequest(
    val tableName: String,
    val expectedCount: Int
)

data class VerifyTableResponse(
    val match: Boolean,
    val serverCount: Int,
    val clientCount: Int,
    val tableName: String
)

data class ReadinessResponse(
    val ready: Boolean,
    val missingTables: List<String>,
    val existingCounts: Map<String, Int>,
    val tenantId: String
)

interface MigrationApiService {

    @POST("api/migration/verify-table")
    suspend fun verifyTable(@Body req: VerifyTableRequest): Response<VerifyTableResponse>

    @GET("api/migration/check-readiness")
    suspend fun checkReadiness(): Response<ReadinessResponse>
}
