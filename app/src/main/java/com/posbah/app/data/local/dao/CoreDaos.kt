package com.posbah.app.data.local.dao

// ─────────────────────────────────────────────────────────────────────────────
// CoreDaos.kt — Full Online mode STUB
// Room @Dao annotations dihapus. Semua method mengembalikan empty/no-op.
// File ini dipertahankan agar kompilasi tidak gagal untuk ViewModel yang
// belum direfactor. Tidak ada SQLite query yang dieksekusi.
// ─────────────────────────────────────────────────────────────────────────────

import com.posbah.app.data.local.entities.Employee
import com.posbah.app.data.local.entities.LocalUser
import com.posbah.app.data.local.entities.Outlet
import com.posbah.app.data.local.entities.Tenant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalUserDao @Inject constructor() {
    suspend fun getBySub(sub: String): LocalUser? = null
    suspend fun getByEmail(email: String): LocalUser? = null
    suspend fun upsert(user: LocalUser) {}
    suspend fun update(user: LocalUser) {}
    fun observeAll(): Flow<List<LocalUser>> = emptyFlow()
    suspend fun getAll(): List<LocalUser> = emptyList()
    suspend fun setTenant(sub: String, tenantId: String, ts: Long = System.currentTimeMillis()) {}
}

@Singleton
class TenantDao @Inject constructor() {
    suspend fun getById(id: String): Tenant? = null
    fun observeById(id: String): Flow<Tenant?> = emptyFlow()
    fun observeForOwner(email: String): Flow<List<Tenant>> = emptyFlow()
    suspend fun listForOwner(email: String): List<Tenant> = emptyList()
    suspend fun getAll(): List<Tenant> = emptyList()
    suspend fun upsert(tenant: Tenant) {}
    suspend fun setActive(id: String, active: Boolean, ts: Long = System.currentTimeMillis()) {}
    suspend fun deleteById(id: String) {}
}

@Singleton
class OutletDao @Inject constructor() {
    fun observeForTenant(tenantId: String): Flow<List<Outlet>> = emptyFlow()
    suspend fun listForTenant(tenantId: String): List<Outlet> = emptyList()
    suspend fun getById(id: Long): Outlet? = null
    suspend fun insert(outlet: Outlet): Long = 0L
    suspend fun update(outlet: Outlet) {}
    suspend fun getAll(): List<Outlet> = emptyList()
    suspend fun delete(id: Long) {}
    suspend fun markSynced(id: Long) {}
}

@Singleton
class EmployeeDao @Inject constructor() {
    fun observeAll(): Flow<List<Employee>> = emptyFlow()
    fun observeForTenant(tenantId: String): Flow<List<Employee>> = emptyFlow()
    fun observeForOutlet(tenantId: String, outletId: Long): Flow<List<Employee>> = emptyFlow()
    suspend fun listForOutlet(tenantId: String, outletId: Long): List<Employee> = emptyList()
    suspend fun findForLogin(tenantId: String, email: String): Employee? = null
    suspend fun findByEmail(email: String): Employee? = null
    suspend fun getById(id: Long): Employee? = null
    suspend fun insert(employee: Employee): Long = 0L
    suspend fun update(employee: Employee) {}
    suspend fun getAll(): List<Employee> = emptyList()
    suspend fun softDelete(id: Long) {}
    suspend fun deleteIncorrectEmployees(emails: List<String>, allowedTenants: List<String>) {}
    suspend fun deleteByEmail(email: String) {}
    suspend fun deleteById(id: Long) {}
    suspend fun updateSalaryAndPeriod(id: Long, salary: Double, payPeriod: String, updatedAt: Long = System.currentTimeMillis()) {}
    suspend fun markSynced(id: Long) {}
}
