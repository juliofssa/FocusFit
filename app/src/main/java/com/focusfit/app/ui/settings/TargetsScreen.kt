package com.focusfit.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfit.app.ui.common.InfoCard

@Composable
fun TargetsScreen(onClose: () -> Unit, vm: TargetsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.saved) { if (s.saved) onClose() }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                Text("Your targets", style = MaterialTheme.typography.titleLarge)
            }
        }
        item {
            Text(
                "FocusFit calculates these from your profile. Type your own number to override one (for example one from " +
                    "your nutritionist), or leave it blank to keep the suggestion.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        items(TargetKey.entries, key = { it.name }) { key ->
            val f = s.fields[key] ?: return@items
            InfoCard(Modifier.padding(horizontal = 16.dp)) {
                Text(f.label, style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = f.text,
                        onValueChange = { vm.setText(key, it) },
                        placeholder = { Text("${f.suggested}") },
                        suffix = { Text(f.unit) },
                        isError = f.error != null,
                        supportingText = { Text(f.error ?: if (f.text.isBlank()) "Using suggestion: ${f.suggested}" else "Suggested: ${f.suggested}") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    if (f.text.isNotBlank()) TextButton(onClick = { vm.reset(key) }) { Text("Reset") }
                }
            }
        }
        item {
            InfoCard(Modifier.padding(horizontal = 16.dp)) {
                Text("🎟️ Free meals per week", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { vm.setFreeMeals(s.freeMealsPerWeek - 1) }) { Text("−") }
                    Text("${s.freeMealsPerWeek}", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { vm.setFreeMeals(s.freeMealsPerWeek + 1) }) { Text("+") }
                }
                Text(
                    "Meals you enjoy without tracking (80/20 rule). 2–3 is a good balance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Button(onClick = vm::save, enabled = s.loaded && s.valid, modifier = Modifier.fillMaxWidth()) { Text("Save") }
            }
        }
    }
}
