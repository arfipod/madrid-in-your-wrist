package com.arfipod.wearosplayground.examples

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
import com.arfipod.wearosplayground.BuildConfig

private sealed interface EmtUiState {
    data object MissingCredentials : EmtUiState
    data object Loading : EmtUiState
    data class Loaded(val arrival: EmtBusArrival) : EmtUiState
    data object NoArrival : EmtUiState
    data class Failed(val message: String) : EmtUiState
}

@Composable
fun EmtMadridExample(
    onBack: () -> Unit,
    onEvent: (String) -> Unit,
) {
    val client = remember {
        EmtMadridClient(
            credentials = EmtMadridCredentials(
                clientId = BuildConfig.EMT_CLIENT_ID,
                passKey = BuildConfig.EMT_PASS_KEY,
                email = BuildConfig.EMT_EMAIL,
                password = BuildConfig.EMT_PASSWORD,
            ),
        )
    }
    val hasCredentials = (BuildConfig.EMT_CLIENT_ID.isNotBlank() && BuildConfig.EMT_PASS_KEY.isNotBlank()) ||
        (BuildConfig.EMT_EMAIL.isNotBlank() && BuildConfig.EMT_PASSWORD.isNotBlank())
    var requestCount by remember { mutableIntStateOf(0) }
    var state by remember {
        mutableStateOf<EmtUiState>(
            if (!hasCredentials) {
                EmtUiState.MissingCredentials
            } else {
                EmtUiState.Loading
            }
        )
    }

    LaunchedEffect(requestCount) {
        if (!hasCredentials) {
            state = EmtUiState.MissingCredentials
            return@LaunchedEffect
        }

        state = EmtUiState.Loading
        state = runCatching { client.fetchNextE3Arrival() }
            .onSuccess { arrival ->
                onEvent(
                    if (arrival == null) {
                        "EMT E3 example found no arrival"
                    } else {
                        "EMT E3 next arrival ${arrival.minutesUntil} min"
                    }
                )
            }
            .onFailure { error ->
                onEvent("EMT E3 example failed: ${error.message}")
            }
            .fold(
                onSuccess = { arrival ->
                    if (arrival == null) EmtUiState.NoArrival else EmtUiState.Loaded(arrival)
                },
                onFailure = { EmtUiState.Failed(it.message ?: "Unknown EMT error") },
            )
    }

    ExampleChrome(title = ExampleKind.EMT.title, onBack = onBack) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = EmtMadridTarget.LABEL,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            text = state.label(),
            color = if (state is EmtUiState.Failed || state is EmtUiState.MissingCredentials) {
                Color(0xFFFF8A80)
            } else {
                Color.White
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is EmtUiState.Loading,
            onClick = { requestCount += 1 },
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = if (state is EmtUiState.Loading) "LOADING" else "REFRESH",
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun EmtUiState.label(): String = when (this) {
    EmtUiState.MissingCredentials -> "Set EMT login"
    EmtUiState.Loading -> "Reading EMT..."
    is EmtUiState.Loaded -> arrival.compactLabel()
    EmtUiState.NoArrival -> "No E3 arrival"
    is EmtUiState.Failed -> message
}
