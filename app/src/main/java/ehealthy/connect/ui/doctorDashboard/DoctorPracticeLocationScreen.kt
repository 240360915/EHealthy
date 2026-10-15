package ehealthy.connect.ui.doctorDashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale

@Serializable
private data class EditablePracticeLocation(
    val location_name: String? = null,
    val location_address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/** The same stable clinic map pin and doctor_save_practice_location RPC as the web portal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorPracticeLocationScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var selectionHint by remember { mutableStateOf<String?>(null) }
    // Increment only for GPS/search. Manual drags do not recenter the map.
    var mapMoveRevision by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        try {
            val saved = SupabaseClientProvider.client.postgrest
                .rpc("doctor_get_my_editable_practice_location")
                .decodeList<EditablePracticeLocation>().firstOrNull()
            name = saved?.location_name.orEmpty()
            address = saved?.location_address.orEmpty()
            latitude = saved?.latitude
            longitude = saved?.longitude
        } catch (e: Exception) {
            message = e.message ?: "Could not load your saved practice location."
        } finally { loading = false }
    }

    val colors = MaterialTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    var saved by remember { mutableStateOf(false) }

    suspend fun selectCurrentLocation() {
        locating = true
        message = null
        try {
            val point = getDoctorCurrentLocation(context)
            if (point == null) {
                message = "Unable to get a fresh device location. Enable Location services, " +
                    "try again outdoors, or search for the clinic and set a pin manually."
            } else {
                latitude = point.latitude
                longitude = point.longitude
                // Reverse geocoding is optional: don't prevent pin placement if unavailable.
                val nearbyAddress = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    runCatching {
                        Geocoder(context, Locale.getDefault())
                            .getFromLocation(point.latitude, point.longitude, 1)
                            ?.firstOrNull()?.getAddressLine(0)
                    }.getOrNull()
                }
                if (address.isBlank() && !nearbyAddress.isNullOrBlank()) {
                    address = nearbyAddress
                }
                mapMoveRevision++
                saved = false
                selectionHint = "Current position selected as a draft pin. " +
                    "Move it to your actual practice entrance and tap Save practice location."
            }
        } catch (error: Exception) {
            message = error.message ?: "Location is unavailable. Search or place the pin manually."
        } finally {
            locating = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            scope.launch { selectCurrentLocation() }
        } else {
            message = "Location permission was declined. You can still search or tap the map."
        }
    }

    val ready = !loading && !saving && name.trim().length >= 2 &&
        address.trim().length >= 3 && latitude != null && longitude != null

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Practice location", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("Clinic address & map pin", style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back")
                    }
                }
            )
        }
    ) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets)
                .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(2.dp))
            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(17.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(androidx.compose.material.icons.Icons.Outlined.LocationOn,
                        contentDescription = null, tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(33.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Help patients find your practice",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = colors.onPrimaryContainer)
                        Spacer(Modifier.height(4.dp))
                        Text("Choose the exact clinic entrance on the map. This saved pin appears to patients after you accept a follow-up.",
                            style = MaterialTheme.typography.bodySmall, color = colors.onPrimaryContainer)
                    }
                }
            }

            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Practice details", style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("Shown beside the map and copied into new physical visit invoices.",
                        color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = name, onValueChange = { name = it; saved = false; message = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Practice or location name") }, singleLine = true,
                        leadingIcon = { Icon(androidx.compose.material.icons.Icons.Outlined.LocationOn,
                            contentDescription = null) },
                        supportingText = { Text("For example, Riverside Medical Centre") }
                    )
                    OutlinedTextField(
                        value = address, onValueChange = { address = it; saved = false; message = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Street, suburb and city") },
                        minLines = 2, maxLines = 3,
                        supportingText = { Text("Use the physical address of the pinned building.") }
                    )
                }
            }

            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 17.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Outlined.Map,
                            contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.width(9.dp))
                        Text("Your practice on the map",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                    Text("Use GPS, search for a place, or tap and drag the pin. " +
                        "Only the saved practice pin is shared with patients.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it; message = null },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        label = { Text("Search clinic, street or suburb") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            enabled = !loading && !searching && searchText.trim().length >= 3,
                            onClick = {
                                val query = searchText.trim()
                                scope.launch {
                                    searching = true
                                    message = null
                                    try {
                                        val place = withContext(Dispatchers.IO) {
                                            @Suppress("DEPRECATION")
                                            Geocoder(context, Locale.getDefault())
                                                .getFromLocationName(query, 1)
                                                ?.firstOrNull()
                                        }
                                        if (place == null) {
                                            message = "Place not found. Try a street and city, or tap the map."
                                        } else {
                                            latitude = place.latitude
                                            longitude = place.longitude
                                            address = place.getAddressLine(0)
                                                ?.takeIf { it.isNotBlank() } ?: query
                                            mapMoveRevision++
                                            saved = false
                                            selectionHint = "Search result pinned. " +
                                                "Confirm the entrance, enter a practice name, then save."
                                        }
                                    } catch (error: Exception) {
                                        message = "Place search is unavailable on this device. " +
                                            "Try again or select the pin manually."
                                    } finally {
                                        searching = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            if (searching) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else Icon(Icons.Outlined.Search, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("Search")
                        }
                        OutlinedButton(
                            enabled = !loading && !locating,
                            onClick = {
                                val granted = listOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ).any { permission ->
                                    ContextCompat.checkSelfPermission(context, permission) ==
                                        PackageManager.PERMISSION_GRANTED
                                }
                                if (granted) {
                                    scope.launch { selectCurrentLocation() }
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            if (locating) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else Icon(Icons.Outlined.MyLocation, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("Use GPS")
                        }
                    }
                    if (loading) {
                        Box(Modifier.fillMaxWidth().height(300.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))) {
                            PracticePinPicker(
                                latitude = latitude,
                                longitude = longitude,
                                moveRevision = mapMoveRevision
                            ) { lat, lng ->
                                latitude = lat
                                longitude = lng
                                saved = false
                                message = null
                                selectionHint = "Draft pin selected. Check the address and save to share it."
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(
                            if (latitude != null && longitude != null)
                                androidx.compose.material.icons.Icons.Outlined.CheckCircle
                            else androidx.compose.material.icons.Icons.Outlined.Info,
                            contentDescription = null, tint = colors.primary,
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(if (latitude != null && longitude != null) "Practice pin selected"
                                else "Choose a location on the map",
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium)
                            if (latitude != null && longitude != null) {
                                Text(String.format(Locale.US, "%.5f, %.5f", latitude, longitude),
                                    color = colors.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (latitude != null && longitude != null) {
                        TextButton(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            onClick = {
                                val url = android.net.Uri.parse(
                                    "https://www.openstreetmap.org/?mlat=$latitude&mlon=$longitude#map=17/$latitude/$longitude"
                                )
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, url))
                            }
                        ) { Text("Open full map preview") }
                    }
                    Spacer(Modifier.height(3.dp))
                }
            }

            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(androidx.compose.material.icons.Icons.Outlined.Info,
                        contentDescription = null, tint = colors.onSurfaceVariant)
                    Spacer(Modifier.width(10.dp))
                    Text("This is a fixed practice pin, not live tracking. Already-issued invoices keep their original location.",
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            selectionHint?.let {
                Text(it, color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }
            message?.let {
                Text(it, color = if (saved) colors.primary else colors.error,
                    style = MaterialTheme.typography.bodySmall)
            }
            Button(
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                enabled = ready,
                onClick = {
                    saving = true
                    scope.launch {
                        try {
                            SupabaseClientProvider.client.postgrest.rpc(
                                "doctor_save_practice_location",
                                parameters = buildJsonObject {
                                    put("p_location_name", name.trim())
                                    put("p_address", address.trim())
                                    put("p_latitude", requireNotNull(latitude))
                                    put("p_longitude", requireNotNull(longitude))
                                }
                            )
                            saved = true
                            message = "Practice location saved successfully."
                        } catch (e: Exception) {
                            saved = false
                            message = e.message ?: "Could not save practice location."
                        } finally { saving = false }
                    }
                }
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(18.dp),
                        color = colors.onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (saving) "Saving location…" else "Save practice location")
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PracticePinPicker(
    latitude: Double?,
    longitude: Double?,
    moveRevision: Int,
    onPick: (Double, Double) -> Unit
) {
    val externalSelection = rememberUpdatedState(
        if (moveRevision > 0 && latitude != null && longitude != null)
            Pair(latitude, longitude) else null
    )
    // Default map center is NOT a stored doctor pin.
    val startLat = latitude ?: -25.47
    val startLng = longitude ?: 30.98
    val initialMarker = if (latitude != null && longitude != null)
        "placePin($latitude,$longitude,false);" else ""
    // Preserve pan/zoom while the doctor selects a pin; do not reload the HTML on each tap.
    val html = remember {
        """
        <!DOCTYPE html><html><head><meta name="viewport"
        content="width=device-width,initial-scale=1">
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
        <style>
          html,body,#map{height:100%;width:100%;margin:0;background:#edf3f6}
          #tip{position:absolute;top:12px;left:12px;z-index:1000;
            background:#fff;color:#0f2545;border-radius:10px;padding:8px 10px;
            font:600 12px Arial;box-shadow:0 2px 12px #0002}
        </style></head>
        <body><div id="map"></div><div id="tip">Tap the map to set your practice pin</div>
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <script>
          const map=L.map('map').setView([$startLat,$startLng],13);
          L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
            {attribution:'© OpenStreetMap contributors',maxZoom:19}).addTo(map);
          let marker;
          function placePin(lat,lng,notify) {
            if(marker) marker.setLatLng([lat,lng]);
            else marker=L.marker([lat,lng],{draggable:true}).addTo(map);
            if(notify) {
              document.getElementById('tip').innerText='Practice pin selected';
              location.href='ehealthy-pin://pick?lat='+lat+'&lng='+lng;
            }
            marker.off('dragend');
            marker.on('dragend',function(e){
              const p=e.target.getLatLng();placePin(p.lat,p.lng,true);
            });
          }
          $initialMarker
          window.focusPracticePin=function(lat,lng) {
            if(!Number.isFinite(lat) || !Number.isFinite(lng)) return;
            placePin(lat,lng,false);
            map.setView([lat,lng], Math.max(map.getZoom(),16));
          };
          map.on('click',function(e){placePin(e.latlng.lat,e.latlng.lng,true)});
          setTimeout(function(){map.invalidateSize()},350);
        </script></body></html>
        """.trimIndent()
    }
    AndroidView(factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    externalSelection.value?.let { (lat, lng) ->
                        view?.evaluateJavascript(
                            "window.focusPracticePin && window.focusPracticePin($lat,$lng);",
                            null
                        )
                    }
                }
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri: Uri = request?.url ?: return false
                    if (uri.scheme != "ehealthy-pin") return false
                    val lat = uri.getQueryParameter("lat")?.toDoubleOrNull()
                    val lng = uri.getQueryParameter("lng")?.toDoubleOrNull()
                    if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                        onPick(lat, lng)
                    }
                    return true
                }
            }
            loadDataWithBaseURL("https://appassets.androidplatform.net/",html,"text/html","UTF-8",null)
        }
    }, update = { webView ->
        val point = externalSelection.value
        if (point != null && webView.tag != moveRevision) {
            webView.tag = moveRevision
            webView.evaluateJavascript(
                "window.focusPracticePin && window.focusPracticePin(${point.first},${point.second});",
                null
            )
        }
    }, modifier = Modifier.fillMaxWidth().height(310.dp))
}

/** GPS is opt-in. No background tracking, and no position is saved until Save is tapped. */
@SuppressLint("MissingPermission")
private suspend fun getDoctorCurrentLocation(context: Context): Location? =
    withTimeoutOrNull(12_000L) {
        suspendCancellableCoroutine { continuation ->
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val hasFine = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val provider = (if (hasFine)
                listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            else
                listOf(LocationManager.NETWORK_PROVIDER))
                .firstOrNull { manager.isProviderEnabled(it) }

            if (provider == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                try {
                    manager.getCurrentLocation(
                        provider, signal, context.mainExecutor
                    ) { position ->
                        if (continuation.isActive) continuation.resume(position)
                    }
                } catch (_: Exception) {
                    if (continuation.isActive) continuation.resume(null)
                }
            } else {
                @Suppress("DEPRECATION")
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(location)
                    }
                }
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                try {
                    @Suppress("DEPRECATION")
                    manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                } catch (_: Exception) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
    }

