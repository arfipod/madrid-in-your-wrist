package com.arfipod.wearosplayground

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

class WearLoopComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData =
        if (request.complicationType == ComplicationType.SHORT_TEXT) {
            shortTextComplicationData(BuildConfig.BUILD_TIMESTAMP)
        } else {
            NoDataComplicationData()
        }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        if (type == ComplicationType.SHORT_TEXT) {
            shortTextComplicationData("2026-06-02T00:00:00Z")
        } else {
            null
        }

    private fun shortTextComplicationData(buildTimestamp: String): ShortTextComplicationData =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(WearLoopComplicationContent.text()).build(),
            contentDescription = PlainComplicationText.Builder(
                WearLoopComplicationContent.contentDescription(buildTimestamp)
            ).build(),
        )
            .setTitle(
                PlainComplicationText.Builder(WearLoopComplicationContent.title()).build()
            )
            .build()
}
