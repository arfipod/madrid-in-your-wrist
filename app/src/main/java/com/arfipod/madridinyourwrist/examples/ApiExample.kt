package com.arfipod.madridinyourwrist.examples

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ApiExampleConfig {
    const val ENDPOINT = "https://api.github.com/zen"
}

object ApiOutputFormatter {
    fun preview(rawOutput: String, maxChars: Int = 180): String {
        require(maxChars > 4) { "Preview limit must leave room for content." }
        val compact = rawOutput
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n")

        if (compact.length <= maxChars) return compact
        return compact.take(maxChars - 3) + "..."
    }
}

class ApiExampleClient(
    private val endpoint: String = ApiExampleConfig.ENDPOINT,
) {
    suspend fun fetchOutput(): String = withContext(Dispatchers.IO) {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "wearos-playground")

        try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (responseCode !in 200..299) {
                throw IOException("HTTP $responseCode ${ApiOutputFormatter.preview(body, 80)}")
            }
            ApiOutputFormatter.preview(body)
        } finally {
            connection.disconnect()
        }
    }
}

private sealed interface ApiUiState {
    data object Loading : ApiUiState
    data class Loaded(val output: String) : ApiUiState
    data class Failed(val message: String) : ApiUiState
}

@Composable
fun ApiExample(
    onBack: () -> Unit,
    onEvent: (String) -> Unit,
) {
    val client = remember { ApiExampleClient() }
    var requestCount by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<ApiUiState>(ApiUiState.Loading) }

    LaunchedEffect(requestCount) {
        state = ApiUiState.Loading
        state = runCatching { client.fetchOutput() }
            .onSuccess { onEvent("API example fetched ${it.length} chars") }
            .onFailure { onEvent("API example failed: ${it.message}") }
            .fold(
                onSuccess = { ApiUiState.Loaded(it) },
                onFailure = { ApiUiState.Failed(it.message ?: "Unknown network error") },
            )
    }

    ExampleChrome(title = ExampleKind.API.title, onBack = onBack) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            text = when (val current = state) {
                ApiUiState.Loading -> "Loading..."
                is ApiUiState.Loaded -> current.output
                is ApiUiState.Failed -> current.message
            },
            color = when (state) {
                is ApiUiState.Failed -> Color(0xFFFF8A80)
                else -> Color.White
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { requestCount += 1 },
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "FETCH",
                textAlign = TextAlign.Center,
            )
        }
    }
}
