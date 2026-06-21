package com.posbah.app.data.repository

import com.posbah.app.data.local.dao.OutletDao
import com.posbah.app.data.local.dao.TenantDao
import com.posbah.app.data.local.entities.Outlet
import com.posbah.app.data.local.entities.Tenant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TenantRepository @Inject constructor(
    private val tenantDao: TenantDao,
    private val outletDao: OutletDao
) {
    fun observeForOwner(email: String) = tenantDao.observeForOwner(email)
    suspend fun getById(id: String) = tenantDao.getById(id)
    fun observeById(id: String) = tenantDao.observeById(id)
    suspend fun create(ownerEmail: String, name: String, businessMode: String = "BMP"): Tenant {
        val tenant = Tenant(
            id = "ten_" + UUID.randomUUID().toString().replace("-", "").take(16),
            name = name,
            ownerEmail = ownerEmail,
            businessMode = businessMode
        )
        tenantDao.upsert(tenant)
        outletDao.insert(Outlet(tenantId = tenant.id, name = "Outlet Utama", isDefault = true))
        return tenant
    }

    suspend fun rename(id: String, newName: String) {
        val existing = tenantDao.getById(id) ?: return
        tenantDao.upsert(existing.copy(name = newName, updatedAt = System.currentTimeMillis()))
    }
}

@Singleton
class OutletRepository @Inject constructor(
    private val outletDao: OutletDao
) {
    fun observe(tenantId: String) = outletDao.observeForTenant(tenantId)
    suspend fun list(tenantId: String) = outletDao.listForTenant(tenantId)
    suspend fun getById(id: Long) = outletDao.getById(id)
    suspend fun create(tenantId: String, name: String, address: String? = null, phone: String? = null): Long {
        val existing = outletDao.listForTenant(tenantId)
        val isDefault = existing.isEmpty()
        return outletDao.insert(
            Outlet(tenantId = tenantId, name = name, address = address, phone = phone, isDefault = isDefault)
        )
    }
    suspend fun update(outlet: Outlet) = outletDao.update(outlet.copy(isSynced = false, updatedAt = System.currentTimeMillis()))
    suspend fun delete(id: Long) = outletDao.delete(id)
}
