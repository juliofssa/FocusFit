package com.focusfit.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "book")
@kotlinx.serialization.Serializable
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalPages: Int,
    val currentPage: Int,
    val finishedOn: String?,
    val createdAt: Long,
)

@Entity(tableName = "reading_session")
@kotlinx.serialization.Serializable
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val bookId: Long?,
    val minutes: Int,
    val pages: Int,
)

@Dao
interface ReadingDao {
    @Query("SELECT * FROM book ORDER BY finishedOn IS NOT NULL, createdAt DESC")
    fun observeBooks(): Flow<List<BookEntity>>

    @Insert
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("SELECT * FROM book WHERE id = :id")
    suspend fun book(id: Long): BookEntity?

    @Query("DELETE FROM book WHERE id = :id")
    suspend fun deleteBook(id: Long)

    @Query("SELECT * FROM reading_session WHERE date >= :from ORDER BY date")
    fun observeSessionsSince(from: String): Flow<List<ReadingSessionEntity>>

    @Insert
    suspend fun insertSession(session: ReadingSessionEntity)
}
