package com.focusfit.app.data

import com.focusfit.app.data.local.WeightDao
import com.focusfit.app.data.local.WeightEntity
import com.focusfit.app.domain.WeightEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class WeightRepository @Inject constructor(
    private val dao: WeightDao,
    private val profiles: ProfileRepository,
) {
    val entries: Flow<List<WeightEntry>> = dao.observeAll().map { rows ->
        rows.map { WeightEntry(LocalDate.parse(it.date), it.kg, it.waistCm) }
    }

    /**
     * Saves a weigh-in. When it differs from the profile weight by [RECALC_THRESHOLD_KG] or more,
     * the profile is updated so calorie and protein targets follow (docs/HEALTH_ENGINE.md §5).
     */
    suspend fun log(date: LocalDate, kg: Double, waistCm: Double?) {
        dao.upsert(WeightEntity(date.toString(), kg, waistCm))
        val profile = profiles.savedProfile.first() ?: return
        if (abs(profile.weightKg - kg) >= RECALC_THRESHOLD_KG) profiles.save(profile.copy(weightKg = kg))
    }

    suspend fun delete(date: LocalDate) = dao.delete(date.toString())

    companion object {
        const val RECALC_THRESHOLD_KG = 2.0
    }
}
