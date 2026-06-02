package com.arfipod.wearosplayground

import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class WearLoopTileService : TileService() {
    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<Tile> {
        val layout = materialScope(this, requestParams.deviceConfiguration) {
            primaryLayout(
                titleSlot = {
                    text(
                        text = WearLoopTileContent.title().layoutString,
                        typography = Typography.TITLE_MEDIUM,
                    )
                },
                mainSlot = {
                    text(
                        text = WearLoopTileContent.body(BuildConfig.BUILD_TIMESTAMP).layoutString,
                        typography = Typography.BODY_MEDIUM,
                        maxLines = 2,
                    )
                },
                bottomSlot = {
                    text(
                        text = WearLoopTileContent.footer().layoutString,
                        typography = Typography.BODY_SMALL,
                        maxLines = 2,
                    )
                },
            )
        }

        return Futures.immediateFuture(
            Tile.Builder()
                .setResourcesVersion(WearLoopTileContent.RESOURCES_VERSION)
                .setFreshnessIntervalMillis(WearLoopTileContent.FRESHNESS_INTERVAL_MILLIS)
                .setTileTimeline(Timeline.fromLayoutElement(layout))
                .build()
        )
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<Resources> =
        Futures.immediateFuture(
            Resources.Builder()
                .setVersion(WearLoopTileContent.RESOURCES_VERSION)
                .build()
        )
}
