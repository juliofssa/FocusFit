package com.focusfit.app.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfit.app.domain.Exercise
import com.focusfit.app.domain.Progression
import com.focusfit.app.domain.SessionBuilder
import com.focusfit.app.ui.common.ExerciseVideoDialog
import com.focusfit.app.ui.common.InfoCard
import kotlin.math.roundToInt

@Composable
fun SessionScreen(onClose: () -> Unit, vm: SessionViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var video by remember { mutableStateOf<Exercise?>(null) }
    LaunchedEffect(s.phase) { if (s.phase == Phase.SAVED) onClose() }
    val routine = s.routine ?: return

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Close session") }
            Column {
                Text(routine.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Level ${s.levelStatus.level} of ${Progression.MAX_LEVEL}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (s.phase == Phase.ACTIVE) {
            LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (s.phase) {
                Phase.OVERVIEW -> Overview(s, vm::start, onVideo = { video = it })
                Phase.ACTIVE -> Active(s, vm, onVideo = { vm.pause(); video = it })
                Phase.RATE, Phase.SAVED -> Rate(vm::finish)
            }
        }
    }
    video?.let { e -> ExerciseVideoDialog(e, s.videoFor(e), onDismiss = { video = null }) }
}

@Composable
private fun Overview(s: SessionUiState, onStart: () -> Unit, onVideo: (Exercise) -> Unit) {
    val routine = s.routine!!
    val ls = s.levelStatus
    InfoCard {
        Text("~${SessionBuilder.estimatedMinutes(s.steps)} min · " + if (routine.isCircuit) "circuit" else "sets")
        Text(
            when {
                ls.tooHard -> "Your last 2 sessions felt very hard. Use the Easier buttons where needed."
                ls.isMax -> "Top level reached. Great work!"
                else -> "${ls.easySessions}/${Progression.SESSIONS_TO_LEVEL_UP} comfortable sessions (effort ≤ ${Progression.EASY_RPE}) to unlock level ${ls.level + 1}."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    // Unique exercises in order, with their amount.
    s.steps.filter { !it.isRest }.distinctBy { it.routineStepIndex }.forEach { step ->
        val sets = s.steps.count { !it.isRest && it.routineStepIndex == step.routineStepIndex && it.sideLabel != "right" }
        val ex = step.exercise!!
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("• ${ex.name}", modifier = Modifier.weight(1f))
            Text("${sets}× ${step.amount}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { onVideo(ex) }) { Text(if (s.videoFor(ex) != null) "🎬" else "🔎") }
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Start") }
    Text(
        "Stop if something hurts, especially your lower back, and switch to the easier version.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Active(s: SessionUiState, vm: SessionViewModel, onVideo: (Exercise) -> Unit) {
    val step = s.current ?: return
    Card(
        Modifier.fillMaxWidth(),
        colors = if (step.isRest) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(step.setLabel, style = MaterialTheme.typography.labelLarge)
            Text(step.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            if (s.remaining != null) {
                Text(
                    "%d:%02d".format(s.remaining / 60, s.remaining % 60),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(step.amount, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = vm::back, enabled = s.index > 0, modifier = Modifier.weight(1f)) { Text("Back") }
        if (s.remaining != null) {
            OutlinedButton(onClick = vm::togglePause, modifier = Modifier.weight(1f)) { Text(if (s.paused) "Resume" else "Pause") }
            Button(onClick = vm::advance, modifier = Modifier.weight(1f)) { Text("Skip") }
        } else {
            Button(onClick = vm::advance, modifier = Modifier.weight(2f)) { Text("Done ✔") }
        }
    }

    step.exercise?.let { ex ->
        AssistChip(onClick = { onVideo(ex) }, label = { Text("🎬 How to do it (pauses the timer)") })
    }

    if (s.easier != null || s.harder != null) {
        Column {
            s.easier?.let { e -> AssistChip(onClick = { vm.swap(e) }, label = { Text("⬇ Easier: ${e.name}") }) }
            s.harder?.let { h -> AssistChip(onClick = { vm.swap(h) }, label = { Text("⬆ Harder: ${h.name}") }) }
        }
    }

    step.exercise?.cues?.takeIf { it.isNotEmpty() }?.let { cues ->
        InfoCard { cues.forEach { Text("✔ $it") } }
    }
    s.next?.let { n ->
        Text(
            "Next: ${n.title} · ${n.amount}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Rate(onSave: (Int) -> Unit) {
    var rpe by remember { mutableFloatStateOf(6f) }
    val value = rpe.roundToInt()
    Text("Session done! 🎉", style = MaterialTheme.typography.headlineSmall)
    Text("How hard was it?", style = MaterialTheme.typography.titleMedium)
    Text("$value / 10 · ${rpeLabel(value)}", style = MaterialTheme.typography.bodyLarge)
    Slider(value = rpe, onValueChange = { rpe = it }, valueRange = 1f..10f, steps = 8)
    Text(
        "Sessions rated ${Progression.EASY_RPE} or less count towards the next level.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(onClick = { onSave(value) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
}

private fun rpeLabel(rpe: Int) = when (rpe) {
    in 1..3 -> "very easy"
    in 4..5 -> "comfortable"
    in 6..7 -> "challenging"
    8 -> "hard"
    else -> "very hard"
}
