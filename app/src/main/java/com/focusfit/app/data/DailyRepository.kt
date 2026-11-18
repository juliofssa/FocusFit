package com.focusfit.app.data

import com.focusfit.app.data.local.CustomTaskEntity
import com.focusfit.app.data.local.DailyDao
import com.focusfit.app.data.local.DaySummaryEntity
import com.focusfit.app.data.local.TaskLogEntity
import com.focusfit.app.domain.CustomTask
import com.focusfit.app.domain.TaskProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyRepository @Inject constructor(private val dao: DailyDao) {

    fun progress(date: LocalDate): Flow<Map<String, TaskProgress>> =
        dao.observeLog(date.toString()).map { rows -> rows.associate { it.taskId to TaskProgress(it.value, it.checked) } }

    suspend fun saveProgress(date: LocalDate, taskId: String, progress: TaskProgress) =
        dao.upsertLog(TaskLogEntity(date.toString(), taskId, progress.value, progress.checked, System.currentTimeMillis()))

    /** Saved step counts per day in [from]..[to] (from Health Connect syncs or manual entries). */
    suspend fun loggedSteps(from: LocalDate, to: LocalDate): Map<LocalDate, Int> =
        dao.logsFor("steps", from.toString(), to.toString()).associate { LocalDate.parse(it.date) to it.value }

    /** Whether a check task was ticked, per day in [from]..[to] (days never touched are absent). */
    suspend fun loggedChecks(taskId: String, from: LocalDate, to: LocalDate): Map<LocalDate, Boolean> =
        dao.logsFor(taskId, from.toString(), to.toString()).associate { LocalDate.parse(it.date) to it.checked }

    /** Session to-dos ticked per day in [from]..[to]. */
    suspend fun sessionChecks(from: LocalDate, to: LocalDate): Map<LocalDate, Set<String>> =
        dao.sessionChecks(from.toString(), to.toString())
            .groupBy { LocalDate.parse(it.date) }
            .mapValues { (_, rows) -> rows.map { it.taskId }.toSet() }

    /** Completion ratio per day since [from] (inclusive). */
    fun ratiosSince(from: LocalDate): Flow<Map<LocalDate, Double>> =
        dao.observeSummariesSince(from.toString()).map { rows ->
            rows.filter { it.total > 0 }.associate { LocalDate.parse(it.date) to it.done.toDouble() / it.total }
        }

    suspend fun saveSummary(date: LocalDate, done: Int, total: Int) =
        dao.upsertSummary(DaySummaryEntity(date.toString(), done, total))

    val customTasks: Flow<List<CustomTask>> = dao.observeCustomTasks().map { rows ->
        rows.map { e ->
            CustomTask(
                id = e.id,
                title = e.title,
                emoji = e.emoji,
                days = e.days.split(',').filter { it.isNotBlank() }.map(DayOfWeek::valueOf).toSet(),
                oneOffDate = e.oneOffDate?.let(LocalDate::parse),
            )
        }
    }

    suspend fun addCustomTask(title: String, days: Set<DayOfWeek>, oneOffDate: LocalDate?) {
        dao.insertCustomTask(
            CustomTaskEntity(
                title = title.trim(),
                emoji = "📝",
                days = days.sorted().joinToString(",") { it.name },
                oneOffDate = oneOffDate?.toString(),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun deleteCustomTask(id: Long) = dao.deleteCustomTask(id)
}
