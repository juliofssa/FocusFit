package com.focusfit.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Progress of one task on one day. Dates are ISO strings (yyyy-MM-dd), so they sort correctly. */
@Entity(tableName = "task_log", primaryKeys = ["date", "taskId"])
@kotlinx.serialization.Serializable
data class TaskLogEntity(
    val date: String,
    val taskId: String,
    val value: Int,
    val checked: Boolean,
    val updatedAt: Long,
)

/** Done/total per day, kept for streaks and the weekly score. */
@Entity(tableName = "day_summary")
@kotlinx.serialization.Serializable
data class DaySummaryEntity(
    @PrimaryKey val date: String,
    val done: Int,
    val total: Int,
)

@Entity(tableName = "custom_task")
@kotlinx.serialization.Serializable
data class CustomTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val emoji: String,
    /** Comma-separated DayOfWeek names; ignored when [oneOffDate] is set. */
    val days: String,
    val oneOffDate: String?,
    val createdAt: Long,
)

@Dao
interface DailyDao {
    @Query("SELECT * FROM task_log WHERE date = :date")
    fun observeLog(date: String): Flow<List<TaskLogEntity>>

    @Upsert
    suspend fun upsertLog(entry: TaskLogEntity)

    @Query("SELECT * FROM task_log WHERE taskId = :taskId AND date >= :from AND date <= :to")
    suspend fun logsFor(taskId: String, from: String, to: String): List<TaskLogEntity>

    @Query("SELECT * FROM task_log WHERE taskId LIKE 'session_%' AND checked = 1 AND date >= :from AND date <= :to")
    suspend fun sessionChecks(from: String, to: String): List<TaskLogEntity>

    @Query("SELECT * FROM day_summary WHERE date >= :from")
    fun observeSummariesSince(from: String): Flow<List<DaySummaryEntity>>

    @Upsert
    suspend fun upsertSummary(summary: DaySummaryEntity)

    @Query("SELECT * FROM custom_task ORDER BY createdAt")
    fun observeCustomTasks(): Flow<List<CustomTaskEntity>>

    @Insert
    suspend fun insertCustomTask(task: CustomTaskEntity): Long

    @Query("DELETE FROM custom_task WHERE id = :id")
    suspend fun deleteCustomTask(id: Long)
}
