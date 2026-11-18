package com.focusfit.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focusfit.app.domain.NotificationSettings
import com.focusfit.app.domain.ReminderCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    private object Keys {
        val ENABLED = stringSetPreferencesKey("notif_enabled")
        val WAKE = stringPreferencesKey("wake_time")
        val BED = stringPreferencesKey("bed_time")
        val QUIET_START = stringPreferencesKey("quiet_start")
        val QUIET_END = stringPreferencesKey("quiet_end")
        val STEP_GOAL = intPreferencesKey("step_goal")
        val STEP_GOAL_WEEK = stringPreferencesKey("step_goal_week")
        val NUDGES_OFF = booleanPreferencesKey("nudges_off")
        val HOME_WORKOUT = stringPreferencesKey("home_workout_time")
        val READING = stringPreferencesKey("reading_time")
        val LAST_WATER = stringPreferencesKey("last_water_time")
        val REVIEW = stringPreferencesKey("weekly_review_time")
    }

    val notifications: Flow<NotificationSettings> = context.settingsStore.data.map { p ->
        val d = NotificationSettings()
        NotificationSettings(
            // Smart suggestions are stored as their own on/off flag so they default to ON for settings
            // saved before they existed.
            enabled = (
                p[Keys.ENABLED]
                    ?.mapNotNull { name -> ReminderCategory.entries.firstOrNull { it.name == name } }?.toSet()
                    ?: d.enabled
                ).let { if (p[Keys.NUDGES_OFF] == true) it - ReminderCategory.NUDGES else it + ReminderCategory.NUDGES },
            wakeTime = p[Keys.WAKE]?.let(LocalTime::parse) ?: d.wakeTime,
            bedTime = p[Keys.BED]?.let(LocalTime::parse) ?: d.bedTime,
            quietStart = p[Keys.QUIET_START]?.let(LocalTime::parse) ?: d.quietStart,
            quietEnd = p[Keys.QUIET_END]?.let(LocalTime::parse) ?: d.quietEnd,
            homeWorkoutTime = p[Keys.HOME_WORKOUT]?.let(LocalTime::parse) ?: d.homeWorkoutTime,
            readingTime = p[Keys.READING]?.let(LocalTime::parse) ?: d.readingTime,
            lastWaterTime = p[Keys.LAST_WATER]?.let(LocalTime::parse) ?: d.lastWaterTime,
            weeklyReviewTime = p[Keys.REVIEW]?.let(LocalTime::parse) ?: d.weeklyReviewTime,
        )
    }

    /** Current step goal (null until first set) and the Monday of the week it was last evaluated. */
    val stepGoal: Flow<Int?> = context.settingsStore.data.map { it[Keys.STEP_GOAL] }

    suspend fun stepGoalState(): Pair<Int?, LocalDate?> {
        val p = context.settingsStore.data.first()
        return p[Keys.STEP_GOAL] to p[Keys.STEP_GOAL_WEEK]?.let(LocalDate::parse)
    }

    suspend fun saveStepGoal(goal: Int, weekStart: LocalDate) {
        context.settingsStore.edit {
            it[Keys.STEP_GOAL] = goal
            it[Keys.STEP_GOAL_WEEK] = weekStart.toString()
        }
    }

    suspend fun save(s: NotificationSettings) {
        context.settingsStore.edit { p ->
            p[Keys.ENABLED] = (s.enabled - ReminderCategory.NUDGES).map { it.name }.toSet()
            p[Keys.NUDGES_OFF] = ReminderCategory.NUDGES !in s.enabled
            p[Keys.WAKE] = s.wakeTime.toString()
            p[Keys.BED] = s.bedTime.toString()
            p[Keys.QUIET_START] = s.quietStart.toString()
            p[Keys.QUIET_END] = s.quietEnd.toString()
            p[Keys.HOME_WORKOUT] = s.homeWorkoutTime.toString()
            p[Keys.READING] = s.readingTime.toString()
            p[Keys.LAST_WATER] = s.lastWaterTime.toString()
            p[Keys.REVIEW] = s.weeklyReviewTime.toString()
        }
    }
}
