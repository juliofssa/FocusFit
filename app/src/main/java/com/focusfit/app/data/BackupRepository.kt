package com.focusfit.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.focusfit.app.data.local.ActivityEntity
import com.focusfit.app.data.local.AppDatabase
import com.focusfit.app.data.local.BackupDao
import com.focusfit.app.data.local.BookEntity
import com.focusfit.app.data.local.CustomTaskEntity
import com.focusfit.app.data.local.DaySummaryEntity
import com.focusfit.app.data.local.FreeMealEntity
import com.focusfit.app.data.local.HiddenMealEntity
import com.focusfit.app.data.local.PlanChangeEntity
import com.focusfit.app.data.local.ProfileEntity
import com.focusfit.app.data.local.ReadingSessionEntity
import com.focusfit.app.data.local.SuggestionLogEntity
import com.focusfit.app.data.local.TaskLogEntity
import com.focusfit.app.data.local.WeightEntity
import com.focusfit.app.data.local.WorkoutLogEntity
import com.focusfit.app.domain.NotificationSettings
import com.focusfit.app.domain.ReminderCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupSettings(
    val enabled: List<String>,
    val wakeTime: String,
    val bedTime: String,
    val quietStart: String,
    val quietEnd: String,
    val stepGoal: Int? = null,
    val stepGoalWeek: String? = null,
    val homeWorkoutTime: String? = null,
    val readingTime: String? = null,
    val lastWaterTime: String? = null,
    val weeklyReviewTime: String? = null,
)

/** Everything FocusFit stores, as one JSON file. [format] guards against importing an unknown layout. */
@Serializable
data class Backup(
    val format: Int = FORMAT,
    val exportedAt: String,
    val settings: BackupSettings,
    val profiles: List<ProfileEntity> = emptyList(),
    val activities: List<ActivityEntity> = emptyList(),
    val taskLogs: List<TaskLogEntity> = emptyList(),
    val daySummaries: List<DaySummaryEntity> = emptyList(),
    val customTasks: List<CustomTaskEntity> = emptyList(),
    val workoutLogs: List<WorkoutLogEntity> = emptyList(),
    val freeMeals: List<FreeMealEntity> = emptyList(),
    val hiddenMeals: List<HiddenMealEntity> = emptyList(),
    val books: List<BookEntity> = emptyList(),
    val readingSessions: List<ReadingSessionEntity> = emptyList(),
    val suggestionLogs: List<SuggestionLogEntity> = emptyList(),
    val weights: List<WeightEntity> = emptyList(),
    val planChanges: List<PlanChangeEntity> = emptyList(),
) {
    companion object {
        const val FORMAT = 1
        val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
        fun decode(text: String): Backup = json.decodeFromString<Backup>(text).also {
            require(it.format <= FORMAT) { "This backup is from a newer version of FocusFit." }
        }
    }

    fun encode(): String = json.encodeToString(serializer(), this)
}

/** Export/import of all data to a file the user picks (nothing leaves the phone unless they move it). */
@Singleton
class BackupRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val dao: BackupDao,
    private val settings: SettingsRepository,
) {
    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
        val s = settings.notifications.first()
        val (goal, week) = settings.stepGoalState()
        val backup = Backup(
            exportedAt = LocalDate.now().toString(),
            settings = BackupSettings(
                enabled = s.enabled.map { it.name },
                wakeTime = s.wakeTime.toString(),
                bedTime = s.bedTime.toString(),
                quietStart = s.quietStart.toString(),
                quietEnd = s.quietEnd.toString(),
                stepGoal = goal,
                stepGoalWeek = week?.toString(),
                homeWorkoutTime = s.homeWorkoutTime.toString(),
                readingTime = s.readingTime.toString(),
                lastWaterTime = s.lastWaterTime.toString(),
                weeklyReviewTime = s.weeklyReviewTime.toString(),
            ),
            profiles = dao.profiles(),
            activities = dao.activities(),
            taskLogs = dao.taskLogs(),
            daySummaries = dao.daySummaries(),
            customTasks = dao.customTasks(),
            workoutLogs = dao.workoutLogs(),
            freeMeals = dao.freeMeals(),
            hiddenMeals = dao.hiddenMeals(),
            books = dao.books(),
            readingSessions = dao.readingSessions(),
            suggestionLogs = dao.suggestionLogs(),
            weights = dao.weights(),
            planChanges = dao.planChanges(),
        )
        context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(backup.encode()) }
    }

    /** Replaces all data with the backup's. Throws (and changes nothing) if the file can't be read. */
    suspend fun import(uri: Uri) = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
        val b = Backup.decode(text)
        db.withTransaction {
            dao.clearProfiles(); dao.clearActivities(); dao.clearTaskLogs(); dao.clearDaySummaries()
            dao.clearCustomTasks(); dao.clearWorkoutLogs(); dao.clearFreeMeals(); dao.clearHiddenMeals()
            dao.clearBooks(); dao.clearReadingSessions(); dao.clearSuggestionLogs(); dao.clearWeights(); dao.clearPlanChanges()
            dao.insertProfiles(b.profiles); dao.insertActivities(b.activities); dao.insertTaskLogs(b.taskLogs)
            dao.insertDaySummaries(b.daySummaries); dao.insertCustomTasks(b.customTasks)
            dao.insertWorkoutLogs(b.workoutLogs); dao.insertFreeMeals(b.freeMeals); dao.insertHiddenMeals(b.hiddenMeals)
            dao.insertBooks(b.books); dao.insertReadingSessions(b.readingSessions)
            dao.insertSuggestionLogs(b.suggestionLogs); dao.insertWeights(b.weights); dao.insertPlanChanges(b.planChanges)
        }
        val s = b.settings
        settings.save(
            NotificationSettings(
                enabled = s.enabled.mapNotNull { n -> ReminderCategory.entries.firstOrNull { it.name == n } }.toSet(),
                wakeTime = LocalTime.parse(s.wakeTime),
                bedTime = LocalTime.parse(s.bedTime),
                quietStart = LocalTime.parse(s.quietStart),
                quietEnd = LocalTime.parse(s.quietEnd),
                homeWorkoutTime = s.homeWorkoutTime?.let(LocalTime::parse) ?: NotificationSettings().homeWorkoutTime,
                readingTime = s.readingTime?.let(LocalTime::parse) ?: NotificationSettings().readingTime,
                lastWaterTime = s.lastWaterTime?.let(LocalTime::parse) ?: NotificationSettings().lastWaterTime,
                weeklyReviewTime = s.weeklyReviewTime?.let(LocalTime::parse) ?: NotificationSettings().weeklyReviewTime,
            ),
        )
        if (s.stepGoal != null && s.stepGoalWeek != null) settings.saveStepGoal(s.stepGoal, LocalDate.parse(s.stepGoalWeek))
    }
}
