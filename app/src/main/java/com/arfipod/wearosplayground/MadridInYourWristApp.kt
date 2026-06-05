package com.arfipod.wearosplayground

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.arfipod.wearosplayground.examples.EmtBusArrival
import com.arfipod.wearosplayground.examples.EmtMadridCredentials
import com.arfipod.wearosplayground.examples.MetroDeparture
import com.arfipod.wearosplayground.transit.MadridGeoPoint
import com.arfipod.wearosplayground.transit.MadridLocationProvider
import com.arfipod.wearosplayground.transit.MadridTransitCatalog
import com.arfipod.wearosplayground.transit.MadridTransitCounts
import com.arfipod.wearosplayground.transit.MadridTransitFavorite
import com.arfipod.wearosplayground.transit.MadridTransitKind
import com.arfipod.wearosplayground.transit.MadridTransitLoadResult
import com.arfipod.wearosplayground.transit.MadridTransitOption
import com.arfipod.wearosplayground.transit.MadridTransitRuntime
import com.arfipod.wearosplayground.transit.MadridTransitStore
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private enum class MadridTransitScreen {
    HOME,
    ADD_METRO,
    ADD_BUS,
    NEARBY,
}

private sealed interface TransitUiState {
    data object Idle : TransitUiState
    data object Loading : TransitUiState
    data class Loaded(
        val results: List<MadridTransitLoadResult>,
        val updatedAt: String,
    ) : TransitUiState

    data class Failed(val message: String) : TransitUiState
}

@Composable
fun MadridInYourWristApp(
    onEvent: (String) -> Unit,
    onHaptic: () -> Unit,
) {
    MaterialTheme {
        val context = LocalContext.current
        val store = remember { MadridTransitStore(context.applicationContext) }
        val runtime = remember {
            MadridTransitRuntime(
                napApiKey = BuildConfig.NAP_API_KEY,
                emtCredentials = EmtMadridCredentials(
                    clientId = BuildConfig.EMT_CLIENT_ID,
                    passKey = BuildConfig.EMT_PASS_KEY,
                    email = BuildConfig.EMT_EMAIL,
                    password = BuildConfig.EMT_PASSWORD,
                ),
            )
        }
        var screen by remember { mutableStateOf(MadridTransitScreen.HOME) }
        var favorites by remember { mutableStateOf(store.loadFavorites()) }
        var refreshCount by remember { mutableIntStateOf(0) }
        var uiState by remember { mutableStateOf<TransitUiState>(TransitUiState.Idle) }

        fun persist(nextFavorites: List<MadridTransitFavorite>) {
            favorites = nextFavorites
            store.saveFavorites(nextFavorites)
            refreshCount += 1
            onHaptic()
        }

        fun addOption(option: MadridTransitOption) {
            if (favorites.any { it.option.id == option.id }) return
            persist(
                favorites + MadridTransitFavorite(
                    option = option,
                    count = MadridTransitCounts.DEFAULT,
                )
            )
        }

        fun removeOption(optionId: String) {
            persist(favorites.filterNot { it.option.id == optionId })
        }

        fun changeCount(optionId: String, delta: Int) {
            persist(
                favorites.map { favorite ->
                    if (favorite.option.id == optionId) {
                        favorite.withCount(favorite.clampedCount + delta)
                    } else {
                        favorite
                    }
                }
            )
        }

        LaunchedEffect(favorites, refreshCount) {
            if (favorites.isEmpty()) {
                uiState = TransitUiState.Idle
                return@LaunchedEffect
            }

            uiState = TransitUiState.Loading
            uiState = runCatching {
                runtime.load(favorites)
            }.fold(
                onSuccess = { results ->
                    onEvent("Madrid transit refreshed: ${results.size} favorites")
                    TransitUiState.Loaded(
                        results = results,
                        updatedAt = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                    )
                },
                onFailure = { error ->
                    onEvent("Madrid transit refresh failed: ${error.message}")
                    TransitUiState.Failed(error.message ?: "Unknown transport error")
                },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (screen) {
                MadridTransitScreen.HOME -> MadridTransitHome(
                    favorites = favorites,
                    uiState = uiState,
                    onRefresh = {
                        refreshCount += 1
                        onHaptic()
                    },
                    onAddMetro = { screen = MadridTransitScreen.ADD_METRO },
                    onAddBus = { screen = MadridTransitScreen.ADD_BUS },
                    onNearby = { screen = MadridTransitScreen.NEARBY },
                    onMinus = { optionId -> changeCount(optionId, -1) },
                    onPlus = { optionId -> changeCount(optionId, 1) },
                    onRemove = ::removeOption,
                )
                MadridTransitScreen.ADD_METRO -> MadridTransitPicker(
                    title = "Add Metro",
                    options = MadridTransitCatalog.metroOptions,
                    favorites = favorites,
                    onAdd = ::addOption,
                    onBack = { screen = MadridTransitScreen.HOME },
                )
                MadridTransitScreen.ADD_BUS -> MadridTransitPicker(
                    title = "Add Bus",
                    options = MadridTransitCatalog.busOptions,
                    favorites = favorites,
                    onAdd = ::addOption,
                    onBack = { screen = MadridTransitScreen.HOME },
                )
                MadridTransitScreen.NEARBY -> MadridNearbyScreen(
                    favorites = favorites,
                    onAdd = ::addOption,
                    onBack = { screen = MadridTransitScreen.HOME },
                )
            }
        }
    }
}

@Composable
private fun MadridTransitHome(
    favorites: List<MadridTransitFavorite>,
    uiState: TransitUiState,
    onRefresh: () -> Unit,
    onAddMetro: () -> Unit,
    onAddBus: () -> Unit,
    onNearby: () -> Unit,
    onMinus: (String) -> Unit,
    onPlus: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .wearRotaryVerticalScroll(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Header(title = "Madrid Wrist", subtitle = uiState.headerStatus())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SmallActionButton(
                modifier = Modifier.weight(1f),
                label = "REFRESH",
                enabled = uiState !is TransitUiState.Loading,
                onClick = onRefresh,
            )
            SmallActionButton(
                modifier = Modifier.weight(1f),
                label = "NEARBY",
                onClick = onNearby,
            )
        }

        if (favorites.isEmpty()) {
            EmptyBlock(text = "No favorites")
        } else {
            val loadedResults = (uiState as? TransitUiState.Loaded)?.results.orEmpty()
            favorites.forEach { favorite ->
                val result = loadedResults.firstOrNull { it.favorite.option.id == favorite.option.id }
                TransitFavoriteBlock(
                    favorite = favorite,
                    result = result,
                    isLoading = uiState is TransitUiState.Loading,
                    onMinus = onMinus,
                    onPlus = onPlus,
                    onRemove = onRemove,
                )
            }
        }

        if (uiState is TransitUiState.Failed) {
            EmptyBlock(text = uiState.message)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SmallActionButton(
                modifier = Modifier.weight(1f),
                label = "+ METRO",
                onClick = onAddMetro,
            )
            SmallActionButton(
                modifier = Modifier.weight(1f),
                label = "+ BUS",
                onClick = onAddBus,
            )
        }
    }
}

