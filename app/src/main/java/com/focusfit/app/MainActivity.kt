package com.focusfit.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.focusfit.app.ui.FocusFitRoot
import com.focusfit.app.ui.theme.FocusFitTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Routine to open, when the app was launched from a core/mobility reminder. */
    private val pendingRoutine = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingRoutine.value = intent.getStringExtra(EXTRA_ROUTINE_ID)
        setContent {
            FocusFitTheme {
                FocusFitRoot(
                    pendingRoutine = pendingRoutine.value,
                    onRoutineOpened = { pendingRoutine.value = null },
                )
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
