package com.focusfit.app.data

import android.content.Context
import com.focusfit.app.domain.LibraryParser
import com.focusfit.app.domain.MealLibrary
import com.focusfit.app.domain.WorkoutLibrary
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Read-only content bundled with the app (the JSON files in assets), parsed once on first use. */
@Singleton
class LibraryRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    val workouts: WorkoutLibrary by lazy { LibraryParser.workouts(read("workouts.json")) }

    val meals: MealLibrary by lazy { LibraryParser.meals(read("meals.json")) }

    private fun read(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }
}
