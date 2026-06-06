package com.arfipod.madridinyourwrist.examples

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.arfipod.madridinyourwrist.BuildConfig

private sealed interface MetroUiState {
    data object MissingKey : MetroUiState
    data object Loading : MetroUiState
    data class Loaded(val result: MetroScheduleResult) : MetroUiState
    data object NoDeparture : MetroUiState
    data class Failed(val message: String) : MetroUiState
}

@Composable
fun MetroMadridExample(
    onBack: () -> Unit,
    onEvent: (String) -> Unit,
) {
    val target = remember { MetroScheduleTarget() }
    val client = remember { NapMetroClient(apiKey = BuildConfig.NAP_API_KEY) }
    val schedule = remember { MetroGtfsSchedule() }
    var requestCount by remember { mutableIntStateOf(0) }
    var state by remember {
        mutableStateOf<MetroUiState>(
            if (BuildConfig.NAP_API_KEY.isBlank()) MetroUiState.MissingKey else MetroUiState.Loading
        )
    }

    LaunchedEffect(requestCount) {
        if (BuildConfig.NAP_API_KEY.isBlank()) {
            state = MetroUiState.MissingKey
            return@LaunchedEffect
        }

        state = MetroUiState.Loading
        state = runCatching {
            val zipBytes = client.fetchMetroGtfsZip()
            schedule.nextDepartureResult(gtfsZipBytes = zipBytes, target = target)
        }
            .onSuccess { result ->
                onEvent(
                    if (result.departure == null) {
                        "Metro Madrid example found no departure"
                    } else {
                        "Metro Madrid next departure ${result.departure.minutesUntil} min"
                    }
                )
            }
            .onFailure { error ->
                onEvent("Metro Madrid example failed: ${error.message}")
            }
            .fold(
                onSuccess = { result ->
                    if (result.departure == null) MetroUiState.NoDeparture else MetroUiState.Loaded(result)
                },
                onFailure = { MetroUiState.Failed(it.message ?: "Unknown NAP error") },
            )
    }

    ExampleChrome(title = ExampleKind.METRO.title, onBack = onBack) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = target.label,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            text = state.label(),
            color = if (state is MetroUiState.Failed || state is MetroUiState.MissingKey) {
                Color(0xFFFF8A80)
            } else {
                Color.White
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is MetroUiState.Loading,
            onClick = { requestCount += 1 },
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = if (state is MetroUiState.Loading) "LOADING" else "REFRESH",
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun MetroUiState.label(): String = when (this) {
    MetroUiState.MissingKey -> "Set NAP_API_KEY"
    MetroUiState.Loading -> "Reading NAP..."
    is MetroUiState.Loaded -> result.compactLabel()
    MetroUiState.NoDeparture -> "No scheduled train"
    is MetroUiState.Failed -> message
}
