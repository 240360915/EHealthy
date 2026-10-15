package ehealthy.connect.ui.doctorDashboard

import android.net.Uri
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
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
                    Text("Pan and zoom, then tap or drag the marker to the right spot.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                    if (loading) {
                        Box(Modifier.fillMaxWidth().height(300.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))) {
                            PracticePinPicker(latitude, longitude) { lat, lng ->
                                latitude = lat
                                longitude = lng
                                saved = false
                                message = null
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
    onPick: (Double, Double) -> Unit
) {
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
    }, modifier = Modifier.fillMaxWidth().height(310.dp))
}
