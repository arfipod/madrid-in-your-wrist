package com.arfipod.madridinyourwrist.examples

import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.VideoView
import java.net.URI
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text

object VideoExampleSource {
    const val STREAM_URL = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4"

    fun host(): String = URI(STREAM_URL).host

    fun isHttpsMp4(): Boolean {
        val uri = URI(STREAM_URL)
        return uri.scheme == "https" && uri.path.endsWith(".mp4")
    }
}

@Composable
fun VideoExample(
    onBack: () -> Unit,
    onEvent: (String) -> Unit,
) {
    var status by remember { mutableStateOf("LOADING") }
    var playing by remember { mutableStateOf(true) }
    var videoView by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(videoView) {
        onDispose {
            videoView?.stopPlayback()
        }
    }

    ExampleChrome(title = ExampleKind.VIDEO.title, onBack = onBack) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = { context ->
                VideoView(context).apply {
                    setBackgroundColor(AndroidColor.BLACK)
                    setOnPreparedListener { player ->
                        player.isLooping = true
                        status = "PLAYING"
                        onEvent("Video example prepared from ${VideoExampleSource.host()}")
                        if (playing) start()
                    }
                    setOnErrorListener { _, what, extra ->
                        status = "ERROR $what/$extra"
                        onEvent("Video example failed: $what/$extra")
                        true
                    }
                    setVideoURI(Uri.parse(VideoExampleSource.STREAM_URL))
                    videoView = this
                }
            },
            update = { view ->
                if (playing && !view.isPlaying) {
                    view.start()
                } else if (!playing && view.isPlaying) {
                    view.pause()
                }
            },
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = status,
            color = if (status.startsWith("ERROR")) Color(0xFFFF8A80) else Color.White,
            textAlign = TextAlign.Center,
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { playing = !playing },
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = if (playing) "PAUSE" else "PLAY",
                textAlign = TextAlign.Center,
            )
        }
    }
}
