package com.focusfit.app.domain

import java.time.LocalDate

/** Weekly step-goal progression (docs/HEALTH_ENGINE.md §4). */
object StepGoals {
    /** New goal after a week, given that week's daily step totals. */
    fun evaluate(goal: Int, weekSteps: List<Int>): Int =
        HealthCalculator.nextStepGoal(goal, daysGoalHit = weekSteps.count { it >= goal })
}

data class Book(
    val id: Long,
    val title: String,
    val totalPages: Int,
    val currentPage: Int,
    val finishedOn: LocalDate? = null,
) {
    val progress: Float get() = if (totalPages <= 0) 0f else (currentPage.toFloat() / totalPages).coerceIn(0f, 1f)
    val isFinished: Boolean get() = finishedOn != null
}

data class ReadingSession(val date: LocalDate, val bookId: Long?, val minutes: Int, val pages: Int)

data class ReadingStats(
    val todayMinutes: Int,
    val weekMinutes: Int,
    val monthPages: Int,
    val booksThisYear: Int,
    /** Consecutive days with any reading, ending today or yesterday. */
    val streak: Int,
) {
    companion object {
        fun of(sessions: List<ReadingSession>, books: List<Book>, today: LocalDate): ReadingStats {
            val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
            val byDay = sessions.groupBy { it.date }.mapValues { (_, s) -> s.sumOf { it.minutes } }
            var day = if ((byDay[today] ?: 0) > 0) today else today.minusDays(1)
            var streak = 0
            while ((byDay[day] ?: 0) > 0) { streak++; day = day.minusDays(1) }
            return ReadingStats(
                todayMinutes = byDay[today] ?: 0,
                weekMinutes = sessions.filter { !it.date.isBefore(monday) && !it.date.isAfter(today) }.sumOf { it.minutes },
                monthPages = sessions.filter { it.date.year == today.year && it.date.month == today.month }.sumOf { it.pages },
                booksThisYear = books.count { it.finishedOn?.year == today.year },
                streak = streak,
            )
        }
    }
}
