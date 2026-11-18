# Architecture — FocusFit

## 1. Tech stack (all free / open source)

| Concern | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 (dynamic colour, dark mode) |
| Navigation | Navigation-Compose |
| DI | Hilt |
| Local DB | Room (SQLite) |
| Settings | DataStore (Preferences) |
| Background work | AlarmManager only: exact reminders, inexact hourly suggestion checks, 00:05 rollover; re-scheduled on boot/time change (no WorkManager needed) |
| Notifications | NotificationCompat, channel per category, action buttons (Done / +250 ml / Snooze) |
| Steps | Health Connect (`androidx.health.connect:connect-client`) — read-only `StepsRecord` |
| Charts | Plain Compose (Canvas line chart, bar rows, heatmap grid) — no chart library |
| Content & backup | kotlinx.serialization (assets JSON, backup file) |
| Widget | Jetpack Glance |
| Tests | JUnit on the pure-Kotlin domain layer (`app/src/test`) |
| Build | Gradle (Kotlin DSL) + version catalog, AGP 9 built-in Kotlin, min SDK 28, R8 for release |

> Sections below describe the original MVP design; the code is the source of truth for the final
> data model (Room v7: profile, activity, task_log, day_summary, custom_task, workout_log, free_meal,
> hidden_meal, book, reading_session, suggestion_log, weight).

## 2. Layers (single module for MVP, packages by feature)

```
com.focusfit.app
├── data/            Room entities, DAOs, repositories, DataStore, HealthConnect source
├── domain/          Pure Kotlin: HealthCalculator, PlanGenerator, SuggestionEngine, use cases
├── notifications/   Channels, ReminderScheduler, workers, BroadcastReceivers (boot, actions)
├── ui/
│   ├── onboarding/
│   ├── today/
│   ├── plan/        training + nutrition
│   ├── habits/
│   ├── progress/
│   ├── profile/
│   └── theme/
└── di/              Hilt modules
```

- **domain** has no Android dependencies → easy unit tests of formulas and rules.
- UI uses MVVM: `ViewModel` exposes `StateFlow<UiState>`; repositories expose `Flow`.

## 3. Data model (Room)

| Entity | Key fields |
|---|---|
| `UserProfile` | sex, birthYear, heightCm, goal, targetWeightKg, pace, wakeTime, bedTime |
| `Activity` | name (Krav Maga…), type (SPORT/WALK/MOBILITY/CORE), daysOfWeek, time, durationMin, userDefined |
| `DayContext` | dayOfWeek, workMode (OFFICE/REMOTE/OFF) |
| `WeekTemplate` | name, list of Activity + DayContext (e.g. "Normal week", "Holiday") |
| `Exercise` | name, category, focus, reps/seconds, cues, easierId, harderId, equipment (seeded JSON) |
| `Routine` / `RoutineStep` | name (Core A…), format (SETS/CIRCUIT), rounds / exerciseId, sets, work, rest, order |
| `WorkoutLog` | date, routineId, level, durationMin, rpe |
| `FoodPreference` | item, kind (LIKE/DISLIKE/ALLERGY) |
| `DailyTarget` | date, kcal, proteinG, waterMl, steps, readingMin (snapshot per day) |
| `TaskTemplate` | category, title, recurrence, reminderTime, source (SYSTEM/USER) |
| `TaskInstance` | date, templateId, status (TODO/DONE/SKIPPED), value, completedAt |
| `WeightEntry` | date, kg, waistCm? |
| `Book` / `ReadingSession` | title, totalPages / date, minutes, pages |
| `MealIdea` | name, mealType, proteinG, tags, (seeded from JSON asset) |
| `Suggestion` | ruleId, date, shownAt, dismissed, acted |

## 4. Core flows

1. **Daily generation** — at first app open of the day (or 00:05 worker): `PlanGenerator` creates
   `TaskInstance`s for the date from templates + training plan + `DailyTarget`.
2. **Reminders** — `ReminderScheduler` sets exact alarms for today's timed tasks; re-scheduled on
   boot (`BOOT_COMPLETED`) and when the profile/plan changes.
3. **Context check** — `SuggestionWorker` runs every ~60 min (WorkManager, battery-friendly):
   pulls steps from Health Connect, evaluates `SuggestionEngine` rules, respects quiet hours and
   daily cap, posts notification if a rule fires.
4. **Weekly review** — Sunday worker computes trend, adjusts step/reading goals, proposes
   kcal changes (user confirms).

## 5. Permissions

`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`,
Health Connect `READ_STEPS`. No internet permission needed for the MVP.

## 6. Privacy & backup
- All data local. JSON export/import from Settings (uses Storage Access Framework, user picks file).
- Android Auto Backup disabled by default to keep health data off cloud unless the user opts in.
