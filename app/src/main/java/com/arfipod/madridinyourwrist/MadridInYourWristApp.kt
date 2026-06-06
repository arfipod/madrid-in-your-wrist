package com.arfipod.madridinyourwrist

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.arfipod.madridinyourwrist.transit.EmtBusArrival
import com.arfipod.madridinyourwrist.transit.EmtMadridCredentials
import com.arfipod.madridinyourwrist.transit.MadridGeoPoint
import com.arfipod.madridinyourwrist.transit.MadridLocationProvider
import com.arfipod.madridinyourwrist.transit.MadridNetworkProvider
import com.arfipod.madridinyourwrist.transit.MadridTransitCatalog
import com.arfipod.madridinyourwrist.transit.MadridTransitColors
import com.arfipod.madridinyourwrist.transit.MadridTransitCounts
import com.arfipod.madridinyourwrist.transit.MadridTransitFavorite
import com.arfipod.madridinyourwrist.transit.MadridTransitKind
import com.arfipod.madridinyourwrist.transit.MadridTransitLoadResult
import com.arfipod.madridinyourwrist.transit.MadridTransitOption
import com.arfipod.madridinyourwrist.transit.MadridTransitPlace
import com.arfipod.madridinyourwrist.transit.MadridTransitProximity
import com.arfipod.madridinyourwrist.transit.MadridTransitRefreshDecision
import com.arfipod.madridinyourwrist.transit.MadridTransitRefreshPolicy
import com.arfipod.madridinyourwrist.transit.MadridTransitRefreshSource
import com.arfipod.madridinyourwrist.transit.MadridTransitRuntime
import com.arfipod.madridinyourwrist.transit.MadridTransitSearch
import com.arfipod.madridinyourwrist.transit.MadridTransitSnapshot
import com.arfipod.madridinyourwrist.transit.MadridTransitSnapshotItem
import com.arfipod.madridinyourwrist.transit.MadridTransitSnapshotStore
import com.arfipod.madridinyourwrist.transit.MadridTransitStore
import com.arfipod.madridinyourwrist.transit.MetroDeparture
import com.arfipod.madridinyourwrist.transit.distanceMeters
import com.arfipod.madridinyourwrist.transit.madridTransitBusMinuteLabel
import com.arfipod.madridinyourwrist.transit.madridTransitMinuteLabel
import com.arfipod.madridinyourwrist.transit.madridTransitShortRoute
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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

    data class Cached(
        val updatedAt: String,
        val reason: String,
    ) : TransitUiState

    data class Failed(val message: String) : TransitUiState
}

private enum class MadridTransitRefreshKind {
    AUTOMATIC,
    USER,
}

private data class MadridTransitRefreshTrigger(
    val id: Int = 0,
    val kind: MadridTransitRefreshKind = MadridTransitRefreshKind.AUTOMATIC,
) {
    fun next(kind: MadridTransitRefreshKind): MadridTransitRefreshTrigger = copy(
        id = id + 1,
        kind = kind,
    )
}

private const val LOCATION_RECHECK_MILLIS = 10 * 60 * 1000L
private const val SEARCH_DEBOUNCE_MILLIS = 180L
private val SecondaryTextColor = Color(0xFFC7C7C7)
private val FailureTextColor = Color(0xFFFF8A80)

private sealed interface TransitSearchState {
    data object Blank : TransitSearchState
    data object TooShort : TransitSearchState
    data object Loading : TransitSearchState
    data class Loaded(val results: List<MadridTransitOption>) : TransitSearchState
}

