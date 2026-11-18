package com.focusfit.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row table holding the user's body metrics, goal and work week. */
@Entity(tableName = "profile")
@kotlinx.serialization.Serializable
data class ProfileEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val sex: String,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val goal: String,
    val targetWeightKg: Double,
    val pace: String,
    val avgDailySteps: Int,
    /** "MONDAY=REMOTE;WEDNESDAY=OFFICE" — days not listed are OFF. Added in v2. */
    @ColumnInfo(defaultValue = "") val workModes: String = "",
    @ColumnInfo(defaultValue = "SUNDAY") val restDay: String = "SUNDAY",
    // v8: target overrides (null = use the calculated suggestion), food & free meals.
    val kcalOverride: Int? = null,
    val proteinOverride: Int? = null,
    val waterOverride: Int? = null,
    val stepsOverride: Int? = null,
    val readingOverride: Int? = null,
    @ColumnInfo(defaultValue = "ANY") val dietStyle: String = "ANY",
    /** Comma-separated FoodTag names. */
    @ColumnInfo(defaultValue = "") val foodExcludes: String = "",
    @ColumnInfo(defaultValue = "3") val freeMealsPerWeek: Int = 3,
) {
    companion object { const val SINGLE_ROW_ID = 1 }
}

/** One-off change to a specific date's plan (see domain PlanChange). Session columns are set for ADD. */
@Entity(tableName = "plan_change")
@kotlinx.serialization.Serializable
data class PlanChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val kind: String,
    val key: String,
    val moveGroup: String?,
    val sessionType: String?,
    val title: String?,
    val emoji: String?,
    val durationMin: Int?,
    val timeMinutes: Int?,
    val routineId: String?,
)

/** A user-defined sport. [days] is a comma-separated list of DayOfWeek names. */
@Entity(tableName = "activity")
@kotlinx.serialization.Serializable
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val days: String,
    /** Minutes after midnight, or null when there's no fixed time. */
    val timeMinutes: Int?,
    val durationMin: Int,
    val sortOrder: Int,
)
