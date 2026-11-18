package com.focusfit.app.data

import android.content.Context
import android.util.Log
import com.focusfit.app.domain.ContentManifest
import com.focusfit.app.domain.ContentValidator
import com.focusfit.app.domain.LibraryParser
import com.focusfit.app.domain.MealLibrary
import com.focusfit.app.domain.WorkoutLibrary
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** A complete set of exercises/routines and meal ideas, from the app or from an online content pack. */
data class ContentPack(val version: Int, val workouts: WorkoutLibrary, val meals: MealLibrary, val downloaded: Boolean)

/**
 * Exercises, routines and meal ideas. Uses the last downloaded content pack if it's newer than the
 * built-in one and still valid; otherwise the content shipped in assets/. Never throws for bad content.
 */
@Singleton
class LibraryRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    private val dir = File(context.filesDir, "content")
    private val _pack = MutableStateFlow<ContentPack?>(null)

    /** The active pack (emits again after an online update is installed). */
    val pack: StateFlow<ContentPack?> = _pack.asStateFlow()

    private fun current(): ContentPack = _pack.value ?: synchronized(this) {
        _pack.value ?: (loadCached() ?: bundled()).also { _pack.value = it }
    }

    val workouts: WorkoutLibrary get() = current().workouts
    val meals: MealLibrary get() = current().meals
    val version: Int get() = current().version

    /** Saves a verified pack to disk (atomically) and switches to it. */
    @Synchronized
    fun install(manifestText: String, workoutsText: String, mealsText: String, pack: ContentPack) {
        val tmp = File(context.filesDir, "content.tmp").apply { deleteRecursively(); mkdirs() }
        File(tmp, "manifest.json").writeText(manifestText)
        File(tmp, ContentManifest.WORKOUTS).writeText(workoutsText)
        File(tmp, ContentManifest.MEALS).writeText(mealsText)
        dir.deleteRecursively()
        check(tmp.renameTo(dir)) { "Couldn't save content" }
        _pack.value = pack
    }

    /** Drops downloaded content and goes back to the built-in pack. */
    @Synchronized
    fun resetToBundled() {
        dir.deleteRecursively()
        _pack.value = bundled()
    }

    private fun bundled() = ContentPack(
        version = BUNDLED_VERSION,
        workouts = LibraryParser.workouts(asset(ContentManifest.WORKOUTS)),
        meals = LibraryParser.meals(asset(ContentManifest.MEALS)),
        downloaded = false,
    )

    private fun loadCached(): ContentPack? = runCatching {
        val manifestFile = File(dir, "manifest.json")
        if (!manifestFile.exists()) return null
        val manifest = ContentManifest.parse(manifestFile.readText())
        if (manifest.version <= BUNDLED_VERSION || manifest.format > ContentManifest.SUPPORTED_FORMAT) return null
        val w = File(dir, ContentManifest.WORKOUTS).readText()
        val m = File(dir, ContentManifest.MEALS).readText()
        require(sha256(w) == manifest.files[ContentManifest.WORKOUTS] && sha256(m) == manifest.files[ContentManifest.MEALS])
        val pack = ContentPack(manifest.version, LibraryParser.workouts(w), LibraryParser.meals(m), downloaded = true)
        require(ContentValidator.validate(pack.workouts, pack.meals).isEmpty())
        pack
    }.onFailure { Log.w(TAG, "Ignoring cached content", it) }.getOrNull()

    private fun asset(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }

    companion object {
        /** Version of the content shipped inside the app; online packs must be newer to be used. */
        const val BUNDLED_VERSION = 1
        private const val TAG = "LibraryRepository"

        fun sha256(text: String): String =
            MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
