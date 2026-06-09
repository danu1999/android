package com.posbah.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val action: String,
    val description: String,
    val date: Long = System.currentTimeMillis(),
    val employeeName: String = "Owner",
    val appMode: String
)
