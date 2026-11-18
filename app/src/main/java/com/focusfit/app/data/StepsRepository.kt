package com.focusfit.app.data

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.focusfit.app.domain.HealthCalculator
import com.focusfit.app.domain.StepGoals
import com.focusfit.app.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject
import javax.inject.Singleton

enum class HealthConnectStatus { AVAILABLE, NEEDS_UPDATE, NOT_INSTALLED }

/** Read-only step counts from Health Connect, plus the weekly progressive step goal. */
@Singleton
class StepsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val daily: DailyRepository,
) {
    val permissions: Set<String> = setOf(HealthPermission.getReadPermission(StepsRecord::class))

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    fun status(): HealthConnectStatus = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectStatus.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus.NEEDS_UPDATE
        else -> HealthConnectStatus.NOT_INSTALLED
    }

    suspend fun hasPermission(): Boolean = status() == HealthConnectStatus.AVAILABLE &&
        runCatching { client.permissionController.getGrantedPermissions().containsAll(permissions) }.getOrDefault(false)

    /** Steps walked on [date], or null when Health Connect isn't usable. */
    suspend fun stepsOn(date: LocalDate): Int? {
        if (!hasPermission()) return null
        return runCatching {
            val result = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(date.atStartOfDay(), date.plusDays(1).atStartOfDay()),
                ),
            )
            (result[StepsRecord.COUNT_TOTAL] ?: 0L).toInt()
        }.onFailure { Log.w(TAG, "Reading steps failed", it) }.getOrNull()
    }

    /** Daily steps for [from]..[to] from Health Connect, falling back to what was saved in the app. */
    suspend fun dailySteps(from: LocalDate, to: LocalDate): Map<LocalDate, Int> {
        val fromHc = if (hasPermission()) runCatching {
            client.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(from.atStartOfDay(), to.plusDays(1).atStartOfDay()),
                    timeRangeSlicer = Period.ofDays(1),
                ),
            ).associate { it.startTime.toLocalDate() to (it.result[StepsRecord.COUNT_TOTAL] ?: 0L).toInt() }
        }.onFailure { Log.w(TAG, "Reading daily steps failed", it) }.getOrNull() else null
        return fromHc ?: daily.loggedSteps(from, to)
    }

    /**
     * The step goal for this week. Starts at the profile's initial goal; each new week it's re-evaluated
     * from last week's daily steps (+1,000 after 5 days on target, up to 10,000).
     */
    suspend fun currentGoal(profile: UserProfile, today: LocalDate = LocalDate.now()): Int {
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        profile.targetOverrides.steps?.let { return it } // fixed goal set by the user
        val (goal, week) = settings.stepGoalState()
        return when {
            goal == null -> HealthCalculator.initialStepGoal(profile.avgDailySteps).also { settings.saveStepGoal(it, monday) }
            week == null || week.isBefore(monday) -> {
                val lastMonday = monday.minusDays(7)
                val steps = dailySteps(lastMonday, monday.minusDays(1))
                StepGoals.evaluate(goal, steps.values.toList()).also { settings.saveStepGoal(it, monday) }
            }
            else -> goal
        }
    }

    private companion object { const val TAG = "StepsRepository" }
}
