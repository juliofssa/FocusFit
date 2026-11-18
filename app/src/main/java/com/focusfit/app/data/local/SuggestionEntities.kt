package com.focusfit.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Per day and rule: whether the suggestion was sent as a notification and/or dismissed. */
@Entity(tableName = "suggestion_log", primaryKeys = ["date", "rule"])
@kotlinx.serialization.Serializable
data class SuggestionLogEntity(
    val date: String,
    val rule: String,
    val notified: Boolean,
    val dismissed: Boolean,
)

@Dao
interface SuggestionDao {
    @Query("SELECT * FROM suggestion_log WHERE date = :date")
    fun observeDay(date: String): Flow<List<SuggestionLogEntity>>

    @Query("SELECT * FROM suggestion_log WHERE date = :date")
    suspend fun day(date: String): List<SuggestionLogEntity>

    @Upsert
    suspend fun upsert(entry: SuggestionLogEntity)
}
