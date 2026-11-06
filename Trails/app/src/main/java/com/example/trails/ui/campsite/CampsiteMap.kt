// Map (osmdroid with MapTiler tiles) and the full-screen pin picker for campsites (US 02.01.02).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Reusable: the entrant-side map view can call CampsiteMap with interactive = false.
// Outstanding issues: tiles need internet and MAPTILER_KEY in local.properties; without either the map is a blank grid.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.trails.R
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File

/**
 * Identifies the app to the tile servers.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
private const val USER_AGENT =
    "QuadleTrails/0.1 (CMPUT 301 student project; +https://github.com/cmput301f26-quadle/quadle-trails)"

/**
 * URL of one MapTiler map tile, e.g. ".../streets-v2-dark/256/6/11/21.png?key=KEY".
 * Plain function so it can be unit-tested.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param key the MapTiler API key.
 * @param zoom zoom level (0 = whole world).
 * @param x tile column at that zoom.
 * @param y tile row at that zoom.
 */
fun mapTilerTileUrl(key: String, zoom: Int, x: Int, y: Int): String =
    "https://api.maptiler.com/maps/streets-v2-dark/256/$zoom/$x/$y.png?key=$key"

/**
 * MapTiler's dark street map (OpenStreetMap data). openstreetmap.org's own tile servers
 * block this app, so tiles come from MapTiler's free plan, which needs an API key.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param key the MapTiler key from local.properties (R.string.maptiler_key).
 */
private class MapTilerTileSource(private val key: String) : OnlineTileSourceBase(
    "MapTilerDark", 0, 20, 256, ".png",
    arrayOf("https://api.maptiler.com/maps/streets-v2-dark/256/"),
    "© MapTiler © OpenStreetMap contributors"
) {
    override fun getTileURLString(pMapTileIndex: Long): String = mapTilerTileUrl(
        key,
        MapTileIndex.getZoom(pMapTileIndex),
        MapTileIndex.getX(pMapTileIndex),
        MapTileIndex.getY(pMapTileIndex)
    )
}

/**
 * Where the map starts when there is no pin yet: central Alberta.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
private val ALBERTA = GeoPoint(53.5, -116.0)

/**
 * One-time osmdroid setup: identify the app to the tile server and keep the tile cache
 * in app storage so no storage permission is needed.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
private fun configureOsmdroid(context: Context) {
    val config = Configuration.getInstance()
    config.load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    config.userAgentValue = USER_AGENT
    val base = File(context.cacheDir, "osmdroid")
    config.osmdroidBasePath = base
    config.osmdroidTileCache = File(base, "tiles")
}

/**
 * An OpenStreetMap map showing an optional pin.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param latitude pin latitude, or null for no pin.
 * @param longitude pin longitude, or null for no pin.
 * @param interactive false for a still preview that doesn't pan, zoom or steal page scrolling.
 * @param onTap called with the tapped latitude and longitude; null means taps do nothing.
 */
@Composable
fun CampsiteMap(
    latitude: Double?,
    longitude: Double?,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    onTap: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val currentOnTap by rememberUpdatedState(onTap)
    val mapView = remember {
        configureOsmdroid(context)
        MapView(context).apply {
            setTileSource(MapTilerTileSource(context.getString(R.string.maptiler_key)))
            setMultiTouchControls(interactive)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            val pin = if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null
            controller.setZoom(if (pin != null) 13.0 else 6.0)
            controller.setCenter(pin ?: ALBERTA)
        }
    }
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Box(modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                map.overlays.clear()
                map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        currentOnTap?.invoke(p.latitude, p.longitude)
                        return currentOnTap != null
                    }

                    override fun longPressHelper(p: GeoPoint): Boolean = false
                }))
                if (latitude != null && longitude != null) {
                    map.overlays.add(Marker(map).apply {
                        position = GeoPoint(latitude, longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setInfoWindow(null)
                    })
                }
                map.invalidate()
            }
        )
        if (!interactive) {
            // Swallows taps so the preview stays still; vertical drags still scroll the page.
            Box(
                Modifier
                    .matchParentSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            )
        }
        // MapTiler's terms and OpenStreetMap's licence require this credit on the map.
        Text(
            stringResource(R.string.campsite_map_attribution),
            style = MaterialTheme.typography.labelSmall,
            color = Color.DarkGray,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color.White.copy(alpha = 0.7f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

/**
 * Full-screen map: tap to drop or move the pin, then confirm.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param initialLatitude the current pin, or null if none.
 * @param initialLongitude the current pin, or null if none.
 * @param onConfirm called with the chosen latitude and longitude.
 * @param onDismiss called when the owner backs out without choosing.
 */
@Composable
fun MapPinPickerDialog(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onConfirm: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var lat by rememberSaveable { mutableStateOf(initialLatitude) }
    var lng by rememberSaveable { mutableStateOf(initialLongitude) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                FormHeader(stringResource(R.string.campsite_map_pick_title), onDismiss)
                Text(
                    lat?.let { a -> lng?.let { b -> formatCoordinates(a, b) } }
                        ?: stringResource(R.string.campsite_map_pick_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                CampsiteMap(
                    latitude = lat,
                    longitude = lng,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag(MapTags.PICKER_MAP),
                    onTap = { a, b -> lat = a; lng = b }
                )
                Button(
                    onClick = { onConfirm(lat!!, lng!!) },
                    enabled = lat != null && lng != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .height(48.dp)
                        .testTag(MapTags.CONFIRM)
                ) { Text(stringResource(R.string.campsite_map_confirm)) }
            }
        }
    }
}

/**
 * Test tags for the map and picker.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
object MapTags {
    const val PICKER_MAP = "map_picker_map"
    const val CONFIRM = "map_confirm"
}
