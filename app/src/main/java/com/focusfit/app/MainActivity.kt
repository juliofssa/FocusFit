package com.focusfit.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfit.app.data.SettingsRepository
import com.focusfit.app.ui.FocusFitRoot
import com.focusfit.app.ui.theme.FocusFitTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settings: SettingsRepository

    /** Routine to open, when the app was launched from a core/mobility reminder. */
    private val pendingRoutine = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingRoutine.value = intent.getStringExtra(EXTRA_ROUTINE_ID)
        setContent {
            val wallpaperColors by settings.useWallpaperColors.collectAsStateWithLifecycle(initialValue = false)
            FocusFitTheme(useWallpaperColors = wallpaperColors) {
                // One themed background behind every screen, so text always sits on a matching colour
                // (onboarding and full-screen pages have no Scaffold of their own).
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    FocusFitRoot(
                        pendingRoutine = pendingRoutine.value,
                        onRoutineOpened = { pendingRoutine.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_ROUTINE_ID)?.let { pendingRoutine.value = it }
    }

    companion object {
        const val EXTRA_ROUTINE_ID = "routine_id"
    }
}
