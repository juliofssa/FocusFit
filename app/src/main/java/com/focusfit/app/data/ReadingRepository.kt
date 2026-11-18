package com.focusfit.app.data

import com.focusfit.app.data.local.BookEntity
import com.focusfit.app.data.local.ReadingDao
import com.focusfit.app.data.local.ReadingSessionEntity
import com.focusfit.app.domain.Book
import com.focusfit.app.domain.ReadingSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingRepository @Inject constructor(private val dao: ReadingDao) {

    val books: Flow<List<Book>> = dao.observeBooks().map { rows ->
        rows.map { Book(it.id, it.title, it.totalPages, it.currentPage, it.finishedOn?.let(LocalDate::parse)) }
    }

    fun sessionsSince(from: LocalDate): Flow<List<ReadingSession>> = dao.observeSessionsSince(from.toString()).map { rows ->
        rows.map { ReadingSession(LocalDate.parse(it.date), it.bookId, it.minutes, it.pages) }
    }

    suspend fun addBook(title: String, totalPages: Int) {
        dao.insertBook(BookEntity(title = title.trim(), totalPages = totalPages, currentPage = 0, finishedOn = null, createdAt = System.currentTimeMillis()))
    }

    suspend fun deleteBook(id: Long) = dao.deleteBook(id)

    suspend fun finishBook(id: Long, date: LocalDate) {
        val b = dao.book(id) ?: return
        dao.updateBook(b.copy(currentPage = b.totalPages, finishedOn = date.toString()))
    }

    /** Logs a session; when it's for a book, moves its bookmark forward (and finishes it at the last page). */
    suspend fun logSession(date: LocalDate, bookId: Long?, minutes: Int, pages: Int) {
        dao.insertSession(ReadingSessionEntity(date = date.toString(), bookId = bookId, minutes = minutes, pages = pages))
        val b = bookId?.let { dao.book(it) } ?: return
        val page = (b.currentPage + pages).coerceAtMost(b.totalPages)
        dao.updateBook(b.copy(currentPage = page, finishedOn = if (page >= b.totalPages) date.toString() else b.finishedOn))
    }
}