@Composable
private fun MadridTransitPicker(
    title: String,
    options: List<MadridTransitOption>,
    favorites: List<MadridTransitFavorite>,
    onAdd: (MadridTransitOption) -> Unit,
    onBack: () -> Unit,
) {
    val selectedIds = favorites.map { it.option.id }.toSet()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .wearRotaryVerticalScroll(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Header(title = title, subtitle = "${options.size} saved choices")
        options.forEach { option ->
            OptionBlock(
                option = option,
                distanceMeters = null,
                isSelected = option.id in selectedIds,
                onAdd = { onAdd(option) },
            )
        }
        SmallActionButton(
            modifier = Modifier.fillMaxWidth(),
            label = "BACK",
            onClick = onBack,
        )
    }
}

@Composable
private fun MadridNearbyScreen(
    favorites: List<MadridTransitFavorite>,
    onAdd: (MadridTransitOption) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val locationProvider = remember { MadridLocationProvider(context.applicationContext) }
    var location by remember { mutableStateOf<MadridGeoPoint?>(locationProvider.lastKnownLocation()) }
    var status by remember {
        mutableStateOf(if (location == null) "No location yet" else "Closest choices")
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions.values.any { it }
        location = if (granted) locationProvider.lastKnownLocation() else null
        status = if (location == null) "No location yet" else "Closest choices"
    }
    val selectedIds = favorites.map { it.option.id }.toSet()
    val nearby = location
        ?.let { MadridTransitCatalog.nearbyOptions(from = it, limit = 6) }
        .orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .wearRotaryVerticalScroll(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Header(title = "Nearby", subtitle = status)
        SmallActionButton(
            modifier = Modifier.fillMaxWidth(),
            label = "LOCATE",
            onClick = {
                if (locationProvider.hasLocationPermission()) {
                    location = locationProvider.lastKnownLocation()
                    status = if (location == null) "No location yet" else "Closest choices"
                } else {
                    launcher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                }
            },
        )

        if (nearby.isEmpty()) {
            EmptyBlock(text = "Location unavailable")
        } else {
            nearby.forEach { (option, meters) ->
                OptionBlock(
                    option = option,
                    distanceMeters = meters,
                    isSelected = option.id in selectedIds,
                    onAdd = { onAdd(option) },
                )
            }
        }
        SmallActionButton(
            modifier = Modifier.fillMaxWidth(),
            label = "BACK",
            onClick = onBack,
        )
    }
}

