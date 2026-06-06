package com.arfipod.madridinyourwrist

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.arfipod.madridinyourwrist.transit.MadridTransitKind
import com.arfipod.madridinyourwrist.transit.MadridTransitPlace
import com.arfipod.madridinyourwrist.transit.MadridTransitSnapshot
import com.arfipod.madridinyourwrist.transit.MadridTransitSnapshotItem
import com.arfipod.madridinyourwrist.transit.MadridTransitSnapshotStore
import com.arfipod.madridinyourwrist.transit.MadridTransitStore

class WearLoopComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData =
        if (request.complicationType == ComplicationType.SHORT_TEXT) {
            val selectedPlace = MadridTransitStore(applicationContext).loadSelectedPlace()
            shortTextComplicationData(
                snapshot = MadridTransitSnapshotStore(applicationContext)
                    .loadSnapshot()
                    ?.forPlace(selectedPlace),
                selectedPlace = selectedPlace,
                buildTimestamp = BuildConfig.BUILD_TIMESTAMP,
            )
        } else {
            NoDataComplicationData()
        }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        if (type == ComplicationType.SHORT_TEXT) {
            shortTextComplicationData(
                snapshot = MadridTransitSnapshot(
                    updatedAt = "08:15",
                    items = listOf(
                        MadridTransitSnapshotItem(
                            optionId = "preview",
                            kind = MadridTransitKind.BUS,
                            place = MadridTransitPlace.PROFILE_1,
                            optionLabel = "Daroca E3",
                            detail = "Valderrivas",
                            routeLabel = "E3",
                            destination = "VALDERRIVAS",
                            timeLabel = "4m",
                            rankMinutes = 4,
                        )
                    ),
                ),
                selectedPlace = MadridTransitPlace.PROFILE_1,
                buildTimestamp = "2026-06-02T00:00:00Z",
            )
        } else {
            null
        }

    private fun shortTextComplicationData(
        snapshot: MadridTransitSnapshot?,
        selectedPlace: MadridTransitPlace?,
        buildTimestamp: String,
    ): ShortTextComplicationData =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(WearLoopComplicationContent.text(snapshot)).build(),
            contentDescription = PlainComplicationText.Builder(
                WearLoopComplicationContent.contentDescription(snapshot, buildTimestamp)
            ).build(),
        )
            .setTitle(
                PlainComplicationText.Builder(WearLoopComplicationContent.title(snapshot, selectedPlace)).build()
            )
            .build()
}
