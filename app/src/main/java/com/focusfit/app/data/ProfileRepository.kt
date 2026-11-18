package com.focusfit.app.data

import androidx.room.withTransaction
import com.focusfit.app.data.local.ActivityEntity
import com.focusfit.app.data.local.AppDatabase
import com.focusfit.app.data.local.PlanChangeEntity
import com.focusfit.app.data.local.ProfileDao
import com.focusfit.app.data.local.ProfileEntity
import com.focusfit.app.domain.PlanChange
import com.focusfit.app.domain.PlanChangeKind
import com.focusfit.app.domain.PlannedSession
import com.focusfit.app.domain.SessionType
import com.focusfit.app.domain.model.DietStyle
import com.focusfit.app.domain.model.FoodTag
import com.focusfit.app.domain.model.Goal
import com.focusfit.app.domain.model.Pace
import com.focusfit.app.domain.model.ScheduledActivity
import com.focusfit.app.domain.model.Sex
import com.focusfit.app.domain.model.TargetOverrides
import com.focusfit.app.domain.model.UserProfile
import com.focusfit.app.domain.model.WorkMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val db: AppDatabase,
    private val dao: ProfileDao,
) {
    /** The saved profile (with recent one-off plan changes), or null until onboarding has been completed. */
    val savedProfile: Flow<UserProfile?> = combine(
        dao.observe(),
        dao.observeActivities(),
        dao.observePlanChanges(LocalDate.now().minusDays(PLAN_CHANGE_HISTORY_DAYS).toString()),
    ) { p, acts, changes -> p?.toDomain(acts, changes.mapNotNull { it.toDomain() }) }

    /** The saved profile, falling back to the example profile (only used before onboarding finishes). */
    val profile: Flow<UserProfile> = savedProfile.map { it ?: UserProfile.REFERENCE }

    /** Saves the profile and its sports. One-off plan changes are stored separately and untouched. */
    suspend fun save(profile: UserProfile) = db.withTransaction {
        dao.upsert(profile.toEntity())
        dao.clearActivities()
        dao.insertActivities(profile.activities.mapIndexed { i, a -> a.toEntity(i) })
    }

    // ---- One-off week changes ----

    suspend fun skip(date: LocalDate, session: PlannedSession) =
        dao.insertPlanChanges(listOf(PlanChange(date = date, kind = PlanChangeKind.SKIP, key = session.key).toEntity()))

    suspend fun add(date: LocalDate, session: PlannedSession) =
        dao.insertPlanChanges(listOf(PlanChange(date = date, kind = PlanChangeKind.ADD, key = session.key, session = session).toEntity()))

    /** Moves [session] from [from] to [to] for that week only (undo removes both halves). */
    suspend fun move(session: PlannedSession, from: LocalDate, to: LocalDate) {
        val group = UUID.randomUUID().toString()
        dao.insertPlanChanges(
            listOf(
                PlanChange(date = from, kind = PlanChangeKind.SKIP, key = session.key, moveGroup = group).toEntity(),
                PlanChange(date = to, kind = PlanChangeKind.ADD, key = session.key, session = session, moveGroup = group).toEntity(),
            ),
        )
    }

    suspend fun undo(change: PlanChange) =
        if (change.moveGroup != null) dao.deleteMoveGroup(change.moveGroup) else dao.deletePlanChange(change.id)

    suspend fun resetWeek(monday: LocalDate) = dao.clearPlanChanges(monday.toString(), monday.plusDays(6).toString())

    // ---- Mapping ----

    private fun ProfileEntity.toDomain(acts: List<ActivityEntity>, changes: List<PlanChange>) = UserProfile(
        sex = Sex.valueOf(sex),
        age = age,
        heightCm = heightCm,
        weightKg = weightKg,
        goal = Goal.valueOf(goal),
        targetWeightKg = targetWeightKg,
        pace = Pace.valueOf(pace),
        avgDailySteps = avgDailySteps,
        activities = acts.map { it.toDomain() },
        workModes = decodeWorkModes(workModes),
        restDay = DayOfWeek.valueOf(restDay),
        targetOverrides = TargetOverrides(kcalOverride, proteinOverride, waterOverride, stepsOverride, readingOverride),
        dietStyle = DietStyle.entries.firstOrNull { it.name == dietStyle } ?: DietStyle.ANY,
        foodExcludes = decodeFoodTags(foodExcludes),
        freeMealsPerWeek = freeMealsPerWeek,
        planChanges = changes,
    )

    private fun UserProfile.toEntity() = ProfileEntity(
        sex = sex.name,
        age = age,
        heightCm = heightCm,
        weightKg = weightKg,
        goal = goal.name,
        targetWeightKg = targetWeightKg,
        pace = pace.name,
        avgDailySteps = avgDailySteps,
        workModes = encodeWorkModes(workModes),
        restDay = restDay.name,
        kcalOverride = targetOverrides.kcal,
        proteinOverride = targetOverrides.proteinG,
        waterOverride = targetOverrides.waterMl,
        stepsOverride = targetOverrides.steps,
        readingOverride = targetOverrides.readingMin,
        dietStyle = dietStyle.name,
        foodExcludes = foodExcludes.sorted().joinToString(",") { it.name },
        freeMealsPerWeek = freeMealsPerWeek,
    )

    private fun ActivityEntity.toDomain() = ScheduledActivity(
        name = name,
        emoji = emoji,
        days = days.split(',').filter { it.isNotBlank() }.map(DayOfWeek::valueOf).toSet(),
        time = timeMinutes?.let { LocalTime.of(it / 60, it % 60) },
        durationMin = durationMin,
    )

    private fun ScheduledActivity.toEntity(order: Int) = ActivityEntity(
        name = name.trim(),
        emoji = emoji,
        days = days.sorted().joinToString(",") { it.name },
        timeMinutes = time?.let { it.hour * 60 + it.minute },
        durationMin = durationMin,
        sortOrder = order,
    )

    private fun PlanChangeEntity.toDomain(): PlanChange? = runCatching {
        PlanChange(
            id = id,
            date = LocalDate.parse(date),
            kind = PlanChangeKind.valueOf(kind),
            key = key,
            moveGroup = moveGroup,
            session = if (sessionType != null && title != null) PlannedSession(
                type = SessionType.valueOf(sessionType),
                title = title,
                emoji = emoji.orEmpty(),
                durationMin = durationMin ?: 0,
                time = timeMinutes?.let { LocalTime.of(it / 60, it % 60) },
                routineId = routineId,
            ) else null,
        )
    }.getOrNull()

    private fun PlanChange.toEntity() = PlanChangeEntity(
        date = date.toString(),
        kind = kind.name,
        key = key,
        moveGroup = moveGroup,
        sessionType = session?.type?.name,
        title = session?.title,
        emoji = session?.emoji,
        durationMin = session?.durationMin,
        timeMinutes = session?.time?.let { it.hour * 60 + it.minute },
        routineId = session?.routineId,
    )

    companion object {
        /** Older plan changes aren't loaded (they only matter for recent reviews/suggestions). */
        private const val PLAN_CHANGE_HISTORY_DAYS = 21L

        fun encodeWorkModes(modes: Map<DayOfWeek, WorkMode>): String =
            modes.filterValues { it != WorkMode.OFF }.toSortedMap()
                .entries.joinToString(";") { "${it.key.name}=${it.value.name}" }

        fun decodeWorkModes(s: String): Map<DayOfWeek, WorkMode> =
            s.split(';').filter { '=' in it }.associate { entry ->
                val (day, mode) = entry.split('=')
                DayOfWeek.valueOf(day) to WorkMode.valueOf(mode)
            }

        fun decodeFoodTags(s: String): Set<FoodTag> =
            s.split(',').mapNotNull { n -> FoodTag.entries.firstOrNull { it.name == n.trim() } }.toSet()
    }
}
