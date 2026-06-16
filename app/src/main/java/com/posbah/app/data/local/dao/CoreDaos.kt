package com.posbah.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.posbah.app.data.local.entities.Employee
import com.posbah.app.data.local.entities.LocalUser
import com.posbah.app.data.local.entities.Outlet
import com.posbah.app.data.local.entities.Tenant
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalUserDao {
    @Query("SELECT * FROM local_users WHERE googleSub = :sub LIMIT 1")
    suspend fun getBySub(sub: String): LocalUser?

    @Query("SELECT * FROM local_users WHERE email = :email COLLATE NOCASE LIMIT 1")
    suspend fun getByEmail(email: String): LocalUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: LocalUser)

    @Update suspend fun update(user: LocalUser)

    @Query("SELECT * FROM local_users")
    fun observeAll(): Flow<List<LocalUser>>

    @Query("SELECT * FROM local_users")
    suspend fun getAll(): List<LocalUser>

    @Query("UPDATE local_users SET tenantId = :tenantId, updatedAt = :ts WHERE googleSub = :sub")
    suspend fun setTenant(sub: String, tenantId: String, ts: Long = System.currentTimeMillis())
}

@Dao
interface TenantDao {
    @Query("SELECT * FROM tenants WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Tenant?

    @Query("SELECT * FROM tenants WHERE ownerEmail = :email COLLATE NOCASE")
    fun observeForOwner(email: String): Flow<List<Tenant>>

    @Query("SELECT * FROM tenants WHERE ownerEmail = :email COLLATE NOCASE")
    suspend fun listForOwner(email: String): List<Tenant>

    @Query("SELECT * FROM tenants")
    suspend fun getAll(): List<Tenant>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tenant: Tenant)

    @Query("UPDATE tenants SET isActive = :active, updatedAt = :ts WHERE id = :id")
    suspend fun setActive(id: String, active: Boolean, ts: Long = System.currentTimeMillis())

    @Query("DELETE FROM tenants WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface OutletDao {
    @Query("SELECT * FROM outlets WHERE tenantId = :tenantId ORDER BY isDefault DESC, name ASC")
    fun observeForTenant(tenantId: String): Flow<List<Outlet>>

    @Query("SELECT * FROM outlets WHERE tenantId = :tenantId ORDER BY isDefault DESC, name ASC")
    suspend fun listForTenant(tenantId: String): List<Outlet>

    @Query("SELECT * FROM outlets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Outlet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(outlet: Outlet): Long

    @Update suspend fun update(outlet: Outlet)

    @Query("SELECT * FROM outlets")
    suspend fun getAll(): List<Outlet>

    @Query("DELETE FROM outlets WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface EmployeeDao {
    // ── Tenant-level (owner sees all) ────────────────────────────────────────
    @Query("SELECT * FROM employees WHERE tenantId = :tenantId AND isActive = 1 ORDER BY name ASC")
    fun observeForTenant(tenantId: String): Flow<List<Employee>>

    // ── Outlet-level (strict isolation) ──────────────────────────────────────
    @Query("""
        SELECT * FROM employees
        WHERE tenantId = :tenantId
          AND (outletId = :outletId OR outletId IS NULL)
          AND isActive = 1
        ORDER BY name ASC
    """)
    fun observeForOutlet(tenantId: String, outletId: Long): Flow<List<Employee>>

    @Query("""
        SELECT * FROM employees
        WHERE tenantId = :tenantId
          AND (outletId = :outletId OR outletId IS NULL)
          AND isActive = 1
        ORDER BY name ASC
    """)
    suspend fun listForOutlet(tenantId: String, outletId: Long): List<Employee>

    @Query("SELECT * FROM employees WHERE email = :email COLLATE NOCASE AND tenantId = :tenantId LIMIT 1")
    suspend fun findForLogin(tenantId: String, email: String): Employee?

    @Query("SELECT * FROM employees WHERE email = :email COLLATE NOCASE LIMIT 1")
    suspend fun findByEmail(email: String): Employee?

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Employee?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(employee: Employee): Long

    @Update suspend fun update(employee: Employee)

    @Query("SELECT * FROM employees")
    suspend fun getAll(): List<Employee>

    @Query("UPDATE employees SET isActive = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("DELETE FROM employees WHERE email IN (:emails) AND tenantId NOT IN (:allowedTenants)")
    suspend fun deleteIncorrectEmployees(emails: List<String>, allowedTenants: List<String>)

    /** Hapus karyawan berdasarkan email (untuk cleanup data duplikat/typo). */
    @Query("DELETE FROM employees WHERE email = :email COLLATE NOCASE")
    suspend fun deleteByEmail(email: String)

    /** Update gaji dan siklus pembayaran karyawan oleh Owner. */
    @Query("UPDATE employees SET salary = :salary, payPeriod = :payPeriod, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSalaryAndPeriod(id: Long, salary: Double, payPeriod: String, updatedAt: Long = System.currentTimeMillis())
}