@Composable
fun MadridInYourWristApp(
    onEvent: (String) -> Unit,
    onHaptic: () -> Unit,
) {
    MaterialTheme {
        val context = LocalContext.current
        val store = remember { MadridTransitStore(context.applicationContext) }
        val snapshotStore = remember { MadridTransitSnapshotStore(context.applicationContext) }
        val locationProvider = remember { MadridLocationProvider(context.applicationContext) }
        val networkProvider = remember { MadridNetworkProvider(context.applicationContext) }
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
        var selectedPlace by remember { mutableStateOf(store.loadSelectedPlace()) }
        var favorites by remember { mutableStateOf(store.loadFavorites()) }
        var refreshTrigger by remember { mutableStateOf(MadridTransitRefreshTrigger()) }
        var handledUserRefreshId by remember { mutableIntStateOf(0) }
        var editMode by remember { mutableStateOf(false) }
        var lastSnapshot by remember { mutableStateOf(snapshotStore.loadSnapshot()) }
        var lastKnownLocation by remember { mutableStateOf<MadridGeoPoint?>(null) }
        var lastLocationCheckedAtMillis by remember { mutableStateOf(0L) }
        var uiState by remember { mutableStateOf<TransitUiState>(TransitUiState.Idle) }

        fun requestRefresh(kind: MadridTransitRefreshKind) {
            refreshTrigger = refreshTrigger.next(kind)
        }

        fun persist(nextFavorites: List<MadridTransitFavorite>) {
            favorites = nextFavorites
            store.saveFavorites(nextFavorites)
            requestRefresh(MadridTransitRefreshKind.AUTOMATIC)
            onHaptic()
        }

        fun selectPlace(place: MadridTransitPlace) {
            selectedPlace = place
            store.saveSelectedPlace(place)
            editMode = false
            screen = MadridTransitScreen.HOME
            onHaptic()
        }

        fun addOption(option: MadridTransitOption) {
            if (favorites.any { favorite -> favorite.option.id == option.id && favorite.place == selectedPlace }) return
            persist(
                favorites + MadridTransitFavorite(
                    option = option,
                    count = MadridTransitCounts.DEFAULT,
                    place = selectedPlace,
                )
            )
        }

        fun removeOption(optionId: String) {
            persist(favorites.filterNot { favorite -> favorite.option.id == optionId && favorite.place == selectedPlace })
        }

        fun changeCount(optionId: String, delta: Int) {
            persist(
                favorites.map { favorite ->
                    if (favorite.option.id == optionId && favorite.place == selectedPlace) {
                        favorite.withCount(favorite.clampedCount + delta)
                    } else {
                        favorite
                    }
                }
            )
        }

        fun changeProximityTrigger(optionId: String) {
            persist(
                favorites.map { favorite ->
                    if (favorite.option.id == optionId && favorite.place == selectedPlace) {
                        favorite.withNextProximityTrigger()
                    } else {
                        favorite
                    }
                }
            )
        }

        LaunchedEffect(favorites, selectedPlace, refreshTrigger) {
            val nowEpochMillis = System.currentTimeMillis()
            val forceNetwork = refreshTrigger.kind == MadridTransitRefreshKind.USER &&
                refreshTrigger.id != handledUserRefreshId
            if (forceNetwork) {
                handledUserRefreshId = refreshTrigger.id
            }
            val selectedFavorites = favorites.filter { favorite -> favorite.place == selectedPlace }
            val selectedFavoriteIds = selectedFavorites.map { favorite -> favorite.option.id }.toSet()
            val previousSnapshot = snapshotStore.loadSnapshot() ?: lastSnapshot

            fun applyRefreshDecision(decision: MadridTransitRefreshDecision) {
                lastSnapshot = decision.snapshot
                decision.snapshotToSave?.let { snapshot -> snapshotStore.saveSnapshot(snapshot) }
            }

            if (selectedFavorites.isEmpty()) {
                val updatedAt = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                applyRefreshDecision(
                    MadridTransitRefreshPolicy.clearSelection(
                        previous = previousSnapshot,
                        place = selectedPlace,
                        updatedAt = updatedAt,
                        storedAtEpochMillis = nowEpochMillis,
                    )
                )
                uiState = TransitUiState.Idle
                return@LaunchedEffect
            }

            val needsLocationForProximity = selectedFavorites.any { favorite ->
                favorite.normalizedProximityTriggerMeters != null
            }
            val shouldCheckLocation = forceNetwork ||
                (needsLocationForProximity && nowEpochMillis - lastLocationCheckedAtMillis >= LOCATION_RECHECK_MILLIS)
            if (shouldCheckLocation) {
                lastKnownLocation = locationProvider.lastKnownLocation()
                lastLocationCheckedAtMillis = nowEpochMillis
            }
            val favoritesToRefresh = if (forceNetwork) {
                selectedFavorites
            } else {
                selectedFavorites.filter { favorite ->
                    favorite.isWithinProximityTrigger(favorite.distanceFrom(lastKnownLocation))
                }
            }

            if (favoritesToRefresh.isEmpty()) {
                val decision = MadridTransitRefreshPolicy.distanceSkipped(
                    previous = previousSnapshot,
                    place = selectedPlace,
                    optionIds = selectedFavoriteIds,
                )
                applyRefreshDecision(decision)
                uiState = when (decision.source) {
                    MadridTransitRefreshSource.DISTANCE_SKIPPED_CACHE -> {
                        onEvent("Madrid transit skipped: ${selectedPlace.label} favorites outside proximity radius")
                        TransitUiState.Cached(
                            updatedAt = decision.snapshot?.updatedAt.orEmpty(),
                            reason = "Fuera de radio",
                        )
                    }
                    else -> {
                        onEvent("Madrid transit skipped: no ${selectedPlace.label} favorites inside proximity radius")
                        TransitUiState.Failed("Fuera de radio y sin datos guardados")
                    }
                }
                return@LaunchedEffect
            }

            if (!forceNetwork) {
                val cacheDecision = MadridTransitRefreshPolicy.automaticCache(
                    previous = previousSnapshot,
                    place = selectedPlace,
                    optionIds = selectedFavoriteIds,
                    nowEpochMillis = nowEpochMillis,
                )
                if (cacheDecision != null) {
                    applyRefreshDecision(cacheDecision)
                    uiState = TransitUiState.Cached(
                        updatedAt = cacheDecision.snapshot?.updatedAt.orEmpty(),
                        reason = "Cache reciente",
                    )
                    onEvent("Madrid transit skipped: recent ${selectedPlace.label} cache")
                    return@LaunchedEffect
                }
            }

            if (!networkProvider.hasInternet()) {
                val decision = MadridTransitRefreshPolicy.offline(
                    previous = previousSnapshot,
                    place = selectedPlace,
                    optionIds = selectedFavoriteIds,
                )
                applyRefreshDecision(decision)
                uiState = when (decision.source) {
                    MadridTransitRefreshSource.OFFLINE_CACHE -> {
                        onEvent("Madrid transit offline: using cached ${selectedPlace.label} snapshot")
                        TransitUiState.Cached(
                            updatedAt = decision.snapshot?.updatedAt.orEmpty(),
                            reason = "Sin conexión",
                        )
                    }
                    else -> {
                        onEvent("Madrid transit offline: no cached ${selectedPlace.label} snapshot")
                        TransitUiState.Failed("Sin conexión y sin datos guardados")
                    }
                }
                return@LaunchedEffect
            }

            uiState = TransitUiState.Loading
            uiState = runCatching {
                runtime.load(favoritesToRefresh)
            }.fold(
                onSuccess = { results ->
                    val updatedAt = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                    val decision = MadridTransitRefreshPolicy.onlineSuccess(
                        previous = snapshotStore.loadSnapshot() ?: lastSnapshot,
                        place = selectedPlace,
                        optionIds = selectedFavoriteIds,
                        updatedAt = updatedAt,
                        storedAtEpochMillis = nowEpochMillis,
                        results = results,
                    )
                    applyRefreshDecision(decision)
                    when (decision.source) {
                        MadridTransitRefreshSource.ONLINE_LIVE -> {
                            onEvent(
                                "Madrid transit refreshed: ${results.size}/${selectedFavorites.size} " +
                                    "${selectedPlace.label} favorites"
                            )
                            TransitUiState.Loaded(
                                results = results,
                                updatedAt = updatedAt,
                            )
                        }
                        MadridTransitRefreshSource.ONLINE_CACHE_FALLBACK -> {
                            onEvent("Madrid transit refresh used cached ${selectedPlace.label} snapshot")
                            TransitUiState.Cached(
                                updatedAt = decision.snapshot?.updatedAt.orEmpty(),
                                reason = "Cache por error",
                            )
                        }
                        else -> {
                            onEvent("Madrid transit refresh returned no live data for ${selectedPlace.label}")
                            TransitUiState.Loaded(
                                results = results,
                                updatedAt = updatedAt,
                            )
                        }
                    }
                },
                onFailure = { error ->
                    onEvent("Madrid transit refresh failed: ${error.message}")
                    val decision = MadridTransitRefreshPolicy.onlineFailure(
                        previous = previousSnapshot,
                        place = selectedPlace,
                        optionIds = selectedFavoriteIds,
                    )
                    applyRefreshDecision(decision)
                    if (decision.source == MadridTransitRefreshSource.ONLINE_CACHE_FALLBACK) {
                        TransitUiState.Cached(
                            updatedAt = decision.snapshot?.updatedAt.orEmpty(),
                            reason = "Cache por error",
                        )
                    } else {
                        TransitUiState.Failed(error.message ?: "Unknown transport error")
                    }
                },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(start = 14.dp, top = 18.dp, end = 14.dp, bottom = 8.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (screen) {
                MadridTransitScreen.HOME -> MadridTransitHome(
                    selectedPlace = selectedPlace,
                    favorites = favorites,
                    snapshot = lastSnapshot,
                    lastKnownLocation = lastKnownLocation,
                    uiState = uiState,
                    editMode = editMode,
                    onPlaceSelected = ::selectPlace,
                    onRefresh = {
                        requestRefresh(MadridTransitRefreshKind.USER)
                        onHaptic()
                    },
                    onToggleEdit = {
                        editMode = !editMode
                        onHaptic()
                    },
                    onAddMetro = { screen = MadridTransitScreen.ADD_METRO },
                    onAddBus = { screen = MadridTransitScreen.ADD_BUS },
                    onNearby = { screen = MadridTransitScreen.NEARBY },
                    onMinus = { optionId -> changeCount(optionId, -1) },
                    onPlus = { optionId -> changeCount(optionId, 1) },
                    onToggleProximity = ::changeProximityTrigger,
                    onRemove = ::removeOption,
                )
                MadridTransitScreen.ADD_METRO -> MadridTransitPicker(
                    title = "Metro",
                    place = selectedPlace,
                    kind = MadridTransitKind.METRO,
                    favorites = favorites,
                    onAdd = ::addOption,
                    onBack = { screen = MadridTransitScreen.HOME },
                )
                MadridTransitScreen.ADD_BUS -> MadridTransitPicker(
                    title = "Bus",
                    place = selectedPlace,
                    kind = MadridTransitKind.BUS,
                    favorites = favorites,
                    onAdd = ::addOption,
                    onBack = { screen = MadridTransitScreen.HOME },
                )
                MadridTransitScreen.NEARBY -> MadridNearbyScreen(
                    place = selectedPlace,
                    favorites = favorites,
                    initialLocation = lastKnownLocation,
                    onLocationChanged = { location ->
                        lastKnownLocation = location
                        lastLocationCheckedAtMillis = System.currentTimeMillis()
                    },
                    onAdd = ::addOption,
                    onBack = { screen = MadridTransitScreen.HOME },
                )
            }
        }
    }
}

@Composable
private fun MadridTransitHome(
    selectedPlace: MadridTransitPlace,
    favorites: List<MadridTransitFavorite>,
    snapshot: MadridTransitSnapshot?,
    lastKnownLocation: MadridGeoPoint?,
    uiState: TransitUiState,
    editMode: Boolean,
    onPlaceSelected: (MadridTransitPlace) -> Unit,
    onRefresh: () -> Unit,
    onToggleEdit: () -> Unit,
    onAddMetro: () -> Unit,
    onAddBus: () -> Unit,
    onNearby: () -> Unit,
    onMinus: (String) -> Unit,
    onPlus: (String) -> Unit,
    onToggleProximity: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val visibleFavorites = favorites.filter { favorite -> favorite.place == selectedPlace }
    val visibleFavoriteIds = visibleFavorites.map { favorite -> favorite.option.id }.toSet()
    val loadedResults = (uiState as? TransitUiState.Loaded)?.results.orEmpty()
    val visibleSnapshotItems = snapshot?.items
        .orEmpty()
        .filter { item -> item.place == selectedPlace && item.optionId in visibleFavoriteIds }
    val headline = visibleSnapshotItems.minWithOrNull(
        compareBy<MadridTransitSnapshotItem> { item -> item.rankMinutes }
            .thenBy { item -> item.optionLabel }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .wearRotaryVerticalScroll(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Header(
            title = "Madrid Wrist",
            subtitle = "${selectedPlace.label} · ${uiState.headerStatus()}",
        )
        PlaceSelector(
            selectedPlace = selectedPlace,
            onPlaceSelected = onPlaceSelected,
        )
        NextGlanceBlock(
            selectedPlace = selectedPlace,
            headline = headline,
            isLoading = uiState is TransitUiState.Loading,
            isFailed = uiState is TransitUiState.Failed,
            hasFavorites = visibleFavorites.isNotEmpty(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SmallActionButton(
                modifier = Modifier.weight(1f),
                label = if (uiState is TransitUiState.Loading) "..." else "↻",
                enabled = uiState !is TransitUiState.Loading,
                onClick = onRefresh,
            )
            SmallActionButton(
                modifier = Modifier.weight(1f),
                label = "CERCA",
                onClick = onNearby,
            )
            SmallActionButton(
                modifier = Modifier.weight(1f),
                label = if (editMode) "OK" else "EDIT",
                onClick = onToggleEdit,
            )
        }

        if (visibleFavorites.isEmpty()) {
            EmptyBlock(text = "Sin favoritos en ${selectedPlace.label}")
        } else {
            visibleFavorites.forEach { favorite ->
                val result = loadedResults.firstOrNull { result ->
                    result.favorite.option.id == favorite.option.id && result.favorite.place == selectedPlace
                }
                val snapshotItem = visibleSnapshotItems.firstOrNull { item -> item.optionId == favorite.option.id }
                TransitFavoriteBlock(
                    favorite = favorite,
                    result = result,
                    snapshotItem = snapshotItem,
                    distanceMeters = favorite.distanceFrom(lastKnownLocation),
                    isLoading = uiState is TransitUiState.Loading,
                    editMode = editMode,
                    onMinus = onMinus,
                    onPlus = onPlus,
                    onToggleProximity = onToggleProximity,
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
private fun PlaceSelector(
    selectedPlace: MadridTransitPlace,
    onPlaceSelected: (MadridTransitPlace) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        MadridTransitPlace.selectable.forEach { place ->
            SmallActionButton(
                modifier = Modifier.weight(1f),
                label = place.selectorLabel(isSelected = place == selectedPlace),
                onClick = { onPlaceSelected(place) },
            )
        }
    }
}

@Composable
private fun NextGlanceBlock(
    selectedPlace: MadridTransitPlace,
    headline: MadridTransitSnapshotItem?,
    isLoading: Boolean,
    isFailed: Boolean,
    hasFavorites: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF101820))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Próximo · ${selectedPlace.label}",
            color = Color(0xFFC7C7C7),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
        )
        when {
            isLoading -> {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Actualizando",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 24.sp,
                    ),
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Leyendo Metro y EMT",
                    color = Color(0xFFC7C7C7),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
                )
            }
            isFailed -> {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Sin datos",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 24.sp,
                    ),
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Revisa conexión y toca ↻",
                    color = Color(0xFFC7C7C7),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
                )
            }
            headline != null -> {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = headline.timeLabel,
                    color = headline.accentColor(),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        lineHeight = 32.sp,
                    ),
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "${headline.routeLabel} · ${headline.optionLabel}",
                    color = headline.accentColor(),
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
                    text = "→ ${headline.destination}",
                    color = headline.accentColor().copy(alpha = 0.78f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
                )
            }
            hasFavorites -> {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Sin datos",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 24.sp,
                    ),
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Toca ↻ para actualizar",
                    color = Color(0xFFC7C7C7),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
                )
            }
            else -> {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Añade favoritos",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 24.sp,
                    ),
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "+ Metro o + Bus para ${selectedPlace.label}",
                    color = Color(0xFFC7C7C7),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
                )
            }
        }
    }
}

