package com.posbah.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.posbah.app.data.local.entities.ActivityLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs WHERE tenantId = :tenantId AND appMode = :appMode ORDER BY date DESC")
    fun observeLogs(tenantId: String, appMode: String): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE tenantId = :tenantId ORDER BY date DESC")
    fun observeAllLogs(tenantId: String): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity)

    @Query("SELECT * FROM activity_logs")
    suspend fun getAll(): List<ActivityLogEntity>
}
