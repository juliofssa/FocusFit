package com.focusfit.app.domain

import com.focusfit.app.domain.model.FoodTag

/** Which meal ideas a person can be offered, given what they don't eat. */
object MealFilter {
    /** Tags in meals.json "contains" that aren't a known [FoodTag] are ignored. */
    fun tagsOf(meal: MealIdea): Set<FoodTag> =
        meal.contains.mapNotNull { name -> FoodTag.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } }.toSet()

    fun allowed(meal: MealIdea, excluded: Set<FoodTag>): Boolean = tagsOf(meal).none { it in excluded }
}