@Composable
private fun rememberTransitSearchState(
    query: String,
    kind: MadridTransitKind?,
    limit: Int,
): TransitSearchState {
    var state by remember { mutableStateOf<TransitSearchState>(TransitSearchState.Blank) }
    LaunchedEffect(query, kind, limit) {
        when {
            query.isBlank() -> {
                state = TransitSearchState.Blank
                return@LaunchedEffect
            }
            !MadridTransitSearch.isQueryReady(query) -> {
                state = TransitSearchState.TooShort
                return@LaunchedEffect
            }
        }

        state = TransitSearchState.Loading
        delay(SEARCH_DEBOUNCE_MILLIS)
        val results = withContext(Dispatchers.Default) {
            MadridTransitCatalog.searchOptions(
                query = query,
                kind = kind,
                limit = limit,
            )
        }
        state = TransitSearchState.Loaded(results)
    }
    return state
}

@Composable
private fun MadridTransitPicker(
    title: String,
    place: MadridTransitPlace,
    kind: MadridTransitKind,
    favorites: List<MadridTransitFavorite>,
    onAdd: (MadridTransitOption) -> Unit,
    onBack: () -> Unit,
) {
    val selectedIds = remember(favorites, place) {
        favorites
            .filter { favorite -> favorite.place == place }
            .map { favorite -> favorite.option.id }
            .toSet()
    }
    var query by remember { mutableStateOf("") }
    val searchState = rememberTransitSearchState(
        query = query,
        kind = kind,
        limit = 8,
    )
    val visibleOptions = (searchState as? TransitSearchState.Loaded)?.results.orEmpty()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .wearRotaryVerticalScroll(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Header(title = title, subtitle = "Añadir a ${place.label}")
        SearchField(
            query = query,
            onQueryChange = { nextQuery -> query = nextQuery },
        )
        if (visibleOptions.isEmpty()) {
            EmptyBlock(
                text = when (searchState) {
                    TransitSearchState.Blank -> "Escribe para buscar"
                    TransitSearchState.TooShort -> "Mínimo 2 letras o un número"
                    TransitSearchState.Loading -> "Buscando..."
                    is TransitSearchState.Loaded -> "Sin resultados"
                }
            )
        } else {
            visibleOptions.forEach { option ->
                OptionBlock(
                    option = option,
                    distanceMeters = null,
                    isSelected = option.id in selectedIds,
                    onAdd = { onAdd(option) },
                )
            }
        }
        SmallActionButton(
            modifier = Modifier.fillMaxWidth(),
            label = "VOLVER",
            onClick = onBack,
        )
    }
}

