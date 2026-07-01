package com.posbah.app.data.remote.api

import retrofit2.Response
import retrofit2.http.*

// ─────────────────────────────────────────────────────────────────────────────
// BMP API Service — Invoice & Manufaktur
// Base URL: https://zedmz.cloud/
// Auth: Authorization: Bearer <token>
// ─────────────────────────────────────────────────────────────────────────────

@JvmSuppressWildcards
interface BmpApiService {

    // ── Clients ───────────────────────────────────────────────────────────────
    @GET("api/rt/bmp/clients")
    suspend fun getClients(): Response<List<Map<String, Any?>>>

    @POST("api/rt/bmp/clients")
    suspend fun createClient(@Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @PUT("api/rt/bmp/clients/{id}")
    suspend fun updateClient(@Path("id") id: Long, @Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @DELETE("api/rt/bmp/clients/{id}")
    suspend fun deleteClient(@Path("id") id: Long): Response<Map<String, Any?>>

    // ── Invoices ──────────────────────────────────────────────────────────────
    @GET("api/rt/bmp/invoices")
    suspend fun getInvoices(): Response<List<Map<String, Any?>>>

    @POST("api/rt/bmp/invoices")
    suspend fun createInvoice(@Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @PUT("api/rt/bmp/invoices/{id}")
    suspend fun updateInvoice(@Path("id") id: Long, @Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @DELETE("api/rt/bmp/invoices/{id}")
    suspend fun deleteInvoice(@Path("id") id: Long): Response<Map<String, Any?>>

    // ── Invoice Products (line items) ─────────────────────────────────────────
    @GET("api/rt/bmp/products")
    suspend fun getBmpProducts(@Query("invoiceId") invoiceId: Long): Response<List<Map<String, Any?>>>

    @POST("api/rt/bmp/products")
    suspend fun createBmpProduct(@Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @PUT("api/rt/bmp/products/{id}")
    suspend fun updateBmpProduct(@Path("id") id: Long, @Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @DELETE("api/rt/bmp/products/{id}")
    suspend fun deleteBmpProduct(@Path("id") id: Long): Response<Map<String, Any?>>

    // ── Master Products ───────────────────────────────────────────────────────
    @GET("api/rt/bmp/master-products")
    suspend fun getMasterProducts(): Response<List<Map<String, Any?>>>

    @POST("api/rt/bmp/master-products")
    suspend fun createMasterProduct(@Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @PUT("api/rt/bmp/master-products/{id}")
    suspend fun updateMasterProduct(@Path("id") id: Long, @Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @DELETE("api/rt/bmp/master-products/{id}")
    suspend fun deleteMasterProduct(@Path("id") id: Long): Response<Map<String, Any?>>

    // ── Machines ──────────────────────────────────────────────────────────────
    @GET("api/rt/bmp/machines")
    suspend fun getMachines(): Response<List<Map<String, Any?>>>

    @POST("api/rt/bmp/machines")
    suspend fun createMachine(@Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @PUT("api/rt/bmp/machines/{id}")
    suspend fun updateMachine(@Path("id") id: Long, @Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @DELETE("api/rt/bmp/machines/{id}")
    suspend fun deleteMachine(@Path("id") id: Long): Response<Map<String, Any?>>

    // ── Molds ─────────────────────────────────────────────────────────────────
    @GET("api/rt/bmp/molds")
    suspend fun getMolds(): Response<List<Map<String, Any?>>>

    @POST("api/rt/bmp/molds")
    suspend fun createMold(@Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @PUT("api/rt/bmp/molds/{id}")
    suspend fun updateMold(@Path("id") id: Long, @Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @DELETE("api/rt/bmp/molds/{id}")
    suspend fun deleteMold(@Path("id") id: Long): Response<Map<String, Any?>>

    // ── Cashflow ──────────────────────────────────────────────────────────────
    @GET("api/rt/bmp/cashflow")
    suspend fun getCashflow(): Response<List<Map<String, Any?>>>

    @GET("api/rt/bmp/cashflow")
    suspend fun getCashFlow(
        @Query("tenantId") tenantId: String,
        @Query("month") month: String
    ): Response<List<Map<String, Any?>>>

    @POST("api/rt/bmp/cashflow")
    suspend fun createCashflow(@Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @PUT("api/rt/bmp/cashflow/{id}")
    suspend fun updateCashflow(@Path("id") id: Long, @Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @DELETE("api/rt/bmp/cashflow/{id}")
    suspend fun deleteCashflow(@Path("id") id: Long): Response<Map<String, Any?>>

    // ── Payments (Invoice Payments) ───────────────────────────────────────────
    @GET("api/rt/bmp/payments")
    suspend fun getPayments(): Response<List<Map<String, Any?>>>

    @POST("api/rt/bmp/payments")
    suspend fun createPayment(@Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @PUT("api/rt/bmp/payments/{id}")
    suspend fun updatePayment(@Path("id") id: Long, @Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @DELETE("api/rt/bmp/payments/{id}")
    suspend fun deletePayment(@Path("id") id: Long): Response<Map<String, Any?>>

    // ── BMP Employees ─────────────────────────────────────────────────────────
    @GET("api/rt/bmp/employees")
    suspend fun getBmpEmployees(): Response<List<Map<String, Any?>>>

    @POST("api/rt/bmp/employees")
    suspend fun createBmpEmployee(@Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @PUT("api/rt/bmp/employees/{id}")
    suspend fun updateBmpEmployee(@Path("id") id: Long, @Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @DELETE("api/rt/bmp/employees/{id}")
    suspend fun deleteBmpEmployee(@Path("id") id: Long): Response<Map<String, Any?>>

    // ── Payrolls ──────────────────────────────────────────────────────────────
    @GET("api/rt/bmp/payrolls")
    suspend fun getPayrolls(): Response<List<Map<String, Any?>>>

    @POST("api/rt/bmp/payrolls")
    suspend fun createPayroll(@Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @PUT("api/rt/bmp/payrolls/{id}")
    suspend fun updatePayroll(@Path("id") id: String, @Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @DELETE("api/rt/bmp/payrolls/{id}")
    suspend fun deletePayroll(@Path("id") id: String): Response<Map<String, Any?>>

    // ── Bahan Baku (Raw Materials) ────────────────────────────────────────────
    @GET("api/rt/bmp/bahan-baku")
    suspend fun getBahanBaku(): Response<List<Map<String, Any?>>>

    @POST("api/rt/bmp/bahan-baku")
    suspend fun createBahanBaku(@Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @PUT("api/rt/bmp/bahan-baku/{id}")
    suspend fun updateBahanBaku(@Path("id") id: Long, @Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @DELETE("api/rt/bmp/bahan-baku/{id}")
    suspend fun deleteBahanBaku(@Path("id") id: Long): Response<Map<String, Any?>>

    // ── Bahan Baku Items ──────────────────────────────────────────────────────
    @GET("api/rt/bmp/bahan-baku-items")
    suspend fun getBahanBakuItems(@Query("bahanBakuId") bahanBakuId: Long): Response<List<Map<String, Any?>>>

    @POST("api/rt/bmp/bahan-baku-items")
    suspend fun createBahanBakuItems(@Body items: List<Map<String, Any?>>): Response<Map<String, Any?>>

    @DELETE("api/rt/bmp/bahan-baku-items")
    suspend fun deleteBahanBakuItems(@Query("bahanBakuId") id: Long): Response<Map<String, Any?>>

    // ── Production Logs ───────────────────────────────────────────────────────
    @GET("api/rt/bmp/production-logs")
    suspend fun getProductionLogs(): Response<List<Map<String, Any?>>>

    @POST("api/rt/bmp/production-logs")
    suspend fun createProductionLog(@Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @DELETE("api/rt/bmp/production-logs/{id}")
    suspend fun deleteProductionLog(@Path("id") id: Long): Response<Map<String, Any?>>

    // ── Product Stocks ────────────────────────────────────────────────────────
    @GET("api/rt/bmp/product-stocks")
    suspend fun getProductStocks(): Response<List<Map<String, Any?>>>

    @POST("api/rt/bmp/product-stocks")
    suspend fun createProductStock(@Body body: Map<String, Any?>): Response<Map<String, Any?>>

    // ── Stock Ledger ──────────────────────────────────────────────────────────
    @GET("api/rt/bmp/stock-ledger")
    suspend fun getStockLedger(): Response<List<Map<String, Any?>>>

    @POST("api/rt/bmp/stock-ledger")
    suspend fun addStockLedgerEntry(@Body body: Map<String, Any?>): Response<Map<String, Any?>>

    // ── BMP Settings ──────────────────────────────────────────────────────────
    @GET("api/rt/bmp/settings")
    suspend fun getBmpSettings(): Response<Map<String, Any?>>

    @POST("api/rt/bmp/settings")
    suspend fun saveBmpSettings(@Body body: Map<String, Any?>): Response<Map<String, Any?>>

    @GET("api/rt/bmp/suppliers")
    suspend fun getSuppliers(): Response<List<String>>

    // ── BMP Reports ───────────────────────────────────────────────────────────
    @GET("api/rt/bmp/reports/financial")
    suspend fun getFinancialReport(
        @Query("periodType") periodType: String,
        @Query("date") date: String
    ): Response<Map<String, Any?>>

    @GET("api/rt/bmp/reports/export")
    @Streaming
    suspend fun downloadFinancialReportExcel(
        @Query("periodType") periodType: String,
        @Query("date") date: String
    ): Response<okhttp3.ResponseBody>

    @GET("api/rt/bmp/reports/depreciation")
    suspend fun getDepreciation(
        @Query("period") period: String
    ): Response<Map<String, Any?>>

    @POST("api/rt/bmp/reports/depreciation")
    suspend fun saveDepreciation(
        @Body body: Map<String, Any?>
    ): Response<Map<String, Any?>>
}