@Composable
private fun TransitFavoriteBlock(
    favorite: MadridTransitFavorite,
    result: MadridTransitLoadResult?,
    isLoading: Boolean,
    onMinus: (String) -> Unit,
    onPlus: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val accent = favorite.option.kind.accentColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF171717))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = favorite.option.label,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 13.sp,
                ),
            )
            Text(
                text = "${favorite.clampedCount}x",
                color = Color.White,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            )
        }
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = favorite.option.detail,
            color = Color(0xFFC7C7C7),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
        )
        ResultLines(
            lines = if (isLoading) listOf("Loading...") else result.displayLines(),
            color = if (result is MadridTransitLoadResult.MissingConfig ||
                result is MadridTransitLoadResult.Failed
            ) {
                Color(0xFFFF8A80)
            } else {
                Color.White
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            SmallActionButton(
                modifier = Modifier.weight(1f),
                label = "-",
                enabled = favorite.clampedCount > MadridTransitCounts.MIN,
                onClick = { onMinus(favorite.option.id) },
            )
            SmallActionButton(
                modifier = Modifier.weight(1f),
                label = "+",
                enabled = favorite.clampedCount < MadridTransitCounts.MAX,
                onClick = { onPlus(favorite.option.id) },
            )
            SmallActionButton(
                modifier = Modifier.weight(1f),
                label = "DEL",
                onClick = { onRemove(favorite.option.id) },
            )
        }
    }
}

@Composable
private fun OptionBlock(
    option: MadridTransitOption,
    distanceMeters: Int?,
    isSelected: Boolean,
    onAdd: () -> Unit,
) {
    val detail = listOfNotNull(
        option.detail,
        distanceMeters?.let { formatDistance(it) },
    ).joinToString(" | ")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF171717))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = option.label,
            color = option.kind.accentColor(),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 13.sp,
            ),
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = detail,
            color = Color(0xFFC7C7C7),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
        )
        SmallActionButton(
            modifier = Modifier.fillMaxWidth(),
            label = if (isSelected) "ADDED" else "ADD",
            enabled = !isSelected,
            onClick = onAdd,
        )
    }
}

@Composable
private fun Header(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 15.sp,
                lineHeight = 16.sp,
            ),
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = subtitle,
            color = Color(0xFFC7C7C7),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
        )
    }
}

@Composable
private fun ResultLines(lines: List<String>, color: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        lines.forEach { line ->
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = line,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
            )
        }
    }
}

@Composable
private fun EmptyBlock(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF171717))
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = text,
            color = Color(0xFFC7C7C7),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
        )
    }
}

@Composable
private fun SmallActionButton(
    modifier: Modifier = Modifier,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = label,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun Modifier.wearRotaryVerticalScroll(): Modifier {
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val rotaryBehavior = RotaryScrollableDefaults.behavior(
        scrollableState = scrollState,
        hapticFeedbackEnabled = true,
    )

    return requestFocusOnHierarchyActive()
        .rotaryScrollable(
            behavior = rotaryBehavior,
            focusRequester = focusRequester,
        )
        .verticalScroll(scrollState)
}

private fun TransitUiState.headerStatus(): String = when (this) {
    TransitUiState.Idle -> "Ready"
    TransitUiState.Loading -> "Updating"
    is TransitUiState.Loaded -> "Updated $updatedAt"
    is TransitUiState.Failed -> "Refresh failed"
}

private fun MadridTransitLoadResult?.displayLines(): List<String> = when (this) {
    null -> listOf("Waiting")
    is MadridTransitLoadResult.Metro -> {
        val prefix = if (result.isStale) {
            result.validUntil?.let { "GTFS $it" } ?: "GTFS expired"
        } else {
            null
        }
        listOfNotNull(prefix) + result.departures
            .take(favorite.clampedCount)
            .map { it.metroLine() }
            .ifEmpty { listOf("No trains") }
    }
    is MadridTransitLoadResult.Bus -> arrivals
        .take(favorite.clampedCount)
        .map { it.busLine() }
        .ifEmpty { listOf("No buses") }
    is MadridTransitLoadResult.MissingConfig -> listOf(message)
    is MadridTransitLoadResult.Failed -> listOf(message)
}

private fun MetroDeparture.metroLine(): String {
    return "${minutesUntil.timeLabel()} ${routeName.shortRoute()} -> $destination"
}

private fun EmtBusArrival.busLine(): String {
    return "${secondsUntil.busTimeLabel()} $lineId -> $destination"
}

private fun Long.timeLabel(): String = if (this <= 0L) "Now" else "${this}m"

private fun Int.busTimeLabel(): String = when {
    this == 0 -> "Now"
    this >= 999_999 -> "+20m"
    else -> "${(this + 59) / 60}m"
}

private fun String.shortRoute(): String = substringBefore(" ")
    .let { if (it.startsWith("L")) it else "L$it" }

private fun MadridTransitKind.accentColor(): Color = when (this) {
    MadridTransitKind.METRO -> Color(0xFF82B1FF)
    MadridTransitKind.BUS -> Color(0xFF9CCC65)
}

private fun formatDistance(meters: Int): String {
    return if (meters < 1000) {
        "${meters}m"
    } else {
        "${meters / 1000}.${(meters % 1000) / 100}km"
    }
}
