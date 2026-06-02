package com.arfipod.wearosplayground.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

@Composable
fun ExampleSurface(
    example: ExampleKind,
    onBack: () -> Unit,
    onEvent: (String) -> Unit,
) {
    when (example) {
        ExampleKind.FLAPPY -> FlappyBirdExample(onBack = onBack, onEvent = onEvent)
        ExampleKind.API -> ApiExample(onBack = onBack, onEvent = onEvent)
        ExampleKind.METRO -> MetroMadridExample(onBack = onBack, onEvent = onEvent)
        ExampleKind.EMT -> EmtMadridExample(onBack = onBack, onEvent = onEvent)
        ExampleKind.CUBE_3D -> ThreeDExample(onBack = onBack, onEvent = onEvent)
        ExampleKind.AUDIO -> AudioExample(onBack = onBack, onEvent = onEvent)
        ExampleKind.VIDEO -> VideoExample(onBack = onBack, onEvent = onEvent)
    }
}

@Composable
internal fun ExampleChrome(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
            )
            content()
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "BACK",
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
