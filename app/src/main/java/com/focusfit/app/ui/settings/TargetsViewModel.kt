package com.focusfit.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfit.app.data.ProfileRepository
import com.focusfit.app.domain.HealthCalculator
import com.focusfit.app.domain.Targets
import com.focusfit.app.domain.model.TargetOverrides
import com.focusfit.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One editable target: blank text = use the suggestion. */
data class TargetField(val label: String, val unit: String, val suggested: Int, val range: IntRange, val text: String) {
    val value: Int? get() = text.trim().toIntOrNull()
    val error: String? get() = when {
        text.isBlank() -> null
        value == null || value!! !in range -> "Between ${range.first} and ${range.last}"
        else -> null
    }
}

enum class TargetKey { KCAL, PROTEIN, WATER, STEPS, READING }

data class TargetsUiState(
    val loaded: Boolean = false,
    val fields: Map<TargetKey, TargetField> = emptyMap(),
    val freeMealsPerWeek: Int = 3,
    val saved: Boolean = false,
) {
    val valid: Boolean get() = fields.values.all { it.error == null }
}

@HiltViewModel
class TargetsViewModel @Inject constructor(private val profiles: ProfileRepository) : ViewModel() {

    private val _state = MutableStateFlow(TargetsUiState())
    val state: StateFlow<TargetsUiState> = _state.asStateFlow()
    private var profile: UserProfile = UserProfile.NEW_USER

    init {
        viewModelScope.launch {
            profile = profiles.savedProfile.first() ?: return@launch
            val s: Targets = HealthCalculator.suggestedTargets(profile)
            val o = profile.targetOverrides
            fun text(v: Int?) = v?.toString().orEmpty()
            _state.value = TargetsUiState(
                loaded = true,
                fields = mapOf(
                    TargetKey.KCAL to TargetField("Calories (daily average)", "kcal", s.kcalAverage, HealthCalculator.KCAL_RANGE, text(o.kcal)),
                    TargetKey.PROTEIN to TargetField("Protein", "g", s.proteinG, HealthCalculator.PROTEIN_RANGE, text(o.proteinG)),
                    TargetKey.WATER to TargetField("Water (rest days; +500 on training days)", "ml", s.waterMl, HealthCalculator.WATER_RANGE, text(o.waterMl)),
                    TargetKey.STEPS to TargetField("Steps (fixed; turns off weekly increase)", "steps", s.stepGoal, HealthCalculator.STEPS_RANGE, text(o.steps)),
                    TargetKey.READING to TargetField("Reading", "min", s.readingMin, HealthCalculator.READING_RANGE, text(o.readingMin)),
                ),
                freeMealsPerWeek = profile.freeMealsPerWeek,
            )
        }
    }

    fun setText(key: TargetKey, text: String) = _state.update { s ->
        val f = s.fields.getValue(key)
        if (text.length > 5 || !text.all(Char::isDigit)) s else s.copy(fields = s.fields + (key to f.copy(text = text)), saved = false)
    }

    fun reset(key: TargetKey) = setText(key, "")

    fun setFreeMeals(n: Int) = _state.update { it.copy(freeMealsPerWeek = n.coerceIn(0, 7), saved = false) }

    fun save() {
        val s = _state.value
        if (!s.valid) return
        val f = s.fields
        val overrides = TargetOverrides(
            kcal = f[TargetKey.KCAL]?.value,
            proteinG = f[TargetKey.PROTEIN]?.value,
            waterMl = f[TargetKey.WATER]?.value,
            steps = f[TargetKey.STEPS]?.value,
            readingMin = f[TargetKey.READING]?.value,
        )
        viewModelScope.launch {
            val latest = profiles.savedProfile.first() ?: profile
            profiles.save(latest.copy(targetOverrides = overrides, freeMealsPerWeek = s.freeMealsPerWeek))
            _state.update { it.copy(saved = true) }
        }
    }
}
