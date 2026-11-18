package com.focusfit.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.focusfit.app.ui.theme.FocusFitTheme

/** Shown by Health Connect when the user asks why FocusFit wants step data. */
class PrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusFitTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("How FocusFit uses your data", style = MaterialTheme.typography.headlineSmall)
                        Text("FocusFit only reads your step count. It shows today's steps on your to-do list and raises your step goal week by week.")
                        Text("Your data stays on this phone. FocusFit has no account, no server and no ads, and it never writes to Health Connect.")
                        Text("You can remove access at any time in Health Connect settings.")
                        Button(onClick = { finish() }) { Text("OK") }
                    }
                }
            }
        }
    }
}
