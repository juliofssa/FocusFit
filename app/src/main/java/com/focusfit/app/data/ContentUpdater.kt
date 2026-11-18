package com.focusfit.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.focusfit.app.domain.ContentManifest
import com.focusfit.app.domain.ContentValidator
import com.focusfit.app.domain.LibraryParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed interface UpdateResult {
    data class Updated(val version: Int, val notes: String?) : UpdateResult
    data object UpToDate : UpdateResult
    /** Not checked (updates off, not on Wi-Fi, or checked recently). */
    data class Skipped(val reason: String) : UpdateResult
    data class Failed(val reason: String) : UpdateResult
}

/**
 * Downloads newer content packs (exercises, routines, videos, meals) from the configured URL.
 * Download-only: nothing about the user is sent. A pack is used only if its checksums match and it passes
 * [ContentValidator]; otherwise the current content stays.
 */
@Singleton
class ContentUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val library: LibraryRepository,
    private val settings: SettingsRepository,
) {
    private val mutex = Mutex()

    /** Automatic check: at most weekly and only on an unmetered (Wi-Fi) connection. */
    suspend fun checkIfDue(): UpdateResult {
        val last = settings.contentLastCheck.first()
        if (System.currentTimeMillis() - last < CHECK_INTERVAL_MS) return UpdateResult.Skipped("Checked recently")
        if (!onUnmeteredNetwork()) return UpdateResult.Skipped("Waiting for Wi-Fi")
        return checkNow()
    }

    /** Checks right away (the "Check now" button), on any connection. */
    suspend fun checkNow(): UpdateResult = mutex.withLock {
        val base = settings.contentUrl.first().trim()
        if (base.isEmpty()) return UpdateResult.Skipped("Online updates are off")
        val result = withContext(Dispatchers.IO) {
            runCatching { download(base.trimEnd('/') + "/") }
                .getOrElse { e ->
                    Log.w(TAG, "Content update failed", e)
                    UpdateResult.Failed(
                        when (e) {
                            is HttpStatusException -> if (e.code == 404) "Content not found (is the repo public?)" else "Server answered ${e.code}"
                            is IOException -> "No connection"
                            else -> e.message ?: e.javaClass.simpleName
                        },
                    )
                }
        }
        settings.recordContentCheck(System.currentTimeMillis(), describe(result))
        result
    }

    private fun download(base: String): UpdateResult {
        val manifestText = get(base + "manifest.json", MAX_MANIFEST_BYTES)
        val manifest = ContentManifest.parse(manifestText)
        if (manifest.format > ContentManifest.SUPPORTED_FORMAT) return UpdateResult.Failed("Needs a newer app version")
        if (manifest.version <= library.version) return UpdateResult.UpToDate

        val workoutsText = get(base + ContentManifest.WORKOUTS, MAX_FILE_BYTES)
        val mealsText = get(base + ContentManifest.MEALS, MAX_FILE_BYTES)
        if (LibraryRepository.sha256(workoutsText) != manifest.files[ContentManifest.WORKOUTS] ||
            LibraryRepository.sha256(mealsText) != manifest.files[ContentManifest.MEALS]
        ) return UpdateResult.Failed("Checksum mismatch, update ignored")

        val pack = ContentPack(manifest.version, LibraryParser.workouts(workoutsText), LibraryParser.meals(mealsText), downloaded = true)
        val errors = ContentValidator.validate(pack.workouts, pack.meals)
        if (errors.isNotEmpty()) return UpdateResult.Failed("Invalid content: ${errors.first()}")

        library.install(manifestText, workoutsText, mealsText, pack)
        return UpdateResult.Updated(manifest.version, manifest.notes)
    }

    private fun get(url: String, maxBytes: Int): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            useCaches = false
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (conn.responseCode != 200) throw HttpStatusException(conn.responseCode)
            val bytes = conn.inputStream.use { input ->
                val buf = input.readNBytesCompat(maxBytes + 1)
                if (buf.size > maxBytes) throw IOException("File too large")
                buf
            }
            return bytes.toString(Charsets.UTF_8)
        } finally {
            conn.disconnect()
        }
    }

    private fun onUnmeteredNetwork(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    private class HttpStatusException(val code: Int) : IOException("HTTP $code")

    companion object {
        private const val TAG = "ContentUpdater"
        private const val CHECK_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000
        private const val TIMEOUT_MS = 15_000
        private const val MAX_MANIFEST_BYTES = 64 * 1024
        private const val MAX_FILE_BYTES = 1024 * 1024

        fun describe(r: UpdateResult): String = when (r) {
            is UpdateResult.Updated -> "Updated to content v${r.version}" + (r.notes?.let { ": $it" } ?: "")
            UpdateResult.UpToDate -> "Up to date"
            is UpdateResult.Skipped -> r.reason
            is UpdateResult.Failed -> "Update failed: ${r.reason}"
        }
    }
}

/** InputStream.readNBytes is API 33+; read up to [limit] bytes on older Androids too. */
private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(8 * 1024)
    while (out.size() < limit) {
        val n = read(buf, 0, minOf(buf.size, limit - out.size()))
        if (n < 0) break
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}
