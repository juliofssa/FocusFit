package com.focusfit.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** One weigh-in per day (logging again the same day replaces it). */
@Entity(tableName = "weight")
@kotlinx.serialization.Serializable
data class WeightEntity(
    @PrimaryKey val date: String,
    val kg: Double,
    val waistCm: Double?,
)

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight ORDER BY date")
    fun observeAll(): Flow<List<WeightEntity>>

    @Upsert
    suspend fun upsert(entry: WeightEntity)

    @Query("DELETE FROM weight WHERE date = :date")
    suspend fun delete(date: String)
}
