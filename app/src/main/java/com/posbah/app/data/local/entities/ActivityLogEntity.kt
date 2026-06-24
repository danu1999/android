package com.posbah.app.data.local.entities

// ── ActivityLogEntity — Full Online mode stub ─────────────────────────────────
// Room @Entity dihapus. Plain data class.

data class ActivityLogEntity(
    val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,
    val action: String,
    val description: String,
    val date: Long = System.currentTimeMillis(),
    val employeeName: String = "Owner",
    val appMode: String
)
