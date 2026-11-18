package com.focusfit.app.ui.common

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.focusfit.app.domain.Exercise
import com.focusfit.app.domain.YouTube
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

/** Embedded YouTube player (official IFrame player via android-youtube-player; no API key). */
@Composable
fun YouTubePlayer(videoId: String, startSec: Int, modifier: Modifier = Modifier) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    AndroidView(
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f),
        factory = { ctx ->
            YouTubePlayerView(ctx).apply {
                enableAutomaticInitialization = false
                lifecycle.addObserver(this)
                initialize(
                    object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) = youTubePlayer.loadVideo(videoId, startSec.toFloat())
                    },
                    IFramePlayerOptions.Builder(ctx).controls(1).rel(0).ivLoadPolicy(3).build(),
                )
            }
        },
        onRelease = { view ->
            lifecycle.removeObserver(view)
            view.release()
        },
    )
}

/**
 * Full-width dialog playing [videoId] for [exercise]. With no video, offers a YouTube search instead.
 */
@Composable
fun ExerciseVideoDialog(exercise: Exercise, videoId: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    fun open(url: String) = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) { }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth().padding(12.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                if (videoId != null) {
                    YouTubePlayer(videoId, if (videoId == exercise.youtubeId) exercise.videoStartSec else 0)
                    Text(
                        "Needs internet. Videos play from YouTube.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text("No video for this exercise yet.")
                }
                exercise.cues.forEach { Text("✔ $it") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (videoId != null) {
                        TextButton(onClick = { open(YouTube.watchUrl(videoId, exercise.videoStartSec)) }) { Text("Open in YouTube") }
                    } else {
                        TextButton(onClick = { open(YouTube.searchUrl(exercise.name)) }) { Text("Search YouTube") }
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}