@Composable
private fun MadridNearbyScreen(
    place: MadridTransitPlace,
    favorites: List<MadridTransitFavorite>,
    initialLocation: MadridGeoPoint?,
    onLocationChanged: (MadridGeoPoint?) -> Unit,
    onAdd: (MadridTransitOption) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val locationProvider = remember { MadridLocationProvider(context.applicationContext) }
    var location by remember { mutableStateOf(initialLocation ?: locationProvider.lastKnownLocation()) }
    var status by remember {
        mutableStateOf(if (location == null) "Sin ubicación" else "Paradas cercanas")
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions.values.any { granted -> granted }
        location = if (granted) locationProvider.lastKnownLocation() else null
        onLocationChanged(location)
        status = if (location == null) "Sin ubicación" else "Paradas cercanas"
    }
    val selectedIds = remember(favorites, place) {
        favorites
            .filter { favorite -> favorite.place == place }
            .map { favorite -> favorite.option.id }
            .toSet()
    }
    var query by remember { mutableStateOf("") }
    var nearby by remember { mutableStateOf<List<Pair<MadridTransitOption, Int>>>(emptyList()) }
    var isLoadingNearby by remember { mutableStateOf(false) }
    LaunchedEffect(location) {
        val point = location
        if (point == null) {
            nearby = emptyList()
            isLoadingNearby = false
            return@LaunchedEffect
        }
        isLoadingNearby = true
        nearby = withContext(Dispatchers.Default) {
            MadridTransitCatalog.nearbyOptions(from = point, limit = 6)
        }
        isLoadingNearby = false
    }
    val searchState = rememberTransitSearchState(
        query = query,
        kind = null,
        limit = 8,
    )
    val visibleOptions = if (query.isBlank()) {
        nearby
    } else {
        (searchState as? TransitSearchState.Loaded)?.results.orEmpty().map { option ->
            val meters = location?.let { point ->
                option.location?.let { optionLocation -> distanceMeters(from = point, to = optionLocation) }
            }
            option to meters
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .wearRotaryVerticalScroll(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Header(title = "Cerca", subtitle = "$status · Añadir a ${place.label}")
        SmallActionButton(
            modifier = Modifier.fillMaxWidth(),
            label = "UBICAR",
            onClick = {
                if (locationProvider.hasLocationPermission()) {
                    location = locationProvider.lastKnownLocation()
                    onLocationChanged(location)
                    status = if (location == null) "Sin ubicación" else "Paradas cercanas"
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
        SearchField(
            query = query,
            onQueryChange = { nextQuery -> query = nextQuery },
        )

        if (visibleOptions.isEmpty()) {
            EmptyBlock(
                text = if (query.isBlank()) {
                    if (isLoadingNearby) "Calculando cercanas..." else "Ubicación no disponible"
                } else {
                    when (searchState) {
                        TransitSearchState.Blank -> "Escribe para buscar"
                        TransitSearchState.TooShort -> "Mínimo 2 letras o un número"
                        TransitSearchState.Loading -> "Buscando..."
                        is TransitSearchState.Loaded -> "Sin resultados"
                    }
                }
            )
        } else {
            visibleOptions.forEach { (option, meters) ->
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
            label = "VOLVER",
            onClick = onBack,
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    BasicTextField(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF171717))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        value = query,
        onValueChange = { value -> onQueryChange(value.replace('\n', ' ').take(48)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { keyboardController?.hide() },
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = Color.White,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            lineHeight = 13.sp,
        ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (query.isBlank()) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Buscar",
                        color = Color(0xFF8E8E8E),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 12.sp),
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun TransitFavoriteBlock(
    favorite: MadridTransitFavorite,
    result: MadridTransitLoadResult?,
    snapshotItem: MadridTransitSnapshotItem?,
    distanceMeters: Int?,
    isLoading: Boolean,
    editMode: Boolean,
    onMinus: (String) -> Unit,
    onPlus: (String) -> Unit,
    onToggleProximity: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val accent = favorite.accentColor()
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
                text = snapshotItem?.timeLabel ?: if (isLoading) "..." else "--",
                color = Color.White,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 13.sp,
                ),
            )
        }
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = listOfNotNull(
                favorite.option.detail,
                distanceMeters?.let { meters -> formatDistance(meters) },
            ).joinToString(" | "),
            color = Color(0xFFC7C7C7),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
        )
        favorite.proximityStatusLine(distanceMeters)?.let { statusLine ->
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = statusLine,
                color = if (favorite.isWithinProximityTrigger(distanceMeters)) {
                    Color(0xFF9CCC65)
                } else {
                    Color(0xFFC7C7C7)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
            )
        }
        ResultLines(
            lines = if (isLoading) {
                listOf(TransitDisplayLine("Actualizando...", accent.copy(alpha = 0.78f)))
            } else {
                result.displayLines(fallback = snapshotItem)
            },
        )
        if (editMode) {
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
                    label = "${favorite.clampedCount}x",
                    enabled = false,
                    onClick = {},
                )
                SmallActionButton(
                    modifier = Modifier.weight(1f),
                    label = "+",
                    enabled = favorite.clampedCount < MadridTransitCounts.MAX,
                    onClick = { onPlus(favorite.option.id) },
                )
                SmallActionButton(
                    modifier = Modifier.weight(1f),
                    label = favorite.proximityButtonLabel(),
                    onClick = { onToggleProximity(favorite.option.id) },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                SmallActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    label = "DEL",
                    onClick = { onRemove(favorite.option.id) },
                )
            }
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
        option.source.label.takeIf { !option.source.hasLiveArrivals },
        distanceMeters?.let { meters -> formatDistance(meters) },
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
            color = option.accentColor(),
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
            label = if (isSelected) "AÑADIDO" else "AÑADIR",
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

private data class TransitDisplayLine(
    val text: String,
    val color: Color,
)

@Composable
private fun ResultLines(lines: List<TransitDisplayLine>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        lines.forEach { line ->
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = line.text,
                color = line.color,
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
    TransitUiState.Idle -> "Listo"
    TransitUiState.Loading -> "Actualizando"
    is TransitUiState.Loaded -> "Actualizado $updatedAt"
    is TransitUiState.Cached -> "$reason · $updatedAt"
    is TransitUiState.Failed -> "Error al actualizar"
}

private fun MadridTransitLoadResult?.displayLines(
    fallback: MadridTransitSnapshotItem?,
): List<TransitDisplayLine> = when (this) {
    null -> fallback?.let { item ->
        listOf(TransitDisplayLine("${item.routeLabel} hacia ${item.destination}", item.accentColor()))
    } ?: listOf(TransitDisplayLine("Esperando", SecondaryTextColor))
    is MadridTransitLoadResult.Metro -> {
        val prefix = if (result.isStale) {
            TransitDisplayLine(
                text = result.validUntil?.let { validUntil -> "GTFS $validUntil" } ?: "GTFS caducado",
                color = SecondaryTextColor,
            )
        } else {
            null
        }
        listOfNotNull(prefix) + result.departures
            .take(favorite.clampedCount)
            .map { departure -> departure.metroLine() }
            .ifEmpty { listOf(TransitDisplayLine("Sin trenes", favorite.accentColor().copy(alpha = 0.78f))) }
    }
    is MadridTransitLoadResult.Bus -> arrivals
        .take(favorite.clampedCount)
        .map { arrival -> arrival.busLine(color = favorite.accentColor()) }
        .ifEmpty { listOf(TransitDisplayLine("Sin buses", favorite.accentColor().copy(alpha = 0.78f))) }
    is MadridTransitLoadResult.MissingConfig -> fallback?.cachedLines(message)
        ?: listOf(TransitDisplayLine(message, favorite.accentColor().copy(alpha = 0.78f)))
    is MadridTransitLoadResult.Failed -> fallback?.cachedLines(message)
        ?: listOf(TransitDisplayLine(message, FailureTextColor))
}

private fun MadridTransitSnapshotItem.cachedLines(reason: String): List<TransitDisplayLine> {
    return listOf(
        TransitDisplayLine("${routeLabel} hacia $destination", accentColor()),
        TransitDisplayLine("Cache · $reason", SecondaryTextColor),
    )
}

private fun MetroDeparture.metroLine(): TransitDisplayLine {
    val routeLabel = routeName.madridTransitShortRoute()
    return TransitDisplayLine(
        text = "${minutesUntil.madridTransitMinuteLabel()} $routeLabel → $destination",
        color = Color(MadridTransitColors.textArgbForMetroLine(routeLabel)),
    )
}

private fun EmtBusArrival.busLine(color: Color): TransitDisplayLine {
    return TransitDisplayLine(
        text = "${secondsUntil.madridTransitBusMinuteLabel()} $lineId → $destination",
        color = color,
    )
}

private fun MadridTransitSnapshotItem.accentColor(): Color =
    Color(MadridTransitColors.textArgbForSnapshotItem(this))

private fun MadridTransitFavorite.accentColor(): Color = option.accentColor()

private fun MadridTransitOption.accentColor(): Color =
    Color(MadridTransitColors.textArgbForOption(this))

private fun MadridTransitPlace.selectorLabel(isSelected: Boolean): String {
    return if (isSelected) shortLabel.uppercase(Locale.ROOT) else shortLabel
}

private fun MadridTransitFavorite.distanceFrom(location: MadridGeoPoint?): Int? {
    val optionLocation = option.location ?: return null
    return location?.let { point -> distanceMeters(from = point, to = optionLocation) }
}

private fun MadridTransitFavorite.proximityButtonLabel(): String {
    return when (normalizedProximityTriggerMeters) {
        null -> "AUTO"
        500 -> "500m"
        1000 -> "1km"
        2000 -> "2km"
        else -> "AUTO"
    }
}

private fun MadridTransitFavorite.proximityStatusLine(distanceMeters: Int?): String? {
    val triggerMeters = normalizedProximityTriggerMeters ?: return null
    val triggerLabel = MadridTransitProximity.label(triggerMeters)
    return when {
        distanceMeters == null -> "$triggerLabel · esperando ubicación"
        distanceMeters <= triggerMeters -> "$triggerLabel · activo a ${formatDistance(distanceMeters)}"
        else -> "$triggerLabel · a ${formatDistance(distanceMeters)}"
    }
}

private fun formatDistance(meters: Int): String {
    return if (meters < 1000) {
        "${meters}m"
    } else {
        "${meters / 1000}.${(meters % 1000) / 100}km"
    }
}
