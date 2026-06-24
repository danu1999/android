package com.posbah.app.data.remote.api

import retrofit2.Response
import retrofit2.http.*

// ─────────────────────────────────────────────────────────────────────────────
// Core POS API Service — FnB / Laundry / Rental
// Base URL: https://zedmz.cloud/
// Auth: Authorization: Bearer <token>
// ─────────────────────────────────────────────────────────────────────────────

interface PosApiService {

    // ── Products ──────────────────────────────────────────────────────────────
    @GET("api/rt/products")
    suspend fun getProducts(
        @Query("outletId") outletId: String? = null
    ): Response<List<Map<String, Any?>>>

    @POST("api/rt/products")
    suspend fun createProduct(
        @Body body: Map<String, Any?>
    ): Response<Map<String, Any?>>

    @PUT("api/rt/products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Long,
        @Body body: Map<String, Any?>
    ): Response<Map<String, Any?>>

    @DELETE("api/rt/products/{id}")
    suspend fun deleteProduct(
        @Path("id") id: Long
    ): Response<Map<String, Any?>>

    // ── Customers ─────────────────────────────────────────────────────────────
    @GET("api/rt/customers")
    suspend fun getCustomers(): Response<List<Map<String, Any?>>>

    @POST("api/rt/customers")
    suspend fun createCustomer(
        @Body body: Map<String, Any?>
    ): Response<Map<String, Any?>>

    @DELETE("api/rt/customers/{id}")
    suspend fun deleteCustomer(
        @Path("id") id: Long
    ): Response<Map<String, Any?>>

    // ── Transactions ─────────────────────────────────────────────────────────
    @GET("api/rt/transactions")
    suspend fun getTransactions(
        @Query("outletId") outletId: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<Map<String, Any?>>>

    @POST("api/rt/transactions")
    suspend fun createTransaction(
        @Body body: Map<String, Any?>
    ): Response<Map<String, Any?>>

    @PUT("api/rt/transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: Long,
        @Body body: Map<String, Any?>
    ): Response<Map<String, Any?>>

    @DELETE("api/rt/transactions/{id}")
    suspend fun deleteTransaction(
        @Path("id") id: Long
    ): Response<Map<String, Any?>>

    // ── Transaction Items ─────────────────────────────────────────────────────
    @GET("api/rt/transaction-items")
    suspend fun getTransactionItems(
        @Query("transactionId") transactionId: Long
    ): Response<List<Map<String, Any?>>>

    @POST("api/rt/transaction-items")
    suspend fun createTransactionItems(
        @Body items: List<Map<String, Any?>>
    ): Response<Map<String, Any?>>

    // ── Employees ─────────────────────────────────────────────────────────────
    @GET("api/rt/employees")
    suspend fun getEmployees(): Response<List<Map<String, Any?>>>

    @POST("api/rt/employees")
    suspend fun createEmployee(
        @Body body: Map<String, Any?>
    ): Response<Map<String, Any?>>

    @PUT("api/rt/employees/{id}")
    suspend fun updateEmployee(
        @Path("id") id: Long,
        @Body body: Map<String, Any?>
    ): Response<Map<String, Any?>>

    @DELETE("api/rt/employees/{id}")
    suspend fun deleteEmployee(
        @Path("id") id: Long
    ): Response<Map<String, Any?>>

    // ── Outlets ───────────────────────────────────────────────────────────────
    @GET("api/rt/outlets")
    suspend fun getOutlets(): Response<List<Map<String, Any?>>>

    @POST("api/rt/outlets")
    suspend fun createOutlet(
        @Body body: Map<String, Any?>
    ): Response<Map<String, Any?>>

    @PUT("api/rt/outlets/{id}")
    suspend fun updateOutlet(
        @Path("id") id: Long,
        @Body body: Map<String, Any?>
    ): Response<Map<String, Any?>>

    @DELETE("api/rt/outlets/{id}")
    suspend fun deleteOutlet(
        @Path("id") id: Long
    ): Response<Map<String, Any?>>

    // ── Print Settings ────────────────────────────────────────────────────────
    @GET("api/rt/print-settings")
    suspend fun getPrintSettings(
        @Query("moduleKey") moduleKey: String? = null
    ): Response<List<Map<String, Any?>>>

    @POST("api/rt/print-settings")
    suspend fun savePrintSettings(
        @Body body: Map<String, Any?>
    ): Response<Map<String, Any?>>

    // ── PIN Login ─────────────────────────────────────────────────────────────
    @POST("api/auth/pin-login")
    suspend fun pinLogin(
        @Body body: Map<String, Any?>
    ): Response<Map<String, Any?>>
}
