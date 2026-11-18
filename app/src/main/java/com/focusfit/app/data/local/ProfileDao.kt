package com.focusfit.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observe(): Flow<ProfileEntity?>

    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Query("SELECT * FROM activity ORDER BY sortOrder")
    fun observeActivities(): Flow<List<ActivityEntity>>

    @Query("DELETE FROM activity")
    suspend fun clearActivities()

    @Insert
    suspend fun insertActivities(activities: List<ActivityEntity>)

    @Query("SELECT * FROM plan_change WHERE date >= :from ORDER BY id")
    fun observePlanChanges(from: String): Flow<List<PlanChangeEntity>>

    @Insert
    suspend fun insertPlanChanges(changes: List<PlanChangeEntity>)

    @Query("DELETE FROM plan_change WHERE id = :id")
    suspend fun deletePlanChange(id: Long)

    @Query("DELETE FROM plan_change WHERE moveGroup = :group")
    suspend fun deleteMoveGroup(group: String)

    @Query("DELETE FROM plan_change WHERE date >= :from AND date <= :to")
    suspend fun clearPlanChanges(from: String, to: String)
}
